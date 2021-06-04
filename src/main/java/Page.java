import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;


public class Page implements Serializable{
//	private Table table;
	private static final long SerialVersionUID=123456l;
	
	private String PageFileName;
	private Vector<Hashtable<String,Object>> vecTuples;
	private Vector<Comparable>PrimaryKeys;
//	private Hashtable<String, String> htColNameType;
//	private Hashtable<String, String> htColNameMin;
//	private Hashtable<String, String> htColNameMax;
	
	
	
	public Page(String PageFileName) {
		this.PageFileName=PageFileName;
		vecTuples=new Vector<Hashtable<String,Object>>();
		PrimaryKeys=new Vector<Comparable>();
	}
	
//	public boolean contains(String PK,Object val) {
//		
//	}
	
	public String getPageFileName() {
		return PageFileName;
	}

	public void setPageFileName(String pageFileName) {
		PageFileName = pageFileName;
	}

	public Vector<Hashtable<String,Object>> getVecTuples() {
		return vecTuples;
	}

	public void setVecTuples(Vector<Hashtable<String,Object>> vecTuples) {
		this.vecTuples = vecTuples;
	}

	public Vector<Comparable> getPrimaryKeys() {
		return PrimaryKeys;
	}

	public void setPrimaryKeys(Vector<Comparable> primaryKeys) {
		PrimaryKeys = primaryKeys;
	}
	//insert or update true if insert false if update
	public int getPKvalPositionBinarySearch(Comparable pkVal,boolean insertORupdate) throws DBAppException {
		int l = 0;
		int r = this.getPrimaryKeys().size() - 1;
		while (l <= r) {
			int m = l + (r - l) / 2;

			// Check if x is present at mid
			if (this.getPrimaryKeys().get(m).compareTo(pkVal) == 0) {
				if(insertORupdate){
					throw new DBAppException("Primary key already exists!");
				}else{
					return m;
				}

			}
			// If x greater, ignore left half
			if (this.getPrimaryKeys().get(m).compareTo(pkVal)<0)
				l = m + 1;

			// If x is smaller, ignore right half
			else
				r = m - 1;
		}
		//System.out.println(l+" " +r);
		// if we reach here, then element was
		// not present
		if(insertORupdate){
			return l;
		}else{
			throw new DBAppException("Primary key not found");
		}

	}

	public void print() {
		for (int i = 0; i < getVecTuples().size(); i++) {

				Object[] a=getVecTuples().get(i).values().toArray();
				for (int k = 0; k < a.length; k++) {
					Object o = a[k];
					if (o instanceof Date) {
						System.out.print(new SimpleDateFormat("yyyy-MM-dd").format((Date) o) + " ");
					} else {
						System.out.print(o + " ");
					}
				}

			System.out.println();
		}
	}

	public static void serializePage(Page p) {
		String filename=p.getPageFileName();
		try {
			// Saving of object in a file
			FileOutputStream file = new FileOutputStream("src//main//resources//data//"+filename+".ser");
			ObjectOutputStream out = new ObjectOutputStream(file);

			// FileOutputStream file2 = new FileOutputStream(p.getPageFileName()+".ser");
			// ObjectOutputStream out2 = new ObjectOutputStream(file2);

			// Method for serialization of object
			out.writeObject(p);

			// out2.writeObject(p);

			out.close();
			file.close();

			// out2.close();
			// file2.close();

			//System.out.println("Object has been serialized");

		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	public static Page deserializePage(String pageName) {
		Page p = null;
		try {
			// Reading the object from a file
			FileInputStream file = new FileInputStream("src//main//resources//data//" + pageName + ".ser");
			ObjectInputStream in = new ObjectInputStream(file);
			// Method for deserialization of object
			p = (Page) in.readObject();
			in.close();
			file.close();
			//System.out.println("Object has been deserialized ");
		} catch (IOException | ClassNotFoundException ex) {
			ex.printStackTrace();
		}
		return p;
	}

	public Vector<Hashtable<String, Object>> getSuitableTuplesOfSQLterm(Vector<SQLTerm> sqlTerms) throws DBAppException {
		Vector<Hashtable<String, Object>> result = getHashtablesCompatibleWithSQlTerms(vecTuples,sqlTerms);
		return result;
	}

	public static Vector<Hashtable<String, Object>> getHashtablesCompatibleWithSQlTerms(Vector<Hashtable<String, Object>> vecTuples,Vector<SQLTerm> sqlTerms) throws DBAppException {
		Vector<Hashtable<String,Object>> result = new Vector<>();
		loop:for (int i = 0; i < vecTuples.size(); i++) {
			for (int j = 0; j < sqlTerms.size(); j++) {
				SQLTerm current = sqlTerms.get(j);
				String colName = current._strColumnName;
				Comparable SQLvalue = (Comparable) current._objValue;
				//if (vecTuples.get(i).containsKey(colName)) {
					Comparable TupleValue = (Comparable) vecTuples.get(i).get(colName);
					if(!Operator.parseOperator(sqlTerms.get(j)._strOperator).apply(TupleValue,SQLvalue)){
						continue loop;
					}
				//}
			}
			result.add(vecTuples.get(i));
		}
		return result;
	}
}
