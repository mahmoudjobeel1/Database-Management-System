
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

public class SQLparser extends SQLiteParserBaseListener {
    private DBApp dbApp;

    public SQLparser(String sql ,DBApp dbApp){
        this.dbApp=dbApp;
        SQLiteLexer sqLiteLexer=new SQLiteLexer(CharStreams.fromString(sql));
        CommonTokenStream tokens=new CommonTokenStream(sqLiteLexer);
        SQLiteParser parser=new SQLiteParser(tokens);
        ParseTree tree=parser.sql_stmt();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(this, tree);
    }
    public SQLparser(String sql){
        SQLiteLexer sqLiteLexer=new SQLiteLexer(CharStreams.fromString(sql));
        CommonTokenStream tokens=new CommonTokenStream(sqLiteLexer);
        SQLiteParser parser=new SQLiteParser(tokens);
        ParseTree tree=parser.sql_stmt();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(this, tree);
    }
    public  SQLparser(){

    }
    @Override
    public void enterParse(SQLiteParser.ParseContext ctx) {

    }

    @Override
    public void enterCreate_index_stmt(SQLiteParser.Create_index_stmtContext ctx) throws DBAppException {
        String tableName=ctx.table_name().getText();
        List<SQLiteParser.Index_nameContext> temp=ctx.index_name();
        String [] colNames=new String[temp.size()];
        int i=0;
        for(SQLiteParser.Index_nameContext x:temp){
            colNames[i++]=x.getText();
        }
        dbApp.createIndex(tableName,colNames);
    }

    @Override
    public void enterCreate_table_stmt(SQLiteParser.Create_table_stmtContext ctx) throws DBAppException {
        String tableName =ctx.table_name().getText();
        List<SQLiteParser.Column_defContext> temp=ctx.column_def();

        Hashtable<String,String> htblColNameType=new Hashtable<>();
        Hashtable<String,String> htblColNameMin=new Hashtable<>();
        Hashtable<String,String> htblColNameMax=new Hashtable<>();

        for(SQLiteParser.Column_defContext x:temp){
            String colName=x.column_name().getText();
            String colDataType=x.type_name().getText().toLowerCase();
            switch (colDataType) {
                case "integer": htblColNameType.put(colName,"java.lang.Integer");
                                htblColNameMin.put(colName,"-2147483648");
                                htblColNameMax.put(colName,"2147483647");
                                break;
                case "double" : htblColNameType.put(colName,"java.lang.Double");
                                htblColNameMin.put(colName,"-9223372036854775");
                                htblColNameMax.put(colName,"9223372036854775");
                                break;
                case "string" : htblColNameType.put(colName,"java.lang.String");
                                htblColNameMin.put(colName,"a");
                                htblColNameMax.put(colName,"zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz");
                                break;
                case "date"   : htblColNameType.put(colName,"java.util.Date");
                                htblColNameMin.put(colName,"-999999999-01-01");
                                htblColNameMax.put(colName,"292278993-12-30");
                                break;
                default: throw new DBAppException("undefined data type");
            }

        }
        String strClusteringKeyColumn=ctx.primaryKey_name().getText();

        System.out.println(tableName+"\n"+strClusteringKeyColumn+"\n"+htblColNameType+"\n"+htblColNameMin+"\n"+htblColNameMax);
        dbApp.createTable(tableName,strClusteringKeyColumn,htblColNameType,htblColNameMin,htblColNameMax);

    }


    // DELETE_ FROM_ table_name (WHERE_ column_name ASSIGN literal_value )
    @Override
    public void enterDelete_stmt(SQLiteParser.Delete_stmtContext ctx) throws DBAppException {
        String tableName=ctx.table_name().getText();

        List<SQLiteParser.Column_nameContext> temp1=ctx.column_name();
        List<SQLiteParser.Literal_valueContext> temp2=ctx.literal_value();

        Hashtable<String,String> htblColNameValue=new Hashtable<>();

        //TODO check delete in db app (we need to handle object)
        for(int i=0;i<temp1.size();i++){
            String colName=temp1.get(i).getText();
            String value=temp2.get(i).getText();
            htblColNameValue.put(colName,value);
        }

        dbApp.deleteFromTable(tableName,Utilities.parseStringToObject(tableName,htblColNameValue));
    }

    @Override
    public void enterInsert_stmt(SQLiteParser.Insert_stmtContext ctx) throws DBAppException {
        String tableName=ctx.table_name().getText();
        //TODO parse datatype

        List<SQLiteParser.Column_nameContext> temp1=ctx.column_name();
        List<SQLiteParser.Literal_valueContext> temp2=ctx.literal_value();

        Hashtable<String,String> htblColNameValue=new Hashtable<>();

        for(int i=0;i<temp1.size();i++){
            String colName=temp1.get(i).getText();
            String value=temp2.get(i).getText();
            htblColNameValue.put(colName,value);
        }

        dbApp.insertIntoTable(tableName,Utilities.parseStringToObject(tableName,htblColNameValue));
    }

    @Override
    public void enterSelect_stmt(SQLiteParser.Select_stmtContext ctx) throws DBAppException {

        //TODO parse object values
        String tableName=ctx.table_name().getText();
        List<SQLiteParser.Select_exprContext> temp1=ctx.select_expr();

        Hashtable<String,String> hashtable=new Hashtable<>();
        SQLTerm [] sqlTerms=new SQLTerm[temp1.size()];
        int i=0;
        for(SQLiteParser.Select_exprContext x:temp1){
            hashtable.put(x.column_name().getText(),x.literal_value().getText());
        }

        Hashtable<String,Object> parsedData=Utilities.parseStringToObject(tableName,hashtable);

        for(SQLiteParser.Select_exprContext x:temp1){
            sqlTerms[i]=new SQLTerm();
            sqlTerms[i]._strTableName=tableName;
            sqlTerms[i]._strColumnName=x.column_name().getText();
            sqlTerms[i]._strOperator=x.operators2().getText();
            sqlTerms[i++]._objValue=parsedData.get(x.column_name().getText());
        }
        List<SQLiteParser.Operators1Context> temp2=ctx.operators1();
        String[] strarrOperators=new String[temp2.size()];
        i=0;
        for(SQLiteParser.Operators1Context x:temp2){
            strarrOperators[i++]=x.getText();
        }
        System.out.println(Arrays.toString(sqlTerms)+"\n"+Arrays.toString(strarrOperators));
        dbApp.selectFromTable(sqlTerms,strarrOperators);
    }

    @Override
    public void enterUpdate_stmt(SQLiteParser.Update_stmtContext ctx) throws DBAppException {
        String tableName=ctx.table_name().getText();
        //TODO parse datatype
        String strClusteringKeyValue=ctx.primaryKey_value().getText();
        List<SQLiteParser.Column_nameContext> temp1=ctx.column_name();
        List<SQLiteParser.Literal_valueContext> temp2=ctx.literal_value();

        Hashtable<String,String> htblColNameValue=new Hashtable<>();

        for(int i=0;i<temp1.size();i++){
            String colName=temp1.get(i).getText();
            String value=temp2.get(i).getText();
            htblColNameValue.put(colName,value);
        }

        //System.out.println(tableName+" "+strClusteringKeyValue+"\n"+htblColNameValue);
        dbApp.updateTable(tableName,strClusteringKeyValue,Utilities.parseStringToObject(tableName,htblColNameValue));
    }

    public void setDbApp(DBApp dbApp) {
        this.dbApp = dbApp;
    }

    public static void main(String[] args) {
        //String createIndexCheck = "create index on Student ( name , ID )";
                 String createTableCheck ="create table student (" +
                                          "name String, id integer , primary key (name) )";

        //String deleteCheck="DELETE from Student where name=Mahmoud and id=18406 ";

        //String insertCheck="insert into Student (name , id) values (mahmoud , 18)";

         //String updateCheck=" update student set 1840 (name mahmoud , id 468406)";

       // String checkSelect="select ALL from student where  age > 500 AND id=552";
        SQLparser sqLparser=new SQLparser(createTableCheck);
    }
}
