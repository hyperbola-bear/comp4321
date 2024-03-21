import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.helper.FastIterator;
import jdbm.htree.HTree;

import java.util.Map;

public class Reader {

    public static void main(String[] args) {

        try {
        
            ForwardIndex forwardindex = new ForwardIndex("fiRM", "forwardindex");
            forwardindex.printAll();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
