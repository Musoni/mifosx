/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.helper;

import au.com.bytecode.opencsv.CSVWriter;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.WordUtils;
import org.mifosplatform.infrastructure.codes.data.CodeValueData;
import org.mifosplatform.infrastructure.dataexport.api.DataExportApiConstants;
import org.mifosplatform.infrastructure.dataexport.data.DataExportCoreTable;
import org.mifosplatform.infrastructure.dataexport.data.MysqlDataType;
import org.mifosplatform.useradministration.data.AppUserData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSetMetaData;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/** 
 * Helper class that provides useful methods to manage CSV files 
 **/
public class CsvFileHelper {
    public static final char SEPARATOR = ';';
    public static final char QUOTE_CHARACTER = CSVWriter.NO_QUOTE_CHARACTER;
    public static final char ESCAPE_CHARACTER = CSVWriter.NO_ESCAPE_CHARACTER;
    public static final String ENCODING = "UTF-8";
    
    private final static Logger logger = LoggerFactory.getLogger(CsvFileHelper.class);
    
    /**
     * Creates a new CSV file
     * 
     * @param sqlRowSet
     * @param file
     */
    public static void createFile(final SqlRowSet sqlRowSet, final File file, 
    		final HashMap<Long, CodeValueData> codeValueMap, final HashMap<Long, AppUserData> appUserMap, 
    		final DataExportCoreTable coreTable) {
    	try {
            // create a new CSVWriter object
            final CSVWriter csvWriter = new CSVWriter(new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(file), ENCODING)), SEPARATOR, QUOTE_CHARACTER,
                    ESCAPE_CHARACTER, DataExportApiConstants.WINDOWS_END_OF_LINE_CHARACTER);
            final SqlRowSetMetaData sqlRowSetMetaData = sqlRowSet.getMetaData();
            final int columnCount = sqlRowSetMetaData.getColumnCount();
            final String[] headers = new String[columnCount];
            final List<String[]> data = new ArrayList<>();
            
            int columnIndex = 0;
            
            for (int i=1; i<=columnCount; i++) {
            	// get the column label of the dataset
            	String columnLabel = WordUtils.capitalize(sqlRowSetMetaData.getColumnLabel(i));
            	
            	// add column label to headers array
            	headers[columnIndex++] = columnLabel;
            }
            
            while (sqlRowSet.next()) {
            	// create a new empty string array of length "columnCount"
            	final String[] rowData = new String[columnCount];
            	
            	int rowIndex = 0;
            	
            	for (int i=1; i<=columnCount; i++) {
            		String columnTypeName = sqlRowSetMetaData.getColumnTypeName(i);
            		MysqlDataType mysqlDataType = MysqlDataType.newInstance(columnTypeName);
            		String columnValue = sqlRowSet.getString(i);
            		String columnName = sqlRowSetMetaData.getColumnName(i);
            		
            		// replace code value id with the code value name
            		AbstractMap.SimpleEntry<String, MysqlDataType> columnValueDataType = 
            				DataExportUtils.replaceCodeValueIdWithValue(codeValueMap, columnName, columnValue, mysqlDataType);
            		
            		// update the column value
            		columnValue = columnValueDataType.getKey();
            		
            		// replace app user id with respective username
            		columnValueDataType = 
            				DataExportUtils.replaceAppUserIdWithUserName(appUserMap, columnName, columnValue, mysqlDataType);
            		
            		// update the column value
            		columnValue = columnValueDataType.getKey();
            		
            		rowData[rowIndex++] = StringEscapeUtils.escapeCsv(columnValue);
            	}
            	
            	// add the row data to the array list of row data
            	data.add(rowData);
            }
            
            // write file headers to file
            csvWriter.writeNext(headers);
            
            // write file data to file
            csvWriter.writeAll(data);
            
            // close stream writer
            csvWriter.close();
        }
        
        catch (Exception exception) {
            logger.error(exception.getMessage(), exception);
        }
    }
}
