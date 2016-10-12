/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.helper;

import au.com.bytecode.opencsv.CSVWriter;
import org.mifosplatform.infrastructure.dataexport.api.DataExportApiConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
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
     * create a new CSV file  
     **/
    public static void createFile(final File file, final String[] fileHeaders, 
            final List<String[]> fileData) {
        try {
            // create a new CSVWriter object
            final CSVWriter csvWriter = new CSVWriter(new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(file), ENCODING)), SEPARATOR, QUOTE_CHARACTER,
                    ESCAPE_CHARACTER, DataExportApiConstants.WINDOWS_END_OF_LINE_CHARACTER);
            
            // write file headers to file
            csvWriter.writeNext(fileHeaders);
            
            // write file data to file
            csvWriter.writeAll(fileData);
            
            // close stream writer
            csvWriter.close();
        }
        
        catch (Exception exception) {
            logger.error(exception.getMessage(), exception);
        }
    }
}
