package client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class SingleClient implements Client {
    private final Scanner scanner;
    private final SocketChannel conn;
    private final File requestFile;
    private final File responseFile;

    SingleClient(Scanner scanner, SocketChannel conn, File requestFile, File responseFile) {
        this.scanner = scanner;
        this.conn = conn;
        this.requestFile = requestFile;
        this.responseFile = responseFile;
    }

    @Override
    public long Run() {
        Scanner fileScanner;
        try {
            fileScanner = new Scanner(requestFile);
        } catch (FileNotFoundException e) {
            System.out.println("File not found!");
            return -1;
        }
        int threads;
        try {
            threads = fileScanner.nextInt();
        } catch (NumberFormatException e) {
            System.out.println("Invalid file format! First thing in the file must be the number of threads!");
            return -1;
        }

        int len;
        try {
            len = fileScanner.nextInt();
        } catch (NumberFormatException e) {
            System.out.println("Invalid file format! Second thing in the file must be the length of the array!");
            return -1;
        }

        ByteBuffer buf = ByteBuffer.allocate(1024);
        buf.putInt(threads);
        buf.putInt(len);
        buf.flip();

        try {
            int n = conn.write(buf);
            if (n == -1) {
                System.out.println("The connection is closed!");
                return -1;
            }
        } catch (IOException e) {
            System.out.println("Error while sending the thread number and the array len: " + e.getMessage());
            return -1;
        }

        buf.compact();
        long el = 0;
        for (int i = 0; i < len; i++) {
            try {
                el = fileScanner.nextLong();
            } catch (NumberFormatException e) {
                System.out.println("Invalid file format! The element with index " + i + " in the file must be a number!");
                return -1;
            }
            buf.putLong(el);
            if (buf.position() + 8 > buf.limit() || i + 1 == len) {
                buf.flip();
                try {
                    int n = conn.write(buf);
                    if (n == -1) {
                        System.out.println("The connection is closed!");
                        return -1;
                    }
                } catch (IOException e) {
                    System.out.println("Error while sending elements: " + e.getMessage());
                    return -1;
                } catch (Exception e) {
                    System.out.println("some thing");
                }
                buf.compact();
            }
        }

        long start = System.currentTimeMillis();
        buf.clear();

        try {
            int n = conn.read(buf);
            if (n == -1) {
                System.out.println("The connection is closed!");
                return -1;
            }
        } catch (IOException e) {
            System.out.println("Error while reading the status code: " + e.getMessage());
            return -1;
        }
        long end = System.currentTimeMillis();
        buf.flip();
        byte code = buf.get();

        if (code != 0) {
            byte[] bytes = new byte[buf.remaining()];
            buf.get(bytes);
            String msg = new String(bytes);
            System.out.println("Status code " + code + " message " + msg);
            return -1;
        }


        if (buf.position() + 4 > buf.limit()) {
            buf.compact();
            try {
                int n = conn.read(buf);
                if (n == -1) {
                    System.out.println("The connection is closed!");
                    return -1;
                }
            } catch (IOException e) {
                System.out.println("Error while reading the status code: " + e.getMessage());
                return -1;
            }
            buf.flip();
        }

        int len2 = buf.getInt();
        if (len != len2) {
            System.out.println("Len in the request(" + len + ") does not match len int response(" + len2 + ")");
            return -1;
        }

        FileWriter writer;
        try {
            writer = new FileWriter(responseFile);
        } catch (IOException e) {
            System.out.println("Error while opening the response file: " + e.getMessage());
            return -1;
        }

        try {
            writer.write(len + "\n");
        } catch (IOException e) {
            System.out.println("Error while writing the response file");
            return -1;
        }

        for (int i = 0; i < len2; i++) {
            if (buf.position() + 8 > buf.limit()) {
                buf.compact();
                try {
                    int n = conn.read(buf);
                    if (n == -1) {
                        System.out.println("The connection is closed!");
                        return -1;
                    }
                } catch (IOException e) {
                    System.out.println("Error while reading the status code: " + e.getMessage());
                    return -1;
                }
                buf.flip();
            }
            el = buf.getLong();
            try {
                writer.write(el + " ");
            } catch (IOException e) {
                System.out.println("Error while writing the response file");
                return -1;
            }
        }
        try {
            conn.close();
            writer.close();
            scanner.close();
            fileScanner.close();
        } catch (IOException e) {
            System.out.println("Error while closing!");
        }
        return end - start;
    }
}
