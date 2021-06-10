package client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

public class RegController {
    @FXML
    public TextField loginFieldReg;
    @FXML
    public PasswordField passwordFieldReg;

    @FXML
    public PasswordField passwordFieldCheck;
    @FXML
    public Label label;
    private ClientController controller;
    public  void setController(ClientController controller){
        this.controller=controller;
    }

//попытка регистрации
    public void tryToReg(ActionEvent actionEvent) throws IOException {
        String login = loginFieldReg.getText().trim();
        String password = passwordFieldReg.getText().trim();
        String passwordCheck =passwordFieldCheck.getText().trim();
        if (login.length() * password.length() * passwordCheck.length() == 0) {
            return;
        }

        if (password.equals(passwordCheck)) {
            ByteBuffer buffer = ByteBuffer.allocate(600000);
            Selector selector = Selector.open();
            SocketChannel channel = SocketChannel.open();
            channel.configureBlocking(false);
            channel.register(selector, SelectionKey.OP_CONNECT);
            channel.connect(new InetSocketAddress("localhost", 5678));


            String message = (String.format("%s %s %s", "reg", login, password));
            boolean exit = false;
            int totalKey = 0;
            Iterator<SelectionKey> iter = null;
            SelectionKey key;

            System.out.println("START REGClient");
            for(;;)
            {
                totalKey = selector.select();

                if(exit)
                {
                    selector.close();
                    return;
                }

                if(totalKey > 0)
                {
                    iter = selector.selectedKeys().iterator();


                    while(iter.hasNext())
                    {
                        key = iter.next();
                        iter.remove();

                        if(key.isValid())
                        {
                            if(key.isAcceptable())
                            {
                                //ignore
                            }
                            else if(key.isConnectable())
                            {
                                SocketChannel sock =(SocketChannel) key.channel();
                                sock.finishConnect();

                                if(sock.isConnected())
                                {
                                    key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                                }

                                continue;
                            }

                            if(key.isReadable())
                            {
                                System.out.println("Интерес на чтение");


                                 channel = ((SocketChannel) key.channel());
                                SocketAddress client = channel.getRemoteAddress();
                                int readBytes = channel.read(buffer);
                                if (readBytes > 0) {
                                    System.out.println("Читаем !");

                                buffer.flip();

                                StringBuilder sb = new StringBuilder();
                                while (buffer.hasRemaining()) {
                                    sb.append( (char) buffer.get());
                                }

                                buffer.clear();



                                    System.out.println(sb);
                                    String command = sb
                                            .toString();

                                    if (command.equals("reg_OK")) {
                                        System.out.println("Регистрация прошла успешно\n");
                                        loginFieldReg.clear();
                                        passwordFieldReg.clear();
                                        passwordFieldCheck.clear();
                                        controller.closeRegWindow();
                                    }
                                    if (command.equals("reg_NO")) {
                                        label.setText("Логин уже существует\n");
                                        passwordFieldReg.clear();
                                        passwordFieldCheck.clear();
                                        loginFieldReg.clear();
                                    }

                                    System.out.println("Закрываем соеденение!");
                                    key.channel().close();
                                    key.cancel();
                                    selector.close();
                                    return;
                                }
                                else
                                {
                                    System.out.println("Клиент принудительно закрыл соединение! result <= 0");

                                    key.channel().close();
                                    key.cancel();
                                }
                            }

                            if(key.isWritable())
                            {
                                try
                                {
                                    TimeUnit.SECONDS.sleep(3);
                                    System.out.println("Пишем...!");


                                    channel = ((SocketChannel) key.channel());

                                   channel.write(ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8)));


                                    key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
                                    System.out.println("Закончили писать !");
                                    key.interestOps(key.interestOps() | SelectionKey.OP_READ);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                } finally
                                {
                                    System.out.println("Почистим буффер !");
                                    buffer.clear();
                                }
                            }
                        }
                    }

                }

            }


        } else {
            label.setText("Пароли не совпадают");
            passwordFieldReg.clear();
            passwordFieldCheck.clear();
            loginFieldReg.clear();
        }

    }



}

