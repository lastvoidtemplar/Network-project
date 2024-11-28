package network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

public class RequestHandler {
    // main fields
    private int threads = -1;
    private int arrLen = -1;
    private long[] arr;
    private int arrInd = 0;

    // dependencies
    private final SocketChannel conn;
    private final SelectionKey key;
    private final Selector selector;

    // buffer fields
    private final ByteBuffer buf;
    private int readBytes = 0;
    private int writeBytes = 0;
    private boolean statusCodeSend = false;

    RequestHandler(SocketChannel conn, SelectionKey key, Selector selector) {
        this.conn = conn;
        this.buf = ByteBuffer.allocate(1024);
        this.key = key;
        this.selector = selector;
    }

    public boolean HandleRead() {
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
            SpawnThreads();
            //key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        }
        return true;
    }

    private  void SpawnThreads(){
        HashMap<Integer, Node> map = new HashMap<>();
        Semaphore sem = new Semaphore(1);
        BlockingQueue<Task> queue = new LinkedBlockingQueue<>();
        Integer rootId = IdGenerator.GenerateId();
        queue.add( new Task(rootId, (byte)0, 0, arrLen));
        map.put(rootId, new Node(-1, 0, arrLen));
        for (int i = 0; i < threads; i++) {
            new WorkerThread(threads, arr, this, queue, map, sem).start();
        }
    }

    public boolean HandleWrite() {
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

    public void ProcessReady(){
        key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        selector.wakeup();
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