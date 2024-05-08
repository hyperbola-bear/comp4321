<%@ page language="java" contentType="application/json; charset=UTF-8"
    pageEncoding="UTF-8"%>
    
<%@ page import="IRUtilities.*" %>
<%@ page import="Posting.*" %>
<%@ page import="jdbm.RecordManager" %>
<%@ page import="jdbm.RecordManagerFactory" %>
<%@ page import="jdbm.htree.HTree" %>
<%@ page import="jdbm.helper.FastIterator" %>
<%@ page import="java.io.*" %>
<%@ page import="java.lang.*" %>
<%@ page import="java.util.*" %>

<%
	
	//read the stop words - massive overhead see if can Remove - can try session cookie
	HashSet<String> stopWords = new HashSet<String>();
	FileReader fs = new FileReader("stopwords.txt");
	BufferedReader bs = new BufferedReader(fs);
	String word = bs.readLine();
	while(word != null) {
		stopWords.add(word);
		word = bs.readLine();
	}
	// read in database
    Porter porter = new Porter();
	String dbPath = getServletContext().getRealPath("/WEB-INF/database/Database");
	RecordManager recman = RecordManagerFactory.createRecordManager(dbPath);
	long recid = recman.getNamedObject("invertedindex");
	HTree docInvertedIndex = HTree.load(recman, recid);
	recid = recman.getNamedObject("forwardindex");
	HTree docForwardIndex = HTree.load(recman, recid);
	recid = recman.getNamedObject("titleInvertedindex");
	HTree titleInvertedIndex = HTree.load(recman, recid);
	recid = recman.getNamedObject("titleForwardindex");
	HTree titleForwardIndex = HTree.load(recman, recid);
	recid = recman.getNamedObject("urlToId");
	HTree urlToId = HTree.load(recman, recid);
	recid = recman.getNamedObject("idToUrl");
	HTree idToUrl = HTree.load(recman, recid);
	recid = recman.getNamedObject("wordToId");
	HTree wordToId = HTree.load(recman, recid);
	recid = recman.getNamedObject("idToWord");
	HTree idToWord = HTree.load(recman, recid);
	//ecid = recman.getNamedObject("bigram");
	//HTree bigram = HTree.load(recman, recid);
	//recid = recman.getNamedObject("trigram");
	//HTree trigram = HTree.load(recman, recid);
    String input = request.getParameter("input").trim();
    //query function
    String query = " " + input + " ";
    String[] tokens = input.split("\"");
    String phrase;
    if(tokens.length == 3) {
        // getphrase function
        String gpquery = " " + query + " ";

		String[] gptokens = gpquery.split("\"");
		String gpphrase = "";

		if(gptokens.length == 3) {
			String[] gpphraseTokens = gptokens[1].split(" ");

			for(String gpphraseToken : gpphraseTokens) {
				gpphrase += porter.stripAffixes(gpphraseToken);
				gpphrase += " ";

				if(stopWords.contains(gpphraseToken)) {
					gpphrase = "";
					break;
				}
			}
		}

		phrase = gpphrase.stripTrailing(); // return phrase from getphrase function
        query = tokens[0] + tokens[2]; // Remove phrase from query
    } else {
        phrase = "";
    }
    
    //Vector<Integer> parsed_query = getWory);
    // get word function
    Vector<Integer> parsed_query = new Vector<Integer>();
        String gwquery = " " + query + " ";
		String[] gwtokens = gwquery.split("\"");

		Vector<Integer> wordIds = new Vector<Integer> ();

		if(tokens.length == 3) {
			gwquery = gwtokens[0] + gwtokens[2];
		}

		gwquery = gwquery.stripLeading().stripTrailing();

		String[] gwwordTokens = gwquery.split(" ");

		Set<Integer> gwtempSet = new HashSet<>();
		
		for(String gwwordToken : gwwordTokens) {
			if(!stopWords.contains(gwwordToken)) {
				String gwstem = porter.stripAffixes(gwwordToken);
				gwtempSet.add((Integer) wordToId.get(gwstem));
			}
		}

		// Remove repeat ID values
		for(Integer id : gwtempSet) {
			wordIds.add(id);
		}

        parsed_query = wordIds; // return wordIds from get word function

    //return search(parsed_query, phrase);
    // start search function
    System.out.println("Query is: ");

		for(Integer id : parsed_query) {
			if(id == null) {
				continue;
			}
			System.out.print(idToWord.get(id) + "\t");
		}

		System.out.println("\nPhrase is:");
		System.out.println(phrase);

		if(phrase == "") {
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
				for(Integer wordId : parsed_query) {
					if(wordId == null) {
						continue;
					}
					Vector<Posting> docPostingVector = (Vector<Posting>) docInvertedIndex.get(wordId);
					Vector<Posting> titlePostingVector = (Vector<Posting>) titleInvertedIndex.get(wordId);
					if(docPostingVector != null) {
						for (Posting docPosting : docPostingVector) {
							int docId = docPosting.id;
							int freq = docPosting.freq;
							//double termWeight = getTermWeightDoc(wordId, docId, freq, N_doc);
							// start getTermWeightDoc(wordId, docId, TF, N)
							Vector<Posting> gtwddocPostingList = (Vector<Posting>) titleInvertedIndex.get(wordId);
							Vector<Posting> gtwdwordPostingList = (Vector<Posting>) titleForwardIndex.get(wordId);
						
								// Score is derived from (Term Frequency * IDF) / (Doc Max Term Frequency)
								int N_hasTerm = gtwddocPostingList.size();
								double IDF = Math.log(N_doc/N_hasTerm) / Math.log(2);
								//int TF_max = getMaxTfTitle(wordId, docId);
								// start getMaxTfTitle function
								Vector<Posting> gmttwordPostingVector = (Vector<Posting>) titleForwardIndex.get(docId);

									int gmttresult = 1;
							
									for(Posting wordPosting : gmttwordPostingVector) {
										if(wordPosting.freq > gmttresult) {
											gmttresult = wordPosting.freq;
										}
									}
									int gmttTF_max = gmttresult; // return result
							
						
								double gtwdscore = (freq * IDF) / (gmttTF_max);
								// end getMaxTfTitle function
								double termWeight = gtwdscore;
		
							//double docMagnitude = getDocMagnitude(docId, N_doc);
							// start getDocMagnitude function
							Vector<Posting> gdmwordPostingVector = (Vector<Posting>) docForwardIndex.get(docId);

								double magnitude = 0;
						
								for(Posting wordPosting : gdmwordPostingVector) {
									//magnitude += Math.pow(getTermWeightDoc(wordPosting.id, docId, wordPosting.freq, N), 2);
									// start getTermWeightDoc(wordId, docId, TF, N)
									Vector<Posting> gtwd2docPostingList = (Vector<Posting>) docInvertedIndex.get(wordPosting.id);
									Vector<Posting> gtwd2wordPostingList = (Vector<Posting>) docForwardIndex.get(wordPosting.id);
							
									int N_hasTerm2 = gtwd2docPostingList.size();
									double IDF2 = Math.log(N_doc/N_hasTerm2) / Math.log(2);
									//int TF_max = getMaxTfDoc(wordPosting.id, docId);
									// start getMaxTfDoc function
									Vector<Posting> gmtdwordPostingVector = (Vector<Posting>) docForwardIndex.get(wordPosting.id);

										int gmtdresult = 1;
								
										for(Posting gmtdwordPosting : gmtdwordPostingVector) {
											if(gmtdwordPosting.freq > gmtdresult) {
												gmtdresult = gmtdwordPosting.freq;
											}
										}
										int gmtdTF_max = gmtdresult; //return result;
										//return result;
							
									double gdmscore = (wordPosting.freq * IDF2) / (gmtdTF_max);
									
									magnitude += Math.pow(gdmscore, 2);
								}
						
								magnitude = Math.sqrt(magnitude);
						
							double docMagnitude = magnitude;
							
							termWeight = termWeight / docMagnitude;

	
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
							//double termWeight = getTermWeightTitle(wordId, docId, freq, N_doc);
							// start getTermWeightTitle(wordID, docId, TF, N)
				
							Vector<Posting> gtwtdocPostingList = (Vector<Posting>) titleInvertedIndex.get(wordId);
							Vector<Posting> gtwtwordPostingList = (Vector<Posting>) titleForwardIndex.get(wordId);
					
							int N_hasTerm = gtwtdocPostingList.size();
							double IDF = Math.log(N_doc/N_hasTerm) / Math.log(2);

							//int TF_max = getMaxTfTitle(wordId, docId);
							// start getMaxTfTitle function
							Vector<Posting> gmtt2wordPostingVector = (Vector<Posting>) titleForwardIndex.get(docId);

								int gmtt2result = 1;
						
								for(Posting wordPosting : gmtt2wordPostingVector) {
									if(wordPosting.freq > gmtt2result) {
										gmtt2result = wordPosting.freq;
									}
								}
						
								//return result;
								int gtwtTF_max = gmtt2result; // return
							// end getMaxTfTitle function
					
							double score = (freq * IDF) / (gtwtTF_max);
					
							double termWeight2 = score; // return
							//double titleMagnitude = getTitleMagnitude(docId, N_doc);
							// start getTitleMagnitude(docId, N)
							Vector<Posting> gtmwordPostingVector = (Vector<Posting>) titleForwardIndex.get(docId);
							double gtmmagnitude = 0;

							for(Posting wordPosting : gtmwordPostingVector) {
								//magnitude += Math.pow(getTermWeightTitle(wordPosting.id, docId, wordPosting.freq, N), 2);
								// start getTermWeightTitle(wordID, docId, TF, N)
								Vector<Posting> gtwt2docPostingList = (Vector<Posting>) titleInvertedIndex.get(wordPosting.id);
								Vector<Posting> gtwt2wordPostingList = (Vector<Posting>) titleForwardIndex.get(wordPosting.id);

							
								int N_hasTerm2 = gtwt2docPostingList.size();
								double IDF2 = Math.log(N_doc/N_hasTerm2) / Math.log(2);
								//int gtwt2TF_max = getMaxTfTitle(wordPosting.id, docId);
								//start getMaxTfTitle(wordId, docId)
								Vector<Posting> gmtt3wordPostingVector = (Vector<Posting>) titleForwardIndex.get(docId);

									int gmtt3result = 1;
							
									for(Posting gmtt3wordPosting : gmtt3wordPostingVector) {
										if(gmtt3wordPosting.freq > gmtt3result) {
											gmtt3result = gmtt3wordPosting.freq;
										}
									}
							
									//return result;
									int gtwt2TF_max = gmtt3result; // return

								double score2 = (wordPosting.freq * IDF2) / (gtwt2TF_max);

								//return score;
								gtmmagnitude += Math.pow(score2, 2);
							}

							gtmmagnitude = Math.sqrt(gtmmagnitude);

							//return magnitude;

							termWeight2 = TITLE_CONSTANT * termWeight2 / gtmmagnitude;

							if (consolidatedScores.containsKey(docId)) {
								double currentScore = consolidatedScores.get(docId);
								consolidatedScores.put(docId, currentScore + termWeight2);
							} else {
								consolidatedScores.put(docId, termWeight2);
							}
						}
					}
				}
			} catch (IOException ex) {
				System.err.println(ex);
			}
			Set<Integer> keys = consolidatedScores.keySet();

			Vector<Vector<Object>> results = new Vector<Vector<Object>>();

			double queryMagnitude = Math.sqrt(parsed_query.size()); // Required for calculating cosine similarity

			for(Integer key : keys) {
				Vector pair = new Vector<>();
				pair.add(key);
				pair.add(consolidatedScores.get(key) / queryMagnitude);
				results.add(pair);
			}

			System.out.println(results.size());

			Collections.sort(results, new Comparator<Vector<Object>>() {
				@Override
				public int compare(Vector<Object> p1, Vector<Object> p2) {
					double diff = (double) p1.get(1) - (double) p2.get(1);
					if (diff < 0) {
						return 1;
					} else if (diff > 0) {
						return -1;
					} else {
						return 0;
					}
				}
			});

			//return results;
			StringBuilder sb = new StringBuilder();
			for(Vector<Object> pair : results) {
				sb.append(pair.get(0));
				sb.append(" ");
				sb.append(idToUrl.get(pair.get(0)));
				sb.append(" ");
				sb.append(pair.get(1));
				System.out.println("Doc ID: " + String.valueOf(pair.get(0)));
				System.out.println("URL: " + idToUrl.get(pair.get(0)));
				System.out.println("Doc Score: " + String.valueOf(pair.get(1)));
			}
			out.println(sb.toString());
		} else {
			String[] elsetokens = phrase.split(" ");

			HashSet<Integer> docList = new HashSet<>();

			//RecordManager recman = RecordManagerFactory.createRecordManager("Database");
			HTree ngram;
			//long recid;

			if(elsetokens.length == 2) {
				recid = recman.getNamedObject("bigram");
				ngram = HTree.load(recman, recid);
				docList = (HashSet<Integer>) ngram.get(phrase);
			} else if(elsetokens.length == 3) {
				recid = recman.getNamedObject("trigram");
				ngram = HTree.load(recman, recid);
				docList = (HashSet<Integer>) ngram.get(phrase);
			}

			if(docList == null) {
				Vector<Vector<Object>> results = new Vector<Vector<Object>>();
				StringBuilder sb = new StringBuilder();
				out.println(sb.toString());
				//return results;
			}

			//Vector<Vector<Object>> results = search(query, "");
			// search function
			phrase = "";
			for(Integer id : parsed_query) {
				if(id == null) {
					continue;
				}
				System.out.print(idToWord.get(id) + "\t");
			}
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
				for(Integer wordId : parsed_query) {
					if(wordId == null) {
						continue;
					}
					Vector<Posting> docPostingVector = (Vector<Posting>) docInvertedIndex.get(wordId);
					Vector<Posting> titlePostingVector = (Vector<Posting>) titleInvertedIndex.get(wordId);
					if(docPostingVector != null) {
						for (Posting docPosting : docPostingVector) {
							int docId = docPosting.id;
							int freq = docPosting.freq;
							//double termWeight = getTermWeightDoc(wordId, docId, freq, N_doc);
							// start getTermWeightDoc(wordId, docId, TF, N)
							Vector<Posting> gtwddocPostingList = (Vector<Posting>) titleInvertedIndex.get(wordId);
							Vector<Posting> gtwdwordPostingList = (Vector<Posting>) titleForwardIndex.get(wordId);
						
								// Score is derived from (Term Frequency * IDF) / (Doc Max Term Frequency)
								int N_hasTerm = gtwddocPostingList.size();
								double IDF = Math.log(N_doc/N_hasTerm) / Math.log(2);
								//int TF_max = getMaxTfTitle(wordId, docId);
								// start getMaxTfTitle function
								Vector<Posting> gmttwordPostingVector = (Vector<Posting>) titleForwardIndex.get(docId);

									int gmttresult = 1;
							
									for(Posting wordPosting : gmttwordPostingVector) {
										if(wordPosting.freq > gmttresult) {
											gmttresult = wordPosting.freq;
										}
									}
									int gmttTF_max = gmttresult; // return result
							
						
								double gtwdscore = (freq * IDF) / (gmttTF_max);
								// end getMaxTfTitle function
								double termWeight = gtwdscore;
		
							//double docMagnitude = getDocMagnitude(docId, N_doc);
							// start getDocMagnitude function
							Vector<Posting> gdmwordPostingVector = (Vector<Posting>) docForwardIndex.get(docId);

								double magnitude = 0;
						
								for(Posting wordPosting : gdmwordPostingVector) {
									//magnitude += Math.pow(getTermWeightDoc(wordPosting.id, docId, wordPosting.freq, N), 2);
									// start getTermWeightDoc(wordId, docId, TF, N)
									Vector<Posting> gtwd2docPostingList = (Vector<Posting>) docInvertedIndex.get(wordPosting.id);
									Vector<Posting> gtwd2wordPostingList = (Vector<Posting>) docForwardIndex.get(wordPosting.id);
							
									int N_hasTerm2 = gtwd2docPostingList.size();
									double IDF2 = Math.log(N_doc/N_hasTerm2) / Math.log(2);
									//int TF_max = getMaxTfDoc(wordPosting.id, docId);
									// start getMaxTfDoc function
									Vector<Posting> gmtdwordPostingVector = (Vector<Posting>) docForwardIndex.get(wordPosting.id);

										int gmtdresult = 1;
								
										for(Posting gmtdwordPosting : gmtdwordPostingVector) {
											if(gmtdwordPosting.freq > gmtdresult) {
												gmtdresult = gmtdwordPosting.freq;
											}
										}
										int gmtdTF_max = gmtdresult; //return result;
										//return result;
							
									double gdmscore = (wordPosting.freq * IDF2) / (gmtdTF_max);
									
									magnitude += Math.pow(gdmscore, 2);
								}
						
								magnitude = Math.sqrt(magnitude);
						
							double docMagnitude = magnitude;
							
							termWeight = termWeight / docMagnitude;

	
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
							//double termWeight = getTermWeightTitle(wordId, docId, freq, N_doc);
							// start getTermWeightTitle(wordID, docId, TF, N)
				
							Vector<Posting> gtwtdocPostingList = (Vector<Posting>) titleInvertedIndex.get(wordId);
							Vector<Posting> gtwtwordPostingList = (Vector<Posting>) titleForwardIndex.get(wordId);
					
							int N_hasTerm = gtwtdocPostingList.size();
							double IDF = Math.log(N_doc/N_hasTerm) / Math.log(2);

							//int TF_max = getMaxTfTitle(wordId, docId);
							// start getMaxTfTitle function
							Vector<Posting> gmtt2wordPostingVector = (Vector<Posting>) titleForwardIndex.get(docId);

								int gmtt2result = 1;
						
								for(Posting wordPosting : gmtt2wordPostingVector) {
									if(wordPosting.freq > gmtt2result) {
										gmtt2result = wordPosting.freq;
									}
								}
						
								//return result;
								int gtwtTF_max = gmtt2result; // return
							// end getMaxTfTitle function
					
							double score = (freq * IDF) / (gtwtTF_max);
					
							double termWeight2 = score; // return
							//double titleMagnitude = getTitleMagnitude(docId, N_doc);
							// start getTitleMagnitude(docId, N)
							Vector<Posting> gtmwordPostingVector = (Vector<Posting>) titleForwardIndex.get(docId);
							double gtmmagnitude = 0;

							for(Posting wordPosting : gtmwordPostingVector) {
								//magnitude += Math.pow(getTermWeightTitle(wordPosting.id, docId, wordPosting.freq, N), 2);
								// start getTermWeightTitle(wordID, docId, TF, N)
								Vector<Posting> gtwt2docPostingList = (Vector<Posting>) titleInvertedIndex.get(wordPosting.id);
								Vector<Posting> gtwt2wordPostingList = (Vector<Posting>) titleForwardIndex.get(wordPosting.id);

							
								int N_hasTerm2 = gtwt2docPostingList.size();
								double IDF2 = Math.log(N_doc/N_hasTerm2) / Math.log(2);
								//int gtwt2TF_max = getMaxTfTitle(wordPosting.id, docId);
								//start getMaxTfTitle(wordId, docId)
								Vector<Posting> gmtt3wordPostingVector = (Vector<Posting>) titleForwardIndex.get(docId);

									int gmtt3result = 1;
							
									for(Posting gmtt3wordPosting : gmtt3wordPostingVector) {
										if(gmtt3wordPosting.freq > gmtt3result) {
											gmtt3result = gmtt3wordPosting.freq;
										}
									}
							
									//return result;
									int gtwt2TF_max = gmtt3result; // return

								double score2 = (wordPosting.freq * IDF2) / (gtwt2TF_max);

								//return score;
								gtmmagnitude += Math.pow(score2, 2);
							}

							gtmmagnitude = Math.sqrt(gtmmagnitude);

							//return magnitude;

							termWeight2 = TITLE_CONSTANT * termWeight2 / gtmmagnitude;

							if (consolidatedScores.containsKey(docId)) {
								double currentScore = consolidatedScores.get(docId);
								consolidatedScores.put(docId, currentScore + termWeight2);
							} else {
								consolidatedScores.put(docId, termWeight2);
							}
						}
					}
				}
			} catch (IOException ex) {
				System.err.println(ex);
			}
			Set<Integer> keys = consolidatedScores.keySet();

			Vector<Vector<Object>> results = new Vector<Vector<Object>>();

			double queryMagnitude = Math.sqrt(parsed_query.size()); // Required for calculating cosine similarity

			for(Integer key : keys) {
				Vector pair = new Vector<>();
				pair.add(key);
				pair.add(consolidatedScores.get(key) / queryMagnitude);
				results.add(pair);
			}

			System.out.println(results.size());

			//Collections.sort(results, new Comparator<Vector<Object>>() {
			//	@Override
			//	public int compare(Vector<Object> p1, Vector<Object> p2) {
			//		double diff = (double) p1.get(1) - (double) p2.get(1);
			//		if (diff < 0) {
			//			return 1;
			//		} else if (diff > 0) {
			//			return -1;
			//		} else {
			//			return 0;
			//		}
			//	}
			//});

			//return results;
			// end search if phrase == ""

			Iterator<Vector<Object>> iterator = results.iterator();
			while (iterator.hasNext()) {
				Vector<Object> pair = iterator.next();
				if (!docList.contains((Integer) pair.get(0))) {
					iterator.remove();
				}
			}

			for(Integer docId : docList) {
				int EXISTS = 0;

				for(Vector<Object> pair : results) {
					if(((int) pair.get(0)) == docId) {
						EXISTS = 1;
						break;
					}
				}

				if(EXISTS == 1) {
					continue; // If entry is already in results, skip adding it
				}

				Vector<Object> tempPair = new Vector<>();
				tempPair.add(docId);
				tempPair.add(0);

				results.add(tempPair);
			}

			//return results;
			StringBuilder sb = new StringBuilder();
			for(Vector<Object> pair : results) {
				sb.append(pair.get(0));
				sb.append(" ");
				sb.append(idToUrl.get(pair.get(0)));
				sb.append(" ");
				sb.append(pair.get(1));
				System.out.println("Doc ID: " + String.valueOf(pair.get(0)));
				System.out.println("URL: " + idToUrl.get(pair.get(0)));
				System.out.println("Doc Score: " + String.valueOf(pair.get(1)));
			}
			out.print(sb.toString());
		}
	
    
%>