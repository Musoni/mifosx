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
import org.mifosplatform.infrastructure.dataexport.data.DataExportCoreTable;
import org.mifosplatform.infrastructure.dataexport.data.DataExportEntityColumnName;
import org.mifosplatform.infrastructure.dataexport.data.EntityColumnMetaData;
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
	    		searchArrayList.add("principal_portion_derived");
	    		searchArrayList.add("interest_portion_derived");
	    		searchArrayList.add("fee_charges_portion_derived");
	    		searchArrayList.add("penalty_charges_portion_derived");
	    		searchArrayList.add("overpayment_portion_derived");
	    		searchArrayList.add("amount");
	    		searchArrayList.add("transaction_date");
	    		
	    		replacementArrayList.add("Principal Repaid");
	    		replacementArrayList.add("Interest Repaid");
	    		replacementArrayList.add("Fees Repaid");
	    		replacementArrayList.add("Penalties Repaid");
	    		replacementArrayList.add("Overpayments Repaid");
	    		replacementArrayList.add("Total Repaid");
	    		replacementArrayList.add("effective date");
	    		break;
	    		
	    	case M_SAVINGS_ACCOUNT_TRANSACTION:
	    		searchArrayList.add("transaction_date");
	    		
	    		replacementArrayList.add("effective date");
	    		break;
	    		
	    	case M_LOAN_REPAYMENT_SCHEDULE:
	    		searchArrayList.add("duedate");
	    		searchArrayList.add("principal_amount");
	    		searchArrayList.add("interest_amount");
	    		searchArrayList.add("fee_charges_amount");
	    		searchArrayList.add("penalty_charges_amount");
	    		
	    		replacementArrayList.add("due date");
	    		replacementArrayList.add("principal expected");
	    		replacementArrayList.add("interest expected");
	    		replacementArrayList.add("fees expected");
	    		replacementArrayList.add("penalties expected");
	    		break;
	    		
	    	default:
	    		break;
	    }
		
		// ==============================================================================
		// List of partial or full field names that needs to be replace by another string
		searchArrayList.add("transaction_date");
		searchArrayList.add("activation_date");
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
		replacementArrayList.add(" by user id");
		replacementArrayList.add(" by user id");
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
                    String columnLabel = createHumanReadableTableColumnLabel(columnName, coreTable);
                    
                    switch (coreTable) {
                    	case M_LOAN_TRANSACTION:
                    		DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.add(
                    				DataExportEntityColumnName.UNRECOGNIZED_INCOME_PORTION);
                    		DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.add(
                    				DataExportEntityColumnName.SUSPENDED_INTEREST_PORTION_DERIVED);
                    		DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.add(
                    				DataExportEntityColumnName.SUSPENDED_FEE_CHARGES_PORTION_DERIVED);
                    		DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.add(
                    				DataExportEntityColumnName.SUSPENDED_PENALTY_CHARGES_PORTION_DERIVED);
                    		DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.add(
                    				DataExportEntityColumnName.OUTSTANDING_LOAN_BALANCE_DERIVED);
                    		DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.add(
                    				DataExportEntityColumnName.RECOVERED_PORTION_DERIVED);
                    		DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.add(
                    				DataExportEntityColumnName.PAYMENT_DETAIL_ID);
                    		DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.add(
                    				DataExportEntityColumnName.OFFICE_ID);
                    		DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.add(
                    				DataExportEntityColumnName.IS_ACCCOUNT_TRANSFER);
                    		DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.add(
                    				DataExportEntityColumnName.APPUSER_ID);
                    		DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.add(
                    				DataExportEntityColumnName.EXTERNAL_ID);
                    		DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.add(
                    				DataExportEntityColumnName.CREATED_DATE);
                    		DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.add(
                    				DataExportEntityColumnName.TRANSACTION_TYPE_ENUM);
                    		DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.add(
                    				DataExportEntityColumnName.LOAN_ID);
                    		DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.add(
                    				DataExportEntityColumnName.AMOUNT);
                    		DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.add(
                    				DataExportEntityColumnName.ID);
                    		break;
                    		
                    	case M_SAVINGS_ACCOUNT_TRANSACTION:
                    		DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.add(
                    				DataExportEntityColumnName.OVERDRAFT_AMOUNT_DERIVED);
                    		DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.add(
                    				DataExportEntityColumnName.RUNNING_BALANCE_DERIVED);
                    		DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.add(
                    				DataExportEntityColumnName.CUMULATIVE_BALANCE_DERIVED);
                    		DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.add(
                    				DataExportEntityColumnName.BALANCE_NUMBER_OF_DAYS_DERIVED);
                    		DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.add(
                    				DataExportEntityColumnName.BALANCE_END_DATE_DERIVED);
                    		DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.add(
                    				DataExportEntityColumnName.CREATED_DATE);
                    		DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.add(
                    				DataExportEntityColumnName.TRANSACTION_TYPE_ENUM);
                    		DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.add(
                    				DataExportEntityColumnName.APPUSER_ID);
                    		DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.add(
                    				DataExportEntityColumnName.SAVINGS_ACCOUNT_ID);
                    		DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.add(
                    				DataExportEntityColumnName.AMOUNT);
                    		DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.add(
                    				DataExportEntityColumnName.ID);
            	    		break;
            	    		
                    	case M_LOAN_REPAYMENT_SCHEDULE:
                    		DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.add(
                    				DataExportEntityColumnName.ID);
                    		DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.add(
                    				DataExportEntityColumnName.LOAN_ID);
                    		DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.add(
                    				DataExportEntityColumnName.FROM_DATE);
                    		DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.add(
                    				DataExportEntityColumnName.INSTALLMENT);
                    		DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.add(
                    				DataExportEntityColumnName.PRINCIPAL_COMPLETED_DERIVED);
                    		DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.add(
                    				DataExportEntityColumnName.PRINCIPAL_WRITTENOFF_DERIVED);
                    		DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.add(
                    				DataExportEntityColumnName.INTEREST_COMPLETED_DERIVED);
                    		DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.add(
                    				DataExportEntityColumnName.INTEREST_WAIVED_DERIVED);
                    		DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.add(
                    				DataExportEntityColumnName.INTEREST_WRITTENOFF_DERIVED);
                    		DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.add(
                    				DataExportEntityColumnName.ACCRUAL_INTEREST_DERIVED);
                    		DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.add(
                    				DataExportEntityColumnName.SUSPENDED_INTEREST_DERIVED);
                    		DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.add(
                    				DataExportEntityColumnName.FEE_CHARGES_WRITTENOFF_DERIVED);
                    		DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.add(
                    				DataExportEntityColumnName.FEE_CHARGES_COMPLETED_DERIVED);
                    		DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.add(
                    				DataExportEntityColumnName.FEE_CHARGES_WAIVED_DERIVED);
                    		DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.add(
                    				DataExportEntityColumnName.ACCRUAL_FEE_CHARGES_DERIVED);
                    		DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.add(
                    				DataExportEntityColumnName.SUSPENDED_FEE_CHARGES_DERIVED);
                    		DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.add(
                    				DataExportEntityColumnName.PENALTY_CHARGES_COMPLETED_DERIVED);
                    		DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.add(
                    				DataExportEntityColumnName.PENALTY_CHARGES_WAIVED_DERIVED);
                    		DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.add(
                    				DataExportEntityColumnName.PENALTY_CHARGES_WRITTEN_DERIVED);
                    		DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.add(
                    				DataExportEntityColumnName.ACCRUAL_PENALTY_CHARGES_DERIVED);
                    		DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.add(
                    				DataExportEntityColumnName.SUSPENDED_PENALTY_CHARGES_DERIVED);
                    		DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.add(
                    				DataExportEntityColumnName.TOTAL_PAID_IN_ADVANCE_DERIVED);
                    		DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.add(
                    				DataExportEntityColumnName.TOTAL_PAID_LATE_DERIVED);
                    		DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.add(
                    				DataExportEntityColumnName.COMPLETED_DERIVED);
                    		DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.add(
                    				DataExportEntityColumnName.OBLIGATIONS_MET_ON_DATE);
                    		DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.add(
                    				DataExportEntityColumnName.CREATED_BY_ID);
                    		DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.add(
                    				DataExportEntityColumnName.CREATED_DATE);
                    		DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.add(
                    				DataExportEntityColumnName.LAST_MODIFIED_BY_ID);
                    		DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.add(
                    				DataExportEntityColumnName.LAST_MODIFIED_DATE);
                    		DataExportEntityColumnName.COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS.add(
                    				DataExportEntityColumnName.RECALCULATED_INTEREST_COMPONENT);
                    		break;
                    		
                    	default:
                    		break;
                    }
                    
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
