import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.htree.HTree;
import jdbm.helper.FastIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashSet;


public class NgramIndex {
    public RecordManager recman;
    public HTree bigram;
    public HTree trigram; 

    public NgramIndex(RecordManager recman) throws IOException {
        this.recman = recman;
        long bigram_recid = recman.getNamedObject("bigram"); 
        long trigram_recid = recman.getNamedObject("trigram");
        
        if (bigram_recid!=0) {
            bigram = HTree.load(recman,bigram_recid);
        } else {
            bigram = HTree.createInstance(recman); 
            recman.setNamedObject("bigram",bigram.getRecid());
        }

        if (trigram_recid!=0) {
            trigram = HTree.load(recman,trigram_recid);
        } else {
            trigram = HTree.createInstance(recman); 
            recman.setNamedObject("trigram",trigram.getRecid());
        }
    }

//    public void finalize() throws IOException {
//        recman.commit();
//        recman.close();
//    }

    public void addBigramEntry(String key,int docId) throws IOException {
        try {
            HashSet<Integer> docs = (HashSet<Integer>)bigram.get(key);
            if (docs != null) {
                docs.add(docId); 
                bigram.put(key,docs); 
            } else {
                docs = new HashSet<Integer>();
                docs.add(docId);
                bigram.put(key,docs);
            }
        } catch (java.io.IOException ex){ 
            System.err.println(ex.toString());
        }
    }

    public void addTrigramEntry(String key,int docId) throws IOException {
        try {
            HashSet<Integer> docs = (HashSet<Integer>)trigram.get(key);
            if (docs != null) {
                docs.add(docId); 
                trigram.put(key,docs); 
            } else {
                docs = new HashSet<Integer>();
                docs.add(docId);
                trigram.put(key,docs);
            }
        } catch (java.io.IOException ex){ 
            System.err.println(ex.toString());
        }
    }

    public void deleteBigramEntry(Vector<String> bigrams, int docId) throws IOException {
        try {
            HashSet<Integer> docs = null;
            for (int i=0; i<bigrams.size();i++) {
                String key = bigrams.get(i); 
                docs = (HashSet<Integer>)bigram.get(key);
                if (docs != null) {
                    docs.remove(docId);
                } else {
                    continue; 
                }
            }
            
        } catch (java.io.IOException ex) {
            System.err.println(ex.toString());
        }
    }

    public void deleteTrigramEntry(Vector<String> trigrams, int docId) throws IOException {
        try {
            HashSet<Integer> docs = null;
            for (int i=0; i<trigrams.size();i++) {
                String key = trigrams.get(i); 
                docs = (HashSet<Integer>)trigram.get(key);
                if (docs != null) {
                    docs.remove(docId);
                } else {
                    continue; 
                }
            }
            
        } catch (java.io.IOException ex) {
            System.err.println(ex.toString());
        }
    }
}
