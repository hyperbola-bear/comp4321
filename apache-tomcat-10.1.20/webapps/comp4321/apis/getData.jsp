<%@ page language="java" contentType="application/json; charset=UTF-8"
    pageEncoding="UTF-8"%>
    
<%@ page import="jdbm.RecordManager" %>
<%@ page import="jdbm.RecordManagerFactory" %>
<%@ page import="jdbm.htree.HTree" %>
<%@ page import="jdbm.helper.FastIterator" %>
<%@ page import="java.io.*" %>
<%@ page import="java.util.*" %>
<%@ page import="IRUtilities.*" %>
<%@ page import="PROJECT.*"%>
<%@ page import="java.io.Serializable"%>
<%@ page import="java.util.Vector"%>

<%! 
    boolean isPhraseSearch(String input) {
        return input.contains("\"");
    }
%>

<%
    if (isPhraseSearch(request.getParameter("input"))) {
        out.print("Phrase Search initiated");
    }

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


    //handle database here
    String dbPath = getServletContext().getRealPath("/WEB-INF/database/Database");

    RecordManager recman = RecordManagerFactory.createRecordManager(dbPath);


    long invertedindexid = recman.getNamedObject("invertedindex");
    HTree invertedindex = HTree.load(recman, invertedindexid);

    long forwardindexid = recman.getNamedObject("forwardindex");
    HTree forwardindex = HTree.load(recman, forwardindexid);

    long titleInvertedindexid = recman.getNamedObject("titleInvertedindex");
    HTree titleInvertedindex = HTree.load(recman, titleInvertedindexid);

    long titleForwardindexid = recman.getNamedObject("titleForwardindex");
    HTree titleForwardindex = HTree.load(recman, titleForwardindexid);

    long urlToIdid = recman.getNamedObject("urlToId");
    HTree urlToId = HTree.load(recman, urlToIdid);

    long idToUrlid = recman.getNamedObject("idToUrl");
    HTree idToUrl = HTree.load(recman, idToUrlid);

    long wordToIdid = recman.getNamedObject("wordToId");
    HTree wordToId = HTree.load(recman, wordToIdid);

    long idToWordid = recman.getNamedObject("idToWord");
    HTree idToWord = HTree.load(recman, idToWordid);

    long bigramid = recman.getNamedObject("bigram");
    HTree bigram = HTree.load(recman, bigramid);

    long trigramid = recman.getNamedObject("trigram");
    HTree trigram = HTree.load(recman, trigramid);

    

    //Posting posting = new Posting(1,1);
    //out.println(posting);
    //out.println(invertedindex);
    //out.println(invertedindex.keys());

    SearchEngine se = new SearchEngine(
    new Porter(), 
    stopWords,
    urlToId,
    wordToId,
    idToWord,
    titleForwardindex,
    forwardindex,
    titleInvertedindex,
    invertedindex,
    bigram,
    trigram,
    recman,
    idToWordid);

    Vector<Vector<Object>> result = se.query(input);
    StringBuilder sb = new StringBuilder();
    sb.append("{\"input\":");
    sb.append("\"" + input + "\",");
    sb.append("\"results\":[");
    for (int i = 0; i < result.size(); i++) {
        sb.append("{");
        sb.append("\"url\":");
        sb.append("\"" +String.valueOf(idToUrl.get(result.get(i).get(0)) + "\","));
        sb.append("\"score\":");
        sb.append(result.get(i).get(1));
        //sb.append(",");
        sb.append("}");
        if (i != result.size() - 1) {
            sb.append(",");
        }
    }
    sb.append("]}");
    //response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    response.setContentType("text/html;charset=UTF-8");
    //out.println(sb.toString());
    response.getWriter().write(sb.toString());
%>


