/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.portfolio.creditcheck.handler;

import org.mifosplatform.commands.annotation.CommandType;
import org.mifosplatform.commands.handler.NewCommandSourceHandler;
import org.mifosplatform.infrastructure.core.api.JsonCommand;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResult;
import org.mifosplatform.portfolio.creditcheck.CreditCheckConstants;
import org.mifosplatform.portfolio.creditcheck.service.CreditCheckWritePlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@CommandType(entity = CreditCheckConstants.CREDIT_CHECK_ENTITY_NAME, action = "UPDATE")
public class UpdateCreditCheckCommandHandler implements NewCommandSourceHandler {
    private final CreditCheckWritePlatformService creditCheckWritePlatformService;

    @Autowired
    public UpdateCreditCheckCommandHandler(final CreditCheckWritePlatformService creditCheckWritePlatformService) {
        this.creditCheckWritePlatformService = creditCheckWritePlatformService;
    }

    @Override
    public CommandProcessingResult processCommand(JsonCommand command) {
        return this.creditCheckWritePlatformService.updateCreditCheck(command.entityId(), command);
    }
}
