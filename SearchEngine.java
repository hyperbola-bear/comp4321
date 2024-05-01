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


public class SearchEngine
{
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
	long recid;

	public SearchEngine() throws IOException
	{
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
		} catch (IOException e) {
			System.out.println(e);
			System.out.println("Error: A JDBM Database required for search funciton is missing");
		}
	}

	public Vector<Integer> parse(String query) throws IOException {
		// Check for phrases
		String[] tokens = query.split("\"");
		String phrase = "";

		if(tokens.length == 3) {
			phrase = tokens[1];
			query = tokens[0] + tokens[2]; // Remove phrase from query
		}

		Vector<Integer> parsed_query = new Vector<>();

		tokens = query.split(" ");

		for (String token : tokens) {
			try {
				int id = (int)wordToId.get(token);
				parsed_query.add(id);
			} catch (IOException ex) {
				System.err.println(ex);
			}
		}

		return search(parsed_query, phrase);
	}

	private String getPhrase(String query) throws IOException {
		query = " " + query + " ";

		String[] tokens = query.split("\"");
		String phrase = "";

		if(tokens.length == 3) {
			String[] phraseTokens = tokens[1].split(" ");

			for(String phraseToken : phraseTokens) {
				phrase += porter.stripAffixes(phraseToken);
				phrase += " ";

				if(stopWords.contains(phraseToken)) {
					System.out.println(phraseToken);
					phrase = "";
					break;
				}
			}
		}

		return phrase;
	}

	private Vector<Integer> getWords(String query) throws IOException {
		query = " " + query + " ";
		String[] tokens = query.split("\"");

		Vector<Integer> wordIds = new Vector<Integer> ();

		if(tokens.length == 3) {
			query = tokens[0] + tokens[2];
		}

		query = query.stripLeading().stripTrailing();

		String[] wordTokens = query.split(" ");
		
		for(String wordToken : wordTokens) {
			if(!stopWords.contains(wordToken)) {
				String stem = porter.stripAffixes(wordToken);
				wordIds.add((Integer) wordToId.get(stem));
			}
		}

		return wordIds;
	}

	private Vector<Integer> search(Vector<Integer> query, String phrase) throws IOException {
		HashMap<Integer, Double> consolidatedScores = new HashMap<>();

		// Title Constant for prioritizing title matches
		int TITLE_CONSTANT = 4;

		// Find the total number of terms
		int N_doc = 0;
		int N_title = 0;

		FastIterator iterTerms = docInvertedIndex.keys();
		Integer tempId;
		while ((tempId = (Integer) iterTerms.next()) != null) {
			N_doc++;
		}

		iterTerms = titleInvertedIndex.keys();
		while ((tempId = (Integer) iterTerms.next()) != null) {
			N_title++;
		}

		try {
			for (int wordId : query) {
				Vector<Posting> docPostingList = (Vector<Posting>) docInvertedIndex.get(wordId);
				Vector<Posting> titlePostingList = (Vector<Posting>) titleInvertedIndex.get(wordId);

				int DF_doc = docPostingList.size();
				int DF_title = titlePostingList.size();

				for (Posting docPosting : docPostingList) {
					int docId = docPosting.id;
					int TF = docPosting.freq;
					double IDF = Math.log(N_doc / DF_doc) / Math.log(2);

					// Finding doc max term frequency

					int MAX_TF = 1;

					Vector<Posting> postingList = (Vector<Posting>) docForwardIndex.get(docId);

					for (Posting wordPosting : postingList) {
						if (wordPosting.freq > MAX_TF) {
							MAX_TF = wordPosting.freq;
						}
					}

					// Score is derived from (Term Frequency * IDF) / (Doc Max Term Frequency)
					double score = (TF * IDF) / MAX_TF;

					if (consolidatedScores.containsKey(docId)) {
						double currentScore = consolidatedScores.get(docId);
						consolidatedScores.put(wordId, currentScore + score);
					} else {
						consolidatedScores.put(docId, score);
					}
				}

				for (Posting titlePosting : titlePostingList) {
					int docId = titlePosting.id;
					int TF = titlePosting.freq;
					double IDF = Math.log(N_title / DF_title) / Math.log(2);

					// Finding title max term frequency

					int MAX_TF = 1;

					Vector<Posting> postingList = (Vector<Posting>) titleForwardIndex.get(docId);

					for (Posting wordPosting : postingList) {
						if (wordPosting.freq > MAX_TF) {
							MAX_TF = wordPosting.freq;
						}
					}

					// Score is derived from (Term Frequency * IDF) / (Doc Max Term Frequency)
					double score = TITLE_CONSTANT * (TF * IDF) / MAX_TF;

					if (consolidatedScores.containsKey(docId)) {
						double currentScore = consolidatedScores.get(docId);
						consolidatedScores.put(wordId, currentScore + score);
					} else {
						consolidatedScores.put(docId, score);
					}
				}
			}
		} catch (IOException ex) {
			System.err.println(ex);
		}

		Vector<Integer> docIds = new Vector<>();
		Vector<Double> scores = new Vector<>();

		// Sort docIds in ascending order by scores
		for (HashMap.Entry<Integer, Double> entry : consolidatedScores.entrySet()) {
			docIds.add(entry.getKey());
			scores.add(entry.getValue());
		}

		docIds.sort((id1, id2) -> scores.get(id1).compareTo(scores.get(id2)));

		if (docIds.size() <= 50) {
			return docIds;
		} else {
			return new Vector<Integer>(docIds.subList(0, 50));
		}
	}

	public static void main(String[] args) {
		String testInput = "My favourite movie is \"terminator returns\"";

		try {
			SearchEngine searchEngine = new SearchEngine();

			String phrase = searchEngine.getPhrase(testInput);
			Vector<Integer> test = searchEngine.getWords(testInput);

			System.out.println(phrase);

			for(Integer wordId : test) {
				System.out.println(wordId);
			}
		} catch (IOException ex) {
			System.out.println(ex);
		}
	}
}
