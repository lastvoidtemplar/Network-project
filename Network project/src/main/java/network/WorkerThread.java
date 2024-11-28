package network;

import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;


public class WorkerThread extends Thread {
    private final int threshold = 32;
    private final int id;
    private final RequestHandler handler;
    private final int threads;
    private final long[] arr;
    private final BlockingQueue<Task> queue;
    private final HashMap<Integer, Node> parents;
    private final Semaphore mutex;

    WorkerThread(int id, int threads, long[] arr, RequestHandler handler, BlockingQueue<Task> queue, HashMap<Integer, Node> map, Semaphore mutex) {
        super();
        this.id = id;
        this.threads = threads;
        this.handler = handler;
        this.arr = arr;
        this.queue = queue;
        this.parents = map;
        this.mutex = mutex;
    }

    @Override
    public void run() {
        int merges = 0;
        while (true) {
            Task task;
            try {
                task = queue.take();
            } catch (InterruptedException e) {
                Log("Queue take error: " + e.getMessage());
                return;
            }

            if (task.stage == 0) {
                if (task.right - task.left <= threshold) {
                    try {
                        Insertion(task);
                    } catch (InterruptedException e) {
                        Log("Error processing single-element array: " + e.getMessage());
                        return;
                    }
                } else {
                    try {
                        SpawnChildren(task);
                    } catch (InterruptedException e) {
                        Log("Error splitting array into two arrays: " + e.getMessage());
                        return;
                    }
                }
            } else if (task.stage == 1) {
                try {
                    Merge(task);
                    merges++;
                } catch (InterruptedException e) {
                    Log("Error merging arrays: " + e.getMessage());
                    return;
                }
            } else {
                Log("Shutdown task is receive. This thread made " + merges + " merges. The thread is closed");
                return;
            }
        }
    }

    private void insertion(int left, int right) {
        for (int i = left + 1; i < right; i++) {
            long tmp = arr[i];
            int j = i - 1;
            while (j >= left && tmp < arr[j]) {
                arr[j + 1] = arr[j];
                j--;
            }
            arr[j + 1] = tmp;
        }
    }

    private void merge(int left, int right) {
        int n = right - left;

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

    private void Insertion(Task task) throws InterruptedException {
        insertion(task.left, task.right);
        SpawnMerge(task);
    }

    private void Merge(Task task) throws InterruptedException {
        merge(task.left, task.right);
        SpawnMerge(task);
    }

    private void SpawnMerge(Task task) throws InterruptedException {
        mutex.acquire();
        Node parent = parents.get(task.id);
        mutex.release();
        parent.mutex.acquire();
        if (parent.id == -1) {
            Log("The work is complete. Sending shutdown task to every thread");
            handler.ProcessReady();
            for (int i = 0; i < threads; i++) {
                queue.put(new Task(-1, (byte) 2, -1, -1));
            }
            return;
        }
        if (parent.readyChildren != 0) {
            queue.put(new Task(parent.id, (byte) 1, parent.left, parent.right));
        }
        parent.readyChildren++;
        parent.mutex.release();
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

    private void Log(String message) {
        System.out.println("[Connection " + handler.GetId() + "] [Thread " + id + "] " + message);
    }
}
