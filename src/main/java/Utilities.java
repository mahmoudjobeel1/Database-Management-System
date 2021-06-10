import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import com.opencsv.CSVWriter;

public class Utilities {

    public static void writeDataLineByLine(String[] data) {
        // first create file object for file placed at location
        // specified by filepath
        File file = new File("src//main//resources//metadata.csv");
        try {
            // create FileWriter object with file as parameter
            FileWriter outputfile = new FileWriter(file, true);

            // create CSVWriter object filewriter object as parameter
            CSVWriter writer = new CSVWriter(outputfile);

            // add data to csv
            writer.writeNext(data);

            // closing writer connection
            writer.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void addHeadertoCSVifEmpty() {
        File file = new File("src//main//resources//metadata.csv");
        if (file.length() == 0) {
            try {
                // create FileWriter object with file as parameter
                FileWriter outputfile = new FileWriter(file, true);
                // create CSVWriter object filewriter object as parameter
                CSVWriter writer = new CSVWriter(outputfile);
                // add data to csv
                writer.writeNext(new String[]{"Table Name", "Column Name", "Column Type", "ClusteringKey", "Indexed",
                        "min", "max"});
                // closing writer connection
                writer.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }

    public static Vector<String[]> read() {
        String line = "";
        Vector<String[]> res = new Vector<String[]>();
        try {

            BufferedReader br = new BufferedReader(new FileReader("src//main//resources//metadata.csv"));
            while ((line = br.readLine()) != null) // returns a Boolean value
            {
                String[] straTableColumn = line.split(","); // use comma as separator
                res.add(straTableColumn);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }

    public static boolean containsTableName(String tname) {
        Vector<String[]> v = read();
        for (int i = 0; i < v.size(); i++) {

            if (v.get(i)[0].equals("\"" + tname + "\"")) {
                return true;
            }
        }
        return false;
    }

    public static Hashtable<String, String> getColumnDatatype(String tname) {
        Hashtable<String, String> ht = new Hashtable<>();
        Vector<String[]> v = Utilities.read();
        for (int i = 0; i < v.size(); i++) {

            if (v.get(i)[0].equals("\"" + tname + "\"")) {
                String s1 = v.get(i)[1].substring(1, v.get(i)[1].length() - 1);
                String s2 = v.get(i)[2].substring(1, v.get(i)[2].length() - 1);
                ht.put(s1, s2);
            }
        }
        return ht;

    }

    public static String[] getPKofTableByItsName(String tname) {
        String[] res = new String[2];
        Vector<String[]> v = Utilities.read();
        for (int i = 0; i < v.size(); i++) {

            if (v.get(i)[0].equals("\"" + tname + "\"")) {
                String s1 = v.get(i)[1].substring(1, v.get(i)[1].length() - 1);
                String s2 = v.get(i)[3].substring(1, v.get(i)[3].length() - 1);

                if (Boolean.parseBoolean(s2.toLowerCase())) {
                    res[0] = s1;
                    res[1] = v.get(i)[2].substring(1, v.get(i)[2].length() - 1);
                }
            }
        }
        return res;

    }

    public static void checkTubleRange(String strTableName, Hashtable<String, Object> htblColNameValue)
            throws DBAppException {
        Set<String> keys = htblColNameValue.keySet();
        Iterator<String> itr = keys.iterator();
        while (itr.hasNext()) {
            String colName = itr.next();
            Comparable value = (Comparable) htblColNameValue.get(colName);
            try {
                if (!(checkColRange(strTableName, colName, value))) {
                    throw new DBAppException("Value inserted out of bound");
                }
            } catch (ParseException e) {
                throw new DBAppException("Date Parsing error");
            }
        }
    }

    public static boolean checkColRange(String tname, String colname, Comparable value) throws ParseException {
        Vector<String[]> vector = Utilities.read();
        Comparable min = -1, max = -1;
        for (String[] st : vector) {
            String tablename = st[0].substring(1, st[0].length() - 1);
            String columnname = st[1].substring(1, st[1].length() - 1);
            if (tablename.equals(tname) && colname.equals(columnname)) {
                String temp = st[2].substring(1, st[2].length() - 1);
                String[] t = temp.split("[.]");
                if (t[2].equalsIgnoreCase("Integer")) {
                    min = Integer.parseInt(st[5].substring(1, st[5].length() - 1));
                    max = Integer.parseInt(st[6].substring(1, st[6].length() - 1));
                    break;
                } else if (t[2].equalsIgnoreCase("Double")) {
                    min = Double.parseDouble(st[5].substring(1, st[5].length() - 1));
                    max = Double.parseDouble(st[6].substring(1, st[6].length() - 1));
                    break;
                } else if (t[2].equalsIgnoreCase("String")) {
                    min = st[5].substring(1, st[5].length() - 1);
                    max = st[6].substring(1, st[6].length() - 1);
                    String maxLen = (String) max;
                    String val =(String) value;
                    if(maxLen.length()<val.length())return false;
                    break;
                } else if (t[2].equalsIgnoreCase("Date")) {
                    //System.out.println(st[5].substring(1, st[5].length() - 1));
                    min = StringToDate(st[5].substring(1, st[5].length() - 1));
                    max = StringToDate(st[6].substring(1, st[5].length() - 1));
                    break;
                }
            }
        }
        int l = value.compareTo(min);
        int r = max.compareTo(value);
        if (l >= 0 && r >= 0) {
            return true;
        }
        return false;
    }

    public static Date StringToDate(String dob) throws ParseException {
        // Instantiating the SimpleDateFormat class
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        // Parsing the given String to Date object
        Date date = formatter.parse(dob);

        return date;
    }

    public static Comparable ParsePkdatatype(String tableName, String clusteringKeyValue) throws DBAppException {
        // TODO Auto-generated method stub
        String[] getPKDataType = getPKofTableByItsName(tableName);
        String[] t = getPKDataType[1].split("[.]");
        if (t[2].equalsIgnoreCase("Integer")) {
            return Integer.parseInt(clusteringKeyValue);
        } else if (t[2].equalsIgnoreCase("Double")) {
            return Double.parseDouble(clusteringKeyValue);
        } else if (t[2].equalsIgnoreCase("String")) {
            return clusteringKeyValue;
        } else if (t[2].equalsIgnoreCase("Date")) {
            try {
                return StringToDate(clusteringKeyValue);
            } catch (ParseException e) {
                throw new DBAppException("Cannot parse date");
            }
        }

        return null;
    }

    public static Hashtable<String,Object> parseStringToObject(String tableName, Hashtable<String,String> hashtable) throws DBAppException {
        Hashtable<String, Object> temp=new Hashtable<String,Object>();
        Hashtable<String,String> colDataType=getColumnDatatype(tableName);

        for (Map.Entry<String,String> entry : hashtable.entrySet()){
            String dataType=colDataType.getOrDefault(entry.getKey(),null);
            if(dataType==null){
                throw  new DBAppException("the column name doesn't exist");
            }
            String[] t = dataType.split("[.]");
            if (t[2].equalsIgnoreCase("Integer")) {
                temp.put(entry.getKey(),Integer.parseInt(entry.getValue()));
            } else if (t[2].equalsIgnoreCase("Double")) {
                temp.put(entry.getKey(),Double.parseDouble(entry.getValue()));
            } else if (t[2].equalsIgnoreCase("String")) {
                temp.put(entry.getKey(),entry.getValue());
            } else if (t[2].equalsIgnoreCase("Date")) {
                try {
                    temp.put(entry.getKey(),StringToDate(entry.getValue()));
                } catch (ParseException e) {
                    throw new DBAppException("Cannot parse date");
                }
            }else{
                throw new DBAppException("undefined dataType");
            }
        }

        return  temp;
    }
    public static boolean columnexist(String tableName, String colName) {
        Vector<String[]> vector = Utilities.read();
        for (String[] st : vector) {
            String tablename = st[0].substring(1, st[0].length() - 1);
            String columnname = st[1].substring(1, st[1].length() - 1);
            if (tablename.equals(tablename) && colName.equals(columnname))
                return true;
        }
        return false;
    }

    //MIRAL
    public static Boolean checkIfIndexIsTrue(String strTableName, String[] strarrColName) {
        Vector<String[]> v = Utilities.read();
        for (int i = 0; i < v.size(); i++) {
            if (v.get(i)[0].equals("\"" + strTableName + "\""))
                for (int j = 0; j < strarrColName.length; j++)
                    if (v.get(i)[1].equals("\"" + strarrColName[j] + "\"")&&v.get(i)[4].equals("\"True\""))
                        return true;
        }
        return false;
    }

    public static Vector<String[]> updateIndexCSVhelper(String tableName, String[] columnNames) {
        Vector<String[]> v = Utilities.read();
        for (int i = 0; i < v.size(); i++) {
            for (int j = 0; j < v.get(i).length; j++) {
                v.get(i)[j]=v.get(i)[j].substring(1,v.get(i)[j].length()-1);
            }
            if (v.get(i)[0].equals(tableName))
                for (int j = 0; j < columnNames.length; j++)
                    if (v.get(i)[1].equals(columnNames[j])){
                        v.get(i)[4]="True";
                    }
        }
        return v;
    }
    public static void updateIndexCSV(String tableName, String[] columnNames) {
       Vector<String[]> New=updateIndexCSVhelper(tableName, columnNames);
       deleteCsv();
       for (int i = 0; i <New.size(); i++) {
           writeDataLineByLine(New.get(i));
       }
    }

    private static void deleteCsv() {
        File file = new File("src//main//resources//metadata.csv");
        try {
            // create FileWriter object with file as parameter
            FileWriter outputfile = new FileWriter(file, false);

            // create CSVWriter object filewriter object as parameter
            CSVWriter writer = new CSVWriter(outputfile);

            // closing writer connection
            writer.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    public static void removeFromDisk(String pageFileName) {
        try
        {
            Files.deleteIfExists(Paths.get("src//main//resources//data//"+pageFileName+".ser"));
        }
        catch(NoSuchFileException e)
        {
            System.out.println("No such file/directory exists");
        }
        catch(DirectoryNotEmptyException e)
        {
            System.out.println("Directory is not empty.");
        }
        catch(IOException e)
        {
            System.out.println("Invalid permissions.");
        }

        //System.out.println("Deletion successful.");
    }
    public static BigInteger calc_Bigint(String x, int index_diff) {
        x=x.toLowerCase();
        int str_len = x.length();
        BigInteger base = new BigInteger("256");
        BigInteger result = new BigInteger("0");
        int raised_power=Math.max(0,index_diff);
        char curr;
        for (int i = 0; i < str_len; i++) {
            raised_power ++;
            curr = x.charAt(str_len - i - 1);
            BigInteger curr_BigInteger = BigInteger.valueOf(curr);
            BigInteger n=(base.pow(raised_power));
            n=n.multiply(curr_BigInteger);
            result = result.add(n);
        }

        return result;

    }
    public static Comparable[] range(String tname, String colname) throws ParseException {
        Vector<String[]> vector = Utilities.read();
        Comparable min, max;
        ArrayList<Comparable> ranges = new ArrayList<Comparable>();
        for (String[] st : vector) {
            String tableName = st[0].substring(1, st[0].length() - 1);
            String columnName = st[1].substring(1, st[1].length() - 1);
            if (tableName.equals(tname) && colname.equals(columnName)) {
                String temp = st[2].substring(1, st[2].length() - 1);
                String[] t = temp.split("[.]");
                if (t[2].equalsIgnoreCase("Integer")) {
                    min = Integer.parseInt(st[5].substring(1, st[5].length() - 1));
                    max = Integer.parseInt(st[6].substring(1, st[6].length() - 1));
                    int total_length = (int) max - (int) min;
                    int subrange_length = total_length / 10;
                    int current_start = (int) min;
                    for (int i = 0; i < 10; ++i) {
                        ranges.add(i, current_start);
                        current_start += subrange_length;
                    }
                    break;
                } else if (t[2].equalsIgnoreCase("Double")) {
                    min = Double.parseDouble(st[5].substring(1, st[5].length() - 1));
                    max = Double.parseDouble(st[6].substring(1, st[6].length() - 1));
                    double total_length = (Double) max - (Double) min;
                    double subrange_length = total_length / 10;
                    double current_start = (Double) min;
                    for (int i = 0; i < 10; ++i) {
                        ranges.add(i , current_start);
                        current_start += subrange_length;
                    }
                    break;
                } else if (t[2].equalsIgnoreCase("Date")) {
                    min = Utilities.StringToDate(st[5].substring(1, st[5].length() - 1));
                    max = Utilities.StringToDate(st[6].substring(1, st[5].length() - 1));
                    int frequency = 10;
                    Date startDate = (Date) min;
                    Date endDate = (Date) max;
                    Long intervalSize = (endDate.getTime() - startDate.getTime()) / frequency;
                    for (int i = 0; i < frequency && intervalSize > 0; i++) {
                        Date date = new Date(startDate.getTime() + intervalSize * i);
                        ranges.add(date);
                    }
                    break;
                } else if (t[2].equalsIgnoreCase("String")) {
                    min = st[5].substring(1, st[5].length() - 1);
                    max = st[6].substring(1, st[6].length() - 1);
                    BigInteger[] str_range=range_helper_str((String)min,(String)max);
                    ranges.addAll(Arrays.asList(str_range));
                }

            }
        }
        Comparable[] result=new Comparable[ranges.size()];
        for (int i = 0; i < ranges.size(); i++) {
            result[i]=ranges.get(i);
        }
        return result;
    }

    public static BigInteger[] range_helper_str(String min, String max) {
        BigInteger[] result= new BigInteger[10];
        BigInteger min_interval = new BigInteger("0");
        BigInteger max_interval = new BigInteger("0");
        BigInteger offsite = new BigInteger("0");
        int min_len = min.length();
        int max_len = max.length();
        if(min_len>max_len) {
            int index_diff=min.length()-max.length();
            min_interval= Utilities.calc_Bigint(min, 0);
            max_interval= Utilities.calc_Bigint(max,index_diff);
        }
        else if (max_len>min_len) {
            int index_diff=max.length()-min.length();
            min_interval= Utilities.calc_Bigint(min, index_diff);
            max_interval= Utilities.calc_Bigint(max,0);
        }
        else {
            min_interval= Utilities.calc_Bigint(min,0);
            max_interval= Utilities.calc_Bigint(max,0);
        }
        BigInteger division_points = BigInteger.valueOf(10);
        offsite=(max_interval.subtract(min_interval)).divide(division_points);
        result[0]=min_interval;
        BigInteger previous = new BigInteger("0");
        previous=min_interval;
        for(int i=1;i<10;i++) {
            result[i]=previous.add(offsite);
            previous=result[i];
        }
        return result;
    }



    public static int maxLength(String tname, String colname)  {
        Vector<String[]> vector = Utilities.read();
        String min, max;
        for (String[] st : vector) {
            String tableName = st[0].substring(1, st[0].length() - 1);
            String columnName = st[1].substring(1, st[1].length() - 1);
            if (tableName.equals(tname) && colname.equals(columnName)) {
                String temp = st[2].substring(1, st[2].length() - 1);
                String[] t = temp.split("[.]");
                min = st[5].substring(1, st[5].length() - 1);
                max = st[6].substring(1, st[6].length() - 1);
                return Math.max(min.length(),max.length());
            }
        }

        return 0;
    }

}
