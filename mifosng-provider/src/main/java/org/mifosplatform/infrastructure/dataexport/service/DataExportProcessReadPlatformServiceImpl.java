/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.service;

import java.util.ArrayList;
import java.util.Collection;

import org.mifosplatform.infrastructure.dataexport.data.DataExportProcessData;
import org.mifosplatform.infrastructure.dataexport.domain.DataExportProcess;
import org.mifosplatform.infrastructure.dataexport.domain.DataExportProcessRepository;
import org.mifosplatform.infrastructure.dataexport.exception.DataExportProcessNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DataExportProcessReadPlatformServiceImpl implements DataExportProcessReadPlatformService {
    private final DataExportProcessRepository dataExportProcessRepository;

    /**
     * @param dataExportProcessRepository
     */
    @Autowired
    public DataExportProcessReadPlatformServiceImpl(final DataExportProcessRepository dataExportProcessRepository) {
        this.dataExportProcessRepository = dataExportProcessRepository;
    }

    @Override
    public DataExportProcessData retrieveOne(Long id) {
        final DataExportProcess dataExportProcess = this.dataExportProcessRepository.findOne(id);
        
        if (dataExportProcess == null) {
            throw new DataExportProcessNotFoundException(id);
        }
        
        return dataExportProcess.toData();
    }

    @Override
    public Collection<DataExportProcessData> retrieveAll() {
        final Collection<DataExportProcess> dataExportProcessCollection = this.dataExportProcessRepository.findAll();
        final Collection<DataExportProcessData> dataExportProcessDataCollection = new ArrayList<>();
        
        for (DataExportProcess dataExportProcess : dataExportProcessCollection) {
            dataExportProcessDataCollection.add(dataExportProcess.toData());
        }
        
        return dataExportProcessDataCollection;
    }
}
