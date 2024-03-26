import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.helper.FastIterator;
import jdbm.htree.HTree;
import java.util.Vector;
import java.util.Date;
import java.text.SimpleDateFormat;

import java.util.Map;

public class Reader {

    public static void main(String[] args) {

        try {
            FileWriter fileWriter = new FileWriter("spider_result.txt");
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        
            ForwardIndex forwardindex = new ForwardIndex();
            HTree hashtable = forwardindex.hashtable; 

            InvertedIndex invertedindex = new InvertedIndex();
            WordMapping wordMap = new WordMapping();
            DocMapping docMap = new DocMapping(); 
            Metadata metadata = new Metadata(); 


            FastIterator iter = hashtable.keys();
            Integer key;

            while ((key = (Integer)iter.next()) != null) {
                String link = docMap.getUrl(key); 
                Container container = metadata.getMeta(link);
                for (String element: container.title) {
                    bufferedWriter.write(element + " "); 
                    //System.out.print(element+ " ");
                }
                bufferedWriter.newLine();
                //System.out.println("");
                bufferedWriter.write(link);
                bufferedWriter.newLine();
                //System.out.println(link);


                Date lastModifiedDate = new Date(container.lastModificationDate);
                String formattedDate = formatDate(lastModifiedDate);

                //System.out.println(formattedDate+ ","+ container.pageSize);
                bufferedWriter.write(formattedDate + "," + container.pageSize);
                bufferedWriter.newLine();
                Vector<Posting> postings = (Vector<Posting>) hashtable.get(key);
                int keywordCounter = 0; 
                for (Posting posting : postings) {
                    if (keywordCounter > 9) { 
                        break;
                    }

                    //System.out.print(wordMap.getWord(posting.id) + " "+ posting.freq + "; ");
                    bufferedWriter.write(wordMap.getWord(posting.id) + " "+ posting.freq + "; ");
                    keywordCounter++;
                }
                bufferedWriter.newLine();
                //System.out.println("");
                int childLinkCounter = 0; 
                for (String childLink: container.childLinks) {
                    if (childLinkCounter > 9) {
                        break;
                    }
                    bufferedWriter.write(childLink);
                    bufferedWriter.newLine();
                    //System.out.println(childLink);
                    childLinkCounter++;
                }

                bufferedWriter.write("---------------------------------------------------------------");
                bufferedWriter.newLine();
                //System.out.println("---------------------------------------------------------------");
            
            }
            
                bufferedWriter.close();
            /*while ((key = (Integer)iter.next()) != null) {
                Vector<Posting> postings = (Vector<Posting>) hashtable.get(key);
                System.out.println("docid: " + key);
                for (Posting posting : postings) {
                    System.out.println("wordId: " + posting.id + ", word: " + wordMap.getWord(posting.id) + ", Frequency: " + posting.freq);
                }
            }
            */



        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(date);
    }

}
