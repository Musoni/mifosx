/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.data;

import java.util.Collection;

import org.mifosplatform.infrastructure.dataqueries.data.DatatableData;

public class DataExportEntityData {
    private final String name;
    private final String table;
    private final Collection<DatatableData> datatables;
    private final Collection<DataExportFieldData> fields;
    
    /**
     * @param name
     * @param table
     * @param datatables
     * @param fields
     */
    private DataExportEntityData(final String name, final String table, final Collection<DatatableData> datatables,
            final Collection<DataExportFieldData> fields) {
        this.name = name;
        this.table = table;
        this.datatables = datatables;
        this.fields = fields;
    }
    
    /**
     * Creates a new {@link DataExportEntityData} object
     * 
     * @param name
     * @param table
     * @param datatables
     * @param fields
     * @return {@link DataExportEntityData} object
     */
    public static DataExportEntityData newInstance(final String name, final String table, final Collection<DatatableData> datatables,
            final Collection<DataExportFieldData> fields) {
        return new DataExportEntityData(name, table, datatables, fields);
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the table
     */
    public String getTable() {
        return table;
    }

    /**
     * @return the datatables
     */
    public Collection<DatatableData> getDatatables() {
        return datatables;
    }

    /**
     * @return the fields
     */
    public Collection<DataExportFieldData> getFields() {
        return fields;
    }
}
