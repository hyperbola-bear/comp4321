import java.util.*;
import IRUtilities.*;
import java.io.*;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Vector;
import java.util.HashMap;
import java.lang.Math;
import jdbm.htree.HTree;
import java.io.Serializable;
import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.helper.FastIterator;
import jdbm.helper.Tuple;
import jdbm.helper.TupleBrowser;

public class DatabaseViewer {
	private Porter porter;
	private static HashSet<String> stopWords;
	private Vector<String> searchTerms;
	private HTree urlToId;
	private HTree idToUrl;
	private HTree wordToId;
	private HTree idToWord;
	private HTree titleForwardIndex;
	private HTree docForwardIndex;
	private HTree titleInvertedIndex;
	private HTree docInvertedIndex;
	private RecordManager recman;
    private HTree bigram;
    private HTree trigram;
	long recid;

    public DatabaseViewer() throws IOException {
    
		try {
			// Create stemmer and stop words
			porter = new Porter();

			stopWords = new HashSet<String>();
			FileReader fs = new FileReader("stopwords.txt");
			BufferedReader bs = new BufferedReader(fs);
			String word = bs.readLine();
			while(word != null){
				stopWords.add(word);
				word = bs.readLine();
			}

			// Load invertedIndexDoc
			recman = RecordManagerFactory.createRecordManager("Database");
			recid = recman.getNamedObject("invertedindex");

			if(recid == 0) {
				throw new IOException ("docInvertedIndex does not exist");
			} else {
				docInvertedIndex = HTree.load(recman, recid);
			}

			// Load forwardIndexDoc
			long recid = recman.getNamedObject("forwardindex");

			if(recid == 0) {
				throw new IOException ("docForwardIndex does not exist");
			} else {
				docForwardIndex = HTree.load(recman, recid);
			}

			// Load invertedIndexTitle
			recid = recman.getNamedObject("titleInvertedindex");

			if(recid == 0) {
				throw new IOException ("titleInvertedIndex does not exist");
			} else {
				titleInvertedIndex = HTree.load(recman, recid);
			}

			// Load forwardIndexTitle
			recid = recman.getNamedObject("titleForwardindex");

			if(recid == 0) {
				throw new IOException ("titleForwardIndex does not exist");
			} else {
				titleForwardIndex = HTree.load(recman, recid);
			}

			// Load docMappings
			recid = recman.getNamedObject("urlToId");

			if(recid == 0) {
				throw new IOException ("urlToId does not exist");
			} else {
				urlToId = HTree.load(recman, recid);
			}

			recid = recman.getNamedObject("idToUrl");

			if(recid == 0) {
				throw new IOException ("idToUrl does not exist");
			} else {
				idToUrl = HTree.load(recman, recid);
			}
//
			// Load wordMappings
			recid = recman.getNamedObject("wordToId");

			if(recid == 0) {
				throw new IOException ("wordToId does not exist");
			} else {
				wordToId = HTree.load(recman, recid);
			}

			recid = recman.getNamedObject("idToWord");

			if(recid == 0) {
				throw new IOException ("idToWord does not exist");
			} else {
				idToWord = HTree.load(recman, recid);
			}

			recid = recman.getNamedObject("bigram");

			if(recid == 0) {
				throw new IOException ("bigram does not exist");
			} else {
				bigram = HTree.load(recman, recid);
			}

			recid = recman.getNamedObject("trigram");

			if(recid == 0) {
				throw new IOException ("trigram does not exist");
			} else {
				trigram = HTree.load(recman, recid);
			}

		} catch (IOException e) {
			System.out.println(e);
			System.out.println("Error: A JDBM Database required for search funciton is missing");
		}
    }

    public void viewTrigrams() throws IOException{
        System.out.println("Viewing trigrams");
        FastIterator iter = trigram.keys(); 
        String key; 
        while ((key = (String) iter.next())!= null) {
            HashSet<Integer> docIds = (HashSet<Integer>) trigram.get(key);
            System.out.println("trigram is: " + key);
            Iterator itr = docIds.iterator();
            while (itr.hasNext()) {
                System.out.println(itr.next());
            }
        }
    }

    public void viewBigrams() throws IOException{
        System.out.println("Viewing bigrams");
        FastIterator iter = bigram.keys(); 
        String key; 
        while ((key = (String) iter.next())!= null) {
            HashSet<Integer> docIds = (HashSet<Integer>) bigram.get(key);
            System.out.println("bigram is: " + key);
            Iterator itr = docIds.iterator();
            while (itr.hasNext()) {
                System.out.println(itr.next());
            }
        }
    }
    public static void main(String[] args) throws IOException {
        DatabaseViewer dbv = new DatabaseViewer(); 
        dbv.viewBigrams();
        dbv.viewTrigrams();

    }


}

