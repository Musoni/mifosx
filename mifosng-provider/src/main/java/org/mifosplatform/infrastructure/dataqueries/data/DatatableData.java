/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataqueries.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Immutable data object representing datatable data.
 */
public class DatatableData {

    @SuppressWarnings("unused")
    private final String applicationTableName;
    private final String registeredTableName;
    @SuppressWarnings("unused")
    private final Long category;

    private final String displayName;

    @SuppressWarnings("unused")
    private final boolean systemDefined;
    private final List<ResultsetColumnHeaderData> columnHeaderData;
    @SuppressWarnings("unused")
    private final List<MetaDataResultSet> metaDataResultSets;

    private final static Logger logger = LoggerFactory.getLogger(DatatableData.class);


    public static DatatableData create(final String applicationTableName, final String registeredTableName,
            final List<ResultsetColumnHeaderData> columnHeaderData,final Long category,final List<MetaDataResultSet> metaDataResultSets,final boolean systemDefined,final String displayName) {
        return new DatatableData(applicationTableName, registeredTableName, columnHeaderData,category,metaDataResultSets,systemDefined,displayName);
    }

    private DatatableData(final String applicationTableName, final String registeredTableName,final List<ResultsetColumnHeaderData> columnHeaderData,
                          final Long category,final List<MetaDataResultSet> metaDataResultSets,final boolean systemDefined, final String displayName) {
        this.applicationTableName = applicationTableName;
        this.registeredTableName = registeredTableName;
        this.columnHeaderData = columnHeaderData;
        this.category  = category;
        this.metaDataResultSets = metaDataResultSets;
        this.systemDefined = systemDefined;
        this.displayName = displayName;

    }

    public boolean hasColumn(final String columnName){

        for(ResultsetColumnHeaderData c : this.columnHeaderData){

            if(c.getColumnName().equals(columnName)) return true;

            logger.info(c.getColumnName()+"is it equal to"+ columnName);
        }

        return false;
    }

    public String getRegisteredTableName(){
        return registeredTableName;
    }
    
    /**
     * @return the displayName
     */
    public String getDisplayName() {
    	return this.displayName;
    }
}