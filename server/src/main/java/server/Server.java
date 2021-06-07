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
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class Server {

    int port = 5678;
    Path pathClient = null;
    public AuthService authService;
    private String message;
    SocketChannel sock;
    private String fileName;
    private String login;

    public Server() throws IOException {
          ByteBuffer buffer = ByteBuffer.allocate(600000);
        Selector selector = Selector.open();
        ServerSocketChannel server = ServerSocketChannel.open();
        server.configureBlocking(false);
        server.bind(new InetSocketAddress(port));
        server.register(selector, server.validOps());
        //server.register(selector, SelectionKey.OP_ACCEPT);
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
                            SocketAddress client = sock.getRemoteAddress();
                            sock.finishConnect();
                            continue;
                        }

                        if(key.isReadable())
                        {
                            System.out.println("Интерес на чтение");
                            SocketChannel sock =(SocketChannel) key.channel();
                            SocketAddress client = sock.getRemoteAddress();
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
//                                        .replace("\n", "")
//                                        .replace("\r", "");
                                System.out.println(command+"++++++++++++++++++++++++++++++++++");

                                //регистарция
                                if (command.startsWith("reg")){
                                    if (!SQL_Handler.connect()) {
                                    RuntimeException e = new RuntimeException("Не удалось подключиться к БД");
                                    // logger.log(Level.SEVERE, "Не удалось подключиться к БД", e);
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
                                    // logger.log(Level.SEVERE, "Не удалось подключиться к БД", e);
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
                                       // key.interestOps(key.interestOps() | SelectionKey.OP_READ);
                                    }
                                    else {
                                        System.out.println("Ошибка,такого логина или пароля не существует");
                                        setMessage("AUTH_NO");
                                        key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                                    }


                                }

//                                if (command.startsWith("UPLOAD")){
//                                    // channel = ((SocketChannel) key.channel());
//                                    String[] cmd= command.split(" ",2);
//                                    fileName=cmd[1];
//                                    System.out.println(fileName);
//                                    recieveFile(sock);
//
//                                }

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
//                                    } else {
//                                        //sendMessage("Файл не найден", selector, client);
                                    }

                                }

                                if (command.startsWith("INFO")){
                                    String[] cmd= command.split(" ",2);
                                    String filename = "root\\"+login+"\\"+cmd[1];
                                    Path path = Paths.get(filename);
                                    System.out.println(path);
                                    if (Files.exists(path)){
                                       long sizeFile=Files.size(path);
//                                       FileTime date=Files.getLastModifiedTime(path);
//
//                                        DateFormat df = new SimpleDateFormat("MM/dd/yyyy");
//                                        String dateCreated = df.format(date);
//                                        System.out.println(dateCreated);


                                        System.out.println("size:" + sizeFile);
                                        setMessage("INFO_OK "+cmd[1]+" "+sizeFile);
                                        key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);

//
//                                    } else {
//                                        //sendMessage("Файл не найден", selector, client);
                                    }

                                }






                                buffer.flip();
                                System.out.println("Закончили чтение!");
                               // key.interestOps(key.interestOps() | SelectionKey.OP_READ);
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
                                try
                                { TimeUnit.SECONDS.sleep(1);
                                    System.out.println("Пишем...!");



                                    SocketChannel sock = ((SocketChannel) key.channel());

                                    sock.write(ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8)));
//
                                    key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
                                    System.out.println("Закончили писать !");
                                    //sendMsg("привет", selector, sock.getRemoteAddress());
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



//
        public AuthService getAuthService() {
    return authService;}

//    private void sendMessage(String message, Selector selector, SocketAddress client) throws IOException {
//        for (SelectionKey key : selector.keys()) {
//            if (key.isValid() && key.channel() instanceof SocketChannel) {
//                if (((SocketChannel)key.channel()).getRemoteAddress().equals(client)) {
//                    ((SocketChannel)key.channel())
//                            .write(ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8)));
//                }
//            }
//        }
//    }

    public String getFileList(String login) {
        return String.join(" ", new File("root\\"+login).list());
    }

        private void setMessage(String message){
        this.message=message;
        }
    private void sendMsg(String message, Selector selector, SocketAddress client) throws IOException {
        for (SelectionKey key : selector.keys()) {
            if (key.isValid() && key.channel() instanceof SocketChannel) {
                if (((SocketChannel)key.channel()).getRemoteAddress().equals(client)) {
                    ((SocketChannel)key.channel())
                            .write(ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8)));
                }
            }
        }
    }
    //не готово
    private  void recieveFile(SocketChannel channel) throws IOException {
        System.out.println("Начнем принимать файл");
        fileName="1.pdf";
        Path outputFile = Paths.get(pathClient + "\\"+fileName);
        int bufSize = 10240;
        //Path path = Paths.get(outputFile);
        FileChannel fileChannel = FileChannel.open(outputFile,
                EnumSet.of(StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE)
        );
        ByteBuffer buffer = ByteBuffer.allocate(bufSize);
        int res =0;
        int counter=0;
        do {
            buffer.clear();
            res = sock.read(buffer);
            System.out.println(res);
            buffer.flip();
            if (res > 0) {
                fileChannel.write(buffer);
                counter += res;
            }
        }
        while (res>=0);
        sock.close();
        fileChannel.close();
        System.out.println("Reciever: " + counter);


    }
}
