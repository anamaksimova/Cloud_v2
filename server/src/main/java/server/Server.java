package server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;

public class Server {
    String host = "Localhost";
    int port = 5678;


    public Server() throws IOException {
        Selector selector = Selector.open();
        ServerSocketChannel server = ServerSocketChannel.open();
        server.configureBlocking(false);
        server.bind(new InetSocketAddress(port));
        server.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("Server started");


        while (true) {
            if (selector.isOpen()) {
                int keys = selector.select();
                if (keys > 0) {
                    Set<SelectionKey> selectedKeys = selector.selectedKeys();
                    Iterator<SelectionKey> iterator = selectedKeys.iterator();
                    while (iterator.hasNext()) {
                        SelectionKey sKey = iterator.next();
                        iterator.remove();
                        if (sKey.isValid() && sKey.isAcceptable()) {
                            //HandlerAccept.handleAccept(sKey, selector);
                            SocketChannel acceptedClient =  server.accept();
                            acceptedClient.configureBlocking(false);
                            System.out.println("Client accepted. IP: " + acceptedClient.getRemoteAddress());

                            acceptedClient.register(selector, SelectionKey.OP_READ);
                            continue;
                        }
                        if(sKey.isValid() && sKey.isReadable()){

                            SocketChannel channel = ((SocketChannel) sKey.channel());
                            //todo получить логин пароль и отправиь ответ если логин есть

                            //зарегистрировать и внести в бд если команда
                            //получить файл по команде
                            //отправить файл по комманде
                            recieveFile(channel);
                            channel.close();
                            return;
                        }
////
//                        if (sKey.isValid() && sKey.isWritable()) {
//                   //todo
//
//                        }
                    }

                }
            }
        }
    }

//    private void sendMsg(String message, Selector selector, SocketAddress client) throws IOException {
//        for (SelectionKey key : selector.keys()) {
//            if (key.isValid() && key.channel() instanceof SocketChannel) {
//                if (((SocketChannel)key.channel()).getRemoteAddress().equals(client)) {
//                    ((SocketChannel)key.channel())
//                            .write(ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8)));
//                }
//            }
//        }
//    }

    private  void recieveFile(SocketChannel channel) throws IOException {
        String outputFile = "C:\\тест\\2.pdf";
        int bufSize = 10240;
        Path path = Paths.get(outputFile);
        FileChannel fileChannel = FileChannel.open(path,
                EnumSet.of(StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE)
        );
        ByteBuffer buffer = ByteBuffer.allocate(bufSize);
        int res =0;
        int counter=0;
        do {
            buffer.clear();
            res = channel.read(buffer);
            System.out.println(res);
            buffer.flip();
            if (res > 0) {
                fileChannel.write(buffer);
                counter += res;
            }
        }
        while (res>=0);
        channel.close();
        fileChannel.close();
        System.out.println("Reciever: " + counter);


    }
}
