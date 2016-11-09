/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataqueries.api;

import org.mifosplatform.commands.domain.CommandWrapper;
import org.mifosplatform.commands.service.CommandWrapperBuilder;
import org.mifosplatform.commands.service.PortfolioCommandSourceWritePlatformService;
import org.mifosplatform.infrastructure.core.api.ApiParameterHelper;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResult;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResultBuilder;
import org.mifosplatform.infrastructure.core.serialization.ToApiJsonSerializer;
import org.mifosplatform.infrastructure.dataqueries.data.DatatableCategoryData;
import org.mifosplatform.infrastructure.dataqueries.data.DatatableData;
import org.mifosplatform.infrastructure.dataqueries.data.GenericResultsetData;
import org.mifosplatform.infrastructure.dataqueries.service.GenericDataService;
import org.mifosplatform.infrastructure.dataqueries.service.ReadWriteNonCoreDataService;
import org.mifosplatform.infrastructure.security.service.PlatformSecurityContext;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.util.List;

//import org.slf4j.Logger;

@Path("/datatables")
@Component
@Scope("singleton")
public class DatatablesApiResource {

    private final PlatformSecurityContext context;
    private final GenericDataService genericDataService;
    private final ReadWriteNonCoreDataService readWriteNonCoreDataService;
    private final ToApiJsonSerializer<GenericResultsetData> toApiJsonSerializer;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final static org.slf4j.Logger logger = LoggerFactory.getLogger(DatatablesApiResource.class);

    @Autowired
    public DatatablesApiResource(final PlatformSecurityContext context, final GenericDataService genericDataService,
            final ReadWriteNonCoreDataService readWriteNonCoreDataService,
            final ToApiJsonSerializer<GenericResultsetData> toApiJsonSerializer,
            final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService) {
        this.context = context;
        this.genericDataService = genericDataService;
        this.readWriteNonCoreDataService = readWriteNonCoreDataService;
        this.toApiJsonSerializer = toApiJsonSerializer;
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
    }

    @GET
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String getDatatables(@QueryParam("apptable") final String apptable, @Context final UriInfo uriInfo) {

        final List<DatatableData> result = this.readWriteNonCoreDataService.retrieveDatatableNames(apptable);

        final boolean prettyPrint = ApiParameterHelper.prettyPrint(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serializePretty(prettyPrint, result);
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String createDatatable(final String apiRequestBodyAsJson) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder().createDBDatatable(apiRequestBodyAsJson).build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        return this.toApiJsonSerializer.serialize(result);
    }

    @PUT
    @Path("{datatableName}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String updateDatatable(@PathParam("datatableName") final String datatableName, final String apiRequestBodyAsJson) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder().updateDBDatatable(datatableName, apiRequestBodyAsJson).build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        return this.toApiJsonSerializer.serialize(result);
    }

    @DELETE
    @Path("{datatableName}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String deleteDatatable(@PathParam("datatableName") final String datatableName, final String apiRequestBodyAsJson) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder().deleteDBDatatable(datatableName, apiRequestBodyAsJson).build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        return this.toApiJsonSerializer.serialize(result);
    }

    @POST
    @Path("register/{datatable}/{apptable}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String registerDatatable(@PathParam("datatable") final String datatable, @PathParam("apptable") final String apptable,
            final String apiRequestBodyAsJson) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder().registerDBDatatable(datatable, apptable)
                .withJson(apiRequestBodyAsJson).build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.toApiJsonSerializer.serialize(result);
    }

    @POST
    @Path("deregister/{datatable}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String deregisterDatatable(@PathParam("datatable") final String datatable) {

        this.readWriteNonCoreDataService.deregisterDatatable(datatable);

        final CommandProcessingResult result = new CommandProcessingResultBuilder().withResourceIdAsString(datatable).build();

        return this.toApiJsonSerializer.serialize(result);
    }

    @GET
    @Path("{datatable}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String getDatatable(@PathParam("datatable") final String datatable, @Context final UriInfo uriInfo) {

        final DatatableData result = this.readWriteNonCoreDataService.retrieveDatatable(datatable);

        final boolean prettyPrint = ApiParameterHelper.prettyPrint(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serializePretty(prettyPrint, result);
    }

    @GET
    @Path("{datatable}/{apptableId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String getDatatable(@PathParam("datatable") final String datatable, @PathParam("apptableId") final Long apptableId,
            @QueryParam("order") final String order,@QueryParam("dropdownValues") final boolean dropdownValues, @Context final UriInfo uriInfo) {

        this.context.authenticatedUser().validateHasDatatableReadPermission(datatable);

        final GenericResultsetData results = this.readWriteNonCoreDataService.retrieveDataTableGenericResultSet(datatable, apptableId,
                order, null,dropdownValues);

        String json = "";
        final boolean genericResultSet = ApiParameterHelper.genericResultSet(uriInfo.getQueryParameters());
        if (genericResultSet) {
            final boolean prettyPrint = ApiParameterHelper.prettyPrint(uriInfo.getQueryParameters());
            json = this.toApiJsonSerializer.serializePretty(prettyPrint, results);
        } else {
            json = this.genericDataService.generateJsonFromGenericResultsetData(results);
        }

        return json;
    }

    @GET
    @Path("{datatable}/{apptableId}/{datatableId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String getDatatableManyEntry(@PathParam("datatable") final String datatable, @PathParam("apptableId") final Long apptableId,
            @PathParam("datatableId") final Long datatableId, @QueryParam("order") final String order,@QueryParam("dropdownValues") final boolean dropdownValues, @Context final UriInfo uriInfo) {


        this.context.authenticatedUser().validateHasDatatableReadPermission(datatable);

        final GenericResultsetData results = this.readWriteNonCoreDataService.retrieveDataTableGenericResultSet(datatable, apptableId,
                order, datatableId,dropdownValues);

        String json = "";
        final boolean genericResultSet = ApiParameterHelper.genericResultSet(uriInfo.getQueryParameters());
        if (genericResultSet) {
            final boolean prettyPrint = ApiParameterHelper.prettyPrint(uriInfo.getQueryParameters());
            json = this.toApiJsonSerializer.serializePretty(prettyPrint, results);
        } else {
            json = this.genericDataService.generateJsonFromGenericResultsetData(results);
        }

        return json;
    }

    @POST
    @Path("{datatable}/{apptableId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String createDatatableEntry(@PathParam("datatable") final String datatable, @PathParam("apptableId") final Long apptableId,
            final String apiRequestBodyAsJson) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                .createDatatable(datatable, apptableId, null) //
                .withJson(apiRequestBodyAsJson) //
                .build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.toApiJsonSerializer.serialize(result);
    }

    @PUT
    @Path("{datatable}/{apptableId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String updateDatatableEntryOnetoOne(@PathParam("datatable") final String datatable,
            @PathParam("apptableId") final Long apptableId, final String apiRequestBodyAsJson) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                .updateDatatable(datatable, apptableId, null) //
                .withJson(apiRequestBodyAsJson) //
                .build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.toApiJsonSerializer.serialize(result);
    }

    @PUT
    @Path("{datatable}/{apptableId}/{datatableId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String updateDatatableEntryOneToMany(@PathParam("datatable") final String datatable,
            @PathParam("apptableId") final Long apptableId, @PathParam("datatableId") final Long datatableId,
            final String apiRequestBodyAsJson) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                .updateDatatable(datatable, apptableId, datatableId) //
                .withJson(apiRequestBodyAsJson) //
                .build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.toApiJsonSerializer.serialize(result);
    }

    @DELETE
    @Path("{datatable}/{apptableId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String deleteDatatableEntries(@PathParam("datatable") final String datatable, @PathParam("apptableId") final Long apptableId) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                .deleteDatatable(datatable, apptableId, null) //
                .build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.toApiJsonSerializer.serialize(result);
    }

    @DELETE
    @Path("{datatable}/{apptableId}/{datatableId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String deleteDatatableEntries(@PathParam("datatable") final String datatable, @PathParam("apptableId") final Long apptableId,
            @PathParam("datatableId") final Long datatableId) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                .deleteDatatable(datatable, apptableId, datatableId) //
                .build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.toApiJsonSerializer.serialize(result);
    }

    @GET
    @Path("categories")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String getDatatable(@Context final UriInfo uriInfo) {

        final List<DatatableCategoryData>categories = this.readWriteNonCoreDataService.retreiveCategories();

        final boolean prettyPrint = ApiParameterHelper.prettyPrint(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serializePretty(prettyPrint,categories);
    }
}