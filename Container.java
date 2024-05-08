package PROJECT;
import java.util.Vector;
import java.io.IOException;
import java.io.Serializable;

public class Container implements Serializable {
	private static final long serialVersionUID = 1L;
    public Vector<String> childLinks; 
    public int pageSize; 
    public long lastModificationDate; 
    public Vector<String> title; 

    public Container(Vector<String> childLinks, int pageSize, long lastModificationDate, Vector<String> title){
        this.childLinks = childLinks;
        this.pageSize = pageSize;
        this.lastModificationDate = lastModificationDate; 
        this.title =title; 
    }

	public long getLastModificationDate() {
		return this.lastModificationDate;
	}
}
