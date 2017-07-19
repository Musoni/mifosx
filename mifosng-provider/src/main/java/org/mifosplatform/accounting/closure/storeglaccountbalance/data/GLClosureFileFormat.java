/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.accounting.closure.storeglaccountbalance.data;

public enum GLClosureFileFormat {
    CSV(0, "csv"),
    GP(1, "gp"),
    T24(2, "t24"),
    XLS(3, "xls"),
    XML(4, "xml");

    private final Integer value;
    private final String format;

    private GLClosureFileFormat(final Integer value, final String format) {
        this.value = value;
        this.format = format;
    }

    public static GLClosureFileFormat fromInteger(final Integer value){
        GLClosureFileFormat dataExportFileFormat = CSV;
        for (GLClosureFileFormat fileFormat : GLClosureFileFormat.values()) {
            if (fileFormat.value == value) {
                dataExportFileFormat = fileFormat;
                break;
            }
        }
        return dataExportFileFormat;
    }
    
    public static GLClosureFileFormat fromString(final String format) {
        GLClosureFileFormat dataExportFileFormat = CSV;
        
        for (GLClosureFileFormat fileFormat : GLClosureFileFormat.values()) {
        	if (fileFormat.format.equalsIgnoreCase(format)) {
        		dataExportFileFormat = fileFormat;
        		break;
        	}
        }
        
        return dataExportFileFormat;
    }

    public Integer getValue() {
        return value;
    }

    public String getFormat() { return format; }
    
    /** 
     * @return true if enum is equal to DataExportFileFormat.CSV, else false
     **/
    public boolean isCsv() {
        return this.equals(CSV);
    }

    /**
     * @return true if enum is equal to DataExportFileFormat.CSV, else false
     **/
    public boolean isGP() {
        return this.equals(GP);
    }

    /**
     * @return true if enum is equal to DataExportFileFormat.CSV, else false
     **/
    public boolean isT24() {
        return this.equals(T24);
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
