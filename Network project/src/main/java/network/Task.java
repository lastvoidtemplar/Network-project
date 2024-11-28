package network;

// Data class so properties
public class Task {
    public Integer id;
    public byte stage;
    public int left;
    public int right;

    Task(Integer id, byte stage, int left, int right){
        this.id = id;
        this.stage = stage;
        this.left = left;
        this.right = right;
    }
}
