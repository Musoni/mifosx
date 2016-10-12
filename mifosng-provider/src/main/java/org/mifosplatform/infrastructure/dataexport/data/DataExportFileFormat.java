/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.data;

public enum DataExportFileFormat {
    CSV("csv"),
    XLS("xls"),
    XML("xml");

    private final String value;

    private DataExportFileFormat(final String value) {
        this.value = value;
    }
    
    public static DataExportFileFormat fromString(final String format) {
        DataExportFileFormat dataExportFileFormat = CSV;
        
        for (DataExportFileFormat fileFormat : DataExportFileFormat.values()) {
        	if (fileFormat.value.equalsIgnoreCase(format)) {
        		dataExportFileFormat = fileFormat;
        		
        		break;
        	}
        }
        
        return dataExportFileFormat;
    }

    public String getValue() {
        return value;
    }
    
    /** 
     * @return true if enum is equal to DataExportFileFormat.CSV, else false
     **/
    public boolean isCsv() {
        return this.equals(CSV);
    }
    
    /** 
     * @return true if enum is equal to DataExportFileFormat.XLS, else false 
     **/
    public boolean isXls() {
        return this.equals(XLS);
    }
    
    /** 
     * @return true if enum is equal to DataExportFileFormat.XML, else false 
     **/
    public boolean isXml() {
        return this.equals(XML);
    }
}
