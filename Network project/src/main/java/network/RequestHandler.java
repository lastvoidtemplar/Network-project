package network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class RequestHandler {
    // main fields
    private int threads = -1;
    private int arrLen = -1;
    private long[] arr;
    private int arrInd = 0;

    // dependencies
    private final SocketChannel conn;
    private final SelectionKey key;

    // buffer fields
    private final ByteBuffer buf;
    private int readBytes = 0;
    private int writeBytes = 0;
    boolean statusCodeSend = false;

    RequestHandler(SocketChannel conn, SelectionKey key) {
        this.conn = conn;
        this.buf = ByteBuffer.allocate(1024);
        this.key = key;
    }

    boolean HandleRead() {
        try {
            int n = conn.read(buf);
            readBytes += n;
            buf.flip();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return false;
        }

        if (threads == -1 && readBytes >= 4) {
            threads = buf.getInt();
            if (!(1 <= threads && threads <= Limits.Threads)) {
                SendErrorMessage(ResponseCode.INVALID_THREADS_NUMBER, "The thread limit is " + Limits.Threads);
                return false;
            }
            readBytes -= 4;
        }
        if (arrLen == -1 && readBytes >= 4) {
            arrLen = buf.getInt();
            if (!(1 <= arrLen && arrLen <= Limits.Elements)) {
                SendErrorMessage(ResponseCode.INVALID_ELEMENTS_NUMBER, "The elements limit is " + Limits.Elements);
                return false;
            }
            arr = new long[arrLen];
            readBytes -= 4;
        }

        if (arrLen > -1) {
            while (arrInd < arrLen && readBytes >= 8) {
                arr[arrInd] = buf.getLong();
                arrInd++;
                readBytes -= 8;
            }
            buf.clear();
        }

        if (arrInd == arrLen) {
            System.out.println(arr[arrLen - 1]);

            arrInd = -1;
            buf.clear();

            key.interestOps(key.interestOps() & ~SelectionKey.OP_READ);
            new WorkerPool(threads, arr, this).Run();
        }
        return true;
    }

    boolean HandleWrite() {
        if (writeBytes > 0) {
            try {
                buf.flip();

                int n = conn.write(buf);
                writeBytes -= n;
                if (writeBytes == 0) {
                    buf.clear();
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
                return false;
            }
        } else {

            if (!statusCodeSend){
                buf.put(ResponseCode.OK.getValue());
                writeBytes++;
                statusCodeSend = true;
            }

            if (arrInd == -1 && writeBytes + 4 < buf.limit()) {
                buf.putInt(arrLen);
                arrInd = 0;
                writeBytes += 4;
            }

            if (arrInd > -1) {
                while (arrInd < arrLen && writeBytes + 8 < buf.limit()) {
                    buf.putLong(arr[arrInd]);
                    arrInd++;
                    writeBytes += 8;
                }
            }

            if (arrInd == arrLen && writeBytes == 0) {
                key.cancel();
            }
        }
        return true;
    }

    void ProcessReady(){
        key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
    }

    private void SendErrorMessage(ResponseCode status, String msg) {
        if (!statusCodeSend) {
            buf.clear();
            buf.put(status.getValue());
            buf.put(msg.getBytes());
            buf.flip();
            try {
                conn.write(buf);
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
            statusCodeSend = true;
        }
    }
}