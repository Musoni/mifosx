package org.mifosplatform.infrastructure.sms.handler;

import org.mifosplatform.commands.handler.NewCommandSourceHandler;
import org.mifosplatform.infrastructure.core.api.JsonCommand;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResult;
import org.mifosplatform.infrastructure.sms.service.SmsCampaignWritePlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivateSmsCampaignCommandHandler implements NewCommandSourceHandler {
    private SmsCampaignWritePlatformService smsCampaignWritePlatformService;

    @Autowired
    public ActivateSmsCampaignCommandHandler(final SmsCampaignWritePlatformService smsCampaignWritePlatformService) {
        this.smsCampaignWritePlatformService = smsCampaignWritePlatformService;
    }

    @Transactional
    @Override
    public CommandProcessingResult processCommand(JsonCommand command) {
        return this.smsCampaignWritePlatformService.activateSmsCampaign(command.entityId(), command);
    }
}