import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.htree.HTree;
import jdbm.helper.FastIterator;
import java.util.Vector;
import java.io.IOException;
import java.io.Serializable;


public class InvertedIndex
{
	public RecordManager recman;
    public HTree hashtable;

	public InvertedIndex(RecordManager recman) throws IOException
	{
		this.recman = recman;
		long recid = recman.getNamedObject("invertedindex");
			
		if (recid != 0)
			hashtable = HTree.load(recman, recid);
		else
		{
			hashtable = HTree.createInstance(recman);
			recman.setNamedObject("invertedindex", hashtable.getRecid());
		}
	}


//	public void finalize() throws IOException
//	{
//		recman.commit();
//		recman.close();
//	}

	public void addEntry(int docID, int wordID, int freq) throws IOException
	{
		// Create new posting object
		Posting newPosting = new Posting(docID, freq);

		try {
			// Cast object type to Vector
			Vector<Posting> curPostingList = (Vector<Posting>) hashtable.get(wordID);

			// Check if wordID already exists in hashtable
			if(curPostingList != null) {
				// Check if docID already exists in Vector
				for(int i = 0; i < curPostingList.size(); i++) {
					// If posting is identical, do nothing
					if(curPostingList.get(i).id == docID && curPostingList.get(i).freq == freq) {
						return;
					}

					// If posting is different, update posting
					else if(curPostingList.get(i).id == docID && curPostingList.get(i).freq != freq) {
						curPostingList.get(i).freq = freq;
						return;
					}
				}

				// Otherwise add new posting to Vector
				curPostingList.add(newPosting);

				// Update posting
				hashtable.put(wordID, curPostingList);
			} else {
				// Initialize Vector with new posting and add to hashtable
				Vector<Posting> postingList = new Vector<Posting>();
				postingList.add(newPosting);
				hashtable.put(wordID, postingList);
			}
		} catch (java.io.IOException ex) {
			System.err.println(ex.toString());
		}
	}
	public void delEntry(Vector<Integer> words, int docID) throws IOException
	{
		// Vector<String> words --> Contains the list of words that are in the doc being deleted
		try {
			for (int i = 0; i < words.size(); i++) {
				// Only search targeted posting lists
				Vector<Posting> postingList = (Vector<Posting>) hashtable.get(words.get(i));
				for (int j = 0; j < postingList.size(); j++) {
					// Check if docID matches posting
					if(postingList.get(j).id == docID) {
						postingList.remove(j);

						// All docID's are unique in a posting list
						break;
					}
				}
			}
		} catch (java.io.IOException ex) {
			System.err.println(ex.toString());
		}
	}

	public void printAll() throws IOException
	{
		try {
			FastIterator iter = hashtable.keys();

			Integer key;

			while ((key = (Integer) iter.next()) != null) {
				// Print info
				Vector<Posting> postingList = (Vector<Posting>) hashtable.get(key);
				System.out.println("Posting list for wordID: " + String.valueOf(key));
				for(int i = 0; i < postingList.size(); i++) {
					System.out.println("docID: " + String.valueOf(postingList.get(i).id) +
							"\tfreq: " + String.valueOf(postingList.get(i).freq));
				}
				System.out.println("--------------------------");
			}
		} catch (java.io.IOException ex) {
			System.err.println(ex.toString());
		}
	}	
	
//	public static void main(String[] args)
//	{
//		try
//		{
//			InvertedIndex index = new InvertedIndex();
//
//			// Adding entries for doc0
//			index.addEntry(0, 0, 30);
//			index.addEntry(0, 1, 20);
//			index.addEntry(0, 2, 40);
//
//			// Adding entries for doc1
//			index.addEntry(1, 0, 35);
//			index.addEntry(1, 1, 23);
//			index.addEntry(1, 4, 40);
//
//			// Adding entries for doc2
//			index.addEntry(2, 2, 30);
//			index.addEntry(2, 4, 20);
//			index.addEntry(2, 3, 40);
//
//			System.out.println("First Print:");
//			index.printAll();
//
//			// Deletion test for doc0
//			Vector<Integer> wordList = new Vector<>();
//			wordList.add(0);
//			wordList.add(1);
//			wordList.add(2);
//
//			index.delEntry(wordList, 0);
//
//			System.out.println("Second Print:");
//			index.printAll();
//
//			index.finalize();
//		}
//		catch(IOException ex)
//		{
//			System.err.println(ex.toString());
//		}
//	}
}
