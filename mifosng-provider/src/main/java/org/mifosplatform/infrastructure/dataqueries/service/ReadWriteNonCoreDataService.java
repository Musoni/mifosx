/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataqueries.service;

import org.mifosplatform.infrastructure.core.api.JsonCommand;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResult;
import org.mifosplatform.infrastructure.dataqueries.data.DatatableCategoryData;
import org.mifosplatform.infrastructure.dataqueries.data.DatatableData;
import org.mifosplatform.infrastructure.dataqueries.data.GenericResultsetData;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

public interface ReadWriteNonCoreDataService {

    List<DatatableData> retrieveDatatableNames(String appTable);

    DatatableData retrieveDatatable(String datatable);
    DatatableData retrieveDatatableById(Long tableId);

    @PreAuthorize(value = "hasAnyRole('ALL_FUNCTIONS', 'REGISTER_DATATABLE')")
    void registerDatatable(JsonCommand command);

    @PreAuthorize(value = "hasAnyRole('ALL_FUNCTIONS', 'REGISTER_DATATABLE')")
    void registerDatatable(String dataTableName, String applicationTableName,Long categoryId,final String displayName);

    @PreAuthorize(value = "hasAnyRole('ALL_FUNCTIONS', 'REGISTER_DATATABLE')")
    void registerDatatable(JsonCommand command, String permissionTable);

    @PreAuthorize(value = "hasAnyRole('ALL_FUNCTIONS', 'DEREGISTER_DATATABLE')")
    void deregisterDatatable(String datatable);

    GenericResultsetData retrieveDataTableGenericResultSet(String datatable, Long appTableId, String order, Long id,boolean dropdownValues);

    CommandProcessingResult createDatatable(JsonCommand command);

    void updateDatatable(String datatableName, JsonCommand command);

    void deleteDatatable(String datatableName);

    CommandProcessingResult createNewDatatableEntry(String datatable, Long appTableId, JsonCommand command);

    CommandProcessingResult createPPIEntry(String datatable, Long appTableId, JsonCommand command);

    CommandProcessingResult updateDatatableEntryOneToOne(String datatable, Long appTableId, JsonCommand command);

    CommandProcessingResult updateDatatableEntryOneToMany(String datatable, Long appTableId, Long datatableId, JsonCommand command);

    CommandProcessingResult  updateDatatableEntryOneAndMany(String datatable, Long appTableId, Long datatableId, JsonCommand command);

    CommandProcessingResult deleteDatatableEntries(String datatable, Long appTableId);

    CommandProcessingResult deleteDatatableEntry(String datatable, Long appTableId, Long datatableId);

    String getTableName(String Url);

    String getDataTableName(String Url);

    Long countDatatableEntries(String datatableName,Long appTableId,String foreignKeyColumn);

    List<DatatableCategoryData> retreiveCategories();

    public boolean isRegisteredDataTable(final String name);

}