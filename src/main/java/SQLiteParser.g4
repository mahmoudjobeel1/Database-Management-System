parser grammar SQLiteParser;

options {
    tokenVocab = SQLiteLexer;
}

parse: (sql_stmt_list)* EOF
;

sql_stmt_list:
    SCOL* sql_stmt (SCOL+ sql_stmt)* SCOL*
;

sql_stmt: (
          create_index_stmt
        | create_table_stmt
        | delete_stmt
        | insert_stmt
        | select_stmt
        | update_stmt
    )
;


create_index_stmt:
    CREATE_ INDEX_ ON_ table_name OPEN_PAR
    index_name (COMMA index_name)*  CLOSE_PAR
;

create_table_stmt:
    CREATE_ TABLE_ table_name (
        OPEN_PAR column_def (COMMA column_def)*
        (COMMA PRIMARY_ KEY_ OPEN_PAR primaryKey_name CLOSE_PAR ) CLOSE_PAR
    )
;

column_def:
    column_name type_name
;

delete_stmt:
    DELETE_ FROM_ (table_name)  WHERE_  (column_name ASSIGN literal_value (AND_ column_name ASSIGN literal_value)*)
;

select_stmt:
       SELECT_ ALL_ FROM_ (table_name) (WHERE_ select_expr ((operators1) select_expr )*)?
;

select_expr:
     column_name (operators2) literal_value
;

operators1:
    AND_ | OR_ | XOR_
;
operators2:
    LT | LT_EQ | GT | GT_EQ | ASSIGN | EQ | NOT_EQ1
;
literal_value:
    any_name
    | NUMERIC_LITERAL
    | STRING_LITERAL
    | BLOB_LITERAL
    | NULL_
    | TRUE_
    | FALSE_
    | CURRENT_TIME_
    | CURRENT_DATE_
    | CURRENT_TIMESTAMP_
;

insert_stmt:
      INSERT_  INTO_  table_name
     ( OPEN_PAR column_name literal_value (COMMA column_name literal_value)* CLOSE_PAR )
;


update_stmt:
     UPDATE_ table_name SET_ primaryKey_value
     OPEN_PAR
     (column_name literal_value (COMMA column_name literal_value)* )
     CLOSE_PAR
;

table_name:
    any_name
;

column_name:
    any_name
;

primaryKey_value:
    literal_value
;

primaryKey_name:
    any_name
;

index_name:
    any_name
;

type_name:
    any_name
;


any_name:
    IDENTIFIER
    | STRING_LITERAL
    | OPEN_PAR any_name CLOSE_PAR
;