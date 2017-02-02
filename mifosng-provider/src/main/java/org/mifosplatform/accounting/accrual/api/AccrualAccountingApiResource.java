/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.accounting.accrual.api;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.gson.JsonElement;
import org.mifosplatform.accounting.accrual.service.AccrualAccountingWritePlatformService;
import org.mifosplatform.commands.domain.CommandWrapper;
import org.mifosplatform.commands.service.CommandWrapperBuilder;
import org.mifosplatform.commands.service.PortfolioCommandSourceWritePlatformService;
import org.mifosplatform.infrastructure.core.api.JsonCommand;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResult;
import org.mifosplatform.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.mifosplatform.infrastructure.core.serialization.FromJsonHelper;
import org.mifosplatform.infrastructure.core.service.ThreadLocalContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/runaccruals")
@Component
@Scope("singleton")
public class AccrualAccountingApiResource {

    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final DefaultToApiJsonSerializer<String> apiJsonSerializerService;
    private final AccrualAccountingWritePlatformService accrualAccountingwritePlatformService;
    private final FromJsonHelper fromApiJsonHelper;
    private final static Logger logger = LoggerFactory.getLogger(AccrualAccountingApiResource.class);



    @Autowired
    public AccrualAccountingApiResource(final DefaultToApiJsonSerializer<String> apiJsonSerializerService,  final FromJsonHelper fromApiJsonHelper,
                                        final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService, final AccrualAccountingWritePlatformService accrualAccountingWritePlatformService) {
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
        this.apiJsonSerializerService = apiJsonSerializerService;
        this.accrualAccountingwritePlatformService = accrualAccountingWritePlatformService;
        this.fromApiJsonHelper = fromApiJsonHelper;
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String executePeriodicAccrualAccounting(final String jsonRequestBody) {

        logger.info("Starting Periodic Accrual Accounting");
        final CommandWrapper commandRequest = new CommandWrapperBuilder().excuteAccrualAccounting().withJson(jsonRequestBody).build();

        JsonCommand command = null;
        final JsonElement parsedCommand = this.fromApiJsonHelper.parse(jsonRequestBody);
        command = JsonCommand.from(jsonRequestBody, parsedCommand, this.fromApiJsonHelper, commandRequest.getEntityName(), commandRequest.getEntityId(),
                commandRequest.getSubentityId(), commandRequest.getGroupId(), commandRequest.getClientId(), commandRequest.getLoanId(), commandRequest.getSavingsId(),
                commandRequest.getTransactionId(), commandRequest.getHref(), commandRequest.getProductId());
        
        //final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        final CommandProcessingResult result = this.accrualAccountingwritePlatformService.executeLoansPeriodicAccrual(command);

        logger.info("Periodic Accrual completed");

        return this.apiJsonSerializerService.serialize(result);
    }

}