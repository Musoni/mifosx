/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.jdbc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * creates an instance that lets you call methods against it to build a SQL statement one step at a time.
 * 
 * @see http://www.mybatis.org/mybatis-3/statement-builders.html
 * @see http://grepcode.com/file/repo1.maven.org/maven2/org.mybatis/mybatis/3.3.0/org/apache/ibatis/jdbc/AbstractSQL.java
 */
public abstract class AbstractSQL<T> {
    private static final String AND = ") \nAND (";
    private static final String OR = ") \nOR (";

    private SQLStatement sql = new SQLStatement();

    public abstract T getSelf();

    /**
     * Starts an update statement and specifies the table to update. 
     * This should be followed by one or more SET() calls, and usually a WHERE() call. 
     * 
     * @param table table name and an alias
     * @return T
     */
    public T UPDATE(String table) {
        sql().statementType = SQLStatement.StatementType.UPDATE;
        sql().tables.add(table);
        
        return getSelf();
    }

    /**
     * Appends to the "set" list for an update statement.
     * 
     * @param sets
     * @return T
     */
    public T SET(String sets) {
        sql().sets.add(sets);
        
        return getSelf();
    }

    /**
     * Starts an insert statement and specifies the table to insert into. 
     * This should be followed by one or more VALUES() calls. 
     * 
     * @param tableName table name and an alias
     * @return T
     */
    public T INSERT_INTO(String tableName) {
        sql().statementType = SQLStatement.StatementType.INSERT;
        sql().tables.add(tableName);
        
        return getSelf();
    }

    /**
     * Appends to an insert statement.
     * 
     * @param columns the column(s) to insert
     * @param values the value(s)
     * @return T
     */
    public T VALUES(String columns, String values) {
        sql().columns.add(columns);
        sql().values.add(values);
        
        return getSelf();
    }

    /**
     * Starts or appends to a SELECT clause. Can be called more than once, 
     * and parameters will be appended to the SELECT clause.
     * 
     * @param columns comma separated list of columns and aliases
     * @return T
     */
    public T SELECT(String columns) {
        sql().statementType = SQLStatement.StatementType.SELECT;
        sql().select.add(columns);
        
        return getSelf();
    }

    /**
     * Starts or appends to a SELECT clause, also adds the DISTINCT keyword to the generated query
     * 
     * @param columns comma separated list of columns and aliases
     * @return T
     */
    public T SELECT_DISTINCT(String columns) {
        sql().distinct = true;
        SELECT(columns);
        
        return getSelf();
    }

    /**
     * Starts a delete statement and specifies the table to delete from. 
     * Generally this should be followed by a WHERE statement! 
     * 
     * @param table table name and an alias
     * @return T
     */
    public T DELETE_FROM(String table) {
        sql().statementType = SQLStatement.StatementType.DELETE;
        sql().tables.add(table);
        
        return getSelf();
    }

    /**
     * Starts or appends to a FROM clause. Can be called more than once, 
     * and parameters will be appended to the FROM clause.
     * 
     * @param table table name and an alias
     * @return T
     */
    public T FROM(String table) {
        sql().tables.add(table);
        
        return getSelf();
    }

    /**
     * Adds a new JOIN clause of the appropriate type, depending on the method called.
     * 
     * @param join can include a standard join consisting of the columns and the conditions to join on.
     * @return T
     */
    public T JOIN(String join) {
        sql().join.add(join);
        
        return getSelf();
    }

    /**
     * Adds a new INNER JOIN clause
     * 
     * @param join can include a standard join consisting of the columns and the conditions to join on.
     * @return T
     */
    public T INNER_JOIN(String join) {
        sql().innerJoin.add(join);
        
        return getSelf();
    }

    /**
     * Adds a new LEFT OUTER JOIN clause
     * 
     * @param join can include a standard join consisting of the columns and the conditions to join on.
     * @return T
     */
    public T LEFT_OUTER_JOIN(String join) {
        sql().leftOuterJoin.add(join);
        
        return getSelf();
    }

    /**
     * Adds a new RIGHT OUTER JOIN clause
     * 
     * @param join can include a standard join consisting of the columns and the conditions to join on.
     * @return T
     */
    public T RIGHT_OUTER_JOIN(String join) {
        sql().rightOuterJoin.add(join);
        
        return getSelf();
    }

    /**
     * Adds a new OUTER JOIN clause
     * 
     * @param join can include a standard join consisting of the columns and the conditions to join on.
     * @return T
     */
    public T OUTER_JOIN(String join) {
        sql().outerJoin.add(join);
        
        return getSelf();
    }

    /**
     * Appends a new WHERE clause condition, concatenated by AND. Can be called multiple times, 
     * which causes it to concatenate the new conditions each time with AND. Use OR() to split with an OR. 
     * 
     * @param conditions
     * @return T
     */
    public T WHERE(String conditions) {
        sql().where.add(conditions);
        sql().lastList = sql().where;
        
        return getSelf();
    }

    /**
     * Splits the current WHERE clause conditions with anOR. Can be called more than once, 
     * but calling more than once in a row will generate erraticSQL. 
     * 
     * @return T
     */
    public T OR() {
        sql().lastList.add(OR);
        
        return getSelf();
    }

    /**
     * Splits the current WHERE clause conditions with anAND. Can be called more than once, 
     * but calling more than once in a row will generate erraticSQL. Because WHERE and HAVING both 
     * automatically concatenate with AND, this is a very uncommon method to use and is only really 
     * included for completeness. 
     * 
     * @return T
     */
    public T AND() {
        sql().lastList.add(AND);
        
        return getSelf();
    }

    /**
     * Appends a new GROUP BY clause elements, concatenated by a comma. Can be called multiple times, 
     * which causes it to concatenate the new conditions each time with a comma. 
     * 
     * @param columns comma separated list of columns and aliases
     * @return T
     */
    public T GROUP_BY(String columns) {
        sql().groupBy.add(columns);
        
        return getSelf();
    }

    /**
     * Appends a new HAVING clause condition, concatenated by AND. Can be called multiple times, 
     * which causes it to concatenate the new conditions each time with AND. Use OR() to split with anOR. 
     * 
     * @param conditions
     * @return T
     */
    public T HAVING(String conditions) {
        sql().having.add(conditions);
        sql().lastList = sql().having;
        
        return getSelf();
    }

    /**
     * Appends a new ORDER BY clause elements, concatenated by a comma. Can be called multiple times, 
     * which causes it to concatenate the new conditions each time with a comma. 
     * 
     * @param columns comma separated list of columns and aliases
     * @return
     */
    public T ORDER_BY(String columns) {
        sql().orderBy.add(columns);
        
        return getSelf();
    }

    private SQLStatement sql() {
        return sql;
    }

    public <A extends Appendable> A usingAppender(A a) {
        sql().sql(a);
        
        return a;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sql().sql(sb);
        
        return sb.toString();
    }

    private static class SafeAppendable {
        private final Appendable a;
        private boolean empty = true;

        public SafeAppendable(Appendable a) {
            super();
            this.a = a;
        }

        public SafeAppendable append(CharSequence s) {
            try {
                if (empty && s.length() > 0) {
                    empty = false;
                }
                
                a.append(s);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            
            return this;
        }

        public boolean isEmpty() {
            return empty;
        }

    }

    private static class SQLStatement {

        public enum StatementType {
            DELETE, INSERT, SELECT, UPDATE
        }

        StatementType statementType;
        List<String> sets = new ArrayList<String>();
        List<String> select = new ArrayList<String>();
        List<String> tables = new ArrayList<String>();
        List<String> join = new ArrayList<String>();
        List<String> innerJoin = new ArrayList<String>();
        List<String> outerJoin = new ArrayList<String>();
        List<String> leftOuterJoin = new ArrayList<String>();
        List<String> rightOuterJoin = new ArrayList<String>();
        List<String> where = new ArrayList<String>();
        List<String> having = new ArrayList<String>();
        List<String> groupBy = new ArrayList<String>();
        List<String> orderBy = new ArrayList<String>();
        List<String> lastList = new ArrayList<String>();
        List<String> columns = new ArrayList<String>();
        List<String> values = new ArrayList<String>();
        boolean distinct;

        public SQLStatement() {
            // Prevent Synthetic Access
        }

        private void sqlClause(SafeAppendable builder, String keyword, List<String> parts, String open, String close,
                           String conjunction) {
            if (!parts.isEmpty()) {
                if (!builder.isEmpty()) {
                    builder.append("\n");
                }
                
                builder.append(keyword);
                builder.append(" ");
                builder.append(open);
                
                String last = "________";
                
                for (int i = 0, n = parts.size(); i < n; i++) {
                    String part = parts.get(i);
                    
                    if (i > 0 && !part.equals(AND) && !part.equals(OR) && !last.equals(AND) && !last.equals(OR)) {
                        builder.append(conjunction);
                    }
                    
                    builder.append(part);
                    last = part;
                }
                
                builder.append(close);
            }
        }

        private String selectSQL(SafeAppendable builder) {
            if (distinct) {
                sqlClause(builder, "SELECT DISTINCT", select, "", "", ", ");
            } else {
                sqlClause(builder, "SELECT", select, "", "", ", ");
            }

            sqlClause(builder, "FROM", tables, "", "", ", ");
            sqlClause(builder, "JOIN", join, "", "", "\nJOIN ");
            sqlClause(builder, "INNER JOIN", innerJoin, "", "", "\nINNER JOIN ");
            sqlClause(builder, "OUTER JOIN", outerJoin, "", "", "\nOUTER JOIN ");
            sqlClause(builder, "LEFT OUTER JOIN", leftOuterJoin, "", "", "\nLEFT OUTER JOIN ");
            sqlClause(builder, "RIGHT OUTER JOIN", rightOuterJoin, "", "", "\nRIGHT OUTER JOIN ");
            sqlClause(builder, "WHERE", where, "(", ")", " AND ");
            sqlClause(builder, "GROUP BY", groupBy, "", "", ", ");
            sqlClause(builder, "HAVING", having, "(", ")", " AND ");
            sqlClause(builder, "ORDER BY", orderBy, "", "", ", ");
            
            return builder.toString();
        }

        private String insertSQL(SafeAppendable builder) {
            sqlClause(builder, "INSERT INTO", tables, "", "", "");
            sqlClause(builder, "", columns, "(", ")", ", ");
            sqlClause(builder, "VALUES", values, "(", ")", ", ");
            
            return builder.toString();
        }

        private String deleteSQL(SafeAppendable builder) {
            sqlClause(builder, "DELETE FROM", tables, "", "", "");
            sqlClause(builder, "WHERE", where, "(", ")", " AND ");
            
            return builder.toString();
        }

        private String updateSQL(SafeAppendable builder) {
            sqlClause(builder, "UPDATE", tables, "", "", "");
            sqlClause(builder, "SET", sets, "", "", ", ");
            sqlClause(builder, "WHERE", where, "(", ")", " AND ");
            
            return builder.toString();
        }

        public String sql(Appendable a) {
            SafeAppendable builder = new SafeAppendable(a);
            
            if (statementType == null) {
                return null;
            }

            String answer;

            switch (statementType) {
                case DELETE:
                    answer = deleteSQL(builder);
                    break;

                case INSERT:
                    answer = insertSQL(builder);
                    break;

                case SELECT:
                    answer = selectSQL(builder);
                    break;

                case UPDATE:
                    answer = updateSQL(builder);
                    break;

                default:
                    answer = null;
            }

            return answer;
        }
    }
}
