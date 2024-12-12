package client;

import java.util.InputMismatchException;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class Main {
    private static void printClientOptions(){
        System.out.println("Choose client option:");
        System.out.println("1.  Run single client with given file with the request params.");
        System.out.println("2.  Run multiple clients in parallel with given file and calculate min/average/max");
        System.out.println("3.  Run multiple clients in in sequence(noisy neighbor) with large array and compare the time for 1..N threads");
    }
    private static int  readClientOption(Scanner scanner){
        int option = 0;
        do {
            printClientOptions();
            try {
                option = scanner.nextInt();
            } catch (InputMismatchException e){
                System.out.println("The client expects the options as number!");
                scanner.next();
                continue;
            }

            if  (1 <= option && option <= 3) {
                break;
            }
            else{
                System.out.println("Invalid option number! Please choose again!");
            }
        }while (true);
        return option;
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        //int option = readClientOption(scanner);

        int option = 1;
        try {
            switch (option){
                case 1:
                    Client client = new SingleClient(scanner, "response.txt");
                    client.Run();
                    break;
                default:
                    break;
            }
        } catch (Exception e){
            System.out.println(e.getMessage());
        }


    }
}