/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.helper;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.mifosplatform.infrastructure.dataexport.data.DataExportEntityColumnName;
import org.mifosplatform.infrastructure.dataexport.data.EntityColumnMetaData;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

public class DataExportUtils {
	/**
	 * Creates a human readable label for the specified column name
	 * 
	 * @param columnName
	 * @return string
	 */
	public static String createHumanReadableTableColumnLabel(final String columnName) {
        final String[] searchList = {
        	"activation_date", "_on_userid", "on_userid", "_on_date", "on_date", "_cv_id", "_enum", "_"
        };
        
        final String[] replacementList = {
        	"activation date", " by user", " by user", " on date", " on date", "", "", " "
        };
        
        // replace all occurrences of the strings the "searchList" array with 
        // their corresponding string in the "replacementList" array
        String columnLabel = StringUtils.replaceEach(columnName, searchList, replacementList);
        
        // remove the "id" string from the end of the label
        // columnLabel = StringUtils.removeEndIgnoreCase(columnLabel, " id");
        
        // finally, trim the string
        return StringUtils.trim(columnLabel);
	}
	
	/**
	 * Gets the meta data of the columns of the specified table
	 * 
	 * @param tableName
	 * @param jdbcTemplate
	 * @return List of {@link EntityColumnMetaData} objects
	 */
	public static List<EntityColumnMetaData> getTableColumnsMetaData(final String tableName, 
			final JdbcTemplate jdbcTemplate) {
		final List<EntityColumnMetaData> entityColumnsMetaData = new ArrayList<>();
        final List<String> columnNames = new ArrayList<>();
        
        try {
            // see - http://dev.mysql.com/doc/refman/5.7/en/limit-optimization.html
            // LIMIT 0 quickly returns an empty set. This can be useful for checking the validity of a query. 
            // It can also be employed to obtain the types of the result columns if you are using a MySQL API 
            // that makes result set metadata available.
            final ResultSetMetaData resultSetMetaData = jdbcTemplate.query("select * from " + tableName + " limit 0", 
                    new ResultSetExtractor<ResultSetMetaData>() {

                @Override
                public ResultSetMetaData extractData(ResultSet rs) throws SQLException, DataAccessException {
                    return rs.getMetaData();
                }
            });
            
            if (resultSetMetaData != null) {
                final int numberOfColumns = resultSetMetaData.getColumnCount();
                
                for (int i = 1; i <= numberOfColumns; i++) {
                    String columnName = resultSetMetaData.getColumnName(i);
                    String columnType = resultSetMetaData.getColumnTypeName(i);
                    Integer columnIsNullable = resultSetMetaData.isNullable(i);
                    boolean isNullable = (columnIsNullable != 0);
                    String columnLabel = createHumanReadableTableColumnLabel(columnName);
                    
                    if (!DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.
                    		contains(columnName)) {
                    	EntityColumnMetaData entityColumnMetaData = EntityColumnMetaData.newInstance(columnName, 
                                columnLabel, columnType, isNullable);
                        
                        entityColumnsMetaData.add(entityColumnMetaData);
                        columnNames.add(columnName);
                    }
                }
            }
        }
        
        catch (Exception exception) {
            exception.printStackTrace();
        }
        
        return entityColumnsMetaData;
	}
}
