/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.helper;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.mifosplatform.infrastructure.codes.data.CodeValueData;
import org.mifosplatform.infrastructure.dataexport.data.DataExportCoreTable;
import org.mifosplatform.infrastructure.dataexport.data.DataExportEntityColumnName;
import org.mifosplatform.infrastructure.dataexport.data.EntityColumnMetaData;
import org.mifosplatform.infrastructure.dataexport.data.MysqlDataType;
import org.mifosplatform.useradministration.data.AppUserData;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

public class DataExportUtils {
	/**
	 * Search and replace string using the searchList string array and replacementList string array
	 * 
	 * @param string
	 * @param searchList
	 * @param replacementList
	 * @return replacement string for the specified string
	 */
	public static String searchAndReplaceString(final String string, 
			final String[] searchList, final String[] replacementList) {
		// replace all occurrences of the strings the "searchList" array with 
        // their corresponding string in the "replacementList" array
        final String replacementString = StringUtils.replaceEach(string, searchList, replacementList);
        
        // finally, trim the string
        return StringUtils.trim(replacementString);
	}
	
	/**
	 * Creates a human readable label for the specified column name
	 * 
	 * @param columnName
	 * @param coreTable
	 * @return string
	 */
	public static String createHumanReadableTableColumnLabel(final String columnName, 
			final DataExportCoreTable coreTable) {
		List<String> searchArrayList = new ArrayList<>();
		List<String> replacementArrayList = new ArrayList<>();
		
		switch (coreTable) {
	    	case M_LOAN_TRANSACTION:
	    		searchArrayList.add(DataExportEntityColumnName.PRINCIPAL_PORTION_DERIVED);
	    		searchArrayList.add(DataExportEntityColumnName.INTEREST_PORTION_DERIVED);
	    		searchArrayList.add(DataExportEntityColumnName.FEE_CHARGES_PORTION_DERIVED);
	    		searchArrayList.add(DataExportEntityColumnName.PENALTY_CHARGES_PORTION_DERIVED);
	    		searchArrayList.add(DataExportEntityColumnName.OVERPAYMENT_PORTION_DERIVED);
	    		searchArrayList.add(DataExportEntityColumnName.AMOUNT);
	    		searchArrayList.add(DataExportEntityColumnName.TRANSACTION_DATE);
	    		
	    		replacementArrayList.add("Principal Repaid");
	    		replacementArrayList.add("Interest Repaid");
	    		replacementArrayList.add("Fees Repaid");
	    		replacementArrayList.add("Penalties Repaid");
	    		replacementArrayList.add("Overpayments Repaid");
	    		replacementArrayList.add("Total Repaid");
	    		replacementArrayList.add("effective date");
	    		break;
	    		
	    	case M_SAVINGS_ACCOUNT_TRANSACTION:
	    		searchArrayList.add(DataExportEntityColumnName.TRANSACTION_DATE);
	    		
	    		replacementArrayList.add("effective date");
	    		break;
	    		
	    	case M_LOAN_REPAYMENT_SCHEDULE:
	    		searchArrayList.add(DataExportEntityColumnName.DUEDATE);
	    		searchArrayList.add(DataExportEntityColumnName.PRINCIPAL_AMOUNT);
	    		searchArrayList.add(DataExportEntityColumnName.INTEREST_AMOUNT);
	    		searchArrayList.add(DataExportEntityColumnName.FEE_CHARGES_AMOUNT);
	    		searchArrayList.add(DataExportEntityColumnName.PENALTY_CHARGES_AMOUNT);
	    		searchArrayList.add(DataExportEntityColumnName.OBLIGATION_MET_ON_DATE);
	    		
	    		replacementArrayList.add("due date");
	    		replacementArrayList.add("principal expected");
	    		replacementArrayList.add("interest expected");
	    		replacementArrayList.add("fees expected");
	    		replacementArrayList.add("penalties expected");
	    		replacementArrayList.add("repayment date");
	    		break;
	    		
	    	default:
	    		break;
	    }
		
		// ==============================================================================
		// List of partial or full field names that needs to be replace by another string
		searchArrayList.add(DataExportEntityColumnName.TRANSACTION_DATE);
		searchArrayList.add(DataExportEntityColumnName.ACTIVATION_DATE);
		searchArrayList.add("_on_userid");
		searchArrayList.add("on_userid");
		searchArrayList.add("_on_date");
		searchArrayList.add("on_date");
		searchArrayList.add("_cv_id");
		searchArrayList.add("_enum");
		searchArrayList.add("_");
		searchArrayList.add("is_reversed");
		// ===============================================================================
		
		// ===============================================================================
		// List of replacement strings for the strings in the "searchList" array list
		replacementArrayList.add("transaction date");
		replacementArrayList.add("activation date");
		replacementArrayList.add(" by user");
		replacementArrayList.add(" by user");
		replacementArrayList.add(" on date");
		replacementArrayList.add(" on date");
		replacementArrayList.add("");
		replacementArrayList.add("");
		replacementArrayList.add(" ");
		replacementArrayList.add("reversed");
		// ===============================================================================
		
		String[] searchList = searchArrayList.toArray(new String[searchArrayList.size()]);
		String[] replacementList = replacementArrayList.toArray(new String[replacementArrayList.size()]);
        
        return searchAndReplaceString(columnName, searchList, replacementList);
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
        final DataExportCoreTable coreTable = DataExportCoreTable.newInstance(tableName);
        Set<String> columnsToBeRemovedFromListsOfEntityColumns = new HashSet<>(Arrays.asList(
        		DataExportEntityColumnName.TRANSFER_TO_OFFICE_ID, DataExportEntityColumnName.VERSION, 
        		DataExportEntityColumnName.IMAGE_ID, DataExportEntityColumnName.ACCOUNT_TYPE_ENUM, DataExportEntityColumnName.DEPOSIT_TYPE_ENUM, 
        		DataExportEntityColumnName.SUB_STATUS, DataExportEntityColumnName.FULL_NAME));
        
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
					String columnLabel = resultSetMetaData.getColumnName(i);
                    String columnType = resultSetMetaData.getColumnTypeName(i);
                    Integer columnIsNullable = resultSetMetaData.isNullable(i);
                    boolean isNullable = (columnIsNullable != 0);
                    
                    if (coreTable != null) {
                    	switch (coreTable) {
	                    	case M_LOAN_TRANSACTION:
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.UNRECOGNIZED_INCOME_PORTION);
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.SUSPENDED_INTEREST_PORTION_DERIVED);
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.SUSPENDED_FEE_CHARGES_PORTION_DERIVED);
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.SUSPENDED_PENALTY_CHARGES_PORTION_DERIVED);
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.OUTSTANDING_LOAN_BALANCE_DERIVED);
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.RECOVERED_PORTION_DERIVED);
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.PAYMENT_DETAIL_ID);
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.OFFICE_ID);
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.IS_ACCCOUNT_TRANSFER);
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.APPUSER_ID);
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.EXTERNAL_ID);
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.CREATED_DATE);
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.TRANSACTION_TYPE_ENUM);
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.LOAN_ID);
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.AMOUNT);
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.PRINCIPAL_PORTION_DERIVED);
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.INTEREST_PORTION_DERIVED);
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.FEE_CHARGES_PORTION_DERIVED);
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.PENALTY_CHARGES_PORTION_DERIVED);
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.OVERPAYMENT_PORTION_DERIVED);
	                    		break;
	                    		
	                    	case M_SAVINGS_ACCOUNT_TRANSACTION:
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.OVERDRAFT_AMOUNT_DERIVED);
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.RUNNING_BALANCE_DERIVED);
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.CUMULATIVE_BALANCE_DERIVED);
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.BALANCE_NUMBER_OF_DAYS_DERIVED);
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.BALANCE_END_DATE_DERIVED);
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.CREATED_DATE);
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.TRANSACTION_TYPE_ENUM);
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.APPUSER_ID);
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.SAVINGS_ACCOUNT_ID);
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.AMOUNT);
	            	    		break;
	            	    		
	                    	case M_LOAN_REPAYMENT_SCHEDULE:
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.ID);
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.LOAN_ID);
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.FROMDATE);
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.INSTALLMENT);
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.PRINCIPAL_COMPLETED_DERIVED);
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.PRINCIPAL_WRITTENOFF_DERIVED);
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.INTEREST_COMPLETED_DERIVED);
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.INTEREST_WAIVED_DERIVED);
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.INTEREST_WRITTENOFF_DERIVED);
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.ACCRUAL_INTEREST_DERIVED);
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.SUSPENDED_INTEREST_DERIVED);
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.FEE_CHARGES_WRITTENOFF_DERIVED);
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.FEE_CHARGES_COMPLETED_DERIVED);
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.FEE_CHARGES_WAIVED_DERIVED);
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.ACCRUAL_FEE_CHARGES_DERIVED);
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.SUSPENDED_FEE_CHARGES_DERIVED);
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.PENALTY_CHARGES_COMPLETED_DERIVED);
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.PENALTY_CHARGES_WAIVED_DERIVED);
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.PENALTY_CHARGES_WRITTEN_DERIVED);
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.ACCRUAL_PENALTY_CHARGES_DERIVED);
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.SUSPENDED_PENALTY_CHARGES_DERIVED);
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.TOTAL_PAID_IN_ADVANCE_DERIVED);
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.TOTAL_PAID_LATE_DERIVED);
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.COMPLETED_DERIVED);
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.CREATED_BY_ID);
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.CREATED_DATE);
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.LAST_MODIFIED_BY_ID);
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.LAST_MODIFIED_DATE);
	                    		columnsToBeRemovedFromListsOfEntityColumns.add(
	                    				DataExportEntityColumnName.RECALCULATED_INTEREST_COMPONENT);
	                    		break;
	                    		
	                    	default:
	                    		break;
	                    }
                    }
                    
                    if (!columnsToBeRemovedFromListsOfEntityColumns.
                    		contains(columnName)) {
						if(columnName.equals(DataExportEntityColumnName.ID)){
							columnLabel = DataExportEntityColumnName.TRANSACTION_ID;
						}
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
	
	/**
	 * Searches for the CodeValueData object mapped to key similar to specified column value
	 * 
	 * @param codeValueMap
	 * @param columnName
	 * @param columnValue
	 * @param mysqlDataType
	 * @return AbstractMap.SimpleEntry object
	 */
	public static AbstractMap.SimpleEntry<String, MysqlDataType> replaceCodeValueIdWithValue(final HashMap<Long, CodeValueData> codeValueMap, 
			final String columnName, String columnValue, MysqlDataType mysqlDataType) {
		if (columnName != null && StringUtils.isNotBlank(columnValue)) {
			if (StringUtils.endsWith(columnName, "_cv_id") || StringUtils.contains(columnName, "_cd_")) {
				Long codeValueId = null;
				
				try {
					codeValueId = Long.valueOf(columnValue);
					
				} catch (Exception e) { }
				
				CodeValueData codeValueData = codeValueMap.get(codeValueId);
				
				if (codeValueData != null) {
					columnValue = codeValueData.getName();
					
					// change the data type from integer to string
					mysqlDataType = MysqlDataType.VARCHAR;
				}
			} else if (StringUtils.contains(columnName, "_cb_")) {
				final String[] csvToArray = StringUtils.split(columnValue, ",");
				
				if (csvToArray != null) {
					final String[] cleanCsvToArray = new String[csvToArray.length];
							
					for (int i=0; i<csvToArray.length; i++) {
						Long codeValueId = null;
						
						try {
							codeValueId = Long.valueOf(csvToArray[i]);
							
						} catch (Exception e) { }
						
						CodeValueData codeValueData = codeValueMap.get(codeValueId);
						
						cleanCsvToArray[i] = null;
						
						if (codeValueData != null) {
							cleanCsvToArray[i] = codeValueData.getName();
						}
					}
					
					columnValue = StringUtils.join(cleanCsvToArray, ", ");
				}
			}
		}
		
		return new AbstractMap.SimpleEntry<String, MysqlDataType>(columnValue, mysqlDataType);
	}
	
	/**
	 * Searches for the AppUserData object mapped to key similar to the specified column value
	 * 
	 * @param appUserMap
	 * @param columnName
	 * @param columnValue
	 * @param mysqlDataType
	 * @return AbstractMap.SimpleEntry object
	 */
	public static AbstractMap.SimpleEntry<String, MysqlDataType> replaceAppUserIdWithUserName(
			final HashMap<Long, AppUserData> appUserMap, 
			final String columnName, String columnValue, MysqlDataType mysqlDataType) {
		if (columnName != null) {
			if (columnName.contains("userid") || columnName.contains("_by")) {
				Long userId = null;
				
				try {
					userId = Long.valueOf(columnValue);
					
				} catch (Exception e) { }
				
				AppUserData appUserData = appUserMap.get(userId);
				
				if (appUserData != null) {
					columnValue = appUserData.getUsername();
					
					// change the data type from integer to string
					mysqlDataType = MysqlDataType.VARCHAR;
				}
			}
		}
		
		return new AbstractMap.SimpleEntry<String, MysqlDataType>(columnValue, mysqlDataType);
	}
}
