/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.scheduledemail.handler;

import org.mifosplatform.commands.annotation.CommandType;
import org.mifosplatform.commands.handler.NewCommandSourceHandler;
import org.mifosplatform.infrastructure.core.api.JsonCommand;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResult;
import org.mifosplatform.infrastructure.scheduledemail.service.EmailCampaignWritePlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@CommandType(entity = "EMAIL_CAMPAIGN", action = "CLOSE")
public class CloseEmailCampaignCommandHandler implements NewCommandSourceHandler {
    private final EmailCampaignWritePlatformService emailCampaignWritePlatformService;

   @Autowired
    public CloseEmailCampaignCommandHandler(final EmailCampaignWritePlatformService emailCampaignWritePlatformService) {
        this.emailCampaignWritePlatformService = emailCampaignWritePlatformService;
    }

    @Override
    public CommandProcessingResult processCommand(JsonCommand command) {
       return this.emailCampaignWritePlatformService.closeEmailCampaign(command.entityId(), command);
    }
}
