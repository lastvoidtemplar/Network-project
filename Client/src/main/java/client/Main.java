package client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;
import java.util.InputMismatchException;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class Main {
    private static void printClientOptions() {
        System.out.println("Choose client option:");
        System.out.println("1.  Run single client with given request file.");
        System.out.println("1.  Run multiple clients in parallel with given request file and display info about min/arg/max.");
        System.out.println("3.  Run multiple clients in parallel with given request file");
        System.out.println("4.  Run multiple clients in in sequence(noisy neighbor) with large array and compare the time for 1..N threads");
        System.out.println("---");
    }

    private static int readClientOption(Scanner scanner) {
        int option = 0;
        do {
            printClientOptions();
            try {
                option = scanner.nextInt();
            } catch (InputMismatchException e) {
                System.out.println("The client expects the options as number!");
                scanner.next();
                continue;
            }

            if (1 <= option && option <= 3) {
                break;
            } else {
                System.out.println("Invalid option number! Please choose again!");
            }
        } while (true);
        System.out.println("Valid input!\n---");
        return option;
    }

    private static int readClientNumber(Scanner scanner) {
        int clients = 0;
        do {
            System.out.println("Enter the number of clients!");
            try {
                clients = scanner.nextInt();
            } catch (InputMismatchException e) {
                System.out.println("The number of client expects the options as number!");
                scanner.next();
                continue;
            }

            if (1 <= clients && clients <= 15) {
                break;
            } else {
                System.out.println("Invalid number of client! Please choose number between 1 and 15!");
            }
        } while (true);
        System.out.println("Valid input!\n---");
        return clients;
    }

    private static File setupRequestFile(Scanner scanner) {
        File file;

        do {
            System.out.println("Enter the path of a request file:");
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
        } while (true);
        System.out.println("Valid input!\n---");
        return file;
    }

    private static File setupResponseFile(Scanner scanner) {
        File file;

        do {
            System.out.println("Enter the path of a response file:");
            String path = scanner.next();

            file = new File(path);
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    System.out.println("Could not create the file!");
                    continue;
                }
            }

            if (!file.isFile()) {
                System.out.println("The file is not normal file!");
                continue;
            }

            if (!file.canWrite()) {
                System.out.println("Does not have permissions to read the file!");
                continue;
            }

            break;
        } while (true);
        System.out.println("Valid input!\n---");
        return file;
    }

    private static SocketChannel setupSocket(Scanner scanner) {
        SocketChannel conn;
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
        System.out.println("Valid input!\n---");
        return conn;
    }

    private static SocketChannel[] setupSocketArray(Scanner scanner, int clientNumber) {
        SocketChannel[] arr = new SocketChannel[clientNumber];
        SocketChannel conn;
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

            arr[0] = conn;
            for (int i = 1; i < clientNumber; i++) {
                try {
                    arr[i] = SocketChannel.open(new InetSocketAddress(inp[0], port));
                } catch (UnresolvedAddressException e) {
                    System.out.println("Unknown host(ip)! Try entering the different server address!");
                    return null;
                } catch (IOException e) {
                    System.out.println("Wrong port! Connection refuse! Try entering the different server address!");
                    return null;
                }
            }

            break;
        } while (true);
        System.out.println("Valid input!\n---");
        return arr;
    }


    private static long[] getRequestArr(File file) {
        Scanner fileScanner;
        try {
            fileScanner = new Scanner(file);
        } catch (FileNotFoundException e) {
            System.out.println("File not found!");
            return null;
        }
        int threads;
        try {
            threads = fileScanner.nextInt();
        } catch (NumberFormatException e) {
            System.out.println("Invalid file format! First thing in the file must be the number of threads!");
            return null;
        }

        int len;
        try {
            len = fileScanner.nextInt();
        } catch (NumberFormatException e) {
            System.out.println("Invalid file format! Second thing in the file must be the length of the array!");
            return null;
        }

        long[] arr;
        try {
            arr = new long[len];
        } catch (OutOfMemoryError e) {
            System.out.println("Out of memory");
            return null;
        }

        long el = 0;
        for (int i = 0; i < len; i++) {
            try {
                el = fileScanner.nextLong();
            } catch (NumberFormatException e) {
                System.out.println("Invalid file format! The element with index " + i + " in the file must be a number!");
                return null;
            }
            arr[i] = el;
        }

        return arr;
    }

    private static long[] getResponseArr(File file) {
        Scanner fileScanner;
        try {
            fileScanner = new Scanner(file);
        } catch (FileNotFoundException e) {
            System.out.println("File not found!");
            return null;
        }

        int len;
        try {
            len = fileScanner.nextInt();
        } catch (NumberFormatException e) {
            System.out.println("Invalid file format! Second thing in the file must be the length of the array!");
            return null;
        }

        long[] arr;
        try {
            arr = new long[len];
        } catch (OutOfMemoryError e) {
            System.out.println("Out of memory");
            return null;
        }

        long el = 0;
        for (int i = 0; i < len; i++) {
            try {
                el = fileScanner.nextLong();
            } catch (NumberFormatException e) {
                System.out.println("Invalid file format! The element with index " + i + " in the file must be a number!");
                return null;
            }
            arr[i] = el;
        }

        return arr;
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int option = readClientOption(scanner);

        try {
            SocketChannel conn;
            int clientNumber = 0;
            File requestFile;
            File responseFile;
            SingleClient[] clients;

            switch (option) {
                case 1:
                    conn = setupSocket(scanner);
                    requestFile = setupRequestFile(scanner);
                    responseFile = setupResponseFile(scanner);
                    SingleClient client = new SingleClient("[Client 1]", conn, requestFile, responseFile);

                    long time = client.Run();

                    if (time == -1) {
                        System.out.println("Failed the benchmark");
                        return;
                    }

                    System.out.println(time);
                    break;
                case 2:
                    clientNumber = readClientNumber(scanner);
                    SocketChannel[] conns = setupSocketArray(scanner, clientNumber);

                    if (conns == null){
                        return;
                    }

                    requestFile = setupRequestFile(scanner);
                    clients = new SingleClient[clientNumber];

                    for (int i = 0; i < clientNumber; i++) {
                        clients[i] = new SingleClient("[Client " + i + "]", conns[i], requestFile, new File("tmp"+i+".txt"));
                    }

                    BenchClient benchClient = new BenchClient(clients);
                    benchClient.Run();
                    break;
                case 3:
                    clientNumber = readClientNumber(scanner);
                    clients = new SingleClient[clientNumber];
                    for (int i = 0; i < clientNumber; i++) {
                        conn = setupSocket(scanner);
                        requestFile = setupRequestFile(scanner);
                        responseFile = setupResponseFile(scanner);
                        clients[i] = new SingleClient("[Client " + i + "]", conn, requestFile, responseFile);
                    }

                    MultiCllient multClient = new MultiCllient(clients);
                    multClient.Run();
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }


    }
}