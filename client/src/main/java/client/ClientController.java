package client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

public class ClientController implements Runnable {

    @FXML
    public TextField loginField;
    @FXML
    public PasswordField passwordField;
    @FXML
    public Label tipLabel;
    @FXML
    public Button regButton;

    private WorkWindowController2 workWindowController2;
    private RegController regController;
    private Stage regStage;
    private Stage workStage;
    public SocketChannel sock = null;
    private String login=null;
    private String password=null;
    Selector selector = null;
    private String message;
    private Path path;
    private SelectionKey key;
    ArrayList<String> initlist;



    public void tryToAuth(ActionEvent actionEvent) {
         login = loginField.getText().trim();
         password = passwordField.getText().trim();
        if (login.length() * password.length() == 0) {
            return;
        }

        if ( sock== null || !sock.isConnected()) {

            run();
        }

    }



    @Override
    public void run() {
        Selector selector = null;
        ByteBuffer buffer =null;

        try {
            selector = Selector.open();
             buffer = ByteBuffer.allocate(32000);
             sock = SocketChannel.open();
            sock.configureBlocking(false);
            sock.connect(new InetSocketAddress("localhost", 5678));
            sock.register(selector, SelectionKey.OP_CONNECT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(login+password);
       // String message=null;
      //  String message = (String.format("%s %s %s", "AUTH", login, password));
        boolean exit = false;
        int totalKey = 0;
        Iterator<SelectionKey> iter = null;
       // SelectionKey key;

        System.out.println("START Client");

        for(;;)
        {
            try {
                totalKey = selector.select();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if(exit)
            {
                try {
                    selector.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
//                        if(key.isAcceptable())
//                        {
//                            //ignore
//                        }
                         if(key.isConnectable())
                        {
                            SocketChannel sock =(SocketChannel) key.channel();
                            try {
                                sock.finishConnect();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            if(sock.isConnected())
                            {        message = (String.format("%s %s %s", "AUTH", login, password));
                                key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                            }

                            continue;
                        }

                        if(key.isReadable())
                        {
                            System.out.println("Интерес на чтение");
                            SocketChannel channel = ((SocketChannel) key.channel());
                            try {
                                SocketAddress client = channel.getRemoteAddress();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

//                            ReadableByteChannel rc = (ReadableByteChannel) key.channel();

                            int readBytes = 0;
                            try {
                                 readBytes = channel.read(buffer);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            if(readBytes > 0)
                            {
                                System.out.println("Читаем !");

                                buffer.flip();

                                StringBuilder builder = new StringBuilder();

                                while(buffer.hasRemaining())
                                {
                                    builder.append((char) buffer.get());
                                }
                                buffer.clear();

                                System.out.println(builder);
                                String command = builder
                                        .toString();

                                if (command.startsWith("AUTH_OK")) {
                                    String[] cmd= command.split(" ",0);
                                    System.out.println(cmd.toString());
                                   initlist =new ArrayList<String>();
                                    for (int i = 1; i < cmd.length; i++) {
                                        initlist.add(cmd[i]);
                                    }
                                    for (String number : initlist)
                                        System.out.println("Number = " + number);
                                    System.out.println("Аутентификация прошла успешно\n");

                                    initWorkWindow2();
                                    loginField.clear();
                                    passwordField.clear();




                                }
                                if (command.equals("AUTH_NO")) {
                                    tipLabel.setText("Логин или пароль не верные \n");
                                    passwordField.clear();
                                    loginField.clear();
                                    try {  key.channel().close();
                                        System.out.println("Закрываем соеденение!");
                                        key.cancel();
                                        selector.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }


                                }

//
//                                try {  key.channel().close();
//                                    System.out.println("Закрываем соеденение!");
//                                    key.cancel();
//                                    selector.close();
//                                } catch (IOException e) {
//                                    e.printStackTrace();
//                                }

                                return;
                            }
                            else
                            {
                                System.out.println("Клиент принудительно закрыл соединение! result <= 0");

                                try {
                                    key.channel().close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                key.cancel();
                            }
                        }

                        if(key.isWritable())
                        {
                            try
                            {
                                TimeUnit.SECONDS.sleep(1);
                                System.out.println("Пишем...!");
                                SocketChannel channel = ((SocketChannel) key.channel());

                                System.out.println("message:"+message);
                                channel.write(ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8)));

                                key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
                                System.out.println("Закончили писать !");
                                key.interestOps(key.interestOps() | SelectionKey.OP_READ);
                            } catch (InterruptedException | IOException e) {
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


    }





    //запуск рабочего окна, оно пока состоит из 2 кнопок up&download
    //todo придумать как сделать нормально просмотр файлов папки хранилища

    //запуск окна работы
    private void initWorkWindow2() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/workWindow2.fxml"));
            Parent root = fxmlLoader.load();

            workWindowController2 = fxmlLoader.getController();
            workWindowController2.setController(this);
            workWindowController2.loadList(initlist);
            workStage = new Stage();
            workStage.setTitle("MaxCloud_V1");


            workStage.setScene(new Scene( root, 700, 700));
            workStage.initStyle(StageStyle.UTILITY);

            workStage.initModality(Modality.APPLICATION_MODAL);
            workStage.show();


        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void setMessage(String message){
        this.message=message;
    }
    // передача файла работает при указании пути вручную. запуталась на моменте как передать путь полученный
    // в workwindowcontroller,чтобы файл отправлялся автоматически при нажатии кнопки upload
    public void sendFile(SocketChannel client) throws IOException {
        //11,4MB
        //String fName = "C:\\тест\\Java руководство для начинающих ( PDFDrive ).pdf";
     //  Path filePath = workWindowController.getPath();
       // Path fileName = filePath.getFileName();
        int bufSize = 10240;
        int counter=0;
       // Path path = Paths.get(fName);
        System.out.println("отправим файл"+path);
        FileChannel fileChannel = FileChannel.open(path);
        ByteBuffer buf = ByteBuffer.allocate(bufSize);
        do{
            int noOfBytesRead = fileChannel.read(buf);
            if(noOfBytesRead<=0){
                break;
            }
            counter+=noOfBytesRead;
            buf.flip();
            do{noOfBytesRead-=client.write(buf);
            } while (noOfBytesRead>0);
            buf.clear();

        } while (true);
        fileChannel.close();
        System.out.println("Reciever " + counter);

    }
        //не готово
    public void uploadFile(ActionEvent actionEvent) throws IOException, InterruptedException {

        FileChooser fileChooser = new FileChooser();

        File selectedFile = fileChooser.showOpenDialog(workStage);
         path= Paths.get(selectedFile.getAbsolutePath());
        Path fileName = path.getFileName();
        System.out.println(path);
        System.out.println(fileName);

        message = (String.format("%s %s", "UPLOAD", fileName));
        ((SocketChannel)key.channel())
                .write(ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8)));
        sendFile(sock);
//

   }
    //ниже все для работы окна регистрации
    public void closeRegWindow() {
        regStage.close();
        tipLabel.setText("Вы зарегистрированы, введите логин и пароль для входа");
        regButton.setVisible(false);
    }

    public void showRegWindow(ActionEvent actionEvent) {
        if (regStage == null) {
            initRegWindow();
        }
        regStage.show();
    }

    private void initRegWindow() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/register.fxml"));
            Parent root = fxmlLoader.load();

            regController = fxmlLoader.getController();
            regController.setController(this);

            regStage = new Stage();
            regStage.setTitle("MaxCloud_V1 registration");
            regStage.setScene(new Scene(root, 500, 500));
            regStage.initStyle(StageStyle.UTILITY);
            regStage.initModality(Modality.APPLICATION_MODAL);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //не готово
    public void downloadFile(ActionEvent actionEvent) {
    }
}
