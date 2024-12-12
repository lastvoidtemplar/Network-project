package client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class SingleClient implements Client {
    private final Scanner scanner;
    private final SocketChannel conn;
    private final File requestFile;
    private final String responsePath;

    SingleClient(Scanner scanner, String responsePath) {
        this.scanner = scanner;
        this.conn = setupSocket();
        this.requestFile = setupFile();
        this.responsePath = responsePath;
    }

    private File setupFile() {
        File file = new File("request.txt");
        return file;
        /*do {
            System.out.println("Enter the address of the file:");
            String path = scanner.next();

            file = new File(path);
            if (!file.exists()) {
                System.out.println("File with this path does not exist!");
                continue;
            }

            if (!file.isFile()) {
                System.out.println("The file is not normal file!");
                continue;
            }

            if (!file.canRead()) {
                System.out.println("Does not have permissions to read the file!");
                continue;
            }

            break;
        } while (false);
         */

    }

    private SocketChannel setupSocket() {
        SocketChannel conn = null;
        try {
            conn = SocketChannel.open(new InetSocketAddress("localhost", 3000));
        } catch (IOException e) {

        }

        return conn;
        /*
        do {
            System.out.println("Enter the address of server(hostname:port)");
            String addr = scanner.next();
            String[] inp = addr.split(":");

            if (inp.length != 2) {
                System.out.println("Invalid format for the server address");
                continue;
            }

            int port = 0;
            try {
                port = Integer.parseInt(inp[1]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid format for the server address");
                continue;
            }

            try {
                conn = SocketChannel.open(new InetSocketAddress(inp[0], port));
            } catch (UnresolvedAddressException e) {
                System.out.println("Unknown host(ip)! Try entering the different server address!");
                continue;
            } catch (IOException e) {
                System.out.println("Wrong port! Connection refuse! Try entering the different server address!");
                continue;
            }

            break;
        } while (true);
        */
    }

    @Override
    public void Run() {
        Scanner fileScanner;
        try {
            fileScanner = new Scanner(requestFile);
        } catch (FileNotFoundException e) {
            System.out.println("File not found!");
            return;
        }
        int threads;
        try {
            threads = fileScanner.nextInt();
        } catch (NumberFormatException e) {
            System.out.println("Invalid file format! First thing in the file must be the number of threads!");
            return;
        }

        int len;
        try {
            len = fileScanner.nextInt();
        } catch (NumberFormatException e) {
            System.out.println("Invalid file format! Second thing in the file must be the length of the array!");
            return;
        }

        ByteBuffer buf = ByteBuffer.allocate(1024);
        buf.putInt(threads);
        buf.putInt(len);
        buf.flip();

        try {
            int n = conn.write(buf);
            if (n == -1) {
                System.out.println("The connection is closed!");
                return;
            }
        } catch (IOException e) {
            System.out.println("Error while sending the thread number and the array len: " + e.getMessage());
            return;
        }

        buf.compact();
        long el = 0;
        for (int i = 0; i < len; i++) {
            try {
                el = fileScanner.nextLong();
            } catch (NumberFormatException e) {
                System.out.println("Invalid file format! The element with index " + i + " in the file must be a number!");
                return;
            }
            buf.putLong(el);
            if (buf.position() + 8 > buf.limit() || i + 1 == len) {
                buf.flip();
                try {
                    int n = conn.write(buf);
                    if (n == -1) {
                        System.out.println("The connection is closed!");
                        return;
                    }
                } catch (IOException e) {
                    System.out.println("Error while sending elements: " + e.getMessage());
                    return;
                } catch (Exception e) {
                    System.out.println("some thing");
                }
                buf.compact();
            }
        }

        buf.clear();

        try {
            int n = conn.read(buf);
            if (n == -1) {
                System.out.println("The connection is closed!");
                return;
            }
        } catch (IOException e) {
            System.out.println("Error while reading the status code: " + e.getMessage());
            return;
        }
        buf.flip();
        byte code = buf.get();

        if (code != 0) {
            byte[] bytes = new byte[buf.remaining()];
            buf.get(bytes);
            String msg = new String(bytes);
            System.out.println("Status code " + code + " message " + msg);
            return;
        }


        if (buf.position() + 4 >buf.limit()) {
            try {
                int n = conn.read(buf);
                if (n == -1) {
                    System.out.println("The connection is closed!");
                    return;
                }
            } catch (IOException e) {
                System.out.println("Error while reading the status code: " + e.getMessage());
                return;
            }
            buf.flip();
        }

        int len2 = buf.getInt();
        if (len != len2) {
            System.out.println("Len in the request(" + len + ") does not match len int response(" + len2 + ")");
            return;
        }

        File responseFile = new File(responsePath);
        FileWriter writer;
        try {
            writer = new FileWriter(responseFile);
        } catch (IOException e){
            System.out.println("Error while opening the response file: " + e.getMessage());
            return;
        }

        try {
            writer.write(len+"\n");
        } catch (IOException e) {
            System.out.println("Error while writing the response file");
            return;
        }

        for (int i = 0; i < len2; i++) {
            if (buf.position() + 8 > buf.limit()) {
                try {
                    int n = conn.read(buf);
                    if (n == -1) {
                        System.out.println("The connection is closed!");
                        return;
                    }
                } catch (IOException e) {
                    System.out.println("Error while reading the status code: " + e.getMessage());
                    return;
                }
                buf.flip();
            }
            el = buf.getLong();
            try {
                writer.write(el+" ");
            } catch (IOException e) {
                System.out.println("Error while writing the response file");
                return;
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

    }
}
