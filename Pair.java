package PROJECT;
import java.io.Serializable;

public class Pair implements Serializable {
    public int docId;
    public double score;
    public Pair(int docId, double score) {
        this.docId = docId;
        this.score = score;
    }
}

