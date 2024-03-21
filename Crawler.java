/* --
COMP4321 Lab2 Exercise
Student Name: Emmanuel Er
Student ID: 21076727
Email: ezwer@connect.ust.hk
*/
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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.io.IOException;

public class Crawler
{
	private String url;
	public Crawler(String _url)
	{
		url = _url;
	}
	public Vector<String> extractWords() throws ParserException

	{

		// extract words in url and return them
		// use StringTokenizer to tokenize the result from StringBean
		// ADD YOUR CODES HERE

        Vector<String> v = new Vector<String>();
        try {
            URL url = new URL(this.url);
            URLConnection uc = url.openConnection(); 

            //get connection via a stringbean
            StringBean sb = new StringBean();
            //sb.setLinks();
            sb.setConnection(uc);
            String stringCollected = sb.getStrings();
            String[] stringVector = stringCollected.split(" |\\s+|\\n",0);
            Collections.addAll(v,stringVector);
            
        } catch (Exception ex) {
            System.err.println(ex.toString());
        
        }

        return v;

	}


    public Vector<String> breadthFirstSearch(int limit) throws ParserException {
        Vector<String> result = new Vector<String>();
        Queue<String> queue = new LinkedList<String>();
        Set<String> visited = new HashSet<String>();

        queue.add(url);
        visited.add(url);

        while (!queue.isEmpty() && result.size() < limit) {
            String currentUrl = queue.poll();
            result.add(currentUrl);

            try {
                URLConnection uc = new URL(currentUrl).openConnection();
                LinkBean lb = new LinkBean();
                lb.setConnection(uc);
                String[] links = Arrays.stream(lb.getLinks()).map(URL::toString).toArray(String[]::new);

                for (String link : links) {
                    if (!visited.contains(link)) {
                        queue.add(link);
                        visited.add(link);
                    }
                }
            } catch (Exception ex) {
                System.err.println(ex.toString());
            }
        }

        return result;
    }


    public Vector<String> extractTitle() throws IOException{
        Document doc = Jsoup.connect(this.url).get();
        String[] title = doc.title().split("\\s+");
        Vector<String> v = new Vector<String>();
        Collections.addAll(v,title);
        return v;
    }

	public Vector<String> extractLinks() throws ParserException

	{
		// extract links in url and return them
		// ADD YOUR CODES HERE
        Vector<String> v = new Vector<String>();
        try {
            URL url = new URL(this.url);
            URLConnection uc = url.openConnection();

            //Use LinkBean
            LinkBean lb = new LinkBean(); 
            lb.setConnection(uc);
            
            String[] links = Arrays.stream(lb.getLinks()).map(URL::toString).toArray(String[]::new);
            Collections.addAll(v,links);
        } catch (Exception ex) {
            System.err.println(ex.toString());
        }
	    
        return v; 
	}
	
	public static void main (String[] args)
	{
		try
		{
			Crawler crawler = new Crawler(args[0]);


			Vector<String> words = crawler.extractWords();		
			
			System.out.println("Words in "+crawler.url+" (size = "+words.size()+") :");
			for(int i = 0; i < words.size(); i++)
				if(i<5 || i>words.size()-6){
					System.out.println(words.get(i));
				} else if(i==5){
					System.out.println("...");
				}
			System.out.println("\n\n");
			

            	
			Vector<String> links = crawler.extractLinks();
			System.out.println("Links in "+crawler.url+":");
			for(int i = 0; i < links.size(); i++)		
				System.out.println(links.get(i));
			System.out.println("");


            int limit = 30; // Specify the number of links to retrieve
            Vector<String> bfsLinks = crawler.breadthFirstSearch(limit);
            System.out.println("BFS Links in " + crawler.url + " (limit = " + limit + "):");
            for (String bfsLink : bfsLinks) {
                System.out.println(bfsLink);
            }            
			
		}
		catch (ParserException e)
            	{
                	e.printStackTrace ();
            	}

	}
}

	
