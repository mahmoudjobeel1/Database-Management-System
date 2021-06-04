import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

public class Table implements Serializable{
	
	private static final long SerialVersionUID=1234l;
	
	private String strTableName;
//	private String strClusteringKey;
	private Vector<String> vecPageFileName;
	private Vector<Comparable> vecPageMinTuple;
	private int PageCounter=1;
	private int GridCounter=1;
	private Vector<String> gridNames;
	private Vector<Vector<String>> GridDimensions;
	
	
	//how to deal with those values
//	private int MaximumRowsCountinPage;
//	private int MaximumKeysCountinIndexBucket;
	
	
	public Table(String strTableName) {
		super();
		this.strTableName = strTableName;
		this.vecPageFileName = new Vector<String>();
		this.vecPageMinTuple = new Vector<Comparable>();
		gridNames=new Vector<>();
		GridDimensions=new Vector<>();
	}
	
	
	public String NextPageName() {
		return strTableName+"_Page"+(PageCounter++);
	}
	public String NextGridName() {
		return strTableName+"_Grid"+(GridCounter++);
	}
	
//	public void addPage(String PageName,Comparable MinTuple) {
//		vecPageFileName.add(PageName);
//		vecPageMinTuple.add(MinTuple);
//	}


	public int getPageCounter() {
		return PageCounter;
	}


	public void setPageCounter(int pageCounter) {
		PageCounter = pageCounter;
	}


	public String getStrTableName() {
		return strTableName;
	}


	public Vector<String> getVecPageFileName() {
		return vecPageFileName;
	}


	public Vector<Comparable> getVecPageMinTuple() {
		return vecPageMinTuple;
	}

	public String insert(Hashtable<String, Object> colNameValue, int maxPerPage, int pagePos, boolean createNewIfFull) throws DBAppException {
		   //check if pk already exists  	
			String[] PKwithType=Utilities.getPKofTableByItsName(this.getStrTableName());
			String pk=PKwithType[0];
			Comparable pkval=(Comparable)colNameValue.get(pk);
			//...........................................................
			
			
			Page p;
			if(pagePos==this.vecPageFileName.size()) {
				String nextName=NextPageName();
        		p=new Page(nextName);
        		getVecPageFileName().add(nextName);
        		getVecPageMinTuple().add(pkval);
			}else {
				p= deserializePagebyPosition(pagePos);
			}
			
			int entryPosInPage=p.getPKvalPositionBinarySearch(pkval,true);
			
			p.getVecTuples().add(entryPosInPage, colNameValue);
			p.getPrimaryKeys().add(entryPosInPage, pkval);
			String pageName=p.getPageFileName();
			//3adel el min
			//12    46
			if(p.getVecTuples().size()>maxPerPage) {
				if(createNewIfFull) {
					Hashtable<String, Object> first=p.getVecTuples().get(0);
					p.getVecTuples().remove(0);
					p.getPrimaryKeys().remove(0);
					String pageName2=InsertInBetweenPage(pagePos , first , pkval);
					if(first.get(pk)==pkval){
						pageName=pageName2;
					}else{
						//TODO
						//editPageNameInGrid(first,p.getPageFileName(),pageName2);
					}
				}else {
					Hashtable<String, Object> last=p.getVecTuples().get(maxPerPage);
					p.getVecTuples().remove(maxPerPage);
					p.getPrimaryKeys().remove(maxPerPage);
					String pageName2=insert(last,maxPerPage,pagePos+1,true);//2
					if(last.get(pk)==pkval){
						pageName=pageName2;
					}else{
						//TODO
						//editPageNameInGrid(last,p.getPageFileName(),pageName2);
					}
				}

				
			}
			
			Comparable newMin=p.getPrimaryKeys().get(0);
			this.vecPageMinTuple.set(pagePos, newMin);
			
			Page.serializePage(p);
			return pageName;
		
	}
	private String InsertInBetweenPage(int position , Hashtable<String, Object> first , Comparable pkVal) {
		String nextName=NextPageName();
		Page p;
		p=new Page(nextName);
		getVecPageFileName().add(position,nextName);
		getVecPageMinTuple().add(position,pkVal);
		p.getVecTuples().add(0, first);
		p.getPrimaryKeys().add(0, pkVal);
		Page.serializePage(p);
		return p.getPageFileName();
	}






	public void print() {
		for (int i = 0; i < getVecPageFileName().size(); i++) {
			Page p= deserializePagebyPosition(i);
			System.out.println(p.getPageFileName());
			p.print();
		}
		for (int i = 0; i < getGridNames().size(); i++) {
			Grid g= Grid.deserializeGrid(getGridNames().get(i));
			g.print();
		}
	}


	public Vector<Hashtable<String,Object>> deleteFromPage(String pageName,Hashtable<String, Object> columnNameValue,Comparable pkValue) {
		int pagePos=vecPageFileName.indexOf(pageName);
		Page p=Page.deserializePage(pageName);
		Vector<Hashtable<String, Object>>tuples=p.getVecTuples();
		Vector<Hashtable<String, Object>>deletedTuples=new Vector<>();
		if(pkValue!=null) {
			try {
				int entryPos = p.getPKvalPositionBinarySearch(pkValue, false);
				Hashtable<String, Object> current = tuples.get(entryPos);
				boolean toBeDeleted=checkProperties(current,columnNameValue);
				if(toBeDeleted) {
					Hashtable<String,Object> currentTuple =tuples.remove(entryPos);
					p.getPrimaryKeys().remove(entryPos);
					currentTuple.put("PageReference",pageName);
					deletedTuples.add(currentTuple);
				}
			}catch(DBAppException e){
				System.out.println("Error in delete from page function");
			}
		}
		else {
			for (int j = 0; j < tuples.size(); j++) {
				Hashtable<String, Object> current = tuples.get(j);
				boolean toBeDeleted = checkProperties(current, columnNameValue);
				if (toBeDeleted) {
					Hashtable<String,Object> currentTuple =tuples.remove(j);
					p.getPrimaryKeys().remove(j);
					currentTuple.put("PageReference",pageName);
					deletedTuples.add(currentTuple);
					j--;
				}
			}
		}

		if(p.getVecTuples().size()==0) {
			Utilities.removeFromDisk(p.getPageFileName());
			getVecPageFileName().remove(pagePos);
			getVecPageMinTuple().remove(pagePos);
		}else {
			Comparable newMin=p.getPrimaryKeys().get(0);
			this.vecPageMinTuple.set(vecPageFileName.indexOf(pageName), newMin);
		}
		Page.serializePage(p);
		return deletedTuples;
	}




	private boolean checkProperties(Hashtable<String, Object> current, Hashtable<String, Object> columnNameValue) {
		Set<String> keys = columnNameValue.keySet();
		Iterator<String> itr = keys.iterator();
		Vector<String> ColNames = new Vector<>();
		while(itr.hasNext()) {
			String ColumnName = itr.next();
			ColNames.add(ColumnName);
		}
		for(int j=0 ; j<ColNames.size() ; j++) {
			if(!(current.get(ColNames.get(j)).equals(columnNameValue.get(ColNames.get(j)))))
				return false;
		}
		return true;

	}

	public Vector<String> getGridNames() {
		return gridNames;
	}

	public void setGridNames(Vector<String> gridNames) {
		this.gridNames = gridNames;
	}

	public Vector<Vector<String>> getGridDimensions() {
		return GridDimensions;
	}

	public void setGridDimensions(Vector<Vector<String>> gridDimensions) {
		GridDimensions = gridDimensions;
	}


	//add page--- remove page---- get data type le column---- check on page size


	public static void serializeTable(Table t) {
		String filename = t.getStrTableName();
		try {
			// Saving of object in a file
			FileOutputStream file = new FileOutputStream("src//main//resources//data//" + filename + ".ser");
			ObjectOutputStream out = new ObjectOutputStream(file);
			out.writeObject(t);
			out.close();
			file.close();
			//System.out.println("Object has been serialized");

		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public static Table deserializeTable(String tableName) {
		Table t = new Table("");
		try {
			// Reading the object from a file
			FileInputStream file = new FileInputStream("src//main//resources//data//" + tableName + ".ser");
			ObjectInputStream in = new ObjectInputStream(file);
			// Method for deserialization of object
			t = (Table) in.readObject();
			in.close();
			file.close();
			//System.out.println("Object has been deserialized ");
		}

		catch (IOException | ClassNotFoundException ex) {
			ex.printStackTrace();
		}
		return t;
	}
	public Page deserializePagebyPosition(int position) {
		Vector<String>pageNames=getVecPageFileName();
		String pageName=pageNames.get(position);
		Page p=new Page("");
		try
		{
			// Reading the object from a file
			FileInputStream file = new FileInputStream("src//main//resources//data//"+pageName+".ser");
			ObjectInputStream in = new ObjectInputStream(file);

			// Method for deserialization of object
			p = (Page)in.readObject();
			in.close();
			file.close();
		}


		catch(IOException | ClassNotFoundException ex)
		{
			ex.printStackTrace();
		}

		return p;
	}
	public Grid getSuitableGrid(Hashtable<String,Object> entry){
		if(gridNames.isEmpty()){
			return null;
		}
		int maxWithPk=0;
		Grid gridWithPk=null;
		int maxWithoutPk=0;
		Grid gridWithoutPk=null;
		for (int i = 0; i < GridDimensions.size(); i++) {
			if(entryHasPk(entry)&&gridHasPk(getGridDimensions().get(i))){
				int commons=countCommonDimensions(entry,getGridDimensions().get(i));
				if(maxWithPk<commons){
					maxWithPk=commons;
					gridWithPk=Grid.deserializeGrid(gridNames.get(i));
				}
			}else{
				int commons=countCommonDimensions(entry,getGridDimensions().get(i));;
				if(maxWithoutPk<commons){
					maxWithoutPk=commons;
					gridWithoutPk=Grid.deserializeGrid(gridNames.get(i));
				}
			}
		}
		if(maxWithPk==0){
			return gridWithoutPk;
		}else{
			return gridWithPk;
		}
	}

	private int countCommonDimensions(Hashtable<String, Object> entry, Vector<String> strings) {
		int c=0;
		for (int i = 0; i < strings.size(); i++) {
			if(entry.containsKey(strings.get(i))){
				c++;
			}
		}
		return c;
	}

	private boolean gridHasPk(Vector<String> strings) {
		String[] s=Utilities.getPKofTableByItsName(this.strTableName);
		if(strings.contains(s[0])){
			return true;
		}else{
			return false;
		}
	}


	private boolean entryHasPk(Hashtable<String, Object> entry) {
		String[] s=Utilities.getPKofTableByItsName(this.strTableName);
		if(entry.containsKey(s[0])){
			return true;
		}else{
			return false;
		}
	}



}
