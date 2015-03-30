package org.mifosplatform.infrastructure.sms.handler;

import org.mifosplatform.commands.handler.NewCommandSourceHandler;
import org.mifosplatform.infrastructure.core.api.JsonCommand;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResult;
import org.mifosplatform.infrastructure.sms.service.SmsCampaignWritePlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CloseSmsCampaignCommandHandler implements NewCommandSourceHandler {
    private final SmsCampaignWritePlatformService smsCampaignWritePlatformService;

   @Autowired
    public CloseSmsCampaignCommandHandler(final SmsCampaignWritePlatformService smsCampaignWritePlatformService) {
        this.smsCampaignWritePlatformService = smsCampaignWritePlatformService;
    }

    @Override
    public CommandProcessingResult processCommand(JsonCommand command) {
       return this.smsCampaignWritePlatformService.closeSmsCampaign(command.entityId(), command);
    }
}