package client;


import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

public class WorkWindowController2 {
    ObservableList list=FXCollections.observableArrayList();

    private ClientController controller;
        private long fileSize;
    private String fileNameDOWN;
      private Path pathUp;
    private long sizeFileUP;


    public  void setController(ClientController controller){
        this.controller=controller;
    }
    private Stage workStage = new Stage();

    @FXML
    public ListView<String> listView;

    @FXML
    public TextField screen;
    @FXML
    public Label dirchoose;
    @FXML
    public Button OK;

    private String message;


    //загружает содержимое пользовательской папки, предварительно очищается на случай вызова при апдейте
    public void loadList(ArrayList<String> initlist){
        list.removeAll(list);
        listView.getItems().clear();
        list.addAll(initlist);
        listView.getItems().addAll(list);
    }

        //отображает выбранный файл
    public void displaySelected(javafx.scene.input.MouseEvent mouseEvent) throws IOException {
        screen.clear();
        dirchoose.setText("");
        String line = listView.getSelectionModel().getSelectedItem();
        if (line == null || line.isEmpty()) {
            screen.setText("Nothing selected");
        } else {
            screen.setText(line);
        }

    }
    //чтение/запись сообщений
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
                                //обработка сообщения об удалении файла
                                if (command.startsWith("DEL_OK")) {

                                    System.out.println("Удаление прошло успешно\n");
                                    String[] cmd = command.split(" ", 0);
                                    System.out.println(cmd.toString());
                                    ArrayList<String> initlist = new ArrayList<String>();
                                    for (int i = 1; i < cmd.length; i++) {
                                        initlist.add(cmd[i]);
                                    }

                                    loadList(initlist);

                                }
                                //обработка сообщения о выдаче информации по файлу
                                if (command.startsWith("INFO_OK")) {

                                    System.out.println("Запрос инфо прошел успешно\n");
                                    String[] cmd = command.split(" ", 4);
                                    String size=cmd[2];

                                    long sizeKB = Long.parseLong(size)/1024;

                                    String dateOfChange=cmd[3];

                                   screen.setText("Размер файла: "+ cmd[1]+" = "+sizeKB +" Кбайт. Дата последнего изменения: "+dateOfChange);

                                }
                                    //получение файла с сервера
                                if (command.startsWith("DOWN_OK")) {

                                    String[] cmd = command.split(" ", 3);
                                     fileNameDOWN=cmd[1];
                                    System.out.println(fileNameDOWN);
                                    fileSize= Integer.parseInt(cmd[2]);
                                    System.out.println(fileSize);
                                    recieveFile(channel);

                                }

                                    //обработка сообщения об успешной загрузке файла
                                if (command.startsWith("UP_OK")) {
                                    String[] cmd = command.split(" ", 0);
                                    System.out.println("Файл загружен успешно\n");

                                    ArrayList<String> initlist = new ArrayList<String>();
                                    for (int i = 1; i < cmd.length; i++) {
                                        initlist.add(cmd[i]);
                                    }
                                    loadList(initlist);

                                    screen.setText("Файл загружен успешно");
                                    key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                                 }
                                //обработка сообщения об успешном создании диретории
                                if (command.startsWith("CREATEDIR_OK")) {
                                    String[] cmd = command.split(" ", 0);
                                    System.out.println("Директория создана\n");

                                    ArrayList<String> initlist = new ArrayList<String>();
                                    for (int i = 1; i < cmd.length; i++) {
                                        initlist.add(cmd[i]);
                                    }
                                    loadList(initlist);
                                    screen.clear();

                                    dirchoose.setText("Директория создана");
                                    OK.setVisible(false);
                                    key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
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

                                  if (message.startsWith("UP")) { sendFile(channel);}



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
    //установка сообщения на отправку
    private void setMessage(String message) {this.message = message;}

    private String getMessage() {return message;}

    //скачивает файл с сервера на клиент
    public void download(ActionEvent actionEvent) throws IOException {

        String message= (String) "DOWN "+screen.getText();
        setMessage(message);
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File dirToSaveFile = directoryChooser.showDialog(workStage);
        Path path= Paths.get(dirToSaveFile.getAbsolutePath());
        dirchoose.setText(String.valueOf(path));
        rw();

    }
    //получает файл с сервера на клиент
    private  void recieveFile(SocketChannel channel) throws IOException {
        System.out.println("Начнем принимать файл");
        String fileName=fileNameDOWN;
        String pathClient = dirchoose.getText();
        Path outputFile = Paths.get(pathClient + "\\"+fileName);
        int bufSize = 10240;

        FileChannel fileChannel = FileChannel.open(outputFile,
                EnumSet.of(StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE)
        );
        ByteBuffer buffer = ByteBuffer.allocate(bufSize);

        int counter=0;

        int res;

        do {
            buffer.clear();
            res = channel.read(buffer);
            System.out.println(res);
            buffer.flip();
            if (res > 0 && counter< fileSize) {
                fileChannel.write(buffer);
                counter += res;
            } else if (res==0 && counter==fileSize) {break;}
        } while (res>=0);

        fileChannel.close();
        System.out.println("Reciever: " + counter);
        dirchoose.setText("Файл загружен в выбранную директорию");
    }
        //загрузка файла с клиента на сервер
    public void upload(ActionEvent actionEvent) throws IOException {
        FileChooser fileChooser = new FileChooser();
        File fileToUpload = fileChooser.showOpenDialog(workStage);
         pathUp= Paths.get(String.valueOf(fileToUpload));
        System.out.println(pathUp);
        String filenameUp= String.valueOf(pathUp.getFileName());
        dirchoose.setText(filenameUp); //сократить удалить
          sizeFileUP= Files.size(pathUp);
        String message = (String) "UP "+filenameUp +" "+sizeFileUP;
        setMessage(message);
        rw();
    }
    //отправка файла
    private void sendFile(SocketChannel channel) throws IOException {

         String filename = dirchoose.getText();
        Path path=Paths.get(filename);

        System.out.println(pathUp);
    int bufSize = 10240;
    int counter = 0;

    System.out.println("отправим файл to server" + path);

    FileChannel fileChannel = FileChannel.open(pathUp);
        ByteBuffer buf = ByteBuffer.allocate(bufSize);
        System.out.println("file channel");
        do {
        int noOfBytesRead =  fileChannel.read(buf);
            System.out.println(noOfBytesRead);

        if (noOfBytesRead <= 0 && counter== sizeFileUP) {
            break;
        }
        counter += noOfBytesRead;
        buf.flip();
        do {
                noOfBytesRead -= channel.write(buf);
        } while (noOfBytesRead > 0);
        buf.clear();
        }
        while (true);

        fileChannel.close();
        //Thread.sleep(10);
     System.out.println("Reciever " + counter);
    }


    public void createDir(ActionEvent actionEvent) {
        screen.clear();
        dirchoose.setText("Введите имя создаваемой директории и нажмите ОК");
        OK.setVisible(true);
    }

    public void OK(ActionEvent actionEvent) throws IOException {


        System.out.println(screen.getText());
        setMessage("CREATEDIR "+ screen.getText());
        rw();
    }
}






