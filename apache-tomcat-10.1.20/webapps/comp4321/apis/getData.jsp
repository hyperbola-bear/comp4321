<%@ page language="java" contentType="application/json; charset=UTF-8"
    pageEncoding="UTF-8"%>
    
<%@ page import="IRUtilities.*, SearchEngine.*" %>
<%@ page import="jdbm.RecordManager" %>
<%@ page import="jdbm.RecordManagerFactory" %>
<%@ page import="jdbm.htree.HTree" %>
<%@ page import="jdbm.helper.FastIterator" %>
<%@ page import="java.io.*" %>
<%@ page import="java.util.*" %>


<%
    String input = request.getParameter("input").trim();
    
    // case to handle null or empty input
    if (input == null || input.isEmpty()) {
        // add the JSON output for empty input (sortedPages and pages)
        out.print("{\"sortedPages\":[],\"pages\":{}}");
        return;
    }
    SearchEngine se = new SearchEngine();
    out.println(input);
    %>
    <!--%
    Vector<Pair> result = new Vector();
    result = se.query(input);
    for (Pair p : result) {
        out.println(p.docId + " " + p.score);
        
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
%>