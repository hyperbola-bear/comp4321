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


    /*private RecordManager wordMappingrecman; 
    private HTree wordToId; 
    private HTree idToWord; 
    */
    private WordMapping wordMap; 
    
    public ForwardIndex(String recordmanager, String objectname) throws IOException {
        recman = RecordManagerFactory.createRecordManager(recordmanager); 
        long recid = recman.getNamedObject(objectname); 

        //wordMappingrecman = RecordManagerFactory.createRecordManager("wordMapping");
        //long ID_wordToId = wordMappingrecman.getNamedObject("wordToId");
        //long ID_idToWord = wordMappingrecman.getNamedObject("idToWord");     

        if (recid!=0) 
            hashtable = HTree.load(recman,recid); 
        else {
            hashtable = HTree.createInstance(recman); 
            recman.setNamedObject("forwardindex",hashtable.getRecid());
        }
        wordMap = new WordMapping("WordMapping");

        /*
        if (ID_wordToId && ID_idToWord) {
            wordToId = HTree.load(wordMappingrecman,ID_wordToId);
            idToWord = HTree.load(wordMappingrecman, ID_idToWord);
        } else {
            wordToId = HTree.createInstance(wordMappingrecman);
            idToWord = HTree.createInstance(wordMappingrecman); 
            wordMappingrecman.setNamedObject("wordToID",wordToId.getRecid());
            wordMappingrecman.setNamedObject("idToWord",wordToId.getRecid());
        }
        */
        
    }

    public void finalize() throws IOException {
        recman.commit();
        recman.close(); 
    }

    public void addEntry(String url, String word, int freq) throws IOException {
        try {
            //key is the document id 
            // Value is the word + frequency
            //
            int wordId = wordMap.getId(word);
                
            ArrayList<Posting> postings = (ArrayList<Posting>)hashtable.get(url);
            if (postings != null) {
                postings.add(new Posting(wordId, freq));
                hashtable.put(url,postings);
            } else {
                //url does not exist\
                postings = new ArrayList<Posting>();
                postings.add(new Posting(wordId,freq));
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
                System.out.println("Word: " + wordMap.getWord(posting.id) + ", Frequency: " + posting.freq);
            }
        }
    } catch (java.io.IOException e) {
        System.err.println(e.toString());
    }
}
    
}
