package client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class BenchClient {
    private final long[] requestArr;
    private final SocketChannel[] conns;
    private final long[] responseArr;

    BenchClient(long[] requestArr, SocketChannel[] conns) {
        this.requestArr = requestArr;
        this.responseArr = new long[requestArr.length];
        this.conns = conns;
    }

    private long runSingleConn(int threads, SocketChannel conn) {
        ByteBuffer buf = ByteBuffer.allocate(1024);
        buf.putInt(threads);
        buf.putInt(requestArr.length);
        buf.flip();

        try {
            int n = conn.write(buf);
            if (n == -1) {
                System.out.println("[Client " + threads + "] The connection is closed!");
                return -1;
            }
        } catch (IOException e) {
            System.out.println("[Client " + threads + "] Error while sending the thread number and the array len: " + e.getMessage());
            return -1;
        }
        buf.compact();

        for (int i = 0; i < requestArr.length; i++) {
            long el = requestArr[i];
            buf.putLong(el);

            if (buf.position() + 8 > buf.limit() || i + 1 == requestArr.length) {
                buf.flip();
                try {
                    int n = conn.write(buf);
                    if (n == -1) {
                        System.out.println("[Client " + threads + "] The connection is closed!");
                        return -1;
                    }
                } catch (IOException e) {
                    System.out.println("[Client " + threads + "] Error while sending elements: " + e.getMessage());
                    return -1;
                }
                buf.compact();
            }
        }

        long start = System.currentTimeMillis();
        buf.clear();

        try {
            int n = conn.read(buf);
            if (n == -1) {
                System.out.println("[Client " + threads + "] The connection is closed!");
                return -1;
            }
        } catch (IOException e) {
            System.out.println("[Client " + threads + "] Error while reading the status code: " + e.getMessage());
            return -1;
        }

        long end = System.currentTimeMillis();
        buf.flip();
        byte code = buf.get();

        if (code != 0) {
            byte[] bytes = new byte[buf.remaining()];
            buf.get(bytes);
            String msg = new String(bytes);
            System.out.println("[Client " + threads + "] Status code " + code + " message " + msg);
            return -1;
        }

        if (buf.position() + 4 > buf.limit()) {
            try {
                int n = conn.read(buf);
                if (n == -1) {
                    System.out.println("[Client " + threads + "] The connection is closed!");
                    return -1;
                }
            } catch (IOException e) {
                System.out.println("[Client " + threads + "] Error while reading the status code: " + e.getMessage());
                return -1;
            }
            buf.flip();
        }

        int len = buf.getInt();
        if (len != requestArr.length) {
            System.out.println("[Client " + threads + "] Len in the request(" + len + ") does not match len int response(" + len + ")");
            return -1;
        }

        for (int i = 0; i < responseArr.length; i++) {
            if (buf.position() + 8 > buf.limit()) {
                buf.compact();
                try {
                    int n = conn.read(buf);
                    if (n == -1) {
                        System.out.println("[Client " + threads + "] The connection is closed!");
                        return -1;
                    }
                } catch (IOException e) {
                    System.out.println("[Client " + threads + "] Error while reading the status code: " + e.getMessage());
                    return -1;
                }
                buf.flip();
            }
            long el = buf.getLong();
            responseArr[i] = el;
        }
        try {
            conn.close();
        } catch (IOException e) {
            System.out.println("[Client " + threads + "] Error while closing!");
        }

        ResultChecker checker = new ResultChecker(requestArr, responseArr);
        if (!checker.IsCorrect()) {
            return -1;
        }

        return end - start;
    }

    public void Run() {
        long[] times = new long[conns.length];
        for (int i = 0; i < conns.length; i++) {
            times[i] = this.runSingleConn(i + 1, conns[i]);
        }

        for (int i = 0; i < times.length; i++) {
            System.out.println(i + 1 + " threads - " + times[i] + "ms!");
        }
    }
}
