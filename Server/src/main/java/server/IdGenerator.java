package server;

import java.util.concurrent.Semaphore;

public class IdGenerator {
    private static final Semaphore mut = new Semaphore(1);
    private static Integer id = 0;

    public static Integer GenerateId(){
        try{
            mut.acquire();
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
            return -1;
        }

        Integer res = id;
        id++;

        mut.release();

        return  res;
    }
}
