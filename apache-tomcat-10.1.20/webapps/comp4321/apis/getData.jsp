<%@ page language="java" contentType="application/json; charset=UTF-8"
    pageEncoding="UTF-8"%>
    
<%@ page import="jdbm.RecordManager" %>
<%@ page import="jdbm.RecordManagerFactory" %>
<%@ page import="jdbm.htree.HTree" %>
<%@ page import="jdbm.helper.FastIterator" %>
<%@ page import="java.io.*" %>
<%@ page import="java.util.*" %>
<%@ page import="IRUtilities.*" %>
<%@ page import="TEST.*"%>
<%@ page import="PROJECT.*"%>

<%
    String input = request.getParameter("input").trim();
    
    // case to handle null or empty input
    if (input == null || input.isEmpty()) {
        // add the JSON output for empty input (sortedPages and pages)
        out.print("{\"sortedPages\":[],\"pages\":{}}");
        return;
    }

    //Here is the part used for StopStem
    String stopWord = getServletContext().getRealPath("/WEB-INF/stopwords.txt");
    HashSet<String> stopWords = new HashSet<String>();
    Porter porter = new Porter();
    BufferedReader in = new BufferedReader(new FileReader(stopWord));
    String line;
    while ((line = in.readLine()) != null) {
        stopWords.add(line);
    }
    in.close();

    // Extract n-grams from the list of words
    List<String> words = Arrays.asList(input.split("\\s+"));
    List<String> one_gram = new ArrayList<String>();
    List<String> two_gram = new ArrayList<String>();
    List<String> three_gram = new ArrayList<String>();

    for (int i=0; i < words.size(); i++) {
        String ngram = "";
        // 1-gram
        for (int j=0; j<1; j++) {
            String word = words.get(i+j).toLowerCase();
            if (stopWords.contains(word)) {
                ngram = "";
                break;
            }
            ngram += porter.stripAffixes(word) + " ";
        }
        if (ngram != ""){
            one_gram.add(ngram.trim());
        }
        // 2-gram
        ngram = "";
        if (i < words.size()-1){
            for (int j=0; j<2; j++) {
                String word = words.get(i+j).toLowerCase();
                if (stopWords.contains(word)) {
                    ngram = "";
                    break;
                }
                ngram += porter.stripAffixes(word) + " ";
            }
            if (ngram != ""){
                two_gram.add(ngram.trim());
            }
        }
        // 3-gram
        ngram = "";
        if (i < words.size()-2){
            for (int j=0; j<3; j++) {
                String word = words.get(i+j).toLowerCase();
                if (stopWords.contains(word)) {
                    ngram = "";
                    break;
                }
                ngram += porter.stripAffixes(word) + " ";
            }
            if (ngram != ""){
                three_gram.add(ngram.trim());
            }
        }
    };

    // create a list of all the n-grams
    List<String> ngrams = new ArrayList<String>();
    ngrams.addAll(one_gram);
    ngrams.addAll(two_gram);
    ngrams.addAll(three_gram);

    TEST test = new TEST();
    SearchEngine se = new SearchEngine();





%>


