import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.htree.HTree;
import jdbm.helper.FastIterator;
import java.util.Vector;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.lang.Exception;

class Posting implements Serializable {
	public int id;
	public int freq;

	Posting(int id, int freq) {
		this.id = id;
		this.freq = freq;
	}
}

public class DocMapping {
	private RecordManager recman;
	private HTree urlToId;
	private HTree idToUrl;

	DocMapping(String recordmanager) throws IOException{
		try {
			recman = RecordManagerFactory.createRecordManager(recordmanager);
			long ID_urlToId = recman.getNamedObject("urlToId");
			long ID_idToUrl = recman.getNamedObject("idToUrl");

			if ((ID_urlToId != 0) && (ID_idToUrl != 0)) {
				// If mappings already exist, load mappings
				urlToId = HTree.load(recman, ID_urlToId);
				idToUrl = HTree.load(recman, ID_idToUrl);
			} else if ((ID_urlToId == 0) && (ID_idToUrl == 0)) {
				// If mappings don't exist, create mappings
				urlToId = HTree.createInstance(recman);
				idToUrl = HTree.createInstance(recman);

				recman.setNamedObject("urlToId", urlToId.getRecid());
				recman.setNamedObject("idToUrl", idToUrl.getRecid());
			} else {
				// If only one mapping exists, throw Exception
				throw new IOException("Doc Mapping corrupted: one of the mapping hashtables do not exist");
			}
		} catch(java.io.IOException ex) {
			System.err.println(ex);
		}
	}


	public void finalize() throws IOException {
		recman.commit();
		recman.close();
	}

	public void addMapping(int docID, String url) throws IOException {
		try {
			// Check if mapping already exists
			if ((idToUrl.get(docID) == null) && (urlToId.get(url) == null)) {
				idToUrl.put(docID, url);
				urlToId.put(url, docID);
				System.out.println("Successfully inserted:\n" + "URL: " + url);
			} else if ((idToUrl.get(docID) == null) && (urlToId.get(url) == null)) {
				System.out.println("URL already found");
			} else {
				// If mapping only exists for one hashtable, throw exception
				throw new IOException("Doc Mapping corrupted: mapping only exists on one hashtable");
			}
		} catch (java.io.IOException ex) {
			System.err.println(ex.toString());
		}
	}

	public void removeMapping(int docID) throws IOException {
		try {
			// Check if docID exists
			if(idToUrl.get(docID) == null) {
				System.out.println("Deletion failed: docID " + String.valueOf(docID) + " does not exist");
				return;
			}

			String url = (String) idToUrl.get(docID);

			idToUrl.remove(docID);
			urlToId.remove(url);

			System.out.println("Successfully removed");
			System.out.println("Doc ID: " + String.valueOf(docID));
			System.out.println("URL: " + url);
		} catch (java.io.IOException ex) {
			System.err.println(ex.toString());
		}
	}

	public String getUrl(int docID) throws IOException {
		try {
			String result = (String) idToUrl.get(docID);
			return result;
		} catch (java.io.IOException ex) {
			System.err.println(ex.toString());
		}

		return "";
	}

	public Integer getId(String url) throws IOException {
		try{
			Integer result = (Integer) urlToId.get(url);
			return result;
		} catch (java.io.IOException ex) {
			System.err.println(ex.toString());
		}

		return -1;
	}

	public void printAll() throws IOException {
		try{
			System.out.println("URL TO ID MAPPINGS:");

			FastIterator iterUrl = urlToId.keys();
			String url;
			while ((url = (String) iterUrl.next()) != null) {
				System.out.println(url + "\t-->\t" + String.valueOf(urlToId.get(url)));
			}

			System.out.println("---------------------");
			System.out.println("ID TO URL MAPPINGS:");

			FastIterator iterId = idToUrl.keys();
			Integer id;
			while ((id = (Integer) iterId.next()) != null) {
				System.out.println(String.valueOf(id) + "\t-->\t" + idToUrl.get(id));
			}
		} catch (java.io.IOException ex) {
			System.err.println(ex);
		}
	}
	public static void main(String[] args)
	{
		try
		{
			DocMapping docMapping = new DocMapping("DocMapping");
			docMapping.addMapping(0, "google.com");
			docMapping.addMapping(1, "facebook.com");
			docMapping.addMapping(2, "yahoo.com");

			System.out.println("First print:");
			docMapping.printAll();

			docMapping.removeMapping(0);

			System.out.println("Second print:");
			docMapping.printAll();
			docMapping.finalize();
		}
		catch(IOException ex)
		{
			System.err.println(ex.toString());
		}

	}
}
