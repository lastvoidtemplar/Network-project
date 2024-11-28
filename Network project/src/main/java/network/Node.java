package network;

public class Node {
    public Integer id;
    public int left;
    public int right;
    public byte readyChildren;

    Node(Integer id, int left, int right){
        this.id = id;
        this.left = left;
        this.right = right;
        readyChildren = 0;
    }
}
