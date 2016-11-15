package org.mifosplatform.portfolio.client.api;

import org.mifosplatform.infrastructure.core.api.ApiRequestParameterHelper;
import org.mifosplatform.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.mifosplatform.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.mifosplatform.infrastructure.dataqueries.domain.RegisteredTable;
import org.mifosplatform.infrastructure.dataqueries.service.RegisteredTableMetaDataReadPlatformService;
import org.mifosplatform.infrastructure.dataqueries.service.RegisteredTableReadPlatformService;
import org.mifosplatform.infrastructure.security.service.PlatformSecurityContext;
import org.mifosplatform.portfolio.client.service.ClientDataTableReadPlatformService;
import org.mifosplatform.portfolio.client.service.ClientReadPlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.util.Collection;

@Path("/clients/{clientId}/datatables")
@Component
@Scope("singleton")
public class ClientDataTablesApiResource {

    private final PlatformSecurityContext context;
    private final ClientReadPlatformService clientReadPlatformService;
    private final ClientDataTableReadPlatformService clientDataTableReadPlatformService;
    private final ApiRequestParameterHelper apiRequestParameterHelper;
    private final DefaultToApiJsonSerializer<RegisteredTable> toApiJsonSerializer;

    @Autowired
    public ClientDataTablesApiResource(final PlatformSecurityContext context, final ClientReadPlatformService clientReadPlatformService,
                                       final ApiRequestParameterHelper apiRequestParameterHelper,
                                       final DefaultToApiJsonSerializer<RegisteredTable> toApiJsonSerializer,
                                       final ClientDataTableReadPlatformService clientDataTableReadPlatformService){
        this.context = context;
        this.clientReadPlatformService = clientReadPlatformService;
        this.apiRequestParameterHelper = apiRequestParameterHelper;
        this.toApiJsonSerializer = toApiJsonSerializer;
        this.clientDataTableReadPlatformService = clientDataTableReadPlatformService;
    }

    @GET
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveAllClientDataTables(@Context final UriInfo uriInfo){

        this.context.authenticatedUser().validateHasReadPermission("Client");

        Collection<RegisteredTable> registeredTables = this.clientDataTableReadPlatformService.retrieveDataTables();

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, registeredTables);
    }

    @GET
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveAllClientDataTableData(@Context final UriInfo uriInfo, @PathParam("clientId") final Long clientId){

        this.context.authenticatedUser().validateHasReadPermission("Client");


    }
}
