/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.service;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.mifosplatform.infrastructure.core.api.JsonCommand;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResult;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResultBuilder;
import org.mifosplatform.infrastructure.core.exception.PlatformDataIntegrityException;
import org.mifosplatform.infrastructure.core.serialization.FromJsonHelper;
import org.mifosplatform.infrastructure.dataexport.data.DataExportCreateRequestData;
import org.mifosplatform.infrastructure.dataexport.data.DataExportEntityData;
import org.mifosplatform.infrastructure.dataexport.data.EntityColumnMetaData;
import org.mifosplatform.infrastructure.dataexport.data.ExportDataValidator;
import org.mifosplatform.infrastructure.dataexport.domain.DataExport;
import org.mifosplatform.infrastructure.dataexport.domain.DataExportRepository;
import org.mifosplatform.infrastructure.dataqueries.data.DatatableData;
import org.mifosplatform.infrastructure.dataqueries.domain.RegisteredTableMetaData;
import org.mifosplatform.infrastructure.dataqueries.domain.RegisteredTableMetaDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class DataExportWritePlatformServiceImpl implements DataExportWritePlatformService {
    private final ExportDataValidator exportDataValidator;
    private final FromJsonHelper fromJsonHelper;
    private final DataExportRepository dataExportRepository;
    private final DataExportReadPlatformService dataExportReadPlatformService;
    private final static Logger logger = LoggerFactory.getLogger(DataExportWritePlatformServiceImpl.class);
    private final RegisteredTableMetaDataRepository registeredTableMetaDataRepository;

    /**
     * @param platformSecurityContext
     * @param exportDataValidator
     * @param fromJsonHelper
     * @param dataExportRepository
     * @param dataExportReadPlatformService
     */
    @Autowired
    public DataExportWritePlatformServiceImpl(ExportDataValidator exportDataValidator, 
            FromJsonHelper fromJsonHelper,
            DataExportRepository dataExportRepository, 
            DataExportReadPlatformService dataExportReadPlatformService, 
            final RegisteredTableMetaDataRepository registeredTableMetaDataRepository) {
        this.exportDataValidator = exportDataValidator;
        this.fromJsonHelper = fromJsonHelper;
        this.dataExportRepository = dataExportRepository;
        this.dataExportReadPlatformService = dataExportReadPlatformService;
        this.registeredTableMetaDataRepository = registeredTableMetaDataRepository;
    }

    @Override
    public CommandProcessingResult createDataExport(final JsonCommand jsonCommand) {
        try {
            // validate the request to create a new data export entity
            this.exportDataValidator.validateCreateDataExportRequest(jsonCommand);
            
            final DataExportCreateRequestData dataExportCreateRequestData = this.fromJsonHelper.fromJson(
                    jsonCommand.json(), DataExportCreateRequestData.class);
            final String baseEntityName = dataExportCreateRequestData.getBaseEntityName();
            final String[] datatableNames = dataExportCreateRequestData.getDatatables();
            final String[] columnNames = dataExportCreateRequestData.getColumns();
            final Map<String, String> filters = dataExportCreateRequestData.getFilters();
            final DataExportEntityData dataExportEntityData = this.dataExportReadPlatformService.
                    retrieveTemplate(baseEntityName);
            final Collection<DatatableData> baseEntityDatatables = dataExportEntityData.getDatatables();
            final Collection<EntityColumnMetaData> baseEntityColumns = dataExportEntityData.getColumns();
            final Collection<DatatableData> selectedDatatables = new ArrayList<>();
            final Collection<EntityColumnMetaData> selectedColumns = new ArrayList<>();
            final Map<EntityColumnMetaData, String> selectedFilters = new HashMap<>();
            final Iterator<Map.Entry<String, String>> filterEntries = filters.entrySet().iterator();
            
            while (filterEntries.hasNext()) {
                Map.Entry<String, String> filterEntry = filterEntries.next();
                EntityColumnMetaData entityColumnMetaData = this.getEntityColumnMetaData(filterEntry.getKey(), 
                        baseEntityColumns);
                
                if (entityColumnMetaData != null) {
                    selectedFilters.put(entityColumnMetaData, filterEntry.getValue());
                }
            }
            
            for (String datatableName : datatableNames) {
                DatatableData datatableData = this.getDatatableData(datatableName, baseEntityDatatables);
                
                if (datatableData != null) {
                    selectedDatatables.add(datatableData);
                }
            }
            
            for (String columnName : columnNames) {
                EntityColumnMetaData entityColumnMetaData = this.getEntityColumnMetaData(columnName, baseEntityColumns);
                
                if (entityColumnMetaData != null) {
                    selectedColumns.add(entityColumnMetaData);
                }
            }
            
            final String dataSql = this.generateDataSql(dataExportEntityData, selectedDatatables, 
                    selectedColumns, selectedFilters);
            final DataExport dataExport = DataExport.newInstance(baseEntityName, jsonCommand.json(), dataSql);
            final String filename = Long.toString(System.currentTimeMillis());
            
            dataExport.updateFilename(filename);
            
            // save the new data export entity
            this.dataExportRepository.save(dataExport);
            
            return new CommandProcessingResultBuilder()
                    .withCommandId(jsonCommand.commandId())
                    .withEntityId(dataExport.getId())
                    .build();
        } catch (final DataIntegrityViolationException dve) {
            handleDataIntegrityIssues(jsonCommand, dve);
            
            return CommandProcessingResult.empty();
        }
    }
    
    private DatatableData getDatatableData(final String datatableName, final Collection<DatatableData> datatables) {
        DatatableData datatableData = null;
        
        for (DatatableData datatable : datatables) {
            if (datatable.getRegisteredTableName().equals(datatableName)) {
                datatableData = datatable;
                
                break;
            }
        }
        
        return datatableData;
    }
    
    private EntityColumnMetaData getEntityColumnMetaData(final String columnName, 
            final Collection<EntityColumnMetaData> columns) {
        EntityColumnMetaData entityColumnMetaData = null;
        
        for (EntityColumnMetaData column : columns) {
            if (column.getName().equals(columnName)) {
                entityColumnMetaData = column;
                
                break;
            }
        }
        
        return entityColumnMetaData;
    }
    
    private String generateDataSql(final DataExportEntityData dataExportEntityData, 
            final Collection<DatatableData> selectedDatatables, 
            final Collection<EntityColumnMetaData> selectedColumns, 
            final Map<EntityColumnMetaData, String> selectedFilters) {
        final String baseEntityTableName = dataExportEntityData.getTableName();
        final String baseEntityName = dataExportEntityData.getEntityName();
        final StringBuilder sqlStringBuilder = new StringBuilder(100);
        final StringBuilder joinSqlStringBuilder = new StringBuilder(100);
        int referencedTableIndex = 0;
        
        sqlStringBuilder.append("select");
        
        for (EntityColumnMetaData column : selectedColumns) {
            String columnName = column.getName();
            
            if (columnName.contains("userid") || columnName.contains("_by")) {
                String tableAlias = "user" + referencedTableIndex++;
                
                sqlStringBuilder.append(" " + tableAlias + ".`username` as `" + column.getLabel() + "`,");
                joinSqlStringBuilder.append(" left join m_appuser `" + tableAlias + "` on `"
                        + tableAlias + "`.`id` = `" + baseEntityName + "`.`" + column.getName() + "`");
            } else if (columnName.contains("office_id")) { 
                String tableAlias = "office" + referencedTableIndex++;
                
                sqlStringBuilder.append(" " + tableAlias + ".`name` as `" + column.getLabel() + "`,");
                joinSqlStringBuilder.append(" left join m_office `" + tableAlias + "` on `"
                        + tableAlias + "`.`id` = `" + baseEntityName + "`.`" + column.getName() + "`");
            } else if (columnName.contains("staff_id")) { 
                String tableAlias = "staff" + referencedTableIndex++;
                
                sqlStringBuilder.append(" " + tableAlias + ".`display_name` as `" + column.getLabel() + "`,");
                joinSqlStringBuilder.append(" left join m_staff `" + tableAlias + "` on `"
                        + tableAlias + "`.`id` = `" + baseEntityName + "`.`" + column.getName() + "`");
            } else {
                sqlStringBuilder.append(" `" + baseEntityName + "`.`" + column.getName() + "` as `" + column.getLabel() + "`,");
            } 
        }
        
        if (selectedDatatables.size() > 0) {
            for (DatatableData datatableData : selectedDatatables) {
                String datatableName = datatableData.getRegisteredTableName();
                String baseEntityReferenceColumn = baseEntityName.concat("_id");
                List<RegisteredTableMetaData> registeredTablesMetaData = this.registeredTableMetaDataRepository.findAllByTableName(datatableName);
                String sqlJoinStatement = null;
                
                for (RegisteredTableMetaData metaData : registeredTablesMetaData) {
                    String fieldName = metaData.getFieldName();

                    if (fieldName.contains("_cd_")) {
                        String tableAlias = "mcv" + referencedTableIndex++;
                        
                        sqlStringBuilder.append(" `" + tableAlias + "`.`code_value` as `" + metaData.getLabelName() + "`,");
                        joinSqlStringBuilder.append(" left join m_code_value `" + tableAlias + "` on `"
                                + tableAlias + "`.`code_value` = `" + metaData.getTableName() + "`.`" + metaData.getFieldName() + "`");
                        
                    } else if (fieldName.contains("userid") || fieldName.endsWith("_by")) {
                        String tableAlias = "user" + referencedTableIndex++;
                        
                        sqlStringBuilder.append(" `" + tableAlias + "`.`username` as `" + metaData.getLabelName() + "`,");
                        joinSqlStringBuilder.append(" left join m_appuser `" + tableAlias + "` on `"
                                + tableAlias + "`.`id` = `" + baseEntityName + "`.`" + metaData.getFieldName() + "`");
                    } else {
                        sqlStringBuilder.append(" `" + datatableName + "`.`" + metaData.getFieldName() + "` as `" + metaData.getLabelName() + "`,");
                        
                        sqlJoinStatement = " left join `" + datatableName + "` `" + datatableName + "` on `"
                                + datatableName + "`.`" + baseEntityReferenceColumn + "` = `" + baseEntityName + "`.`id`";
                    }
                }
                
                joinSqlStringBuilder.append(sqlJoinStatement);
            }
        }
        
        int indexOfLastCharOfStringBuilder = sqlStringBuilder.lastIndexOf(",");
        
        // remove the last comma in the string before appending the next line
        sqlStringBuilder.replace(indexOfLastCharOfStringBuilder, indexOfLastCharOfStringBuilder+1, "");
        sqlStringBuilder.append(" from `" + baseEntityTableName + "` as `" + baseEntityName + "`");
        
        // add the sql join string
        sqlStringBuilder.append(joinSqlStringBuilder);
        
        if (!selectedFilters.isEmpty()) {
            sqlStringBuilder.append(" where");
        }
        
        final Iterator<Entry<EntityColumnMetaData, String>> filterEntries = selectedFilters.entrySet().iterator();
        
        int loopIndex = 0;
        
        while (filterEntries.hasNext()) {
            Entry<EntityColumnMetaData, String> filterEntry = filterEntries.next();
            EntityColumnMetaData columnMetaData = filterEntry.getKey();
            
            if (loopIndex > 0) {
                sqlStringBuilder.append(" and");
            }
            
            sqlStringBuilder.append(" `" + baseEntityName + "`.`" + columnMetaData.getName() + "` " + filterEntry.getValue());
            
            loopIndex++;
        }
        
        return sqlStringBuilder.toString();
    }
    
    /** 
     * Handle any SQL data integrity issue 
     *
     * @param jsonCommand -- JsonCommand object
     * @param dve -- data integrity exception object
     * @return None
     **/
    private void handleDataIntegrityIssues(final JsonCommand jsonCommand, 
            final DataIntegrityViolationException dve) {
        final Throwable realCause = dve.getMostSpecificCause();
        
        logger.error(dve.getMessage(), dve);
        
        throw new PlatformDataIntegrityException("error.msg.charge.unknown.data.integrity.issue",
                "Unknown data integrity issue with resource: " + realCause.getMessage());
    }
}