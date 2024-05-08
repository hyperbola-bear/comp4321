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

public class TitleForwardIndex {
    public RecordManager recman; 
    public HTree hashtable; 

    
    public TitleForwardIndex(RecordManager recman) throws IOException {
        this.recman = recman;
        long recid = recman.getNamedObject("titleForwardindex"); 


        if (recid!=0) 
            hashtable = HTree.load(recman,recid); 
        else {
            hashtable = HTree.createInstance(recman); 
            recman.setNamedObject("titleForwardindex",hashtable.getRecid());
        }

        
        
    }

//    public void finalize() throws IOException {
//        recman.commit();
//        recman.close();
//    }

    public void addEntry(int docId, int wordId, int freq) throws IOException {
        try {
            //key is the document id 
            // Value is the word + frequency
            //
                
            Vector<Posting> postings = (Vector<Posting>)hashtable.get(docId);
            if (postings != null) {
                postings.add(new Posting(wordId, freq));
                hashtable.put(docId,postings);
            } else {
                //url does not exist\
                postings = new Vector<Posting>();
                postings.add(new Posting(wordId,freq));
                hashtable.put(docId,postings);
            }
            
        } catch (java.io.IOException ex) {
            System.err.println(ex.toString());
        }
    }

    public void deleteEntry(int docId) throws IOException {
        try {
            hashtable.remove(docId);
        } catch (java.io.IOException ex) {
            System.err.println(ex.toString());
        }
    }
    public void printAll() throws IOException {
        try {
            FastIterator iter = hashtable.keys();
            Integer key;
            while ((key = (Integer)iter.next()) != null) {
                Vector<Posting> postings = (Vector<Posting>) hashtable.get(key);
                System.out.println("docid: " + key);
                for (Posting posting : postings) {
                    System.out.println("wordId: " + posting.id + ", Frequency: " + posting.freq);
                }
            }
        } catch (java.io.IOException e) {
            System.err.println(e.toString());
        }
    }
}
