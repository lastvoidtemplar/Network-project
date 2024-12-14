package client;

import java.util.concurrent.BlockingQueue;

public class ClientThread extends Thread {
    private final BlockingQueue<Long> queue;
    private final SingleClient client;
    private final long[] times;
    private final int id;
    ClientThread(int id, BlockingQueue<Long> queue, SingleClient client, long[] times){
        this.id = id;
        this.queue = queue;
        this.client = client;
        this.times = times;
    }

    @Override
    public void run() {
        long time = client.Run();
        times[id] = time;
        try{
            queue.put(time);
        } catch (InterruptedException e){
            System.out.println("Problem with the queue! Restart the program! Deadlock will happen!");
        }
    }
}
