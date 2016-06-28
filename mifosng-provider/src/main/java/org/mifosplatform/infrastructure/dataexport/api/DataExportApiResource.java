/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.api;

import org.mifosplatform.commands.domain.CommandWrapper;
import org.mifosplatform.commands.service.CommandWrapperBuilder;
import org.mifosplatform.commands.service.PortfolioCommandSourceWritePlatformService;
import org.mifosplatform.infrastructure.core.api.ApiRequestParameterHelper;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResult;
import org.mifosplatform.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.mifosplatform.infrastructure.core.serialization.ToApiJsonSerializer;
import org.mifosplatform.infrastructure.dataexport.data.DataExportData;
import org.mifosplatform.infrastructure.dataexport.data.DataExportEntityData;
import org.mifosplatform.infrastructure.dataexport.service.DataExportReadPlatformService;
import org.mifosplatform.infrastructure.security.service.PlatformSecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.*;

@Path(DataExportApiConstants.DATA_EXPORT_URI_PATH_VALUE)
@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class DataExportApiResource {
    private final PlatformSecurityContext platformSecurityContext;
    private final DataExportReadPlatformService dataExportReadPlatformService;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final ToApiJsonSerializer<Object> toApiJsonSerializer;
    private final ApiRequestParameterHelper apiRequestParameterHelper;

    @Autowired
    public DataExportApiResource(final DataExportReadPlatformService dataExportReadPlatformService, 
            final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService, 
            final PlatformSecurityContext platformSecurityContext, 
            final ToApiJsonSerializer<Object> toApiJsonSerializer, 
            final ApiRequestParameterHelper apiRequestParameterHelper) {
        this.platformSecurityContext = platformSecurityContext;
        this.dataExportReadPlatformService = dataExportReadPlatformService;
        this.toApiJsonSerializer = toApiJsonSerializer;
        this.apiRequestParameterHelper = apiRequestParameterHelper;
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
    }

    @GET
    @Consumes({ MediaType.APPLICATION_JSON})
    @Produces({ MediaType.APPLICATION_JSON})
    public String retrieveAllDataExports(@Context final UriInfo uriInfo){

        this.platformSecurityContext.authenticatedUser().validateHasReadPermission(DataExportApiConstants.DATA_EXPORT);

        final Collection<DataExportData> dataExports = this.dataExportReadPlatformService.retrieveAll();

        return this.toApiJsonSerializer.serialize(dataExports);
    }

    @GET
    @Path("{resourceId}")
    @Consumes({ MediaType.APPLICATION_JSON})
    @Produces({ MediaType.APPLICATION_JSON})
    public String retrieveOneDataExport(@PathParam("resourceId") final Long resourceId, @Context final UriInfo uriInfo){

        this.platformSecurityContext.authenticatedUser().validateHasReadPermission(DataExportApiConstants.DATA_EXPORT);

        final DataExportData dataExport = this.dataExportReadPlatformService.retrieveOne(resourceId);

        return this.toApiJsonSerializer.serialize(dataExport);
    }

    @GET
    @Path("{entityName}/template")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveDataExportTemplate(@PathParam("entityName") final String entityName, 
            @Context final UriInfo uriInfo) {

        this.platformSecurityContext.authenticatedUser().validateHasReadPermission(entityName);
        
        final DataExportEntityData dataExportEntityData = this.dataExportReadPlatformService.retrieveTemplate(entityName);
        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        
        return this.toApiJsonSerializer.serialize(settings, dataExportEntityData);
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String createDataExport(final String apiRequestBodyAsJson) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                .createDataExport(apiRequestBodyAsJson) //
                .build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.toApiJsonSerializer.serialize(result);
    }

    @GET
    @Path("{resourceId}/download")
    @Produces({ MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON })
    public Response downloadDataExportFile(@PathParam("resourceId") final Long resourceId, 
            @QueryParam(DataExportApiConstants.ENTITY) final String entity, 
            @QueryParam(DataExportApiConstants.FILE_FORMAT) final String fileFormat) {

        //this.platformSecurityContext.authenticatedUser().validateHasReadPermission(entity);

        return this.dataExportReadPlatformService.downloadDataExportFile(resourceId, fileFormat);
    }
}
