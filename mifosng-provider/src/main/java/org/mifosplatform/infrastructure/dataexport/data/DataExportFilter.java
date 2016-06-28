/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.data;

public class DataExportFilter {
    public String tableName;
    public String fieldName;
    public String paramValue;
    public String searchSql;

    public DataExportFilter(String tableName, String paramValue, String fieldName) {
        this.tableName = tableName;
        this.paramValue = paramValue;
        this.fieldName = fieldName;
        setSearchSql(tableName,paramValue,fieldName);
    }

    public String getTableName() {
        return tableName;
    }

    public String getParamValue() {
        return paramValue;
    }

    public String getFieldName(){return fieldName;}

    public String getSearchQuery(){return searchSql;}

    private void setSearchSql(String tableName, String paramValue, String fieldName){
        this.searchSql = tableName + "." + fieldName + " = \"" + paramValue + "\"";
    }

}
