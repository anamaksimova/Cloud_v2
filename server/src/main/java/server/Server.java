package server;

import jdk.nashorn.internal.objects.NativeString;

import java.io.File;
import java.io.FileOutputStream;
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
import java.nio.file.attribute.FileTime;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class Server {

    private  String filenameUP;
    private  int fileSize;
    int port = 5678;
    Path pathClient = null;
    public AuthService authService;
    private String message;
    private String login;

    public Server() throws IOException {
          ByteBuffer buffer = ByteBuffer.allocate(600000);
        Selector selector = Selector.open();
        ServerSocketChannel server = ServerSocketChannel.open();
        server.configureBlocking(false);
        server.bind(new InetSocketAddress(port));
        server.register(selector, server.validOps());
        boolean exit = false;
        int totalKey = 0;
        Iterator<SelectionKey> iter = null;
        SelectionKey key;

        System.out.println("START Server");
        for(;;)
        {
            totalKey = selector.select();

            if(exit)
            {
                server.close();
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
                            SocketChannel sock = server.accept();
                            sock.configureBlocking(false);
                            sock.register(selector, SelectionKey.OP_READ);
                            continue;
                        }
                        else if(key.isConnectable())
                        {
                            SocketChannel sock =(SocketChannel) key.channel();
                            sock.finishConnect();
                            continue;
                        }

                        if(key.isReadable())
                        {
                            System.out.println("Интерес на чтение");
                            SocketChannel sock =(SocketChannel) key.channel();
                            int readBytes = sock.read(buffer);
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

                                //регистарция
                                if (command.startsWith("reg")){
                                    if (!SQL_Handler.connect()) {
                                    RuntimeException e = new RuntimeException("Не удалось подключиться к БД");
                                    throw e;
                                    }
                                    String[] cmd= command.split(" ",3);
                                    authService = new DBAuthServise();
                                    boolean regSuccess = authService.registration(cmd[1], cmd[2]);

                                    if (regSuccess){
                                        setMessage("reg_OK");
                                        System.out.println("Регистрация успешна!");
                                        String username = cmd[1];
                                        System.out.println("username:" + username);
                                        Path clientRoot = Files.createDirectory(Paths.get("root\\" + username));
                                        key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                                    } else {
                                        setMessage("reg_NO");
                                        System.out.println("Регистрация не успешна!");
                                        key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                                    }

                                }
                                //аутентификация
                                if (command.startsWith("AUTH")){
                                    if (!SQL_Handler.connect()) {
                                    RuntimeException e = new RuntimeException("Не удалось подключиться к БД");
                                    throw e;
                                    }
                                    String[] cmd= command.split(" ",3);
                                    authService = new DBAuthServise();
                                    boolean authSuccess = authService.authent(cmd[1], cmd[2]);
                                    setLogin(cmd[1]);
                                     pathClient = Paths.get("root\\"+cmd[1]);
                                    if (authSuccess && Files.exists(pathClient)){
                                        System.out.println("Пользователь найден, можно работать: " + pathClient);
                                        setMessage("AUTH_OK"+" "+getFileList(cmd[1]));

                                        key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                                    }
                                    else {
                                        System.out.println("Ошибка,такого логина или пароля не существует");
                                        setMessage("AUTH_NO");
                                        key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                                    }

                                }

                                // команда на удаление файла
                                if (command.startsWith("DELETE")){
                                    String[] cmd= command.split(" ",2);
                                    String filename = "root\\"+login+"\\"+cmd[1];
                                    Path path = Paths.get(filename);
                                    System.out.println(path);
                                    if (Files.exists(path)){
                                        Files.delete(path);
                                        System.out.println("deleted");
                                        setMessage("DEL_OK "+getFileList(login));
                                        key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);


                                       System.out.println("File deleted: "+filename + sock.getRemoteAddress());
//
                                    }

                                }
                                //команда предоставление информации по размеру и дате изменения
                                if (command.startsWith("INFO")){
                                    String[] cmd= command.split(" ",2);
                                    String filename = "root\\"+login+"\\"+cmd[1];
                                    Path path = Paths.get(filename);
                                    System.out.println(path);
                                    if (Files.exists(path)){

                                       long sizeFile=Files.size(path);
                                        FileTime fileTime=Files.getLastModifiedTime(path);
                                        ZonedDateTime zonedDateTime = ZonedDateTime.parse(fileTime.toString());
                                        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy  HH:mm:ss");
                                        System.out.println(dtf.format(zonedDateTime));
                                        String dateOfChange = dtf.format(zonedDateTime);

                                        System.out.println("size:" + sizeFile);
                                        setMessage("INFO_OK "+cmd[1]+" "+sizeFile+" "+dateOfChange);
                                        key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);

                                    }

                                }
                                //создание диретории
                                if (command.startsWith("CREATEDIR")){
                                    String[] cmd= command.split(" ",2);

                                    String dirname = "root\\"+login+"\\"+cmd[1]; System.out.println(dirname);
                                   Files.createDirectory(Paths.get(dirname));

                                    setMessage("CREATEDIR_OK "+getFileList(login));
                                        key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);

                                }

                                //команда на скачивание файла с сервера
                                if (command.startsWith("DOWN")){
                                    String[] cmd= command.split(" ",2);
                                    String filename = "root\\"+login+"\\"+cmd[1];
                                    Path path = Paths.get(filename);
                                    System.out.println(path);
                                    long sizeFile=Files.size(path);
                                    if (Files.exists(path)){

                                        setMessage("DOWN_OK "+cmd[1] +" "+sizeFile);

                                        key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);

                                    }

                                }
                                //команда на загрузку файла на сервер
                                if (command.startsWith("UP")){
                                    String[] cmd= command.split(" ",3);
                                     filenameUP = "root\\"+login+"\\"+cmd[1];
                                    Path path = Paths.get(filenameUP);
                                    System.out.println(path);

                                    fileSize=0;
                                    fileSize= Integer.parseInt(cmd[2]);
                                    System.out.println(fileSize);

                                        Thread t2 = new Thread(() -> {

                                        try {
                                            recieveFile(sock);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        } try {
                                            Thread.sleep(10);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    });   t2.start();
                                        try {
                                            t2.join();
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }

                                        setMessage("UP_OK "+getFileList(login));

                                        key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                                }

                                buffer.flip();
                                System.out.println("Закончили чтение!");
                            }
                            else
                            {
                                System.out.println("Клиент принудительно закрыл соединение! result <= 0");
                                key.channel().close();
                                key.cancel();
                            }
                        }



                        if(key.isValid())
                            if(key.isWritable())
                            {
                                try {
                                    TimeUnit.SECONDS.sleep(1);
                                    System.out.println("Пишем...!");

                                    SocketChannel sock = ((SocketChannel) key.channel());

                                    Thread t1 = new Thread(() -> {
                                        String[] cmd = message.split(" ", 3);
                                        String filename = "root\\" + login + "\\" + cmd[1];
                                        Path path = Paths.get(filename);
                                        int bufSize = 10240;
                                        int counter = 0;

                                        System.out.println("отправим файл client" + path);
                                        FileChannel fileChannel = null;
                                        try {
                                            fileChannel = FileChannel.open(path);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        ByteBuffer buf = ByteBuffer.allocate(bufSize);
                                        System.out.println("file channel");
                                        do {
                                            int noOfBytesRead = 0;
                                            try {
                                                noOfBytesRead = fileChannel.read(buf);
                                                System.out.println(noOfBytesRead);
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                            if (noOfBytesRead <= 0) {
                                                break;
                                            }
                                            counter += noOfBytesRead;
                                            buf.flip();
                                            do {
                                                try {
                                                    noOfBytesRead -= sock.write(buf);
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }
                                            } while (noOfBytesRead > 0);
                                            buf.clear();

                                        } while (true);
                                        try {
                                            fileChannel.close();Thread.sleep(10);
                                        } catch (IOException | InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        System.out.println("Reciever " + counter);
                                    });
                                    if (message.startsWith("DOWN_OK")) {
//
                                        t1.start(); //  t1.join();
                                    }

                                    sock.write(ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8)));

                                    key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
                                    System.out.println("Закончили писать !");
                                    key.interestOps(key.interestOps() | SelectionKey.OP_READ);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                } finally
                                {

                                    buffer.clear();
                                    System.out.println("Почистим буффер !");
                                    SQL_Handler.disconnect();
                                    System.out.println("DB disconnected");
                                }
                            }
                    }
                }

            }

        }
    }

    private void setLogin(String login) {
        this.login=login;
    }

        public AuthService getAuthService() {
    return authService;}


        //получение списка файлов в директории пользователя
    public String getFileList(String login) {
        return String.join(" ", new File("root\\"+login).list());
    }
        //установка желаемого сообщения на отправку
        private void setMessage(String message){
        this.message=message;
        }
    //получение файла с клиента
    private  void recieveFile(SocketChannel channel) throws IOException {
        System.out.println("Начнем принимать файл");
        System.out.println(fileSize);
        System.out.println(filenameUP);
//        fileName=filenameUP;
        Path outputFile = Paths.get(filenameUP);
        int bufSize = 10240;
        //Path path = Paths.get(outputFile);
        FileChannel fileChannel = FileChannel.open(outputFile,
                EnumSet.of(StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE)
        );
        ByteBuffer buffer = ByteBuffer.allocate(bufSize);
        int res ;
        int counter=0;
        do {
            buffer.clear();
            res = channel.read(buffer);
            System.out.println(res);
            buffer.flip();
            if (res > 0 && counter< fileSize) {
                fileChannel.write(buffer);
                counter += res;
            } else if (res==0 && counter==fileSize) {break;}
        }
        while (res>=0 );

        fileChannel.close();
        System.out.println("Reciever: " + counter);


    }
}
