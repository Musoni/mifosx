/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.api;

import java.util.Collection;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.mifosplatform.infrastructure.core.api.ApiRequestParameterHelper;
import org.mifosplatform.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.mifosplatform.infrastructure.core.serialization.ToApiJsonSerializer;
import org.mifosplatform.infrastructure.dataexport.data.DataExportProcessData;
import org.mifosplatform.infrastructure.dataexport.service.DataExportProcessReadPlatformService;
import org.mifosplatform.infrastructure.security.service.PlatformSecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/dataexportprocesses")
@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class DataExportProcessApiResource {
    private final PlatformSecurityContext platformSecurityContext;
    private final ToApiJsonSerializer<Object> toApiJsonSerializer;
    private final DataExportProcessReadPlatformService dataExportProcessReadPlatformService;
    private final ApiRequestParameterHelper apiRequestParameterHelper;
    
    /**
     * @param platformSecurityContext
     * @param toApiJsonSerializer
     * @param dataExportProcessReadPlatformService
     */
    @Autowired
    public DataExportProcessApiResource(final PlatformSecurityContext platformSecurityContext,
            final ToApiJsonSerializer<Object> toApiJsonSerializer, 
            final DataExportProcessReadPlatformService dataExportProcessReadPlatformService, 
            final ApiRequestParameterHelper apiRequestParameterHelper) {
        this.platformSecurityContext = platformSecurityContext;
        this.toApiJsonSerializer = toApiJsonSerializer;
        this.dataExportProcessReadPlatformService = dataExportProcessReadPlatformService;
        this.apiRequestParameterHelper = apiRequestParameterHelper;
    }
    
    @GET
    @Path("{id}")
    @Consumes({ MediaType.APPLICATION_JSON})
    @Produces({ MediaType.APPLICATION_JSON})
    public String retrieveOne(@PathParam("id") final Long id, @Context final UriInfo uriInfo) {
        this.platformSecurityContext.authenticatedUser().validateHasReadPermission(DataExportApiConstants.DATAEXPORTPROCESS);
        
        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        final DataExportProcessData dataExportProcessData = this.dataExportProcessReadPlatformService.retrieveOne(id);
        
        return this.toApiJsonSerializer.serialize(settings, dataExportProcessData);
    }
    
    @GET
    @Consumes({ MediaType.APPLICATION_JSON})
    @Produces({ MediaType.APPLICATION_JSON})
    public String retrieveAll(@Context final UriInfo uriInfo) {
        this.platformSecurityContext.authenticatedUser().validateHasReadPermission(DataExportApiConstants.DATAEXPORTPROCESS);
        
        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        final Collection<DataExportProcessData> dataExportProcessCollection = this.dataExportProcessReadPlatformService.retrieveAll();
        
        return this.toApiJsonSerializer.serialize(settings, dataExportProcessCollection);
    }
}
