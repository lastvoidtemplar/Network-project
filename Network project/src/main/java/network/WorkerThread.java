package network;

import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;

public class WorkerThread extends Thread {
    private RequestHandler handler;
    int threads;
    private long[] arr;
    private BlockingQueue<Task> queue;
    private HashMap<Integer, Node> parents;
    private Semaphore mutex;

    WorkerThread(int threads, long[] arr, RequestHandler handler, BlockingQueue<Task> queue, HashMap<Integer, Node> map, Semaphore mutex) {
        super();
        this.threads = threads;
        this.handler = handler;
        this.arr = arr;
        this.queue = queue;
        this.parents = map;
        this.mutex = mutex;
    }

    @Override
    public void run() {
        while (true) {
            Task task;
            try {
                task = queue.take();
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
                return;
            }

            System.out.println(task.id +"\t"+task.stage +"\t"+ task.left +"\t"+ task.right);

            if (task.stage == 0) {
                if (task.right - task.left == 1) {
                    try {
                        Merge(task);
                    } catch (InterruptedException e) {
                        System.out.println(e.getMessage());
                        return;
                    }
                } else {
                    try {
                        SpawnChildren(task);
                    } catch (InterruptedException e) {
                        System.out.println(e.getMessage());
                        return;
                    }
                }
            } else if (task.stage == 1){
                    try {
                        Merge(task);
                    } catch (InterruptedException e) {
                        System.out.println(e.getMessage());
                        return;
                    }
            } else {
                return;
            }
        }
    }

    private void merge(int left, int right) {
        int n = right - left;
        if (n == 1) {
            return;
        }

        long[] tmp = new long[n];

        int mid = (left + right) / 2;

        int i = 0;
        int l = left;
        int r = mid;

        while (l < mid && r < right) {
            if (arr[l] < arr[r]) {
                tmp[i] = arr[l];
                l++;
            } else {
                tmp[i] = arr[r];
                r++;
            }
            i++;
        }

        while (l < mid) {
            tmp[i] = arr[l];
            l++;
            i++;
        }

        while (r < right) {
            tmp[i] = arr[r];
            r++;
            i++;
        }

        for (i = 0; i < n; i++) {
            arr[left + i] = tmp[i];
        }
    }

    private void Merge(Task task) throws InterruptedException {
        merge(task.left, task.right);
        mutex.acquire();
        Node parent = parents.get(task.id);
        if (parent.id == -1) {
            handler.ProcessReady();
            for (int i = 0; i < threads; i++) {
                queue.put(new Task(-1, (byte) 2, -1, -1));
            }
            return;
        }
        if (parent.readyChilds == 0) {
            parent.readyChilds++;
        }
        else if (parent.readyChilds == 1) {
            queue.put(new Task(parent.id, (byte) 1, parent.left, parent.right));
            parent.readyChilds++;
        }
        else if (parent.readyChilds>1){
            System.out.println("what" + task.id);
        }
        mutex.release();
    }

    private void SpawnChildren(Task task) throws InterruptedException {
        Integer id1 = IdGenerator.GenerateId();
        Integer id2 = IdGenerator.GenerateId();

        mutex.acquire();

        Node node = new Node(task.id, task.left, task.right);
        parents.put(id1, node);
        parents.put(id2, node);
        mutex.release();

        int mid = (task.left + task.right) / 2;
        queue.put(new Task(id1, (byte) 0, task.left, mid));
        queue.put(new Task(id2, (byte) 0, mid, task.right));
    }
}
