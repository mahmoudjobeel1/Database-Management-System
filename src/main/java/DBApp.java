import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;

public class DBApp implements DBAppInterface {

    // SO2AL ===> na5od el data el fel config w nsta3melha ezaiii??
    // SO2AL ===> hal adding attributes momken yebawaz test cases?
    static public int maximumRowsCountinPage;
    static public int maximumKeysCountinIndexBucket;

    public DBApp() {
        init();
        File f = new File("src//main//resources//data");
        f.mkdir();
    }

    @Override
    public void init() {
        readConfig();
        // do we need to add header to csv file??
        Utilities.addHeadertoCSVifEmpty();

    }

    @Override
    public void createTable(String tableName, String clusteringKey, Hashtable<String, String> colNameType,
                            Hashtable<String, String> colNameMin, Hashtable<String, String> colNameMax) throws DBAppException {

        if (Utilities.containsTableName(tableName)) {
            throw new DBAppException("Table already exists!");
        }

        Set<String> keys = colNameType.keySet();
        for (String colName : keys) {
            String colValue = colNameType.get(colName).toLowerCase();
            if (!(colValue.equals("java.lang.integer") || colValue.equals("java.lang.string")
                    || colValue.equals("java.lang.double") || colValue.equals("java.util.date"))) {
                throw new DBAppException("Unsupported data type.");
            }
        }
        if (colNameType.size() != colNameMax.size() || colNameMax.size() != colNameMin.size()) {
            throw new DBAppException("Inconsistent data for table");
        }

        Table t = new Table(tableName);

        // Serialization
        Table.serializeTable(t);

        for (String key : keys) {
            String[] data = new String[7];

            data[0] = tableName;

            String colName = key;
            data[1] = colName;
            data[2] = colNameType.get(colName);
            if (clusteringKey.equals(colName)) {
                data[3] = "True";
            } else {
                data[3] = "False";
            }
//SO2AL ====> ne3raf mnen el indexing
            data[4] = "False";
            data[5] = colNameMin.get(colName);
            data[6] = colNameMax.get(colName);
            Utilities.writeDataLineByLine(data);
        }
    }

    @Override
    public void createIndex(String tableName, String[] columnNames) throws DBAppException {
        //check index if true
   //    if (Utilities.checkIfIndexIsTrue(tableName, columnNames)) {
//
//            //throw new DBAppException("There is an index on a column already");
//        }
//
        if (!Utilities.containsTableName(tableName)) throw new DBAppException("Table does not exists!");
        for (int i = 0; i < columnNames.length; i++) {
            if(!Utilities.columnexist(tableName,columnNames[i])){
                throw new DBAppException("Column name does not exist!");
            }
        }

        Table t = Table.deserializeTable(tableName);
        Grid g = new Grid(tableName, t.NextGridName(), columnNames, maximumKeysCountinIndexBucket);
        t.getGridNames().add(g.getGridName());
        t.getGridDimensions().add(g.getDimensions());
        Grid.serializeGrid(g);
        Table.serializeTable(t);
        //update csv
        Utilities.updateIndexCSV(tableName, columnNames);

    }

    @Override
    public void insertIntoTable(String tableName, Hashtable<String, Object> colNameValue) throws DBAppException {
// Checking part..........................................................................
        if (!Utilities.containsTableName(tableName)) throw new DBAppException("Table does not exists!");
        // getting PK value
        String[] pkarray = Utilities.getPKofTableByItsName(tableName);
        String Clustering = pkarray[0];

        if (!colNameValue.containsKey(Clustering)) throw new DBAppException("No Primary Key inserted");

        // check data types throw exception if wrong
        checkInsertedDataTypes(tableName, colNameValue);
        // check data entered is between the min max limits throw error if wrong
        Utilities.checkTubleRange(tableName, colNameValue);
// ........................................................................................
        // get table (deserialize)
        Table t = Table.deserializeTable(tableName);
        Vector<Comparable> vctPageMin = t.getVecPageMinTuple();


        //String ClusteringDataType = pkarray[1];
        Comparable pkValue = (Comparable) colNameValue.get(Clustering);
        // ........................................
        // binary search on pages by pk
        int pagePos = getPositionBinarySearch(vctPageMin, pkValue);
        // System.out.println(pagePos);
        String pageName = null;
        if (pagePos == -1) {

            pageName = t.insert(colNameValue, maximumRowsCountinPage, 0, false);

        } else {
            pageName = t.insert(colNameValue, maximumRowsCountinPage, pagePos, false);
        }
        Table.serializeTable(t);
        //loop over all grids
        for (int i = 0; i < t.getGridNames().size(); i++) {
            Grid g = Grid.deserializeGrid(t.getGridNames().get(i));
            g.insertTuple(maximumKeysCountinIndexBucket, colNameValue, pageName);
            Grid.serializeGrid(g);
        }
    }

    @Override
    public void updateTable(String tableName, String clusteringKeyValue, Hashtable<String, Object> columnNameValue)
            throws DBAppException {
        // check if the col already exist
        // check if it is valued data type
        if (!Utilities.containsTableName(tableName)) {
            throw new DBAppException("Table does not exists!");
        }
        // check data types throw exception if wrong
        checkInsertedDataTypes(tableName, columnNameValue);
        // check data entered is between the min max limits throw error if wrong
        Utilities.checkTubleRange(tableName, columnNameValue);

        //check if update on primary key
        String[] y = Utilities.getPKofTableByItsName(tableName);
        if (columnNameValue.containsKey(y[0])) {
            throw new DBAppException("Can not update primary key value");
        }


        Table t = Table.deserializeTable(tableName);
        Vector<Comparable> vctPageMin = t.getVecPageMinTuple();
        Comparable x = Utilities.ParsePkdatatype(tableName, clusteringKeyValue);


        int pagePos = getPositionBinarySearch(vctPageMin, x);
        if (pagePos == -1) {
            throw new DBAppException("no clustering key Value exist");
        }
        Page p = t.deserializePagebyPosition(pagePos);
        int pos = p.getPKvalPositionBinarySearch(x, false);
        if (pos == -1) {
            throw new DBAppException("no clustering Key Value exist");
        }

        Hashtable<String, Object> tupleToDelete = new Hashtable<>();
        tupleToDelete.put(y[0], x);
        Vector<Hashtable<String, Object>> current = t.deleteFromPage(p.getPageFileName(), tupleToDelete, x);
        //System.out.println(current+" "+p.getPrimaryKeys());
        for (int i = 0; i < t.getGridNames().size(); i++) {
            //if(t.getGridNames().get(i).equals(g.getGridName()))
            //	continue;
            Grid g2 = Grid.deserializeGrid(t.getGridNames().get(i));
            g2.deleteAllTuplesGiven(current);
            Grid.serializeGrid(g2);
        }
        Hashtable<String, Object> updatedEntry = current.get(0);
        updatedEntry.remove("PageReference");
        Set<String> keys = columnNameValue.keySet();
        Iterator<String> itr = keys.iterator();
        while (itr.hasNext()) {
            String colName = itr.next();
            updatedEntry.put(colName, columnNameValue.get(colName));
        }
        Table.serializeTable(t);
        //t.print();
        insertIntoTable(tableName, updatedEntry);

        //Hashtable<String, String> colDataType = Utilities.getColumnDatatype(tableName);
//        Set<String> keys = columnNameValue.keySet();
//        Iterator<String> itr2 = keys.iterator();
//        Iterator<String> itr = keys.iterator();
//        while (itr2.hasNext()) {
//            String colName = itr2.next();
//            if (colName.equals(y[0])) {
//                throw new DBAppException("Can not change the primary key");
//            }
//        }
//        while (itr.hasNext()) {
//            String colName = itr.next();
//            p.getVecTuples().get(pos).put(colName, columnNameValue.get(colName));
//        }

    }


    @Override
    public void deleteFromTable(String tableName, Hashtable<String, Object> columnNameValue) throws DBAppException {
        if (!Utilities.containsTableName(tableName)) throw new DBAppException("Table does not exists!");
        checkInsertedDataTypes(tableName, columnNameValue);

        Table t = Table.deserializeTable(tableName);
        Vector<String> vec = t.getVecPageFileName();

        String[] pkarray = Utilities.getPKofTableByItsName(tableName);
        String Clustering = pkarray[0];
        if (t.getSuitableGrid(columnNameValue) != null || !t.getGridNames().isEmpty()) {
            if (t.getSuitableGrid(columnNameValue) == null) {
                //go delete from pages
                Vector<Hashtable<String, Object>> deletedTuples = bruteForceDelete(columnNameValue, t, vec, Clustering);
                for (int i = 0; i < t.getGridNames().size(); i++) {
                    Grid g = Grid.deserializeGrid(t.getGridNames().get(i));
                    g.deleteAllTuplesGiven(deletedTuples);
                    Grid.serializeGrid(g);
                }
            } else {
                Grid g = t.getSuitableGrid(columnNameValue);
                Vector<String> pagesToDeleteFrom = g.selectPagesOfTuple(columnNameValue);

                //To remove duplicates
                LinkedHashSet<String> lhSet = new LinkedHashSet<String>(pagesToDeleteFrom);
                pagesToDeleteFrom.clear();
                pagesToDeleteFrom.addAll(lhSet);
                //.........................


                Vector<Hashtable<String, Object>> deletedTuples = new Vector<>();

                Comparable pkValue = (Comparable) columnNameValue.get(Clustering);
                for (int i = 0; i < pagesToDeleteFrom.size(); i++) {
                    Vector<Hashtable<String, Object>> current = t.deleteFromPage(pagesToDeleteFrom.get(i), columnNameValue, pkValue);
                    deletedTuples.addAll(current);
                }
                for (int i = 0; i < t.getGridNames().size(); i++) {
                    //if(t.getGridNames().get(i).equals(g.getGridName()))
                    //	continue;
                    Grid g2 = Grid.deserializeGrid(t.getGridNames().get(i));
                    g2.deleteAllTuplesGiven(deletedTuples);
                    Grid.serializeGrid(g2);
                }
            }
        } else {
            bruteForceDelete(columnNameValue, t, vec, Clustering);
        }
        Table.serializeTable(t);
    }

    private Vector<Hashtable<String, Object>> bruteForceDelete(Hashtable<String, Object> columnNameValue, Table t, Vector<String> vec, String Clustering) {
        Vector<Hashtable<String, Object>> result = new Vector<>();
        if (columnNameValue.containsKey(Clustering)) {
            Comparable pkValue = (Comparable) columnNameValue.get(Clustering);
            int pagePos = getPositionBinarySearch(t.getVecPageMinTuple(), pkValue);
            if (pagePos != -1) {
                String pageName = t.getVecPageFileName().get(pagePos);
                Vector<Hashtable<String, Object>> subResult = t.deleteFromPage(pageName, columnNameValue, pkValue);
                result.addAll(subResult);
                Table.serializeTable(t);
            }
            return result;
        }

        for (int i = vec.size() - 1; i >= 0; i--) {
            String pageName = t.getVecPageFileName().get(i);
            Vector<Hashtable<String, Object>> subResult = t.deleteFromPage(pageName, columnNameValue, null);
            result.addAll(subResult);
        }
        Table.serializeTable(t);
        return result;

    }

    @Override
    public Iterator selectFromTable(SQLTerm[] sqlTerms, String[] arrayOperators) throws DBAppException {
        // check sqlterms validation
        //Hashtable<String,Object> hashtable=new Hashtable<>();
        for (SQLTerm x : sqlTerms) {
            Utilities.containsTableName(x._strTableName);
            Utilities.columnexist(x._strTableName, x._strColumnName);
            String datatype = Utilities.getColumnDatatype(x._strTableName).get(x._strColumnName);

            if (!((x._objValue.getClass().getName()).equals(datatype))) {
                throw new DBAppException("objValue datatype doesn't match column datatype");
            }
            //hashtable.put(x._strColumnName,x._objValue);
        }

        //try2
        //CASE if only one sqlterm
        if(sqlTerms.length==1){

            Vector<SQLTerm> terms=new Vector<>();
            terms.add(sqlTerms[0]);
            Vector<Hashtable<String, Object>> result =performAndOnSqlTerms(terms);
            return result.iterator();
        }else{
            Vector<Hashtable<String, Object>> result = perform(sqlTerms, arrayOperators);
            return result.iterator();
        }

        //try1
        /*

        Object [] postfixExpression=infixToPostfix(clearXOR(sqlTerms,arrayOperators));

        //execute postfix expression

        //make stacks and find out what to do
        Vector<Vector<SQLTerm>> allAndedTerms =getANDTerms(postfixExpression);

        Vector<Vector<Hashtable<String, Object>>> AllTuplesFromAnds=new Vector<>();
        for (Vector<SQLTerm> x:
             allAndedTerms) {
            Vector<Hashtable<String, Object>> tuples=performAndOnSqlTerms(x);
            AllTuplesFromAnds.add(tuples);
        }


         */


    }

    public static Vector<Hashtable<String, Object>> perform(SQLTerm[] sqlTerms, String[] arrayOperators) throws DBAppException {
        Object[] postfixExpression = infixToPostfix(merge(sqlTerms, arrayOperators));
        Stack<Object> stack = new Stack<>();
        for (Object x : postfixExpression) {
            if (x instanceof SQLTerm) {
                stack.push(x);
            } else {
                Object o1 = stack.pop();
                Object o2 = stack.pop();
                stack.add(execute((String) x, o1, o2));
            }
        }
        Vector<Hashtable<String, Object>> result = (Vector<Hashtable<String, Object>>) stack.pop();
        return result;
    }

    public static Object[] merge(SQLTerm[] sqlTerms, String[] arrayOperators) {
        Object[] arr = new Object[sqlTerms.length + arrayOperators.length];
        int j = 0;
        for (int i = 0; i < arrayOperators.length; i++) {
            arr[j++] = sqlTerms[i];
            arr[j++] = arrayOperators[i];
        }
        arr[j] = sqlTerms[sqlTerms.length - 1];
        return arr;
    }

    public static Vector<Hashtable<String, Object>> execute(String op, Object o1, Object o2) throws DBAppException {
        Vector<Hashtable<String, Object>> vector = new Vector<>();

        switch (op) {
            case "OR":
                vector = executeOR(o1, o2);
                break;
            case "AND":
                vector = executeAND(o1, o2);
                break;
            case "XOR":
                vector = executeXOR(o1, o2);
                break;
        }
        return vector;
    }

    public static Vector<Hashtable<String, Object>> executeXOR(Object o1, Object o2) throws DBAppException {
        if (o1 instanceof SQLTerm) {
            Vector<SQLTerm> v = new Vector<>();
            v.add((SQLTerm) o1);
            o1 = performAndOnSqlTerms(v);
        }
        if (o2 instanceof SQLTerm) {
            Vector<SQLTerm> v = new Vector<>();
            v.add((SQLTerm) o2);
            o2 = performAndOnSqlTerms(v);
        }
        Vector<Hashtable<String, Object>> v1 = (Vector<Hashtable<String, Object>>) o1;
        Vector<Hashtable<String, Object>> v2 = (Vector<Hashtable<String, Object>>) o2;
        Vector<Hashtable<String, Object>> v = new Vector<>();
        for (Hashtable<String, Object> x : v1) {
            if (v2.contains(x)) {
                v.add(x);
            }
        }
        for (Hashtable<String, Object> x : v) {
            v1.remove(x);
            v2.remove(x);
        }
        v1.addAll(v2);
        return v1;
    }

    private static Vector<Hashtable<String, Object>> removeDuplicates(Vector<Vector<Hashtable<String, Object>>> allTuplesFromAnds) {
        Vector<Hashtable<String, Object>> result = new Vector<>();
        for (Vector<Hashtable<String, Object>> x : allTuplesFromAnds) {
            for (Hashtable<String, Object> y : x) {
                if (!result.contains(y)) {
                    result.add(y);
                }
            }
        }
        return result;
    }

    public static Vector<Hashtable<String, Object>> performAndOnSqlTerms(Vector<SQLTerm> toBeAnded) throws DBAppException {
        //TODO tobeanded cant be empty!!
        String tableName = toBeAnded.get(0)._strTableName;
        Hashtable<String, Object> hashtable = new Hashtable<>();
        for (SQLTerm x : toBeAnded) {
            hashtable.put(x._strColumnName, x._objValue);
        }
        Table t = Table.deserializeTable(tableName);
        Grid g = t.getSuitableGrid(hashtable);
        Vector<String> pageReferences = null;
        if (g == null) {
            pageReferences = t.getVecPageFileName();
        } else {
            pageReferences = g.getSuitablePageReferencesOfSQLterm(toBeAnded);
        }

        LinkedHashSet<String> lhSet = new LinkedHashSet<String>(pageReferences);

        //clear the vector
        pageReferences.clear();

        //add all unique elements back to the vector
        pageReferences.addAll(lhSet);

        Vector<Hashtable<String, Object>> allTuples = new Vector<>();
        for (int i = 0; i < pageReferences.size(); i++) {
            Page p = Page.deserializePage(pageReferences.get(i));
            Vector<Hashtable<String, Object>> someTuples = p.getSuitableTuplesOfSQLterm(toBeAnded);
            allTuples.addAll(someTuples);
        }
        return allTuples;
    }

    public static int Prec(String st) throws DBAppException {
        switch (st) {
            case "OR":
                return 1;
            case "AND":
                return 2;
            case "XOR":
                return 3;
        }
        throw new DBAppException("Invalid operator");
    }

    public static Object[] infixToPostfix(Object[] arr) throws DBAppException {
        Object[] postfix = new Object[arr.length];
        int i = 0;
        Stack<Object> stack = new Stack<>();
        for (int j = 0; j < arr.length; j++) {
            Object temp = arr[j];
            if (temp instanceof SQLTerm) {
                postfix[i++] = temp;
            } else {
                while (!stack.isEmpty() && Prec((String) temp) < Prec((String) stack.peek()))
                    postfix[i++] = stack.pop();
                stack.push(temp);
            }
        }
        while (!stack.isEmpty()) postfix[i++] = stack.pop();
        return postfix;
    }

    public static Object[] clearXOR(SQLTerm[] sqlTerms, String[] arrayOperators) throws DBAppException {

        while (true) {
            int i = 0;
            boolean f = false;
            in:
            while (i < arrayOperators.length) {

                if (arrayOperators[i].equals("XOR")) {
                    f = true;
                    break in;
                }
                i++;
            }
            if (!f) {
                break;
            }

            SQLTerm[] newSqlTerms = new SQLTerm[sqlTerms.length + 2];
            String[] newArrayOperators = new String[arrayOperators.length + 2];
            for (int j = 0; j < i; j++) {
                newSqlTerms[j] = sqlTerms[j];
                newArrayOperators[j] = arrayOperators[j];
            }
            SQLTerm x = sqlTerms[i];
            //SQLTerm notX=new SQLTerm(sqlTerms[i]._strTableName,sqlTerms[i]._strColumnName,NotOperator.parseOperator(sqlTerms[i]._strOperator).apply(),sqlTerms[i]._objValue);
            SQLTerm notX = new SQLTerm();
            notX._strTableName = sqlTerms[i]._strTableName;
            notX._strColumnName = sqlTerms[i]._strColumnName;
            notX._strOperator = NotOperator.parseOperator(sqlTerms[i]._strOperator).apply();
            notX._objValue = sqlTerms[i]._objValue;

            SQLTerm y = sqlTerms[i + 1];
            //	SQLTerm notY=new SQLTerm(sqlTerms[i+1]._strTableName,sqlTerms[i+1]._strColumnName,NotOperator.parseOperator(sqlTerms[i+1]._strOperator).apply(),sqlTerms[i+1]._objValue);
            SQLTerm notY = new SQLTerm();
            notY._strTableName = sqlTerms[i + 1]._strTableName;
            notY._strColumnName = sqlTerms[i + 1]._strColumnName;
            notY._strOperator = NotOperator.parseOperator(sqlTerms[i + 1]._strOperator).apply();
            notY._objValue = sqlTerms[i + 1]._objValue;

            newSqlTerms[i] = x;
            newSqlTerms[i + 1] = notY;
            newSqlTerms[i + 2] = notX;
            newSqlTerms[i + 3] = y;

            newArrayOperators[i] = "AND";
            newArrayOperators[i + 1] = "OR";
            newArrayOperators[i + 2] = "AND";


            for (int j = i + 1; j < arrayOperators.length; j++) {
                newArrayOperators[j + 2] = arrayOperators[j];
            }

            for (int j = i; j + 2 < sqlTerms.length; j++) {
                newSqlTerms[j + 4] = sqlTerms[j + 2];
            }
            sqlTerms = newSqlTerms;
            arrayOperators = newArrayOperators;

        }
        Object[] arr = new Object[sqlTerms.length + arrayOperators.length];
        int j = 0;
        for (int k = 0; k < arrayOperators.length; k++) {
            arr[j++] = sqlTerms[k];
            arr[j++] = arrayOperators[k];
        }
        arr[j] = sqlTerms[arrayOperators.length];
//		HashMap<SQLTerm [],String []> map=new HashMap<>();
//		map.put(sqlTerms,arrayOperators);
        return arr;
    }


    private static Vector<Hashtable<String, Object>> executeOR(Object o1, Object o2) throws DBAppException {
        if (o1 instanceof SQLTerm) {
            Vector<SQLTerm> v = new Vector<>();
            v.add((SQLTerm) o1);
            o1 = performAndOnSqlTerms(v);
        }
        if (o2 instanceof SQLTerm) {
            Vector<SQLTerm> v = new Vector<>();
            v.add((SQLTerm) o2);
            o2 = performAndOnSqlTerms(v);
        }
        Vector<Hashtable<String, Object>> v1 = (Vector<Hashtable<String, Object>>) o1;
        Vector<Hashtable<String, Object>> v2 = (Vector<Hashtable<String, Object>>) o2;
        Vector<Vector<Hashtable<String, Object>>> operands = new Vector<>();
        operands.add(v1);
        operands.add(v2);
        Vector<Hashtable<String, Object>> result = removeDuplicates(operands);
        return result;
    }


    private static Vector<Hashtable<String, Object>> executeAND(Object operandA, Object operandB) throws DBAppException {
        if (operandA instanceof SQLTerm && operandB instanceof SQLTerm) {
            Vector<SQLTerm> terms = new Vector<>();
            terms.add((SQLTerm) operandA);
            terms.add((SQLTerm) operandB);
            return performAndOnSqlTerms(terms);
        } else if (operandA instanceof SQLTerm || operandB instanceof SQLTerm) {
            Vector<Hashtable<String, Object>> tuples = (Vector<Hashtable<String, Object>>) ((operandA instanceof SQLTerm) ? operandB : operandA);
            SQLTerm term = (SQLTerm) ((operandB instanceof SQLTerm) ? operandB : operandA);

            Vector<SQLTerm> terms = new Vector<>();
            terms.add(term);
            return Page.getHashtablesCompatibleWithSQlTerms(tuples, terms);
        } else {
            Vector<Hashtable<String, Object>> tuplesA = (Vector<Hashtable<String, Object>>) operandA;
            Vector<Hashtable<String, Object>> tuplesB = (Vector<Hashtable<String, Object>>) operandB;

            Vector<Hashtable<String, Object>> result = new Vector<>();

            for (int i = 0; i < tuplesA.size(); i++) {
                Hashtable<String, Object> current = tuplesA.get(i);
                if (tuplesB.contains(current)) {
                    result.add(current);
                }
            }
            return result;
        }
    }


    // reading .config
    public void readConfig() {
        Properties prop = new Properties();
        String fileName = "DBApp.config";
        InputStream is = null;
        File f = new File("src//main//resources//DBApp.config");
        try {
            is = new FileInputStream(f);
        } catch (FileNotFoundException ex) {
            System.out.println("No .config found");
        }
        try {
            prop.load(is);
        } catch (IOException ex) {
            System.out.println("Ambigious error!!");
        }
        maximumRowsCountinPage = Integer.parseInt(prop.getProperty("MaximumRowsCountinPage"));
        maximumKeysCountinIndexBucket = Integer.parseInt(prop.getProperty("MaximumKeysCountinIndexBucket"));
    }

    private Comparable getValueWithType(String PKdataType, String strValue) {
        Comparable c;

        if (PKdataType.equalsIgnoreCase("java.lang.integer")) {
            c = Integer.parseInt(strValue);
        } else if (PKdataType.equalsIgnoreCase("java.lang.string")) {
            c = strValue;
        } else if (PKdataType.equalsIgnoreCase("java.lang.double")) {
            c = Double.parseDouble(strValue);
        } else {
            c = Date.parse(strValue);
        }
        return c;
    }


    private void checkInsertedDataTypes(String tableName, Hashtable<String, Object> colNameValue)
            throws DBAppException {
        Set<String> keys = colNameValue.keySet();
        Hashtable<String, String> nameDataType = Utilities.getColumnDatatype(tableName);
        for (String colName : keys) {
            Object colValue = colNameValue.get(colName);
            // System.out.println(colValue.getClass().getName());

            if (nameDataType.containsKey(colName)) {
                String currentDataType = nameDataType.get(colName).toLowerCase();
                // 5ali balak men el quotes
                if (!currentDataType.equals(colValue.getClass().getName().toLowerCase())) {
                    //System.out.println(currentDataType + " " + colValue.getClass().getName());
                    throw new DBAppException("Incompatible data types!");
                }
            } else {
                throw new DBAppException("Column does not exist");
            }

        }

    }

    private static int getPositionBinarySearch(Vector<Comparable> pageMin, Comparable valueOfColumn) {
        int l = 0;
        int r = pageMin.size() - 1;
        while (l <= r) {
            int m = l + (r - l) / 2;

            // Check if x is present at mid
            if (pageMin.get(m).compareTo(valueOfColumn) == 0) {
                //CAREFUL!!!!!.........................................................
                //System.out.println("MIGHT BE AN ERROR");
                return m;
            }
            // If x greater, ignore left half
            else if (pageMin.get(m).compareTo(valueOfColumn) < 0)
                l = m + 1;

                // If x is smaller, ignore right half
            else
                r = m - 1;
        }
        // System.out.println(l+" " +r);
        // if we reach here, then element was
        // not present
        return r;
    }

    public static Vector<Vector<SQLTerm>> getANDTerms(Object[] postfixExpresion) {
        Vector<Vector<SQLTerm>> result = new Vector<>();
        Stack<Vector<SQLTerm>> stack = new Stack<>();
        for (Object x : postfixExpresion) {
            if (x instanceof String) {
                if (x.equals("AND")) {
                    Vector<SQLTerm> temp1 = stack.pop();
                    Vector<SQLTerm> temp2 = stack.pop();
                    temp1.addAll(temp2);
                    stack.add(temp1);
                } else {
                    Vector<SQLTerm> temp1 = stack.pop();
                    Vector<SQLTerm> temp2 = stack.pop();
                    result.add(temp1);
                    result.add(temp2);
                }
            } else {
                Vector<SQLTerm> temp = new Vector<>();
                temp.add((SQLTerm) x);
                stack.add(temp);
            }
        }
        //System.out.println(stack.size());
        while (!stack.isEmpty()) {
            result.add(stack.pop());
        }
        return result;
    }

    public static void main(String[] args) throws DBAppException {
        String strTableName = "Student";
        DBApp dbApp = new DBApp();
        Hashtable htblColNameType = new Hashtable();
        htblColNameType.put("id", "java.lang.Integer");
        htblColNameType.put("name", "java.lang.String");
        htblColNameType.put("gpa", "java.lang.Double");
        htblColNameType.put("graduationDate", "java.util.Date");

        Hashtable htblColNameMin = new Hashtable();
        htblColNameMin.put("id", "0");
        htblColNameMin.put("name", "aaaaaa");
        htblColNameMin.put("gpa", "0");
        htblColNameMin.put("graduationDate", new SimpleDateFormat("yyyy-MM-dd").format(new Date(100, 9, 6)));

        Hashtable htblColNameMax = new Hashtable();
        htblColNameMax.put("id", "200");
        htblColNameMax.put("name", "zzzzzz");
        htblColNameMax.put("gpa", "10.0");
        htblColNameMax.put("graduationDate", new SimpleDateFormat("yyyy-MM-dd").format(new Date(200, 9, 6)));


        Hashtable<String, Object> row = new Hashtable();

        //row.put("age", 54);
        //Hashtable<String, Object> row2 = new Hashtable();
        //row2.put(("age" , 24))
        //row.put("graduationDate",new Date(120,11,1));
        row.put("gpa", 3.5);
        //row.put("id", 12);
        //row.put("name", "ahmed18");

        // Date dob = new Date(1993 - 1900, 11 - 1, 21);
        // row.put("name", "Ahmed Noor 2");

        boolean create = false;
        if (create) {
            dbApp.createTable(strTableName, "id", htblColNameType, htblColNameMin, htblColNameMax);

            dbApp.createIndex("Student", new String[]{"name"});
            dbApp.createIndex("Student", new String[]{"gpa", "graduationDate"});
        }
        boolean insert=true;
        int d=0;
        for (int i = 51; i < 52; i++) {
            Hashtable htblColNameValue = new Hashtable();
            htblColNameValue.put("id", new Integer(i));
            htblColNameValue.put("gpa", 4.0);
            htblColNameValue.put("name", "ahmed" + i);
            htblColNameValue.put("graduationDate", new Date(101+i,11,i));
            if (insert) {
                dbApp.insertIntoTable("Student", htblColNameValue);
            }
        }
        //int e=10;
        for (int i = 74; i < 75; i++) {

            Hashtable htblColNameValue = new Hashtable();
            htblColNameValue.put("id", new Integer(i));
            htblColNameValue.put("gpa", new Double(8.0));
            htblColNameValue.put("name", "fyifh");
            htblColNameValue.put("graduationDate", new Date(101,11,10));
            if (insert) {
               // dbApp.insertIntoTable("Student", htblColNameValue);
            }
        }
        //row.put("age", 45);
         dbApp.deleteFromTable("Student", row);
        //dbApp.updateTable("Student", "100", row);

        //dbApp.updateTable("Student","12",row);


        SQLTerm[] arrSQLTerms;
        arrSQLTerms = new SQLTerm[2];
        for (int i = 0; i < arrSQLTerms.length; i++) {
            arrSQLTerms[i] = new SQLTerm();
        }
        arrSQLTerms[0]._strTableName = "Student";
        arrSQLTerms[0]._strColumnName = "id";
        arrSQLTerms[0]._strOperator = "<=";
        arrSQLTerms[0]._objValue = new Integer(20);
        arrSQLTerms[1]._strTableName = "Student";
        arrSQLTerms[1]._strColumnName = "name";
        arrSQLTerms[1]._strOperator = "<";
        arrSQLTerms[1]._objValue = new String("peter46");
//        arrSQLTerms[2]._strTableName = "Student";
//        arrSQLTerms[2]._strColumnName = "name";
//        arrSQLTerms[2]._strOperator = "=";
//        arrSQLTerms[2]._objValue = "ahmed23";

        String[] strarrOperators = new String[1];
        strarrOperators[0] = "OR";
        //strarrOperators[1] = "XOR";
        if (true) {
            Iterator resultSet = dbApp.selectFromTable(arrSQLTerms, strarrOperators);
            int c = 0;
            while (resultSet.hasNext()) {
                System.out.println(resultSet.next());
                c++;
            }
            System.out.println("size = " + c);
        }
        Table t = Table.deserializeTable("Student");
        t.print();
        //System.out.println(getPerm(new int[]{-1,-1,-1,5}));
    }
}