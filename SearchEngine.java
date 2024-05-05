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
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.lang.Math;
import jdbm.htree.HTree;
import java.io.Serializable;
import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.helper.FastIterator;
import org.Pair;


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

	private HTree bigram;

	private HTree trigram;
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

			// Load ngrams
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

		Set<Integer> tempSet = new HashSet<>();
		
		for(String wordToken : wordTokens) {
			if(!stopWords.contains(wordToken)) {
				String stem = porter.stripAffixes(wordToken);
				tempSet.add((Integer) wordToId.get(stem));
			}
		}

		// Remove repeat ID values
		for(Integer id : tempSet) {
			wordIds.add(id);
		}

		return wordIds;
	}

	private int getMaxTfDoc(int wordId, int docId) throws IOException {
		Vector<Posting> wordPostingVector = (Vector<Posting>) docForwardIndex.get(docId);

		int result = 1;

		for(Posting wordPosting : wordPostingVector) {
			if(wordPosting.freq > result) {
				result = wordPosting.freq;
			}
		}

		return result;
	}

	private int getMaxTfTitle(int wordId, int docId) throws IOException {
		Vector<Posting> wordPostingVector = (Vector<Posting>) titleForwardIndex.get(docId);

		int result = 1;

		for(Posting wordPosting : wordPostingVector) {
			if(wordPosting.freq > result) {
				result = wordPosting.freq;
			}
		}

		return result;
	}

	private double getTermWeightDoc(int wordId, int docId, int TF, int N) throws IOException {
		Vector<Posting> docPostingList = (Vector<Posting>) docInvertedIndex.get(wordId);
		Vector<Posting> wordPostingList = (Vector<Posting>) docForwardIndex.get(wordId);

		// Score is derived from (Term Frequency * IDF) / (Doc Max Term Frequency)
		int N_hasTerm = docPostingList.size();
		double IDF = Math.log(N/N_hasTerm) / Math.log(2);
		int TF_max = getMaxTfDoc(wordId, docId);

		double score = (TF * IDF) / (TF_max);

		return score;
	}

	private double getTermWeightTitle(int wordId, int docId, int TF, int N) throws IOException {
		Vector<Posting> docPostingList = (Vector<Posting>) titleInvertedIndex.get(wordId);
		Vector<Posting> wordPostingList = (Vector<Posting>) titleForwardIndex.get(wordId);

		// Score is derived from (Term Frequency * IDF) / (Doc Max Term Frequency)
		int N_hasTerm = docPostingList.size();
		double IDF = Math.log(N/N_hasTerm) / Math.log(2);
		int TF_max = getMaxTfTitle(wordId, docId);

		double score = (TF * IDF) / (TF_max);

		return score;
	}

	private double getDocMagnitude(int docId, int N) throws IOException {
		Vector<Posting> wordPostingVector = (Vector<Posting>) docForwardIndex.get(docId);

		double magnitude = 0;

		for(Posting wordPosting : wordPostingVector) {
			magnitude += Math.pow(getTermWeightDoc(wordPosting.id, docId, wordPosting.freq, N), 2);
		}

		magnitude = Math.sqrt(magnitude);

		return magnitude;
	}

	private double getTitleMagnitude(int docId, int N) throws IOException {
		Vector<Posting> wordPostingVector = (Vector<Posting>) titleForwardIndex.get(docId);

		double magnitude = 0;

		for(Posting wordPosting : wordPostingVector) {
			magnitude += Math.pow(getTermWeightTitle(wordPosting.id, docId, wordPosting.freq, N), 2);
		}

		magnitude = Math.sqrt(magnitude);

		return magnitude;
	}

	private Vector<Pair> search(Vector<Integer> query, String phrase) throws IOException {
		// DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG
		System.out.println("Query is: ");

		for(Integer id : query) {
			if(id == null) {
				continue;
			}
			System.out.print(idToWord.get(id) + "\t");
		}

		System.out.println("\nPhrase is:");
		System.out.println(phrase);
		// DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG

		if(phrase != "") {
			String[] tokens = phrase.split(" ");
			int phraseLength = tokens.length;
			HashSet<Integer> matchingDocs;

			if(phraseLength == 2) {
				matchingDocs = (HashSet<Integer>) bigram.get(phrase);
			} else if(phraseLength == 3) {
				matchingDocs = (HashSet<Integer>) trigram.get(phrase);
			} else {
				throw new IOException("Invalid query length");
			}

			// Find number of unique terms in matching title and docs
		} else {
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
				for(Integer wordId : query) {
					if(wordId == null) {
						continue;
					}

					Vector<Posting> docPostingVector = (Vector<Posting>) docInvertedIndex.get(wordId);
					Vector<Posting> titlePostingVector = (Vector<Posting>) titleInvertedIndex.get(wordId);

					if(docPostingVector != null) {
						for (Posting docPosting : docPostingVector) {
							int docId = docPosting.id;
							int freq = docPosting.freq;
							double termWeight = getTermWeightDoc(wordId, docId, freq, N_doc);
							double docMagnitude = getDocMagnitude(docId, N_doc);

							termWeight = termWeight / docMagnitude;

							// DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG

//							if(termWeight > 0) {
//								System.out.println("MATCH FOUND IN DOC");
//								System.out.println("URL: " + idToUrl.get(docId));
//								System.out.println("TERM: " + idToWord.get(wordId));
//							}

							// DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG

							if (consolidatedScores.containsKey(docId)) {
								double currentScore = consolidatedScores.get(docId);
								consolidatedScores.put(docId, currentScore + termWeight);
							} else {
								consolidatedScores.put(docId, termWeight);
							}
						}
					}

					if(titlePostingVector != null) {
						for (Posting titlePosting : titlePostingVector) {
							int docId = titlePosting.id;
							int freq = titlePosting.freq;
							double termWeight = getTermWeightTitle(wordId, docId, freq, N_doc);
							double titleMagnitude = getTitleMagnitude(docId, N_doc);

							termWeight = TITLE_CONSTANT * termWeight / titleMagnitude;

							// DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG

//							if(termWeight > 0) {
//								System.out.println("MATCH FOUND IN TITLE");
//								System.out.println("URL: " + idToUrl.get(docId));
//								System.out.println("TERM: " + idToWord.get(wordId));
//							}

							// DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG DEBUG

							if (consolidatedScores.containsKey(docId)) {
								double currentScore = consolidatedScores.get(docId);
								consolidatedScores.put(docId, currentScore + termWeight);
							} else {
								consolidatedScores.put(docId, termWeight);
							}
						}
					}
				}
			} catch (IOException ex) {
				System.err.println(ex);
			}

			Set<Integer> keys = consolidatedScores.keySet();

			Vector<Pair> results = new Vector<>();

			double queryMagnitude = Math.sqrt(query.size()); // Required for calculating cosine similarity

			for(Integer key : keys) {
				Pair pair = new Pair(key, consolidatedScores.get(key) / queryMagnitude);
				results.add(pair);
			}

			System.out.println(results.size());

			Collections.sort(results, new Comparator<Pair>() {
				@Override
				public int compare(Pair p1, Pair p2) {
					double diff = p1.score - p2.score;
					if (diff < 0) {
						return 1;
					} else if (diff > 0) {
						return -1;
					} else {
						return 0;
					}
				}
			});

			return results;
		}

		Vector<Pair> results = new Vector<>();
		return results;
	}

	public Vector<Pair> query(String query) throws IOException {
		// Check for phrases
		query = " " + query + " ";
		String[] tokens = query.split("\"");
		String phrase = "";

		if(tokens.length == 3) {
			phrase = tokens[1];
			query = tokens[0] + tokens[2]; // Remove phrase from query
		}
		
		Vector<Integer> parsed_query = getWords(query);

		return search(parsed_query, phrase);
	}

	public static void main(String[] args) {
		String testInputPhrase = "My favourite movie is \"terminator returns\"";
		String testInput = "information retrieval techniques CNN News";

		try {
			SearchEngine searchEngine = new SearchEngine();

			Vector<Pair> results = searchEngine.query(testInput);
			System.out.println("Query: " + testInput);

			for(Pair pair : results) {
				System.out.println("Doc ID: " + String.valueOf(pair.docId));
				System.out.println("URL: " + searchEngine.idToUrl.get(pair.docId));
				System.out.println("Doc Score: " + String.valueOf(pair.score));
			}
			
		} catch (IOException ex) {
			System.out.println(ex);
		}
	}
}
