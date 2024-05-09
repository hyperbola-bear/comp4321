package PROJECT;
import java.io.Serializable;
public class Posting implements Serializable {
    public int id;
    public int freq; 
    public Posting(int id, int freq) {
        this.id = id;
        this.freq = freq; 
    }

    public String toString() {
        return "Posting: (" + this.id + "," + this.freq + ")"; 
    }
}

