/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.service;

import org.mifosplatform.infrastructure.dataexport.data.DataExportEntityData;
import org.mifosplatform.infrastructure.dataexport.data.DataExportFilter;
import org.mifosplatform.infrastructure.dataexport.data.DataExportRequestData;
//import org.mifosplatform.infrastructure.dataexport.data.DataExportTemplateData;
import javax.ws.rs.core.Response;

import java.util.Collection;
import java.util.List;

public interface DataExportReadPlatformService {

    Collection<DataExportEntityData> retrieveDataExportTemplate(String entity);

    DataExportRequestData retrieveDataExportRequestData (String entity);

    DataExportRequestData retrieveDataExportRequestData (String entity, List<DataExportFilter> dataExportFilters, List<String> exportDatatables);

    Collection<DataExportRequestData> retrieveDataExportRequestDataCollection ();

    Response downloadDataExportFile(String entity, Long dataExportProcessId, String fileFormat);
}
