/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.helper;


import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class XlsFileHelper {
    public static void createFile(final File file, final List<Map<String, Object>> data){
        //Blank workbook
        XSSFWorkbook workbook = new XSSFWorkbook();

        //Create a blank sheet
        XSSFSheet sheet = workbook.createSheet();

        Set<String> keyset = data.get(0).keySet();
        int rownum = 0;
        int cellnum = 0;
        Row row = sheet.createRow(rownum++);

        try
        {
        for (String paramkey : keyset) {
            String key = paramkey;
            if(key.startsWith("'")){
                key = key.substring(1);
            }
            Cell cell = row.createCell(cellnum++);
            cell.setCellValue(key);
        }

        for (Map<String,Object> entry : data) {
            row = sheet.createRow(rownum++);
            cellnum = 0;
            for (String key : keyset) {
                String value = entry.get(key)!=null?entry.get(key).toString():null;
                if(value != null && value.startsWith("'")){
                    value = value.substring(1);
                }
                Cell cell = row.createCell(cellnum++);
                cell.setCellValue(value);
            }
        }

            //Write the workbook in file system
            FileOutputStream out = new FileOutputStream(file);
            workbook.write(out);
            out.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
