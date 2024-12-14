package client;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class BenchClient {
    private final BlockingQueue<Long> queue;
    private final SingleClient[] clients;
    private final long[] times;

    BenchClient(SingleClient[] clients) {
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
        long minTime = -1;
        long maxTime = Long.MIN_VALUE;
        long sum = 0;
        long c = 0;
        long errors = 0;

        for (int i = 0; i < clients.length; i++) {
            long time = times[i];
            if (time == -1) {
                System.out.println("There was error with client " + i + "!");
                errors++;
            } else {
                minTime = Math.min(minTime, time);
                maxTime = Math.max(maxTime, time);
                sum += time;
                c++;
            }
        }

        System.out.println("Min/avg/max - " + minTime + "ms/" + sum / c + "ms/" + maxTime + "ms and " + errors + "errors from " + clients.length + "clients!");

        return times;
    }

}
