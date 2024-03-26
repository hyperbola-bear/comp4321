import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.htree.HTree;
import jdbm.helper.FastIterator;
import java.util.Vector;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.lang.Exception;

class Posting implements Serializable {
	public int id;
	public int freq;

	Posting(int id, int freq) {
		this.id = id;
		this.freq = freq;
	}
}

public class WordMapping {
	private RecordManager recman;
	private HTree wordToId;
	private HTree idToWord;

	WordMapping(String recordmanager) throws IOException{
		try {
			recman = RecordManagerFactory.createRecordManager(recordmanager);
			long ID_wordToId = recman.getNamedObject("wordToId");
			long ID_idToWord = recman.getNamedObject("idToWord");

			if ((ID_wordToId != 0) && (ID_idToWord != 0)) {
				// If mappings already exist, load mappings
				wordToId = HTree.load(recman, ID_wordToId);
				idToWord = HTree.load(recman, ID_idToWord);
			} else if ((ID_wordToId == 0) && (ID_idToWord == 0)) {
				// If mappings don't exist, create mappings
				wordToId = HTree.createInstance(recman);
				idToWord = HTree.createInstance(recman);

				recman.setNamedObject("wordToId", wordToId.getRecid());
				recman.setNamedObject("idToWord", idToWord.getRecid());
			} else {
				// If only one mapping exists, throw Exception
				throw new IOException("Doc Mapping corrupted: one of the mapping hashtables do not exist");
			}
		} catch(java.io.IOException ex) {
			System.err.println(ex);
		}
	}


	public void finalize() throws IOException {
		recman.commit();
		recman.close();
	}

	public void addMapping(int wordID, String word) throws IOException {
		try {
			// Check if mapping already exists
			if ((idToWord.get(wordID) == null) && (wordToId.get(word) == null)) {
				idToWord.put(wordID, word);
				wordToId.put(word, wordID);
				System.out.println("Successfully inserted:\n" + "WORD: " + word);
			} else if ((idToWord.get(wordID) == null) && (wordToId.get(word) == null)) {
				System.out.println("WORD already found");
			} else {
				// If mapping only exists for one hashtable, throw exception
				throw new IOException("Doc Mapping corrupted: mapping only exists on one hashtable");
			}
		} catch (java.io.IOException ex) {
			System.err.println(ex.toString());
		}
	}

	public void removeMapping(int wordID) throws IOException {
		try {
			// Check if wordID exists
			if(idToWord.get(wordID) == null) {
				System.out.println("Deletion failed: wordID " + String.valueOf(wordID) + " does not exist");
				return;
			}

			String word = (String) idToWord.get(wordID);

			idToWord.remove(wordID);
			wordToId.remove(word);

			System.out.println("Successfully removed");
			System.out.println("Doc ID: " + String.valueOf(wordID));
			System.out.println("Word: " + word);
		} catch (java.io.IOException ex) {
			System.err.println(ex.toString());
		}
	}

	public String getWord(int wordID) throws IOException {
		try {
			String result = (String) idToWord.get(wordID);
			return result;
		} catch (java.io.IOException ex) {
			System.err.println(ex.toString());
		}

		return "";
	}

	public Integer getId(String word) throws IOException {
		try{
			Integer result = (Integer) wordToId.get(word);
			return result;
		} catch (java.io.IOException ex) {
			System.err.println(ex.toString());
		}

		return -1;
	}

	public void printAll() throws IOException {
		try{
			System.out.println("WORD TO ID MAPPINGS:");

			FastIterator iterWord = wordToId.keys();
			String word;
			while ((word = (String) iterWord.next()) != null) {
				System.out.println(word + "\t-->\t" + String.valueOf(wordToId.get(word)));
			}

			System.out.println("---------------------");
			System.out.println("ID TO WORD MAPPINGS:");

			FastIterator iterId = idToWord.keys();
			Integer id;
			while ((id = (Integer) iterId.next()) != null) {
				System.out.println(String.valueOf(id) + "\t-->\t" + idToWord.get(id));
			}
		} catch (java.io.IOException ex) {
			System.err.println(ex);
		}
	}
	public static void main(String[] args)
	{
		try
		{
			WordMapping wordMapping = new WordMapping("WordMapping");
			wordMapping.addMapping(0, "apple");
			wordMapping.addMapping(1, "chocolate");
			wordMapping.addMapping(2, "hamster");

			System.out.println("First print:");
			wordMapping.printAll();

			wordMapping.removeMapping(0);

			System.out.println("Second print:");
			wordMapping.printAll();
			wordMapping.finalize();
		}
		catch(IOException ex)
		{
			System.err.println(ex.toString());
		}

	}
}
