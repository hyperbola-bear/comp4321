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
        String docid = args[0];
        Inverter inverter = new Inverter("stopwords.txt");

        //create new Crawler to get the words first
        Crawler crawler = new Crawler(args[0]);
        Vector<String> words = crawler.extractWords();

        //Then, we iterate through the entire string, and remove stop words
        words = inverter.removeStopwords(words);
        //Then, we use porter's algorithm to handle stemming
        words = inverter.stemify(words);
        System.out.println("Words in page "+" (size = "+words.size()+") :");
        for(int i = 0; i < words.size(); i++)
            if(i<5 || i>words.size()-6){
                System.out.println(words.get(i));
            } else if(i==5){
                System.out.println("...");
            }
        System.out.println("\n\n");

        Map<String,Integer> wordFrequencies = new HashMap<String,Integer>(); 

        for (String word: words) {
            if (wordFrequencies.containsKey(word)){
                wordFrequencies.put(word,wordFrequencies.get(word) +1);
            } else {
                wordFrequencies.put(word, 1); 
            }
        }

        //create forwardIndex
        System.out.println("Printing word frequencies for page body");
        //print out word frequencies
        ForwardIndex forwardIndex = new ForwardIndex("fiRM","forwardindex");
        InvertedIndex invertedIndex = new InvertedIndex("iiRM","invertedindex");

        for (Map.Entry<String, Integer> entry: wordFrequencies.entrySet()) {
            String word = entry.getKey();
            int frequency = entry.getValue();
            forwardIndex.addEntry(args[0],word,frequency);
            invertedIndex.addEntry(word, docid, frequency);
            System.out.println("file is: " + args[0] + ", word is: " + word + ", freqeuency is" + frequency);
            System.out.println(entry.getKey() + ": " +entry.getValue());
        }
        
        forwardIndex.finalize();
        invertedIndex.finalize();

        Vector<String> title = crawler.extractTitle();
        title = inverter.removeStopwords(title);
        title = inverter.stemify(title);

        System.out.println("Title in page "+" (size = "+words.size()+") :");
        for(int i = 0; i < title.size(); i++)
            if(i<5 || i>title.size()-6){
                System.out.println(title.get(i));
            } else if(i==5){
                System.out.println("...");
            }
        System.out.println("\n\n");

    
    }
}
