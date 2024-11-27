package network;

public class WorkerPool {
    private final RequestHandler handler;
    int threads;
    long[] arr;

    WorkerPool(int threads, long[] arr, RequestHandler handler){
        this.handler = handler;
        this.threads = threads;
        this.arr = arr;
    }


    void Run(){
        new Th
    }
}
