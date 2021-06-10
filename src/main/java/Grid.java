import java.io.*;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.*;

public class Grid implements Serializable {
    //add vector grids in table with dimensions
    private String tableName;
    private String gridName;
    private Vector<Vector<String>> index;
    private Vector<String> dimensions; //column names
    private Vector<Comparable[]> allRangeMin; //column ranges
    private int BucketCounter = 1; // for naming issue
    private int maxPerBucket;

    public Grid(String tableName, String gridName, String[] colNames, int maxPerBucket) throws DBAppException {
        this.maxPerBucket = maxPerBucket;
        this.tableName = tableName;
        this.gridName = gridName;
        dimensions = new Vector<>();
        allRangeMin = new Vector<>();
        for (int i = 0; i < colNames.length; i++) {

            try {
                Comparable[] ranges = Utilities.range(tableName, colNames[i]);
                dimensions.add(colNames[i]);
                allRangeMin.add(ranges);
            } catch (ParseException e) {
                throw new DBAppException("Error in parsing date");
            }
        }
        index = new Vector<>();
        int NewSize = (int) Math.pow(11, dimensions.size());
        for (int i = 0; i < NewSize; i++) {
            index.add(null);
        }
        insertAllTable();
    }

    private void insertAllTable() {
        Table t = Table.deserializeTable(tableName);
        Vector<String> pages = t.getVecPageFileName();
        for (int i = 0; i < pages.size(); i++) {
            Page p = Page.deserializePage(pages.get(i));
            for (int j = 0; j < p.getVecTuples().size(); j++) {
                insertTuple(maxPerBucket, p.getVecTuples().get(j), p.getPageFileName());
            }
        }
    }

    public String NextBucketName() {
        return gridName + "_Bucket" + (BucketCounter++);
    }

    public void addDimension(String colName, Comparable[] rangeMin) {
        dimensions.add(colName);
        allRangeMin.add(rangeMin);

        Vector<Vector<String>> newIndex = new Vector<>();
        int NewSize = (int) Math.pow(11, dimensions.size());
//        for (int i = 0; i < NewSize; i++) {
//            newIndex.add(new String[11]);
//        }
        //reorder
        deleteBuckets(index);
        insertAllTable();
        index = newIndex;
    }

    //1 2 3 4 5 6 7 8 9 10
//2
//3
//...
//10
    private void deleteBuckets(Vector<Vector<String>> index) {
        //deserialize bucket from each position
        for (int i = 0; i < index.size(); i++) {
            if (index.get(i) != null) {
                for (int j = 0; j < index.get(i).size(); j++) {
                    Utilities.removeFromDisk(index.get(i).get(j));
                }

            }

        }
        //loop on entries and insert
    }

    //0             1           2
    // 1 2 3 n    //9 10 11   0 //18 19 20
    // 3 4 5    //12 13 14   1//21 22 23
    // 6 2n 3n nn   //15 16 17   2//24 25 26   1 2 3 4 5 6 7 8 9   22--> [2,1,1]
    public static int calculatePositionInIndexVector(int[] dimension) {
        if (dimension.length == 0)
            return 0;
        int offset = (int) Math.pow(11, dimension.length - 1);
        int partialPosition = dimension[0] * offset;
        int[] newDim = Arrays.copyOfRange(dimension, 1, dimension.length);
        return partialPosition + calculatePositionInIndexVector(newDim);
    }

    public static int[] calculateDimensionFromPosition(int pos, int dim) {
        if (dim == 0)
            return new int[0];
        int[] dimension = new int[dim];
        int offset = (int) Math.pow(11, dim - 1);
        int partialDim = pos / offset;
        dimension[0] = partialDim;
        int[] partialDimensions = calculateDimensionFromPosition(pos % offset, dim - 1);
        System.arraycopy(partialDimensions, 0, dimension, 1, partialDimensions.length);
        return dimension;
    }


    //insert tuple
    //update tuple
    //delete tuple


    public String getGridName() {
        return gridName;
    }


    public void insertTuple(int maxInBucket, Hashtable<String, Object> colNameValue, String pageName) {
        //hashtable bekol column me7tago fel grid wel value includes null
        //get where to insert dimension from ranges [1,4,0]
        //tuple loop over dimensions and ranges of dimension and get range
        //create dimension array
        int[] dimension = gridPosition(colNameValue);
        int posInIndex = calculatePositionInIndexVector(dimension);
        if (index.get(posInIndex) == null) {
            Bucket b = new Bucket(NextBucketName());
            b.insertTuple(maxInBucket, pageName, getNeededDimensions(colNameValue), this);
            Bucket.serializeBucket(b);
            Vector<String> newVec = new Vector<>();
            newVec.add(b.getBucketName());
            //TODO might cause an error
            index.setElementAt(newVec, posInIndex);
        } else {
            //TODO insert sorted problem
            Bucket b = Bucket.deserializeBucket(index.get(posInIndex).lastElement());
            if (b.getBucketTuples().size() == maxInBucket) {
                b = new Bucket(NextBucketName());
                index.get(posInIndex).add(b.getBucketName());
            }
            b.insertTuple(maxInBucket, pageName, getNeededDimensions(colNameValue), this);
            Bucket.serializeBucket(b);
        }
        //if position empty create bucket
        //bucket at index.get(20) insert in
    }

    private Hashtable<String, Object> getNeededDimensions(Hashtable<String, Object> colNameValue) {
        Hashtable<String, Object> GridPositions = new Hashtable<String, Object>();
        for (int j = 0; j < dimensions.size(); j++) {
            if (colNameValue.containsKey(dimensions.get(j)))
                GridPositions.put(dimensions.get(j), colNameValue.get(dimensions.get(j)));
            else
                GridPositions.put(dimensions.get(j), new MyNull());
        }
        return GridPositions;
    }

    private int[] gridPosition(Hashtable<String, Object> colNameValue) {
        Hashtable<String, Object> neededDimension = getNeededDimensions(colNameValue);
        int[] DimensionPositions = new int[dimensions.size()];
        for (int i = 0; i < dimensions.size(); i++) {
            if (neededDimension.get(dimensions.get(i)) instanceof MyNull)
                DimensionPositions[i] = 10;
            else {
                Comparable[] ranges = allRangeMin.get(i);
                for (int j = 0; j < ranges.length; j++) {
                    if (neededDimension.get(dimensions.get(i)) instanceof String) {
                        String s = (String) neededDimension.get(dimensions.get(i));
                        BigInteger bi = Utilities.calc_Bigint(s, Utilities.maxLength(tableName, dimensions.get(i))-s.length());
                        neededDimension.replace(dimensions.get(i), bi);
                    }

                    if (((Comparable) neededDimension.get(dimensions.get(i))).compareTo(ranges[j]) >= 0) {
                        DimensionPositions[i] = j;
                    }
                }
            }
        }
        return DimensionPositions;
    }


    public Vector<String> selectPagesOfTuple(Hashtable<String, Object> columnNameValue) {
        //get positions array    [positions kteer]

        //-------------------
        Vector<Integer> positions = getAllPositions(columnNameValue);


        Vector<String> PageReferences = new Vector<>();
        for (int i = 0; i < positions.size(); i++) {
            int posInIndex = positions.get(i);
            if (index.get(posInIndex) == null)
                continue;
            for (int j = 0; j < index.get(posInIndex).size(); j++) {
                Bucket b = Bucket.deserializeBucket(index.get(posInIndex).get(j));
                Vector<String> partOfPages = b.SelectPageReferencesFromBucket(this, columnNameValue);
                PageReferences.addAll(partOfPages);
            }
        }
        return PageReferences;

    }

    private Vector<Integer> getAllPositions(Hashtable<String, Object> columnNameValue) {
        Hashtable<String, Object> neededDimension = getNeededDimensions(columnNameValue);
        int[] DimensionPositions = new int[dimensions.size()];
        Arrays.fill(DimensionPositions, -1);
        for (int i = 0; i < dimensions.size(); i++) {
            if (!(neededDimension.get(dimensions.get(i)) instanceof MyNull)) {
                if (neededDimension.get(dimensions.get(i)) instanceof String) {
                    String s = (String) neededDimension.get(dimensions.get(i));
                    BigInteger bi = Utilities.calc_Bigint(s, Utilities.maxLength(tableName, dimensions.get(i))-s.length());
                    neededDimension.replace(dimensions.get(i), bi);
                }
                Comparable[] ranges = allRangeMin.get(i);
                for (int j = 0; j < ranges.length; j++) {
                    if (((Comparable) neededDimension.get(dimensions.get(i))).compareTo(ranges[j]) >= 0) {
                        DimensionPositions[i] = j;
                    }
                }
            }
        }
        //[-1,5,2,-1,-1]   [1,-1,-1]
        return generatePositionsFromDimensions(DimensionPositions);

    }

    private static Vector<Integer> generatePositionsFromDimensions(int[] DimensionPositions) {
        int negones = 0;
        for (int i = 0; i < DimensionPositions.length; i++) {
            if (DimensionPositions[i] == -1)
                negones++;
        }
        double limit = Math.pow(11, negones);
        Vector<Integer> res = new Vector<>();
        for (double i = 0; i < limit; i++) {
            int[] division = divide(i, negones);
            int[] DimensionPossibility = new int[DimensionPositions.length];
            int c = 0;
            for (int j = 0; j < DimensionPossibility.length; j++) {
                if (DimensionPositions[j] != -1) {
                    DimensionPossibility[j] = DimensionPositions[j];
                } else {
                    DimensionPossibility[j] = division[c++];
                }

            }
            int posInIndex = calculatePositionInIndexVector(DimensionPossibility);
            res.add(posInIndex);
        }
        return res;
    }

    private static int[] divide(double mask, int i) {
        int[] res = new int[i];
        for (int j = 0; j < res.length; j++) {
            double mod = mask % 11;
            mask /= 11;
            res[j] = (int) mod;
        }
        return res;
    }
    //remove vector and set index to null if vector become empty
//        if(index.get(posInIndex).size()==0){
//            index.remove(posInIndex);
//        }


    public static void serializeGrid(Grid g) {
        String filename = g.getGridName();
        try {
            // Saving of object in a file
            FileOutputStream file = new FileOutputStream("src//main//resources//data//" + filename + ".ser");
            ObjectOutputStream out = new ObjectOutputStream(file);
            out.writeObject(g);
            out.close();
            file.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }


    public static Grid deserializeGrid(String gname) {
        Grid g = null;
        try {
            // Reading the object from a file
            FileInputStream file = new FileInputStream("src//main//resources//data//" + gname + ".ser");
            ObjectInputStream in = new ObjectInputStream(file);

            // Method for deserialization of object
            g = (Grid) in.readObject();
            in.close();
            file.close();
        } catch (IOException | ClassNotFoundException ex) {
            ex.printStackTrace();
        }

        return g;
    }

    public Vector<String> getDimensions() {
        return dimensions;
    }

    public void print() {
        System.out.println(getDimensions());
        for (int i = 0; i < index.size(); i++) {
            if (index.get(i) != null) {
                for (int j = 0; j < index.get(i).size(); j++) {
                    Bucket b = Bucket.deserializeBucket(index.get(i).get(j));
                    String overflow=(j==0)?"":(" overflow "+index.get(i).get(0));
                    System.out.println("----"+b.getBucketName()+overflow);
                    b.print();

                }
            }
        }
    }

    public void deleteAllTuplesGiven(Vector<Hashtable<String, Object>> deletedTuples) {
        for (int i = 0; i < deletedTuples.size(); i++) {
            Hashtable<String, Object> dataOfCurrent = deletedTuples.get(i);
            Hashtable<String, Object> current = (Hashtable<String, Object>) dataOfCurrent.clone();
            String pageRef = (String) current.remove("PageReference");
            int[] dimensions = gridPosition(current);
            int posInIndex = calculatePositionInIndexVector(dimensions);
            for (int j = index.get(posInIndex).size() - 1; j >= 0; j--) {
                Bucket b = Bucket.deserializeBucket(index.get(posInIndex).get(j));
                boolean done = b.deleteTuplesFromBucket(current, pageRef);
                Bucket.serializeBucket(b);
                if (b.getBucketTuples().size() == 0) {
                    index.get(posInIndex).remove(j);
                    Utilities.removeFromDisk(b.getBucketName());
                }
                if (done) {
                    break;
                }
            }
            if (index.get(posInIndex).size() == 0) {
                index.setElementAt(null, posInIndex);
            }
        }
    }

    public static void main(String[] args) {
        int[] arr = new int[]{5, -1, -1, 2};
        generatePositionsFromDimensions(arr);
        //System.out.println(Arrays.toString(calculateDimensionFromPosition(15,3)));
        //System.out.println(calculatePositionInIndexVector(new int[]{0,1,4}));

    }

    public Vector<Integer> getSuitableBucketsForSQLterm(SQLTerm sqlTerm) throws DBAppException {
        String colName = sqlTerm._strColumnName;
        Comparable value = (Comparable) sqlTerm._objValue;
        if (value instanceof String) {
            value = Utilities.calc_Bigint((String) value, Utilities.maxLength(sqlTerm._strTableName, colName)-((String) value).length());
        }
        int colNamePos = dimensions.indexOf(colName);
        Comparable[] ranges = allRangeMin.get(colNamePos);
        Vector<Integer> result = new Vector<>();
        for (int i = 0; i < ranges.length; i++) {
            Comparable c = ranges[i];
            boolean check = Operator.parseOperator(sqlTerm._strOperator).apply(c, value);
            if (sqlTerm._strOperator.equals("!=")) {
                check = true;
            }
            if (sqlTerm._strOperator.contains(">")) {
                if (i + 1 < ranges.length) {
                    check = Operator.parseOperator(">=").apply(ranges[i + 1], value);
                } else {
                    check = true;
                }
            }
            if (sqlTerm._strOperator.equals("=")) {
                if (i + 1 < ranges.length) {
                    check = Operator.parseOperator(">=").apply(value, c) && Operator.parseOperator("<").apply(value, ranges[i + 1]);
                } else {
                    check = Operator.parseOperator(">=").apply(value, c);
                }
            }
            if (check) {
                int[] dimension = new int[dimensions.size()];
                Arrays.fill(dimension, -1);
                dimension[colNamePos] = i;
                Vector<Integer> current = generatePositionsFromDimensions(dimension);
                result.addAll(current);
            }
        }
        return result;
    }

    public Vector<String> getSuitablePageReferencesOfSQLterm(Vector<SQLTerm> toBeAnded) throws DBAppException {
        Vector<Vector<Integer>> allSuitableBuckets = new Vector<>();
        for (int i = 0; i < toBeAnded.size(); i++) {
            if (dimensions.contains(toBeAnded.get(i)._strColumnName)) {
                Vector<Integer> bucketsToSearchIn = getSuitableBucketsForSQLterm(toBeAnded.get(i));
//            //for testing
//            System.out.println(toBeAnded.get(i));
//            for (int j = 0; j < bucketsToSearchIn.size(); j++) {
//                int x=bucketsToSearchIn.get(j);
//                if(index.get(x)!=null){
//                    for (int k = 0; k < index.get(x).size(); k++) {
//                        Bucket b=Bucket.deserializeBucket(index.get(x).get(k));
//                        System.out.println(b.getBucketName());
//                        b.print();
//                        System.out.println("-----------");
//                    }
//                }
//            }
//            System.out.println("----------------------------");
                allSuitableBuckets.add(bucketsToSearchIn);
            }
        }
        Vector<Integer> BucketsPositions = IntersectonOfBuckets(allSuitableBuckets);
        Vector<String> AllPageReference = new Vector<>();
        for (int i = 0; i < BucketsPositions.size(); i++) {
            int x = BucketsPositions.get(i);
            if (index.get(x) != null) {
                for (int j = 0; j < index.get(x).size(); j++) {
                    Bucket b = Bucket.deserializeBucket(index.get(x).get(j));
                    Vector<String> currentPages = b.getSuitablePageReferencesOfSQLterm(toBeAnded);
                    AllPageReference.addAll(currentPages);
                }
            }
        }
        return AllPageReference;
    }

    public static Vector<Integer> IntersectonOfBuckets(Vector<Vector<Integer>> BucketsResults) {
        int minSoFar = BucketsResults.get(0).size();
        int indexOfbucket = 0;
        for (int i = 1; i < BucketsResults.size(); i++) {
            if (BucketsResults.get(i).size() < minSoFar) {
                minSoFar = BucketsResults.get(i).size();
                indexOfbucket = i;
            }
        }
        Vector<Integer> IntersectedBuckets = new Vector<>();
        // boolean matches = false;
        loop:
        for (int i = 0; i < minSoFar; i++) {
            for (int j = 0; j < BucketsResults.size(); j++) {
                if (!BucketsResults.get(j).contains(BucketsResults.get(indexOfbucket).get(i))) {
                    continue loop;
                }
            }
            IntersectedBuckets.add(BucketsResults.get(indexOfbucket).get(i));
            //matches=false;
        }
        return IntersectedBuckets;
    }
}
