/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.service;

import org.mifosplatform.infrastructure.dataexport.data.DataExportData;
import org.mifosplatform.infrastructure.dataexport.data.DataExportEntityData;

import javax.ws.rs.core.Response;

import java.util.Collection;

public interface DataExportReadPlatformService {
    /**
     * Retrieves {@link DataExportData} representation of all {@link DataExport} entities
     * 
     * @return {@link DataExportData} objects
     */
    Collection<DataExportData> retrieveAll();
    
    /**
     * Retrieves {@link DataExportData} representation of the {@link DataExport} entity with id similar to the 
     * parameter passed
     * 
     * @param id entity identifier
     * @return {@link DataExportData} object
     */
    DataExportData retrieveOne(final Long id);
    
    /**
     * Retrieves a template data for the specified base entity
     * 
     * @param baseEntityName
     * @return
     */
    DataExportEntityData retrieveTemplate(final String baseEntityName);

    /**
     * Creates a file with the data export entity data
     * 
     * @param id data export entity identifier
     * @param fileFormat file format (xml, xls, csv)
     * @return
     */
    Response downloadDataExportFile(final Long id, final String fileFormat);
    
    /**
     * Retrieves all base entities
     * 
     * @return {@link DataExportEntityData} objects
     */
    Collection<DataExportEntityData> retrieveAllBaseEntities();
}
