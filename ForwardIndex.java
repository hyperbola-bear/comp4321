import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.htree.HTree;
import jdbm.helper.FastIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.io.IOException;
import java.io.Serializable;


public class ForwardIndex {
    private RecordManager recman; 
    private HTree hashtable; 

    public ForwardIndex(String recordmanager, String objectname) throws IOException {
        recman = RecordManagerFactory.createRecordManager(recordmanager); 
        long recid = recman.getNamedObject(objectname); 

        if (recid!=0) 
            hashtable = HTree.load(recman,recid); 
        else {
            hashtable = HTree.createInstance(recman); 
            recman.setNamedObject("forwardindex",hashtable.getRecid());
        }
    }

    public void finalize() throws IOException {
        recman.commit();
        recman.close(); 
    }

    public void addEntry(String url, String word, int freq) throws IOException {
        try {
            //key is the document id 
            // Value is the word + frequency 
            ArrayList<Posting> postings = (ArrayList<Posting>)hashtable.get(url);
            if (postings != null) {
                postings.add(new Posting(word, freq));
                hashtable.put(url,postings);
            } else {
                //url does not exist\
                postings = new ArrayList<Posting>();
                postings.add(new Posting(word,freq));
                hashtable.put(url,postings);
            }
            
        } catch (java.io.IOException ex) {
            System.err.println(ex.toString());
        }
    }

    public void deleteEntry(String url) throws IOException {
        try {
            hashtable.remove(url);
        } catch (java.io.IOException ex) {
            System.err.println(ex.toString());
        }
    }

public void printAll() throws IOException {
    try {
        FastIterator iter = hashtable.keys();
        String key;
        while ((key = (String) iter.next()) != null) {
            System.out.println("is not null!");
            ArrayList<Posting> postings = (ArrayList<Posting>) hashtable.get(key);
            System.out.println("Key: " + key);
            for (Posting posting : postings) {
                System.out.println("Word: " + posting.word + ", Frequency: " + posting.freq);
            }
        }
    } catch (java.io.IOException e) {
        System.err.println(e.toString());
    }
}
    
}
