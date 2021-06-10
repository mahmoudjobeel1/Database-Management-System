// Generated from E:/GUC/S6/Data Bases II/DP2project/src/main/java\SQLiteParser.g4 by ANTLR 4.9.1
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link SQLiteParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface SQLiteParserVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link SQLiteParser#parse}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParse(SQLiteParser.ParseContext ctx);
	/**
	 * Visit a parse tree produced by {@link SQLiteParser#sql_stmt_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSql_stmt_list(SQLiteParser.Sql_stmt_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link SQLiteParser#sql_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSql_stmt(SQLiteParser.Sql_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link SQLiteParser#create_index_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_index_stmt(SQLiteParser.Create_index_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link SQLiteParser#create_table_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_table_stmt(SQLiteParser.Create_table_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link SQLiteParser#column_def}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumn_def(SQLiteParser.Column_defContext ctx);
	/**
	 * Visit a parse tree produced by {@link SQLiteParser#delete_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDelete_stmt(SQLiteParser.Delete_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link SQLiteParser#select_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelect_stmt(SQLiteParser.Select_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link SQLiteParser#select_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelect_expr(SQLiteParser.Select_exprContext ctx);
	/**
	 * Visit a parse tree produced by {@link SQLiteParser#operators1}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOperators1(SQLiteParser.Operators1Context ctx);
	/**
	 * Visit a parse tree produced by {@link SQLiteParser#operators2}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOperators2(SQLiteParser.Operators2Context ctx);
	/**
	 * Visit a parse tree produced by {@link SQLiteParser#literal_value}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLiteral_value(SQLiteParser.Literal_valueContext ctx);
	/**
	 * Visit a parse tree produced by {@link SQLiteParser#insert_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInsert_stmt(SQLiteParser.Insert_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link SQLiteParser#update_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUpdate_stmt(SQLiteParser.Update_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link SQLiteParser#table_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_name(SQLiteParser.Table_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link SQLiteParser#column_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumn_name(SQLiteParser.Column_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link SQLiteParser#primaryKey_value}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimaryKey_value(SQLiteParser.PrimaryKey_valueContext ctx);
	/**
	 * Visit a parse tree produced by {@link SQLiteParser#primaryKey_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimaryKey_name(SQLiteParser.PrimaryKey_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link SQLiteParser#index_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndex_name(SQLiteParser.Index_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link SQLiteParser#type_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_name(SQLiteParser.Type_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link SQLiteParser#any_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAny_name(SQLiteParser.Any_nameContext ctx);
}