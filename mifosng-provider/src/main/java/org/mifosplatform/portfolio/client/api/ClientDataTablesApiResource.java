/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.portfolio.client.api;

import org.mifosplatform.infrastructure.core.api.ApiRequestParameterHelper;
import org.mifosplatform.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.mifosplatform.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.mifosplatform.infrastructure.dataqueries.data.GenericResultsetData;
import org.mifosplatform.infrastructure.dataqueries.domain.RegisteredTable;
import org.mifosplatform.infrastructure.dataqueries.domain.RegisteredTableRepository;
import org.mifosplatform.infrastructure.dataqueries.service.ReadWriteNonCoreDataService;
import org.mifosplatform.infrastructure.security.service.PlatformSecurityContext;
import org.mifosplatform.portfolio.client.service.ClientReadPlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.util.Collection;
import java.util.Map;

@Path("/clients/{clientId}/datatables")
@Component
@Scope("singleton")
public class ClientDataTablesApiResource {

    private final PlatformSecurityContext context;
    private final ClientReadPlatformService clientReadPlatformService;
    private final RegisteredTableRepository registeredTableRepository;
    private final ReadWriteNonCoreDataService readWriteNonCoreDataService;
    private final ApiRequestParameterHelper apiRequestParameterHelper;
    private final DefaultToApiJsonSerializer<RegisteredTable> toApiJsonSerializer;

    @Autowired
    public ClientDataTablesApiResource(final PlatformSecurityContext context, final ClientReadPlatformService clientReadPlatformService,
                                       final RegisteredTableRepository registeredTableRepository,
                                       final ApiRequestParameterHelper apiRequestParameterHelper,
                                       final DefaultToApiJsonSerializer<RegisteredTable> toApiJsonSerializer,
                                       final ReadWriteNonCoreDataService readWriteNonCoreDataService){
        this.context = context;
        this.clientReadPlatformService = clientReadPlatformService;
        this.registeredTableRepository = registeredTableRepository;
        this.apiRequestParameterHelper = apiRequestParameterHelper;
        this.toApiJsonSerializer = toApiJsonSerializer;
        this.readWriteNonCoreDataService = readWriteNonCoreDataService;
    }

    @GET
    @Path("template")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveAllClientDataTables(@Context final UriInfo uriInfo){

        this.context.authenticatedUser().validateHasReadPermission("Client");

        Collection<RegisteredTable> registeredTables = this.registeredTableRepository.findAllByApplicationTableName("m_client");

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, registeredTables);
    }

    @GET
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveAllClientDataTableData(@Context final UriInfo uriInfo, @PathParam("clientId") final Long clientId){

        this.context.authenticatedUser().validateHasReadPermission("Client");

        Map<String,GenericResultsetData> resultsetData = this.readWriteNonCoreDataService.retrieveAllEntityResultSets("m_client",clientId);

        return this.toApiJsonSerializer.serialize(resultsetData);
    }
}
