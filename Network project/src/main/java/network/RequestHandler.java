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
    private final int id;
    private final SocketChannel conn;
    private final SelectionKey key;
    private final Selector selector;

    // buffer fields
    private final ByteBuffer buf;
    private boolean statusCodeSend = false;

    RequestHandler(int id, SocketChannel conn, SelectionKey key, Selector selector) {
        this.id = id;
        this.conn = conn;
        this.buf = ByteBuffer.allocate(1024);
        this.key = key;
        this.selector = selector;
    }

    public boolean HandleRead() {
        try {
            conn.read(buf);
            buf.flip();
        } catch (IOException e) {
            Log("Error reading from the connection: " + e.getMessage());
            return false;
        }

        if (threads == -1 && buf.remaining() >= 4) {
            threads = buf.getInt();
            if (!(1 <= threads && threads <= Limits.Threads)) {
                SendErrorMessage(ResponseCode.INVALID_THREADS_NUMBER, "The thread limit is " + Limits.Threads);
                return false;
            }
        }
        if (arrLen == -1 && buf.remaining() >= 4) {
            arrLen = buf.getInt();
            if (!(1 <= arrLen && arrLen <= Limits.Elements)) {
                SendErrorMessage(ResponseCode.INVALID_ARRAY_LENGTH, "The elements limit is " + Limits.Elements);
                return false;
            }
            arr = new long[arrLen];
        }

        if (arrLen > -1) {
            while (arrInd < arrLen && buf.remaining() >= 8) {
                arr[arrInd] = buf.getLong();
                arrInd++;
            }
        }

        if (arrInd == arrLen) {
            Log("The data is receive successfully threads=" + threads + " arrLen=" + arrLen);

            arrInd = -1;
            buf.clear();

            key.interestOps(key.interestOps() & ~SelectionKey.OP_READ);
            SpawnThreads();
            return true;
            //key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        }

        buf.compact();
        return true;
    }

    private void SpawnThreads() {
        HashMap<Integer, Node> map = new HashMap<>();
        Semaphore sem = new Semaphore(1);
        BlockingQueue<Task> queue = new LinkedBlockingQueue<>();
        Integer rootId = IdGenerator.GenerateId();
        queue.add(new Task(rootId, (byte) 0, 0, arrLen));
        map.put(rootId, new Node(-1, 0, arrLen));
        for (int i = 1; i <= threads; i++) {
            new WorkerThread(i, threads, arr, this, queue, map, sem).start();
            Log("New worker thread spawn id=" + i);
        }
    }

    public boolean HandleWrite() {
        if (buf.position() > 0) {
            try {
                buf.flip();

                conn.write(buf);
                buf.compact();
            } catch (IOException e) {
                Log("Error writing to the connection: " + e.getMessage());
                return false;
            }
        } else {

            if (!statusCodeSend) {
                Log("Status code is OK");
                buf.put(ResponseCode.OK.getValue());
                statusCodeSend = true;
            }

            if (arrInd == -1 && buf.remaining() >= 4) {
                buf.putInt(arrLen);
                arrInd = 0;
            }

            if (arrInd > -1) {
                while (arrInd < arrLen &&  buf.remaining() >= 8) {
                    buf.putLong(arr[arrInd]);
                    arrInd++;
                }
            }

            if (arrInd == arrLen && buf.position() == 0) {
                Log("The data is send successfully");
                key.cancel();
                return false;
            }
        }
        return true;
    }

    public void ProcessReady() {
        Log("The worker threads completed the work");
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
                Log("Error sending error message: " + e.getMessage());
                return;
            }
            statusCodeSend = true;
            switch (status) {
                case OK:
                    break;
                case INVALID_THREADS_NUMBER:
                    Log("Status code is INVALID_THREAD_NUMBER");
                    break;
                case INVALID_ARRAY_LENGTH:
                    Log("Status code is INVALID_ARRAY_LENGTH");
                    break;
                case SERVER_ERROR:
                    Log("Status code is SERVER_ERROR");
                    break;
            }
        }
    }

    public int GetId() {
        return id;
    }

    private void Log(String message) {
        System.out.println("[Connection " + id + "] " + message);
    }
}