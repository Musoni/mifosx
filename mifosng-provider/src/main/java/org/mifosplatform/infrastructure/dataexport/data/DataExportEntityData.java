/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.data;

import java.util.Collection;

import org.mifosplatform.infrastructure.dataqueries.data.DatatableData;

public class DataExportEntityData {
    private final String entityName;
    private final String tableName;
    private final Collection<DatatableData> datatables;
    private final Collection<EntityColumnMetaData> columns;
    
    /**
     * @param entityName
     * @param tableName
     * @param datatables
     * @param columns
     */
    private DataExportEntityData(final String entityName, final String tableName, final Collection<DatatableData> datatables,
            final Collection<EntityColumnMetaData> columns) {
        this.entityName = entityName;
        this.tableName = tableName;
        this.datatables = datatables;
        this.columns = columns;
    }
    
    /**
     * Creates a new {@link DataExportEntityData} object
     * 
     * @param entityName
     * @param tableName
     * @param datatables
     * @param columns
     * @return {@link DataExportEntityData} object
     */
    public static DataExportEntityData newInstance(final String entityName, final String tableName, final Collection<DatatableData> datatables,
            final Collection<EntityColumnMetaData> columns) {
        return new DataExportEntityData(entityName, tableName, datatables, columns);
    }

    /**
     * @return the entityName
     */
    public String getEntityName() {
        return entityName;
    }

    /**
     * @return the tableName
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * @return the datatables
     */
    public Collection<DatatableData> getDatatables() {
        return datatables;
    }

    /**
     * @return the columns
     */
    public Collection<EntityColumnMetaData> getColumns() {
        return columns;
    }
}
