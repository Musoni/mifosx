/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.handler;

import org.mifosplatform.commands.annotation.CommandType;
import org.mifosplatform.commands.handler.NewCommandSourceHandler;
import org.mifosplatform.infrastructure.core.api.JsonCommand;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResult;
import org.mifosplatform.infrastructure.dataexport.api.DataExportApiConstants;
import org.mifosplatform.infrastructure.dataexport.service.DataExportWritePlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@CommandType(entity = DataExportApiConstants.DATA_EXPORT_ENTITY_NAME, action = "DELETE")
public class DeleteDataExportCommandHandler implements NewCommandSourceHandler {
	private final DataExportWritePlatformService dataExportWritePlatformService;

	/**
	 * @param dataExportWritePlatformService
	 */
	@Autowired
	public DeleteDataExportCommandHandler(DataExportWritePlatformService dataExportWritePlatformService) {
		this.dataExportWritePlatformService = dataExportWritePlatformService;
	}

	@Override
	public CommandProcessingResult processCommand(JsonCommand jsonCommand) {
		return this.dataExportWritePlatformService.deleteDataExport(jsonCommand.entityId());
	}
}
