/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.service;

import java.util.Collection;

import org.mifosplatform.infrastructure.dataexport.data.DataExportProcessData;

public interface DataExportProcessReadPlatformService {
    /**
     * Retrieves a {@link DataExportProcess} entity data by id
     * 
     * @param id {@link DataExportProcess} entity identifier
     * @return {@link DataExportProcessData} object
     */
    DataExportProcessData retrieveOne(final Long id);
    
    /**
     * Retrieves all {@link DataExportProcess} entities
     * 
     * @return {@link DataExportProcessData} objects
     */
    Collection<DataExportProcessData> retrieveAll();
}
