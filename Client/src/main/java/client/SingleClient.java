package client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class SingleClient{
    private final SocketChannel conn;
    private final File requestFile;
    private final File responseFile;
    private long[] requestArr;
    private long[] responseArr;
    private final String logPrefix;

    SingleClient(String logPrefix, SocketChannel conn, File requestFile, File responseFile) {
        this.conn = conn;
        this.requestFile = requestFile;
        this.responseFile = responseFile;
        this.logPrefix = logPrefix;
    }

    private void Log(String message){
        System.out.println(logPrefix +" "+ message);
    }

    public long Run() {
        Scanner fileScanner;
        try {
            fileScanner = new Scanner(requestFile);
        } catch (FileNotFoundException e) {
            Log("File not found!");
            return -1;
        }
        int threads;
        try {
            threads = fileScanner.nextInt();
        } catch (NumberFormatException e) {
            Log("Invalid file format! First thing in the file must be the number of threads!");
            return -1;
        }

        int len;
        try {
            len = fileScanner.nextInt();
        } catch (NumberFormatException e) {
            Log("Invalid file format! Second thing in the file must be the length of the array!");
            return -1;
        }

        try{
            requestArr = new long[len];
        } catch (OutOfMemoryError e) {
            Log("Out of memory");
            return -1;
        }

        ByteBuffer buf = ByteBuffer.allocate(1024);
        buf.putInt(threads);
        buf.putInt(len);
        buf.flip();

        try {
            int n = conn.write(buf);
            if (n == -1) {
                Log("The connection is closed!");
                return -1;
            }
        } catch (IOException e) {
            Log("Error while sending the thread number and the array len: " + e.getMessage());
            return -1;
        }

        buf.compact();
        long el = 0;
        for (int i = 0; i < len; i++) {
            try {
                el = fileScanner.nextLong();
            } catch (NumberFormatException e) {
                Log("Invalid file format! The element with index " + i + " in the file must be a number!");
                return -1;
            }
            requestArr[i] = el;
            buf.putLong(el);
            if (buf.position() + 8 > buf.limit() || i + 1 == len) {
                buf.flip();
                try {
                    int n = conn.write(buf);
                    if (n == -1) {
                        Log("The connection is closed!");
                        return -1;
                    }
                } catch (IOException e) {
                    Log("Error while sending elements: " + e.getMessage());
                    return -1;
                } catch (Exception e) {
                    Log("some thing");
                }
                buf.compact();
            }
        }

        long start = System.currentTimeMillis();
        buf.clear();

        try {
            int n = conn.read(buf);
            if (n == -1) {
                Log("The connection is closed!");
                return -1;
            }
        } catch (IOException e) {
            Log("Error while reading the status code: " + e.getMessage());
            return -1;
        }

        long end = System.currentTimeMillis();
        buf.flip();
        byte code = buf.get();

        if (code != 0) {
            byte[] bytes = new byte[buf.remaining()];
            buf.get(bytes);
            String msg = new String(bytes);
            Log("Status code " + code + " message " + msg);
            return -1;
        }


        if (buf.position() + 4 > buf.limit()) {
            buf.compact();
            try {
                int n = conn.read(buf);
                if (n == -1) {
                    Log("The connection is closed!");
                    return -1;
                }
            } catch (IOException e) {
                Log("Error while reading the status code: " + e.getMessage());
                return -1;
            }
            buf.flip();
        }

        int len2 = buf.getInt();
        if (len != len2) {
            Log("Len in the request(" + len + ") does not match len int response(" + len2 + ")");
            return -1;
        }

        try{
            responseArr = new long[len2];
        } catch (OutOfMemoryError e) {
            Log("Out of memory");
            return -1;
        }

        FileWriter writer;
        try {
            writer = new FileWriter(responseFile);
        } catch (IOException e) {
            Log("Error while opening the response file: " + e.getMessage());
            return -1;
        }

        try {
            writer.write(len + "\n");
        } catch (IOException e) {
            Log("Error while writing the response file");
            return -1;
        }

        for (int i = 0; i < len2; i++) {
            if (buf.position() + 8 > buf.limit()) {
                buf.compact();
                try {
                    int n = conn.read(buf);
                    if (n == -1) {
                        Log("The connection is closed!");
                        return -1;
                    }
                } catch (IOException e) {
                    Log("Error while reading the status code: " + e.getMessage());
                    return -1;
                }
                buf.flip();
            }
            el = buf.getLong();
            responseArr[i] = el;
            try {
                writer.write(el + " ");
            } catch (IOException e) {
                Log("Error while writing the response file");
                return -1;
            }
        }
        try {
            conn.close();
            writer.close();
            fileScanner.close();
        } catch (IOException e) {
            Log("Error while closing!");
        }

        ResultChecker checker = new ResultChecker(requestArr, responseArr);

        if (!checker.IsCorrect()){
            return  -1;
        }

        return end - start;
    }
}
