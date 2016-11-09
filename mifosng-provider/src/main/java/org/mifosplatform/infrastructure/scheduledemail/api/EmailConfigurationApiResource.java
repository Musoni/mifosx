/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.scheduledemail.api;

import org.mifosplatform.commands.domain.CommandWrapper;
import org.mifosplatform.commands.service.CommandWrapperBuilder;
import org.mifosplatform.commands.service.PortfolioCommandSourceWritePlatformService;
import org.mifosplatform.infrastructure.core.api.ApiRequestParameterHelper;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResult;
import org.mifosplatform.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.mifosplatform.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.mifosplatform.infrastructure.scheduledemail.data.EmailConfigurationData;
import org.mifosplatform.infrastructure.scheduledemail.data.EmailData;
import org.mifosplatform.infrastructure.scheduledemail.service.EmailConfigurationReadPlatformService;
import org.mifosplatform.infrastructure.scheduledemail.service.EmailReadPlatformService;
import org.mifosplatform.infrastructure.security.service.PlatformSecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.util.Collection;


@Path("/scheduledemail/configuration")
@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Component
@Scope("singleton")
public class EmailConfigurationApiResource {

    private final String resourceNameForPermissions = "EMAIL_CONFIGURATION";
    private final PlatformSecurityContext context;
    private final EmailReadPlatformService readPlatformService;
    private final DefaultToApiJsonSerializer<EmailConfigurationData> toApiJsonSerializer;
    private final ApiRequestParameterHelper apiRequestParameterHelper;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final EmailConfigurationReadPlatformService emailConfigurationReadPlatformService;

    @Autowired
    public EmailConfigurationApiResource(final PlatformSecurityContext context, final EmailReadPlatformService readPlatformService,
                            final DefaultToApiJsonSerializer<EmailConfigurationData> toApiJsonSerializer,
                            final ApiRequestParameterHelper apiRequestParameterHelper,
                            final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService,
                            final EmailConfigurationReadPlatformService emailConfigurationReadPlatformService) {
        this.context = context;
        this.readPlatformService = readPlatformService;
        this.toApiJsonSerializer = toApiJsonSerializer;
        this.apiRequestParameterHelper = apiRequestParameterHelper;
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
        this.emailConfigurationReadPlatformService = emailConfigurationReadPlatformService;
    }

    @GET
    public String retrieveAll(@Context final UriInfo uriInfo){
        this.context.authenticatedUser().validateHasReadPermission(this.resourceNameForPermissions);

        final Collection<EmailConfigurationData> configuration = this.emailConfigurationReadPlatformService.retrieveAll();

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());

        return this.toApiJsonSerializer.serialize(settings, configuration);
    }


    @PUT
    public String updateConfiguration(@Context final UriInfo uriInfo, final String apiRequestBodyAsJson){

        final CommandWrapper commandRequest = new CommandWrapperBuilder().updateEmailConfiguration().withJson(apiRequestBodyAsJson).build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.toApiJsonSerializer.serialize(result);
    }
}
