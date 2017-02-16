/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.helper;

import org.apache.commons.lang.StringUtils;
import org.mifosplatform.infrastructure.codes.data.CodeValueData;
import org.mifosplatform.infrastructure.dataexport.api.DataExportApiConstants;
import org.mifosplatform.infrastructure.dataexport.data.DataExportCoreTable;
import org.mifosplatform.infrastructure.dataexport.data.DataExportFileData;
import org.mifosplatform.infrastructure.documentmanagement.contentrepository.FileSystemContentRepository;
import org.mifosplatform.useradministration.data.AppUserData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

/** 
 * Helper class that provides useful methods to manage files 
 **/
public class FileHelper {
    private final static Logger logger = LoggerFactory.getLogger(FileHelper.class);
    public static final String ILLEGAL_FILE_NAME_CHAR_REGEX = "\\\\|/|\\||:|\\?|\\*|\"|<|>|\\p{Cntrl}";
    public static final String ILLEGAL_DIR_NAME_CHAR_REGEX = "\\.|\\\\|/|\\||:|\\?|\\*|\"|<|>|\\p{Cntrl}";
    
    /** 
     * Creates a directory by creating all nonexistent parent directories first
     * 
     * @param baseDirPathString -- the path string or initial part of the path string
     * @param morePathString -- additional strings to be joined to form the path string
     **/
    public static Path createDirectories(final String baseDirPathString, String ... morePathString) {
        Path directoryPath = null;
        
        try {
            // convert path string to Path object
            Path fullPath = FileSystems.getDefault().getPath(baseDirPathString, morePathString);
            
            // create the directory
            directoryPath = Files.createDirectories(fullPath);
        }
        
        catch (Exception exception) {
            logger.error(exception.getMessage(), exception);
        }
        
        return directoryPath;
    }
    
    /** 
     * gets the directory path where all data export files are stored
     *
     * @return {@link File} object representing this path
     **/
    public static File getDataExportDirectoryPath() {
        // create directory if non-existent
        final Path directoryPath = createDirectories(FileSystemContentRepository.MIFOSX_BASE_DIR,
                "dataexport");
        
        // return file object if "directoryPath" is not null
        return (directoryPath != null) ? directoryPath.toFile() : null;
    }
    
    /** 
     * write the file to the file system
     * 
     * @param inputStream
     * @param file
     * @throws IOException
     **/
    public static void createFile(final InputStream inputStream, 
            final File file) throws Exception {
        final OutputStream out = new FileOutputStream(file);
        final byte[] bytes = new byte[1024];
        
        int read = 0;

        while ((read = inputStream.read(bytes)) != -1) {
            out.write(bytes, 0, read);
        }
        
        out.flush();
        out.close();
    }
    
    /** 
     * create a data export XML file
     * 
     * @param fileName
     * @return {@link DataExportFileData} object
     **/
    public static DataExportFileData createDataExportXmlFile(final String fileName) {
        DataExportFileData dataExportFileData = null;
        
        try {
            final String fileNamePlusExtension = fileName + "." + StringUtils.lowerCase(DataExportApiConstants.XML_FILE_FORMAT);
            final File parentDirectoryPath = FileHelper.getDataExportDirectoryPath();
            final File file = new File(parentDirectoryPath, fileNamePlusExtension);
            
            dataExportFileData = new DataExportFileData(file, fileNamePlusExtension,
                    DataExportApiConstants.XML_FILE_CONTENT_TYPE);
        } 
        
        catch (Exception exception) {
            logger.error(exception.getMessage(), exception);
        }
        
        return dataExportFileData;
    }
    
    /**
     * Creates a data export CSV file
     * 
     * @param sqlRowSet
     * @param fileName
     * @return {@link DataExportFileData} object
     */
    public static DataExportFileData createDataExportCsvFile(final SqlRowSet sqlRowSet, final String fileName, 
    		final HashMap<Long, CodeValueData> codeValueMap, final HashMap<Long, AppUserData> appUserMap,
    		final DataExportCoreTable coreTable) {
    	DataExportFileData dataExportFileData = null;
        
        try {
            final String fileNamePlusExtension = fileName + "." + StringUtils.lowerCase(DataExportApiConstants.CSV_FILE_FORMAT);
            final File parentDirectoryPath = FileHelper.getDataExportDirectoryPath();
            final File file = new File(parentDirectoryPath, fileNamePlusExtension);
            
            // create a new csv file on the server
            CsvFileHelper.createFile(sqlRowSet, file, codeValueMap, appUserMap, coreTable);
            
            dataExportFileData = new DataExportFileData(file, fileNamePlusExtension,
                    DataExportApiConstants.CSV_FILE_CONTENT_TYPE);
        } 
        
        catch (Exception exception) {
            logger.error(exception.getMessage(), exception);
        }
        
        return dataExportFileData;
    }
    
    /**
     * Creates a data export XLS file
     * 
     * @param sqlRowSet
     * @param fileName
     * @return {@link DataExportFileData} object
     */
    public static DataExportFileData createDataExportXlsFile(final SqlRowSet sqlRowSet, final String fileName, 
    		final HashMap<Long, CodeValueData> codeValueMap, final HashMap<Long, AppUserData> appUserMap,
    		final DataExportCoreTable coreTable) {
    	DataExportFileData dataExportFileData = null;
    	
    	try {
    		final String fileNamePlusExtension = fileName + "." + StringUtils.lowerCase(DataExportApiConstants.XLS_FILE_FORMAT);
            final File parentDirectoryPath = FileHelper.getDataExportDirectoryPath();
            final File file = new File(parentDirectoryPath, fileNamePlusExtension);
            
            // create a new xls file on the server
            XlsFileHelper.createFile(sqlRowSet, file, codeValueMap, appUserMap, coreTable);
            
            dataExportFileData = new DataExportFileData(file, fileNamePlusExtension,
                    DataExportApiConstants.XLS_FILE_CONTENT_TYPE);
            
    	} catch (Exception exception) {
    		logger.error(exception.getMessage(), exception);
    	}
    	
    	return dataExportFileData;
    }
    
    /**
	 * Sanitizes a filename from certain chars.<br />
	 * 
	 * This method enforces the <code>forceSingleExtension</code> property and
	 * then replaces all occurrences of \, /, |, :, ?, *, &quot;, &lt;, &gt;,
	 * control chars by _ (underscore).
	 * 
	 * @param filename a potentially 'malicious' filename
	 * @return sanitized filename
	 */
    public static String sanitizeFilename(final String filename) {
    	String sanitizedFilename = filename;
    	
    	if (StringUtils.isNotBlank(filename)) {
    		sanitizedFilename = filename.replaceAll(ILLEGAL_FILE_NAME_CHAR_REGEX, "_");
    		sanitizedFilename = sanitizedFilename.replaceAll(" ", "_");
    	}
    	
    	return sanitizedFilename;
    }
}
