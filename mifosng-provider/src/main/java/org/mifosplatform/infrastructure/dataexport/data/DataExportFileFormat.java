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

    private String format;

    DataExportFileFormat(String format) {
        this.format = format;
    }

    public String getFormat(){
        return this.format;
    }
}
