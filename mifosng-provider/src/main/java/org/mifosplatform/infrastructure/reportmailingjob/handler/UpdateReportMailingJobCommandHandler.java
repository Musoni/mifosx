/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.reportmailingjob.handler;

import org.mifosplatform.commands.annotation.CommandType;
import org.mifosplatform.commands.handler.NewCommandSourceHandler;
import org.mifosplatform.infrastructure.core.api.JsonCommand;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResult;
import org.mifosplatform.infrastructure.reportmailingjob.ReportMailingJobConstants;
import org.mifosplatform.infrastructure.reportmailingjob.service.ReportMailingJobWritePlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@CommandType(entity = ReportMailingJobConstants.REPORT_MAILING_JOB_ENTITY_NAME, action = "UPDATE")
public class UpdateReportMailingJobCommandHandler implements NewCommandSourceHandler {
    private final ReportMailingJobWritePlatformService reportMailingJobWritePlatformService;
    
    @Autowired
    public UpdateReportMailingJobCommandHandler(final ReportMailingJobWritePlatformService reportMailingJobWritePlatformService) {
        this.reportMailingJobWritePlatformService = reportMailingJobWritePlatformService;
    }

    @Override
    @Transactional
    public CommandProcessingResult processCommand(JsonCommand jsonCommand) {
        return this.reportMailingJobWritePlatformService.updateReportMailingJob(jsonCommand.entityId(), jsonCommand);
    }
}
