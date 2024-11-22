package network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class RequestHandler {
    private int threads = -1;
    private int arrLen = -1;
    private long[] arr;
    private int arrInd = 0;
    private SocketChannel conn;
    private SelectionKey key;
    private Selector selector;
    private ByteBuffer buf;
    private int readBytes = 0;
    private int writeBytes = 0;

    RequestHandler(SocketChannel conn, SelectionKey key, Selector selector) {
        this.conn = conn;
        this.buf = ByteBuffer.allocate(64000);
        this.key = key;
        this.selector = selector;
    }

    void HandleRead() {
        try {
            int n = conn.read(buf);
            readBytes += n;
            buf.flip();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            Clean();
        }

        if (threads == -1 && readBytes >= 4) {
            threads = buf.getInt();
            if (!(1 <= threads && threads <= Limits.Threads)) {
                SendErrorMessage(ResponseCode.INVALID_THREADS_NUMBER, "The thread limit is " + Limits.Threads);
                Clean();
                return;
            }
            readBytes -= 4;
        }
        if (arrLen == -1 && readBytes >= 4) {
            arrLen = buf.getInt();
            if (!(1 <= arrLen && arrLen <= Limits.Elements)) {
                SendErrorMessage(ResponseCode.INVALID_ELEMENTS_NUMBER, "The elements limit is " + Limits.Elements);
                Clean();
                return;
            }
            arr = new long[arrLen];
            readBytes -= 4;
        }

        while (arrInd < arrLen && readBytes >= 8){
            arr[arrInd] = buf.getLong();
            readBytes -= 8;
        }
        arrInd = -1;
        buf.flip();

        key.cancel();
        try {
            key = conn.register(selector, SelectionKey.OP_WRITE);
        } catch (ClosedChannelException e) {
            System.out.println(e.getMessage());
            Clean();
        }

    }

    void HandleWrite(){
        if (arrInd == -1){
            buf.putLong(arrLen);
            buf.flip();
            try {
                int n = conn.write(buf);
            } catch (IOException e) {
                System.out.println(e.getMessage());
                Clean();
            }
        }
    }

    private void SendErrorMessage(ResponseCode status, String msg) {
        buf.flip();
        buf.putInt(status.getValue());
        buf.put(msg.getBytes());
        buf.flip();
        try {
            conn.write(buf);
            buf.flip();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            Clean();
        }

    }

    private void Clean() {
        try {
            buf = null;
            conn.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

}