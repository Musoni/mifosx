/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.portfolio.account.api;

import java.util.Arrays;
import java.util.Date;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.StringUtils;
import org.mifosplatform.commands.domain.CommandWrapper;
import org.mifosplatform.commands.service.CommandWrapperBuilder;
import org.mifosplatform.commands.service.PortfolioCommandSourceWritePlatformService;
import org.mifosplatform.infrastructure.core.api.ApiParameterHelper;
import org.mifosplatform.infrastructure.core.api.ApiRequestParameterHelper;
import org.mifosplatform.infrastructure.core.api.JsonCommand;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResult;
import org.mifosplatform.infrastructure.core.exception.UnrecognizedQueryParamException;
import org.mifosplatform.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.mifosplatform.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.mifosplatform.infrastructure.core.serialization.FromJsonHelper;
import org.mifosplatform.infrastructure.core.service.Page;
import org.mifosplatform.infrastructure.security.exception.NoAuthorizationException;
import org.mifosplatform.infrastructure.security.service.PlatformSecurityContext;
import org.mifosplatform.portfolio.account.data.AccountTransferData;
import org.mifosplatform.portfolio.account.data.StandingInstructionDTO;
import org.mifosplatform.portfolio.account.data.StandingInstructionData;
import org.mifosplatform.portfolio.account.service.AccountTransfersReadPlatformService;
import org.mifosplatform.portfolio.account.service.StandingInstructionReadPlatformService;
import org.mifosplatform.portfolio.account.service.StandingInstructionWritePlatformService;
import org.mifosplatform.infrastructure.core.service.SearchParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.gson.JsonElement;

@Path("/standinginstructions")
@Component
@Scope("singleton")
public class StandingInstructionApiResource {

    private final PlatformSecurityContext context;
    private final DefaultToApiJsonSerializer<StandingInstructionData> toApiJsonSerializer;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final ApiRequestParameterHelper apiRequestParameterHelper;
    private final StandingInstructionReadPlatformService standingInstructionReadPlatformService;
    private final AccountTransfersReadPlatformService accountTransfersReadPlatformService;
    private final StandingInstructionWritePlatformService standingInstructionWritePlatformService;
    private final FromJsonHelper fromApiJsonHelper;

    @Autowired
    public StandingInstructionApiResource(final PlatformSecurityContext context,
            final DefaultToApiJsonSerializer<StandingInstructionData> toApiJsonSerializer,
            final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService,
            final ApiRequestParameterHelper apiRequestParameterHelper,
            final StandingInstructionReadPlatformService standingInstructionReadPlatformService,
            final AccountTransfersReadPlatformService accountTransfersReadPlatformService, 
            final StandingInstructionWritePlatformService standingInstructionWritePlatformService, 
            final FromJsonHelper fromApiJsonHelper) {
        this.context = context;
        this.toApiJsonSerializer = toApiJsonSerializer;
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
        this.apiRequestParameterHelper = apiRequestParameterHelper;
        this.standingInstructionReadPlatformService = standingInstructionReadPlatformService;
        this.accountTransfersReadPlatformService = accountTransfersReadPlatformService;
        this.standingInstructionWritePlatformService = standingInstructionWritePlatformService;
        this.fromApiJsonHelper = fromApiJsonHelper;
    }

    @GET
    @Path("template")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String template(@QueryParam("fromOfficeId") final Long fromOfficeId, @QueryParam("fromClientId") final Long fromClientId,
            @QueryParam("fromGroupId") final Long fromGroupId, @QueryParam("fromAccountId") final Long fromAccountId,
            @QueryParam("fromAccountType") final Integer fromAccountType, @QueryParam("toOfficeId") final Long toOfficeId,
            @QueryParam("toClientId") final Long toClientId, @QueryParam("toGroupId") final Long toGroupId,
            @QueryParam("toAccountId") final Long toAccountId, @QueryParam("toAccountType") final Integer toAccountType,
            @QueryParam("transferType") final Integer transferType, @Context final UriInfo uriInfo) {

        this.context.authenticatedUser().validateHasReadPermission(StandingInstructionApiConstants.STANDING_INSTRUCTION_RESOURCE_NAME);

        final StandingInstructionData standingInstructionData = this.standingInstructionReadPlatformService.retrieveTemplate(fromOfficeId,
                fromClientId, fromGroupId, fromAccountId, fromAccountType, toOfficeId, toClientId, toGroupId, toAccountId, toAccountType, transferType);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, standingInstructionData,
                StandingInstructionApiConstants.RESPONSE_DATA_PARAMETERS);
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String create(final String apiRequestBodyAsJson, @QueryParam("command") final String commandParam) {
        CommandProcessingResult result = null;
        final CommandWrapper commandRequest = new CommandWrapperBuilder().createStandingInstruction().withJson(apiRequestBodyAsJson).build();
        
        if (is(commandParam, "execute")) {
            final boolean hasNotPermission = this.context.authenticatedUser().hasNotPermissionForAnyOf("ALL_FUNCTIONS", "EXECUTE_STANDINGINSTRUCTION");
            
            if (hasNotPermission) {
                final String exceptionMessage = "User is not authorized to execute the standing instruction job";
                
                throw new NoAuthorizationException(exceptionMessage);
            }
            
            final String json = commandRequest.getJson();
            final JsonElement parsedCommand = this.fromApiJsonHelper.parse(json);
            final JsonCommand jsonCommand = JsonCommand.from(json, parsedCommand, this.fromApiJsonHelper, commandRequest.getEntityName(), commandRequest.getEntityId(),
                    commandRequest.getSubentityId(), commandRequest.getGroupId(), commandRequest.getClientId(), commandRequest.getLoanId(), commandRequest.getSavingsId(),
                    commandRequest.getTransactionId(), commandRequest.getHref(), commandRequest.getProductId());
            
            result = this.standingInstructionWritePlatformService.executeStandingInstructions(jsonCommand);
        }
        
        else {
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        }

        return this.toApiJsonSerializer.serialize(result);
    }

    @PUT
    @Path("{standingInstructionId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String update(@PathParam("standingInstructionId") final Long standingInstructionId, final String apiRequestBodyAsJson,
            @QueryParam("command") final String commandParam) {

        CommandWrapper commandRequest = null;
        if (is(commandParam, "update")) {
            commandRequest = new CommandWrapperBuilder().updateStandingInstruction(standingInstructionId).withJson(apiRequestBodyAsJson)
                    .build();
        } else if (is(commandParam, "delete")) {
            commandRequest = new CommandWrapperBuilder().deleteStandingInstruction(standingInstructionId).withJson(apiRequestBodyAsJson)
                    .build();
        }

        if (commandRequest == null) { throw new UnrecognizedQueryParamException("command", commandParam); }
        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.toApiJsonSerializer.serialize(result);

    }

    private boolean is(final String commandParam, final String commandValue) {
        return StringUtils.isNotBlank(commandParam) && commandParam.trim().equalsIgnoreCase(commandValue);
    }

    @GET
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveAll(@Context final UriInfo uriInfo, @QueryParam("sqlSearch") final String sqlSearch,
            @QueryParam("externalId") final String externalId, @QueryParam("offset") final Integer offset,
            @QueryParam("limit") final Integer limit, @QueryParam("orderBy") final String orderBy,
            @QueryParam("sortOrder") final String sortOrder, @QueryParam("transferType") final Integer transferType,
            @QueryParam("clientName") final String clientName, @QueryParam("clientId") final Long clientId,
            @QueryParam("groupName") final String groupName, @QueryParam("groupId") final Long groupId,
            @QueryParam("fromAccountId") final Long fromAccount, @QueryParam("fromAccountType") final Integer fromAccountType) {

        this.context.authenticatedUser().validateHasReadPermission(StandingInstructionApiConstants.STANDING_INSTRUCTION_RESOURCE_NAME);

        final SearchParameters searchParameters = SearchParameters.forAccountTransfer(sqlSearch, externalId, offset, limit, orderBy,
                sortOrder);

        final Date startDateRange = null;
        final Date endDateRange = null;
        StandingInstructionDTO standingInstructionDTO = new StandingInstructionDTO(searchParameters, transferType, clientName, clientId,
                groupName, groupId, fromAccount, fromAccountType, startDateRange, endDateRange, null);

        final Page<StandingInstructionData> transfers = this.standingInstructionReadPlatformService.retrieveAll(standingInstructionDTO);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, transfers, StandingInstructionApiConstants.RESPONSE_DATA_PARAMETERS);
    }

    @GET
    @Path("{standingInstructionId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveOne(@PathParam("standingInstructionId") final Long standingInstructionId, @Context final UriInfo uriInfo,
            @QueryParam("sqlSearch") final String sqlSearch, @QueryParam("externalId") final String externalId,
            @QueryParam("offset") final Integer offset, @QueryParam("limit") final Integer limit,
            @QueryParam("orderBy") final String orderBy, @QueryParam("sortOrder") final String sortOrder) {

        this.context.authenticatedUser().validateHasReadPermission(StandingInstructionApiConstants.STANDING_INSTRUCTION_RESOURCE_NAME);

        StandingInstructionData standingInstructionData = this.standingInstructionReadPlatformService.retrieveOne(standingInstructionId);
        final SearchParameters searchParameters = SearchParameters.forAccountTransfer(sqlSearch, externalId, offset, limit, orderBy,
                sortOrder);
        final Set<String> associationParameters = ApiParameterHelper.extractAssociationsForResponseIfProvided(uriInfo.getQueryParameters());
        Page<AccountTransferData> transfers = null;
        if (!associationParameters.isEmpty()) {
            if (associationParameters.contains("all")) {
                associationParameters.addAll(Arrays.asList("transactions", "template"));
            }
            if (associationParameters.contains("transactions")) {
                transfers = this.accountTransfersReadPlatformService.retrieveByStandingInstruction(standingInstructionId, searchParameters);
                standingInstructionData = StandingInstructionData.withTransferData(standingInstructionData, transfers);
            }
            if (associationParameters.contains("template")) {
                final StandingInstructionData templateData = this.standingInstructionReadPlatformService.retrieveTemplate(
                        standingInstructionData.fromClient().officeId(), standingInstructionData.fromClient().id(),
                        standingInstructionData.fromGroup().getId(), standingInstructionData.fromAccount().accountId(),
                        standingInstructionData.fromAccountType().getValue(), standingInstructionData.toClient().officeId(),
                        standingInstructionData.toClient().id(), standingInstructionData.toGroup().getId(),
                        standingInstructionData.toAccount().accountId(), standingInstructionData.toAccountType().getValue(),
                        standingInstructionData.transferType().getValue());
                standingInstructionData = StandingInstructionData.withTemplateData(standingInstructionData, templateData);
            }
        }

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, standingInstructionData,
                StandingInstructionApiConstants.RESPONSE_DATA_PARAMETERS);
    }
}