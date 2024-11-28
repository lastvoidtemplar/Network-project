package network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.HashMap;

public class Server {
    private final HashMap<SocketChannel, RequestHandler> requestHandlers;

    Server() {
        requestHandlers = new HashMap<>();
    }

    public void Listen(int port) {
        try (var socket = ServerSocketChannel.open(); var selector = Selector.open()) {
            socket.configureBlocking(false);
            socket.bind(new InetSocketAddress(port));
            socket.register(selector, SelectionKey.OP_ACCEPT);
            Log("Server is listening...");
            int connInd = 1;
            while (true) {
                if (selector.select() == 0) {
                    continue;
                }

                for (var key : selector.selectedKeys()) {
                    if (key.isAcceptable()) {
                        // we have only one acceptable key possible which is the socket
                        var conn = socket.accept();
                        handleAccept(conn, selector, connInd);
                        connInd++;
                    } else if (key.isReadable() && key.channel() instanceof SocketChannel conn) {
                        var handler = requestHandlers.get(conn);
                        if (!handler.HandleRead()) {
                            requestHandlers.remove(conn);
                            conn.close();
                            Log("Connection id=" + handler.GetId() + " is closed");
                        }
                    } else if (key.isWritable() && key.channel() instanceof SocketChannel conn) {
                        var handler = requestHandlers.get(conn);
                        if (!handler.HandleWrite()) {
                            requestHandlers.remove(conn);
                            conn.close();
                            Log("Connection id=" + handler.GetId() + " is closed");
                        }
                    }
                }
                selector.selectedKeys().clear();
            }
        } catch (IOException e) {
            Log("Server failed to start: " + e.getMessage());
        }
    }


    private void handleAccept(SocketChannel conn, Selector selector, int id) {
        Log("New connection accepted id=" + id + "(" + conn.socket().getInetAddress() + ":" + conn.socket().getPort() + ")");
        SelectionKey key = null;
        try {
            conn.configureBlocking(false);
            key = conn.register(selector, SelectionKey.OP_READ);
        } catch (IOException e) {
            Log("Connection id=" + id + " setup  error: " + e.getMessage());
            if (key != null) {
                key.cancel();
            }
            try {
                conn.close();
            } catch (IOException ex) {
                Log("Could not close the connection id=" + id + ": " + e.getMessage());
            }
            return;
        }
        requestHandlers.put(conn, new RequestHandler(id, conn, key, selector));
    }

    private void Log(String message) {
        System.out.println("[Server connection] " + message);
    }
}