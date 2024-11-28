package network;

import java.util.concurrent.Semaphore;

public class Node {
    public Integer id;
    public int left;
    public int right;
    public byte readyChildren;
    public final Semaphore mutex;

    Node(Integer id, int left, int right){
        this.id = id;
        this.left = left;
        this.right = right;
        this.readyChildren = 0;
        this.mutex = new Semaphore(1);
    }
}
