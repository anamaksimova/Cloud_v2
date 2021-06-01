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
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

public class ClientController implements Runnable {

    @FXML
    public TextField loginField;
    @FXML
    public PasswordField passwordField;
    @FXML
    public Label tipLabel;
    @FXML
    public Button regButton;
    private WorkWindowController workWindowController;
    private RegController regController;
    private Stage regStage;
    private Stage workStage;
    SocketChannel clientChannel = null;


    public void tryToAuth(ActionEvent actionEvent) {
        if ( clientChannel== null || !clientChannel.isConnected()) {

            run();
        }
        passwordField.clear();
    }



    @Override
    public void run() {
        Selector selector = null;


        try {
            selector = Selector.open();
            SocketChannel clientChannel = SocketChannel.open();
            clientChannel.configureBlocking(false);
            clientChannel.connect(new InetSocketAddress("localhost", 5678));
            clientChannel.register(selector, SelectionKey.OP_CONNECT);
        } catch (IOException e) {
            e.printStackTrace();
        }


        while (true){
            try {
                selector.select();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();
                SocketChannel client = (SocketChannel) key.channel();
                if (key.isConnectable()) {
                    if (client.isConnectionPending()) {
                        System.out.println("Trying to finish connection");
                        try {
                            client.finishConnect();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                    try {
                        client.register(selector, SelectionKey.OP_WRITE);
                        initWorkWindow();
                        sendFile(client);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    continue;}

                //todo разобраться с пересылкой сообщений команд
                //отправить логин пароль и получиь ответ
                //зарегистировать если надо
                //отправить файл
                //принять файл

//                }  if(key.isReadable()){
//
//
//                }
                if(key.isWritable()){
                    try {
                        //sendFile(client); //todo должно работать с кнопки
                        client.close();
                        return;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                }
            }


        }
    }
    //запуск рабочего окна, оно пока состоит из 2 кнопок up&download
    //todo придумать как сделать нормально просмотр файлов папки хранилища
    private void initWorkWindow() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/workWindow.fxml"));
            Parent root = fxmlLoader.load();

            workWindowController = fxmlLoader.getController();
            workWindowController.setController(this);

            workStage = new Stage();
            workStage.setTitle("MaxCloud_V1");


            workStage.setScene(new Scene( root, 500, 500));
            workStage.initStyle(StageStyle.UTILITY);

            workStage.initModality(Modality.APPLICATION_MODAL);
            workStage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // передача файла работает при указании пути вручную. запуталась на моменте как передать путь полученный
    // в workwindowcontroller,чтобы файл отправлялся автоматически при нажатии кнопки upload
    public void sendFile(SocketChannel client) throws IOException {
        //11,4MB
        String fName = "C:\\тест\\Java руководство для начинающих ( PDFDrive ).pdf";

        int bufSize = 10240;
        int counter=0;
        Path path = Paths.get(fName);

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
    //todo
    //    public void registration(String login, String password) {}
}
