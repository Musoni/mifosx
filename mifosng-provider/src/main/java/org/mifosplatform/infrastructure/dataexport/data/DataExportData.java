/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.data;

/**
 * Immutable object representing a {@link DataExport} entity data
 */
public class DataExportData {
    private final Long id;
    private final String baseEntity;
    private final String json;
    
    /**
     * @param id
     * @param baseEntity
     * @param json
     */
    private DataExportData(final Long id, final String baseEntity, final String json) {
        this.id = id;
        this.baseEntity = baseEntity;
        this.json = json;
    }
    
    /**
     * Creates a new {@link DataExportData} object
     * 
     * @param id
     * @param baseEntity
     * @param json
     * @return {@link DataExportData} object
     */
    public static DataExportData newInstance(final Long id, final String baseEntity, final String json) {
        return new DataExportData(id, baseEntity, json);
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @return the baseEntity
     */
    public String getBaseEntity() {
        return baseEntity;
    }

    /**
     * @return the json
     */
    public String getJson() {
        return json;
    }
}
