package PROJECT;
import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.htree.HTree;
import jdbm.helper.FastIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.io.IOException;
import java.io.Serializable;


public class Metadata {
    public RecordManager recman; 
    public HTree hashtable; 

    public Metadata(RecordManager recman) throws IOException {
        this.recman = recman;
        long recid = recman.getNamedObject("metadata");

        if (recid!=0 ) {
            hashtable = HTree.load(recman,recid);
        } else {
            hashtable = HTree.createInstance(recman);
            recman.setNamedObject("metadata", hashtable.getRecid());
        }
    }

//    public void finalize() throws IOException {
//        recman.commit();
//        recman.close();
//    }
    
    public void addEntry(String url, Vector<String> childLinks, int pageSize, long lastModificationDate, Vector<String> title) throws IOException { 
        Container container = new Container(childLinks,pageSize,lastModificationDate,title);
        hashtable.put(url, container); 
    }

    public void deleteEntry(String url) { 
        try {
            hashtable.remove(url); 
        } catch (java.io.IOException ex) {
            System.err.println(ex.toString());
        }
    }

    public Container getMeta(String url) throws IOException {
        Container container = (Container) hashtable.get(url);
        if (container != null) {
            return container; 
        } else {
            System.err.println("Meta data does not exist for " + url);
            return null;
        }

    }

}

