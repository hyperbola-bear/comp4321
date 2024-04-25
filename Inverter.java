import IRUtilities.*;
import java.io.*;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.*;
import java.util.Vector;
import org.htmlparser.beans.StringBean;
import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.filters.AndFilter;
import org.htmlparser.filters.NodeClassFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import java.util.StringTokenizer;
import org.htmlparser.beans.LinkBean;
import java.net.URL;
import java.net.URLConnection;

//hashmap for counting frequencies
import java.util.HashMap;
import java.util.Map;



public class Inverter {

    private Porter porter;
    private HashSet<String> stopWords; 
    public boolean isStopWord(String str) {
        return stopWords.contains(str); 
    }
    private DocMapping docMap;
    private WordMapping wordMap;
    private ForwardIndex forwardindex;
    private InvertedIndex invertedindex; 
    private Metadata metadata; 


	//to Handle the Title
	private TitleForwardIndex titleforwardindex;
	private TitleInvertedIndex titleinvertedindex; 

    

    //constructor
    public Inverter(String str) throws IOException {
        super();    //not sure why this is here
        porter = new Porter(); 
        stopWords = new HashSet<String>(); 
        FileReader fs = new FileReader(str); 
        BufferedReader bs = new BufferedReader(fs);
        String word = bs.readLine();
        while(word != null){
            stopWords.add(word);
            word = bs.readLine();
        }

        this.docMap = new DocMapping();
        this.wordMap = new WordMapping(); 
        this.forwardindex = new ForwardIndex(); 
        this.invertedindex = new InvertedIndex();
		this.titleforwardindex = new TitleForwardIndex();
		this.titleinvertedindex = new TitleInvertedIndex(); 
        this.metadata = new Metadata();

    }

    public Vector<String> removeStopwords(Vector<String> words) {
        Vector<String> stopwordsRemoved = new Vector<>();

        // Iterate over the vector and add non-stopwords to the new vector
        for (String word : words) {
            if (!this.isStopWord(word)) {
                stopwordsRemoved.add(word);
            }
        }
        return stopwordsRemoved;
    }

    public Vector<String> stemify(Vector<String> words) {
        Vector<String> stemified = new Vector<>();
        // Iterate over the vector and add non-stopwords to the new vector
        for (String word : words) {
            stemified.add(this.stem(word));
        }
        return stemified;
    }

    public String stem(String str) {
        return porter.stripAffixes(str); 
    }
    
    public static void main(String[] args) throws IOException,ParserException {
        String rootlink = args[0];
        Inverter inverter = new Inverter("stopwords.txt");

        //create new Crawler to get the words first
        Crawler crawler = new Crawler();
        Vector<String> links = crawler.getThreeHundredLinks(rootlink);

        int docCount = 0; 
        int wordCount = 0; 
        //START
        for (String link: links) {
			//check if the page alreadt exists in the database
			Container container = inverter.metadata.getMeta(link); 
			if (container != null) {
				long lastModifiedDB = inverter.metadata.getMeta(link).getLastModificationDate();
				long lastModifiedWeb = crawler.getLastModificationDate(link);

				//compare the last modification date
				if (lastModifiedWeb <= lastModifiedDB) {
					//skip crawling
					System.out.println("Latest version of page already in database");
					continue;
				}
			}
			


            Vector<String> childLinks = crawler.extractLinks(link); 
            Vector<String> title = crawler.extractTitle(link);
            long lastModificationDate = crawler.getLastModificationDate(link);
            int pageSize = crawler.getPageSize(link);
            inverter.metadata.addEntry(link, childLinks,pageSize,lastModificationDate,title);


            System.out.println("now processing: " + link);
            inverter.docMap.addMapping(docCount,link);
            docCount++;
          
            //to count the words in that link
            Map<String, Integer> wordFrequencies = new HashMap<String,Integer>(); 
            Vector<String> words = inverter.stemify(inverter.removeStopwords(crawler.extractWords(link)));
            for (String word: words) {
                if ((word == "") || (word) == null) {
                    continue;
                }
                if (inverter.wordMap.getId(word) == null) {
                    inverter.wordMap.addMapping(wordCount, word);
                    wordCount++;
                }
                if (wordFrequencies.containsKey(word)) {
                    wordFrequencies.put(word,wordFrequencies.get(word) + 1); 
                } else {
                    wordFrequencies.put(word,1);
                }
            }
            //we now have wordFrequencies for that link
            for (Map.Entry<String, Integer> entry: wordFrequencies.entrySet()) {
                String word = entry.getKey();
                int wordId = inverter.wordMap.getId(word);
                int docId = inverter.docMap.getId(link);
                int frequency = entry.getValue();
                inverter.forwardindex.addEntry(docId,wordId,frequency);
                inverter.invertedindex.addEntry(docId,wordId,frequency);
            }

			//handles Titles almost identically to above
            Map<String, Integer> titleWordFrequencies = new HashMap<String,Integer>(); 
            Vector<String> titlewords = inverter.stemify(inverter.removeStopwords(crawler.extractTitle(link)));
            for (String word: titlewords) {
                if ((word == "") || (word) == null) {
                    continue;
                }
                if (inverter.wordMap.getId(word) == null) {
                    inverter.wordMap.addMapping(wordCount, word);
                    wordCount++;
                }
                if (titleWordFrequencies.containsKey(word)) {
                    titleWordFrequencies.put(word,wordFrequencies.get(word) + 1); 
                } else {
                    titleWordFrequencies.put(word,1);
                }
            }
            //we now have titleWordFrequencies for that link
            for (Map.Entry<String, Integer> entry: titleWordFrequencies.entrySet()) {
                String word = entry.getKey();
                int wordId = inverter.wordMap.getId(word);
                int docId = inverter.docMap.getId(link);
                int frequency = entry.getValue();
                inverter.titleforwardindex.addEntry(docId,wordId,frequency);
                inverter.titleinvertedindex.addEntry(docId,wordId,frequency);
            }

        }

        inverter.forwardindex.finalize();
        inverter.invertedindex.finalize();
        inverter.titleforwardindex.finalize();
        inverter.titleinvertedindex.finalize();
        inverter.wordMap.finalize();
        inverter.docMap.finalize();
        inverter.metadata.finalize();


    }
}
