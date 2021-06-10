import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class Bucket implements Serializable {
    private Vector<String> pageReference;
    private Vector<Hashtable<String, Object>> BucketTuples;
    private String BucketName;

    public Bucket(String BucketName) {
        this.BucketName = BucketName;
        pageReference = new Vector<String>();
        BucketTuples = new Vector<Hashtable<String, Object>>();
    }


    public Vector<String> getPageReference() {
        return pageReference;
    }

    public void setPageReference(Vector<String> pageReference) {
        this.pageReference = pageReference;
    }

    public Vector<Hashtable<String, Object>> getBucketTuples() {
        return BucketTuples;
    }

    public void setBucketTuples(Vector<Hashtable<String, Object>> bucketTuples) {
        BucketTuples = bucketTuples;
    }

    public String getBucketName() {
        return BucketName;
    }

    public void setBucketName(String bucketName) {
        BucketName = bucketName;
    }

    public void insertTuple(int maxInBucket, String pageName, Hashtable<String, Object> entry, Grid parent) {
        if (pageReference.size() < maxInBucket) {
            //insertion happens here
            //13          56         89
            pageReference.add(pageName);
            BucketTuples.add(entry);
        } else {
            System.out.println("Error in bucket.insert tuple");
        }
    }

    public Vector<String> SelectPageReferencesFromBucket(Grid g, Hashtable<String, Object> toDelete) {
        Vector<String> returnedPageReferences = new Vector<>();
        Set<String> keys = toDelete.keySet();
        Iterator<String> itr = keys.iterator();
        Vector<String> ColNames = new Vector<>();
        while (itr.hasNext()) {
            String ColumnName = itr.next();
            ColNames.add(ColumnName);
        }

        loop:
        for (int i = 0; i < BucketTuples.size(); i++) {
            for (int j = 0; j < ColNames.size(); j++) {
                if (BucketTuples.get(i).get(ColNames.get(j))!=null&&!(BucketTuples.get(i).get(ColNames.get(j)).equals(toDelete.get(ColNames.get(j)))))
                    continue loop;
            }
            returnedPageReferences.add(pageReference.get(i));
//      String pageRef=pageReference.get(i);
//      current.put("PageReference",pageRef);
//      returnedTuples.add(current);


        }
        return returnedPageReferences;
    }

    public boolean deleteTuplesFromBucket(Hashtable<String, Object> Current, String PageReference) {
        loop:
        for (int i = 0; i < getBucketTuples().size(); i++) {
            if (!PageReference.equals(pageReference.get(i))) {
                continue;
            }
            Hashtable<String, Object> check = BucketTuples.get(i);
            Set<String> keys = check.keySet();
            Iterator<String> itr = keys.iterator();
            Vector<String> ColNames = new Vector<>();
            while (itr.hasNext()) {
                String ColumnName = itr.next();
                ColNames.add(ColumnName);
            }

            for (int j = 0; j < ColNames.size(); j++) {
                if (!(check.get(ColNames.get(j)).equals(Current.get(ColNames.get(j)))))
                    continue loop;
            }

            BucketTuples.remove(i);
            pageReference.remove(i);
            return true;

        }
        return false;
    }

    public static void serializeBucket(Bucket b) {
        String filename = b.getBucketName();
        try {
            FileOutputStream file = new FileOutputStream("src//main//resources//data//" + filename + ".ser");
            ObjectOutputStream out = new ObjectOutputStream(file);
            out.writeObject(b);
            out.close();
            file.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static Bucket deserializeBucket(String name) {
        Bucket b = null;
        try {
            // Reading the object from a file
            FileInputStream file = new FileInputStream("src//main//resources//data//" + name + ".ser");
            ObjectInputStream in = new ObjectInputStream(file);

            // Method for deserialization of object
            b = (Bucket) in.readObject();
            in.close();
            file.close();
        } catch (IOException | ClassNotFoundException ex) {
            ex.printStackTrace();
        }

        return b;
    }

    public void print() {
//        for (int i = 0; i < getBucketTuples().size(); i++) {
//            System.out.println(getBucketTuples().get(i) + " " + getPageReference().get(i));
//        }
        for (int i = 0; i < getBucketTuples().size(); i++) {
            Object[] a=getBucketTuples().get(i).values().toArray(new Object[0]);
            for (int k = 0; k < a.length; k++) {
                Object o = a[k];
                if (o instanceof Date) {
                    System.out.print(new SimpleDateFormat("yyyy-MM-dd").format((Date) o) + " ");
                } else {
                    System.out.print(o + " ");
                }
            }
            System.out.print(getPageReference().get(i));
            System.out.println();
        }


    }

    public Vector<String> getSuitablePageReferencesOfSQLterm(Vector<SQLTerm> sqlTerms) throws DBAppException {
        Vector<String> result = new Vector<>();
        loop:for (int i = 0; i < BucketTuples.size(); i++) {
            for (int j = 0; j < sqlTerms.size(); j++) {
                SQLTerm current = sqlTerms.get(j);
                String colName = current._strColumnName;
                Comparable SQLvalue = (Comparable) current._objValue;
                if (BucketTuples.get(i).containsKey(colName)) {
                    Comparable TupleValue = (Comparable) BucketTuples.get(i).get(colName);
                    if(!Operator.parseOperator(sqlTerms.get(j)._strOperator).apply(TupleValue,SQLvalue)){
                        continue loop;
                    }
                }
            }
            result.add(pageReference.get(i));
        }
        return result;
    }
}
