package client;


import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

public class WorkWindowController2 {
    ObservableList list=FXCollections.observableArrayList();
    private SocketChannel sock;
    private ClientController controller;
    public  void setController(ClientController controller){
        this.controller=controller;
    }


    @FXML
    public ListView<String> listView;
    @FXML
    public TextField screen;
    private String message;
    private SelectionKey key;

    //загружает содержимое пользовательской папки, предварительно очищается на случай вызова при апдейте
    public void loadList(ArrayList<String> initlist){
        list.removeAll(list);
        listView.getItems().clear();

        String a= "ddfg";
        String b="sdsff";

        list.addAll(initlist);

        listView.getItems().addAll(list);
    }



        //отображает выбранный файл
    public void displaySelected(javafx.scene.input.MouseEvent mouseEvent) throws IOException {
        screen.clear();
        String line = listView.getSelectionModel().getSelectedItem();
        if (line == null || line.isEmpty()) {
            screen.setText("Nothing selected");
        } else {

            screen.setText(line);

        }
        //rw();
    }

   public void rw() throws IOException {
       ByteBuffer buffer = ByteBuffer.allocate(600000);
       Selector selector = Selector.open();
       SocketChannel channel = SocketChannel.open();
       channel.configureBlocking(false);
       channel.register(selector, SelectionKey.OP_CONNECT);
       channel.connect(new InetSocketAddress("localhost", 5678));

        String message = getMessage();
        boolean exit = false;
        int totalKey = 0;
        Iterator<SelectionKey> iter = null;
        SelectionKey key;
       System.out.println("START RW");
        for (; ; ) {
            totalKey = selector.select();

            if (exit) {
                selector.close();
                return;
            }

            if (totalKey > 0) {
                iter = selector.selectedKeys().iterator();


                while (iter.hasNext()) {
                    key = iter.next();
                    iter.remove();

                    if (key.isValid()) {
                        if(key.isConnectable())
                    {
                        SocketChannel sock =(SocketChannel) key.channel();
                        sock.finishConnect();

                        if(sock.isConnected())
                        {
                            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                        }

                        continue;
                    }


                        if (key.isReadable()) {
                            System.out.println("Интерес на чтение");
                            channel = ((SocketChannel) key.channel());
                           // SocketAddress client = sock.getRemoteAddress();
                            int readBytes = channel.read(buffer);
                            if (readBytes > 0) {
                                System.out.println("Читаем !");

                                buffer.flip();

                                StringBuilder sb = new StringBuilder();
                                while (buffer.hasRemaining()) {
                                    sb.append((char) buffer.get());
                                }

                                buffer.clear();

                                System.out.println(sb);
                                String command = sb
                                        .toString();

                                if (command.startsWith("DEL_OK")) {

                                    System.out.println("Удаление прошло успешно\n");
                                    String[] cmd = command.split(" ", 0);
                                    System.out.println(cmd.toString());
                                    ArrayList<String> initlist = new ArrayList<String>();
                                    for (int i = 1; i < cmd.length; i++) {
                                        initlist.add(cmd[i]);
                                    }

                                    loadList(initlist);

                                }  if (command.startsWith("INFO_OK")) {

                                    System.out.println("Запрос инфо прошел успешно\n");
                                    String[] cmd = command.split(" ", 0);
                                    String size=cmd[2];

                                   screen.setText("Размер файла: "+ cmd[1]+" = "+size +" байт");


                                }

                                return;
                            }
                            else {
                                System.out.println("Клиент принудительно закрыл соединение! result <= 0");

                                key.channel().close();
                                key.cancel();
                            }
                        }

                        if (key.isWritable()) {
                            try {
                                TimeUnit.SECONDS.sleep(1);
                                System.out.println("Пишем...!");


                                channel = ((SocketChannel) key.channel());

                                channel.write(ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8)));


                                key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
                                System.out.println("Закончили писать !");
                                key.interestOps(key.interestOps() | SelectionKey.OP_READ);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } finally {
                                System.out.println("Почистим буффер !");
                                buffer.clear();
                            }
                        }
                    }
                }

            }

        }

    }
   

   
           
   




        

    //удаляет выбранный файл
    public void delete(ActionEvent actionEvent) throws IOException, InterruptedException {

         String message= (String) "DELETE "+screen.getText();
         setMessage(message);
         rw();
        screen.clear();
    }

    //выдает информацию о файле(размер, дата создания)
    public void info(ActionEvent actionEvent) throws IOException, InterruptedException {


        String message= (String) "INFO "+screen.getText();
        setMessage(message);
        rw();

    }

    private void setMessage(String message) {this.message = message;}

    private String getMessage() {return message;}



}






