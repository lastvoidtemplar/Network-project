package client;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MultiCllient {
    private final BlockingQueue<Long> queue;
    private final SingleClient[] clients;
    private final long[] times;

    MultiCllient(SingleClient[] clients) {
        this.queue = new LinkedBlockingQueue<Long>();
        this.clients = clients;
        this.times = new long[clients.length];
    }

    public long[] Run() {
        for (int i = 0; i < clients.length; i++) {
            new ClientThread(i, queue, clients[i], times).start();
        }

        for (int i = 0; i < clients.length; i++) {
            try {
                queue.take();
            } catch (InterruptedException e) {
                System.out.println("Problem with the queue!");
                return null;
            }
        }

        for (int i = 0; i < clients.length; i++) {
            if (times[i] == -1) {
                System.out.println("There was error with client " + i + "!");
            } else {
                System.out.println("Time for client " + i + "is " + times[i] + "ms!");
            }
        }

        return times;
    }

}
