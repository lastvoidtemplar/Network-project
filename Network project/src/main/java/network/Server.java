package network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.HashMap;

public class Server {
    private HashMap<SocketChannel, RequestHandler> requestHanlers;
    Server(){
        requestHanlers = new HashMap<>();
    }
    void Listen(int port ){
        try (var socket = ServerSocketChannel.open(); var selector = Selector.open()){
            socket.configureBlocking(false);
            socket.bind(new InetSocketAddress(port));
            socket.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("Server is listening");
            while (true){
                selector.select();

                for (var key : selector.selectedKeys()){
                    if (key.isAcceptable()) {
                        // we have only one acceptable key possible which is the socket
                        var conn = socket.accept();
                        handleAccept(conn, selector);
                    } else if (key.isReadable() && key.channel() instanceof SocketChannel conn) {
                        requestHanlers.get(conn).HandleRead();
                    } else if (key.isWritable() && key.channel() instanceof SocketChannel conn){
                        requestHanlers.get(conn).HandleWrite();
                    }
                }

                selector.selectedKeys().clear();
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }


    private  void handleAccept(SocketChannel conn, Selector selector){
        System.out.println("New connection accepted (" +  conn.socket().getInetAddress() + ":"+ conn.socket().getPort()+")");
        try {
            conn.configureBlocking(false);
            var key =  conn.register(selector, SelectionKey.OP_READ);
            requestHanlers.put(conn, new RequestHandler(conn, key,selector));
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

    }
}