<%@ page language="java" contentType="application/json; charset=UTF-8"
    pageEncoding="UTF-8"%>
    
<%@ page import="IRUtilities.*" %>
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


    // here is the part for database
    String dbPath = getServletContext().getRealPath("/WEB-INF/database/database");

    RecordManager recman = RecordManagerFactory.createRecordManager(dbPath);

    long PageInfoID = recman.getNamedObject("PageInfo");
    HTree PageInfo = HTree.load(recman, PageInfoID);

    long WordMappingID = recman.getNamedObject("WordMapping");
    HTree WordMapping = HTree.load(recman, WordMappingID);

    long InvertedBodyWordID = recman.getNamedObject("InvertedBodyWord");
    HTree InvertedBodyWord = HTree.load(recman, InvertedBodyWordID);

    long InvertedTitleWordID = recman.getNamedObject("InvertedTitleWord");
    HTree InvertedTitleWord = HTree.load(recman, InvertedTitleWordID);

    long PageChildID = recman.getNamedObject("PageChild");
    HTree PageChild = HTree.load(recman, PageChildID);

    long BodyWordMappingID = recman.getNamedObject("BodyWordMapping");
    HTree BodyWordMapping = HTree.load(recman, BodyWordMappingID);

    long PageParentID = recman.getNamedObject("PageParent");
    HTree PageParent = HTree.load(recman, PageParentID);


    // get the list of word id from the n-grams and store them as Map<Integer, Double> for the query where the key is the word id and the value is 1.0
    Map<Integer, Double> query = new HashMap<>();
    for (String ngram : ngrams) {
        String wordID = (String) WordMapping.get(ngram);
        if (wordID != null) {
            query.put(Integer.parseInt(wordID), 1.0);
        }
    }

    // get the list of indexed pages from the word id. in the format of Map<Integer, Map<Integer, Double>> where the key is the page id and the value is another map where the key is the word id and the value is the tf-idf score
    Map<Integer, Map<Integer, Double>> indexedBody = new HashMap<>();
    Map<Integer, Double> indexedBodyScore = new HashMap<>();
    for (Map.Entry<Integer, Double> entry : query.entrySet()) {
        String wordID = entry.getKey().toString();
        String value = (String) InvertedBodyWord.get(wordID);
        if (value != null) {
            String [] values = value.split(",");
            for (String v: values){
                String[] temp = v.split("\\|");
                // temp[0] = page id, temp[2] = tf-idf score
                int pageID = Integer.parseInt(temp[0]);
                double score = Double.parseDouble(temp[2]);
                if (indexedBody.containsKey(pageID)){
                    indexedBody.get(pageID).put(entry.getKey(), score);
                } else {
                    Map<Integer, Double> tempMap = new HashMap<>();
                    tempMap.put(entry.getKey(), score);
                    indexedBody.put(pageID, tempMap);
                }
            }
        }
    }

    // get the list of indexed pages from the word id. in the format of Map<Integer, Map<Integer, Double>> where the key is the page id and the value is another map where the key is the word id and the value is the tf-idf score
    Map<Integer, Map<Integer, Double>> indexedTitle = new HashMap<>();
    for (Map.Entry<Integer, Double> entry : query.entrySet()) {
        String wordID = entry.getKey().toString();
        String value = (String) InvertedTitleWord.get(wordID);
        if (value != null) {
            String [] values = value.split(",");
            for (String v: values){
                String[] temp = v.split("\\|");
                // temp[0] = page id, temp[2] = tf-idf score
                int pageID = Integer.parseInt(temp[0]);
                double score = Double.parseDouble(temp[2]);
                if (indexedTitle.containsKey(pageID)){
                    indexedTitle.get(pageID).put(entry.getKey(), score);
                } else {
                    Map<Integer, Double> tempMap = new HashMap<>();
                    tempMap.put(entry.getKey(), score);
                    indexedTitle.put(pageID, tempMap);
                }
            }
        }
    }

    // calculate the cosine similarity between the query and the indexed pages (both body and title)
    Map<Integer, Double> cosineSimilarityBody = new HashMap<>();
    for (Map.Entry<Integer, Map<Integer, Double>> entryBody : indexedBody.entrySet()) {
        int pageID = entryBody.getKey();
        Map<Integer, Double> indexedPage = entryBody.getValue();
        double dotProduct = 0.0;
        double queryMagnitude = 0.0;    
        double indexedPageMagnitude = Double.parseDouble(((String) PageInfo.get(Integer.toString(pageID))).split("\\|")[5]);
        for (Map.Entry<Integer, Double> q : query.entrySet()) {
            int wordID = q.getKey();
            double queryScore = q.getValue();
            double indexedPageScore = indexedPage.getOrDefault(wordID, 0.0);
            dotProduct += queryScore * indexedPageScore;
            queryMagnitude += queryScore * queryScore;
        }
        queryMagnitude = Math.sqrt(queryMagnitude);
        double similarity = dotProduct / (queryMagnitude * indexedPageMagnitude);

        //double similarity = dotProduct;
        //double similarity = indexedPageMagnitude;

        cosineSimilarityBody.put(pageID, similarity);
    }

    Map<Integer, Double> cosineSimilarityTitle = new HashMap<>();
    for (Map.Entry<Integer, Map<Integer, Double>> entryTitle : indexedTitle.entrySet()) {
        int pageID = entryTitle.getKey();
        Map<Integer, Double> indexedPage = entryTitle.getValue();
        double dotProduct = 0.0;
        double queryMagnitude = 0.0;
        double indexedPageMagnitude = Double.parseDouble(((String) PageInfo.get(Integer.toString(pageID))).split("\\|")[5]);
        for (Map.Entry<Integer, Double> q : query.entrySet()) {
            int wordID = q.getKey();
            double queryScore = q.getValue();
            double indexedPageScore = indexedPage.getOrDefault(wordID, 0.0);
            dotProduct += queryScore * indexedPageScore;
            queryMagnitude += queryScore * queryScore;
        }
        queryMagnitude = Math.sqrt(queryMagnitude);
        double similarity = dotProduct / (queryMagnitude * indexedPageMagnitude);
        cosineSimilarityTitle.put(pageID, similarity);
    }

    // combine the cosine similarity of the body and title by adding them together with a weighting of 0.7 for body and 0.3 for title
    Map<Integer, Double> cosineSimilarity = new HashMap<>();
    double bodyPara = 0.7;
    double titlePara = 0.3;
    for (Map.Entry<Integer, Double> entry : cosineSimilarityBody.entrySet()) {
        int pageID = entry.getKey();
        double bodySimilarity = entry.getValue();
        double titleSimilarity = cosineSimilarityTitle.getOrDefault(pageID, 0.0);
        double similarity = bodyPara * bodySimilarity + titlePara * titleSimilarity;
        cosineSimilarity.put(pageID, similarity);
    }

    // sort the cosine similarity in descending order
    List<Map.Entry<Integer, Double>> sortedCosineSimilarity = new ArrayList<>(cosineSimilarity.entrySet());
    sortedCosineSimilarity.sort((a, b) -> b.getValue().compareTo(a.getValue()));

    // get the top 50 page info from PageInfo
    int count = 0;
    Map<Integer, String> PageInfos = new HashMap<>();
    for (Map.Entry<Integer, Double> entry : sortedCosineSimilarity) {
        int pageID = entry.getKey();
        String value = (String) PageInfo.get(Integer.toString(pageID));
        if (value != null) {
            PageInfos.put(pageID, value);
            count++;
            if (count == 50) {
                break;
            }
        }
    }

    // output the top 50 pages in JSON format
    StringBuilder json = new StringBuilder();
    json.append("{");

    // the json output will have two parts: sorted pages and details of each page. the sorted pages will be sorted by the cosine similarity score and the details of each page will include the title, url, last modification date, size of the page, top 5 key words, child pages (title and url) and the similarity score

    int count_page = 0;

    json.append("\"sortedPages\": [");

    // output the sorted pages (pageID only)
    for (Map.Entry<Integer, Double> entry : sortedCosineSimilarity) {
        count_page++;
        if (count_page > 50) {
            break;
        }
        json.append(entry.getKey() + ",");
    }

    // Remove the last comma
    if (sortedCosineSimilarity.size() > 0) {
        json.setLength(json.length() - 1);
    }

    json.append("],");


    // if there is no result from the search, return an empty JSON object
    if (PageInfos.size() == 0) {
        json.append("\"pages\": {}");
        json.append("}");
        out.print(json.toString());
        return;
    }

    json.append("\"pages\": {");

    // the output will use information from different databases (PageInfo, PageChild, BodyWordMapping)
    for (Map.Entry<Integer, String> entry : PageInfos.entrySet()) {
        String pageID = entry.getKey().toString();

        // get the key words id and frequency from BodyWordMapping
        String PageWordValue = (String) BodyWordMapping.get(pageID);
        String[] PageWordValues = PageWordValue.split(",");
        Map<String, Integer> keyWords = new HashMap<>();
        for (String v: PageWordValues){
            String[] temp = v.split("\\|");
            if (temp.length < 3){
                continue;
            }
            String keyWord = temp[2];
            keyWords.put(keyWord, Integer.parseInt(temp[1]));
        }
        // sort the key words in descending order and we get the top 5 key words
        List<Map.Entry<String, Integer>> sortedKeyWords = new ArrayList<>(keyWords.entrySet());
        sortedKeyWords.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        List<String> top5KeyWords = new ArrayList<>();
        for (int i=0; i<5; i++){
            if (i < sortedKeyWords.size()){
                top5KeyWords.add(sortedKeyWords.get(i).getKey());
            }
        }

        // get the child pages from PageChild (it is the page id of the child pages separated by comma)
        String PageChildValue = (String) PageChild.get(pageID);
        // get the title and url of each child page
        Map<Integer, Map<String, String>> childPages = new HashMap<>();
        if (PageChildValue != null){
            String[] childPageIDs = PageChildValue.split(",");
            for (String childPageID: childPageIDs){
                String childPageInfo = (String) PageInfo.get(childPageID);
                if (childPageInfo != null){
                    String[] temp = childPageInfo.split("\\|");
                    Map<String, String> tempMap = new HashMap<>();
                    tempMap.put("title", temp[0]);
                    tempMap.put("url", temp[1]);
                    childPages.put(Integer.parseInt(childPageID), tempMap);
                }
            }
        }

        // get the title pages from PageParent (it is the page id of the parent page separated by comma)
        String PageParentValue = (String) PageParent.get(pageID);
        // get the title and url of each parent page
        Map<Integer, Map<String, String>> parentPages = new HashMap<>();
        if (PageParentValue != null){
            String[] parentPageIDs = PageParentValue.split(",");
            for (String parentPageID: parentPageIDs){
                String parentPageInfo = (String) PageInfo.get(parentPageID);
                if (parentPageInfo != null){
                    String[] temp = parentPageInfo.split("\\|");
                    Map<String, String> tempMap = new HashMap<>();
                    tempMap.put("title", temp[0]);
                    tempMap.put("url", temp[1]);
                    parentPages.put(Integer.parseInt(parentPageID), tempMap);
                }
            }
        }
        
        // get the title, url, last modification date and the size of page of the page
        String PageInfoValue = entry.getValue();
        String[] PageInfoValues = PageInfoValue.split("\\|");
        String title = PageInfoValues[0];
        String url = PageInfoValues[1];
        String lastModificationDate = PageInfoValues[2];
        String size = PageInfoValues[3];


        // output the information in JSON format
        json.append("\"" + pageID + "\": {");
        json.append("\"title\": \"" + title + "\",");
        json.append("\"url\": \"" + url + "\",");
        json.append("\"lastModificationDate\": \"" + lastModificationDate + "\",");
        json.append("\"size\": \"" + size + "\",");

        // add the similarity score to the JSON output
        json.append("\"similarity\": " + cosineSimilarity.get(Integer.parseInt(pageID)) + ",");

        // output the top 5 key words (words and their frequency)
        json.append("\"keyWords\": {");
        for (String keyWord: top5KeyWords){
            json.append("\"" + keyWord + "\": " + keyWords.get(keyWord) + ",");
        }
        // Remove the last comma
        if (top5KeyWords.size() > 0) {
            json.setLength(json.length() - 1);
        }
        json.append("},");

        // output the parent pages (title and url)
        json.append("\"parentPages\": {");
        for (Map.Entry<Integer, Map<String, String>> parentPage: parentPages.entrySet()){
            int parentPageID = parentPage.getKey();
            Map<String, String> parentPageInfo = parentPage.getValue();
            json.append("\"" + parentPageID + "\": {");
            json.append("\"title\": \"" + parentPageInfo.get("title") + "\",");
            json.append("\"url\": \"" + parentPageInfo.get("url") + "\"");
            json.append("},");
        }
        // Remove the last comma
        if (parentPages.size() > 0) {
            json.setLength(json.length() - 1);
        }
        json.append("},");

        // output the child pages (title and url)
        json.append("\"childPages\": {");
        for (Map.Entry<Integer, Map<String, String>> childPage: childPages.entrySet()){
            int childPageID = childPage.getKey();
            Map<String, String> childPageInfo = childPage.getValue();
            json.append("\"" + childPageID + "\": {");
            json.append("\"title\": \"" + childPageInfo.get("title") + "\",");
            json.append("\"url\": \"" + childPageInfo.get("url") + "\"");
            json.append("},");
        }
        // Remove the last comma
        if (childPages.size() > 0) {
            json.setLength(json.length() - 1);
        }
        json.append("}");
        json.append("},");

        
    }

    // Remove the last comma
    if (json.length() > 1) {
        json.setLength(json.length() - 1);
    }

    json.append("}}");

    // Set response content type to JSON
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");

    // Write JSON data to response
    out.print(json.toString());
%>


