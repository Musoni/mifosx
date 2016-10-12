/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.helper;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;

public class XlsFileHelper {
	public static void createFile(final File file, final List<String> headers, 
    		final List<Map<String, Object>> data) {
        try {
        	//Blank workbook
            XSSFWorkbook workbook = new XSSFWorkbook();

            //Create a blank sheet
            XSSFSheet sheet = workbook.createSheet();

            int rownum = 0;
            int column = 0;
            Row row = sheet.createRow(rownum++);
        	
	        for (String header : headers) {
	        	Cell cell = row.createCell(column++);
	            
	            cell.setCellValue(header);
	        }
	
	        for (Map<String, Object> entry : data) {
	            row = sheet.createRow(rownum++);
	            column = 0;
	            
	            for (String header : headers) {
	                String value = (entry.get(header) != null) ? entry.get(header).toString() : null;
	                
	                // If the value is enclosed in double quotes, and contains a comma, newline or double quote, 
	                // then quotes are removed. 
	                value = StringEscapeUtils.unescapeCsv(value);
	                
	                Cell cell = row.createCell(column++);
	                cell.setCellValue(value);
	            }
	        }

            //Write the workbook in file system
            FileOutputStream out = new FileOutputStream(file);
            workbook.write(out);
            out.close();
            
        } catch (Exception exception) {
        	exception.printStackTrace();
        }
    }
}
