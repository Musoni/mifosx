/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.ArrayUtils;
import org.joda.time.LocalDateTime;
import org.mifosplatform.infrastructure.core.api.JsonCommand;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResult;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResultBuilder;
import org.mifosplatform.infrastructure.core.exception.PlatformDataIntegrityException;
import org.mifosplatform.infrastructure.core.serialization.FromJsonHelper;
import org.mifosplatform.infrastructure.core.service.RoutingDataSource;
import org.mifosplatform.infrastructure.dataexport.api.DataExportApiConstants;
import org.mifosplatform.infrastructure.dataexport.data.DataExportBaseEntity;
import org.mifosplatform.infrastructure.dataexport.data.DataExportCoreColumn;
import org.mifosplatform.infrastructure.dataexport.data.DataExportCoreDatatable;
import org.mifosplatform.infrastructure.dataexport.data.DataExportCreateRequestData;
import org.mifosplatform.infrastructure.dataexport.data.DataExportEntityData;
import org.mifosplatform.infrastructure.dataexport.data.EntityColumnMetaData;
import org.mifosplatform.infrastructure.dataexport.data.ExportDataValidator;
import org.mifosplatform.infrastructure.dataexport.domain.DataExport;
import org.mifosplatform.infrastructure.dataexport.domain.DataExportRepository;
import org.mifosplatform.infrastructure.dataexport.exception.DataExportNotFoundException;
import org.mifosplatform.infrastructure.dataexport.helper.DataExportUtils;
import org.mifosplatform.infrastructure.dataexport.jdbc.SQL;
import org.mifosplatform.infrastructure.dataqueries.data.DatatableData;
import org.mifosplatform.infrastructure.dataqueries.domain.RegisteredTableMetaData;
import org.mifosplatform.infrastructure.dataqueries.domain.RegisteredTableMetaDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DataExportWritePlatformServiceImpl implements DataExportWritePlatformService {
    private final ExportDataValidator exportDataValidator;
    private final FromJsonHelper fromJsonHelper;
    private final DataExportRepository dataExportRepository;
    private final DataExportReadPlatformService dataExportReadPlatformService;
    private final static Logger logger = LoggerFactory.getLogger(DataExportWritePlatformServiceImpl.class);
    private final RegisteredTableMetaDataRepository registeredTableMetaDataRepository;
    private final JdbcTemplate jdbcTemplate;

    /**
     * @param platformSecurityContext
     * @param exportDataValidator
     * @param fromJsonHelper
     * @param dataExportRepository
     * @param dataExportReadPlatformService
     */
    @Autowired
    public DataExportWritePlatformServiceImpl(final ExportDataValidator exportDataValidator, 
            final FromJsonHelper fromJsonHelper,
            final DataExportRepository dataExportRepository, 
            final DataExportReadPlatformService dataExportReadPlatformService, 
            final RegisteredTableMetaDataRepository registeredTableMetaDataRepository, 
            final RoutingDataSource dataSource) {
        this.exportDataValidator = exportDataValidator;
        this.fromJsonHelper = fromJsonHelper;
        this.dataExportRepository = dataExportRepository;
        this.dataExportReadPlatformService = dataExportReadPlatformService;
        this.registeredTableMetaDataRepository = registeredTableMetaDataRepository;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public CommandProcessingResult createDataExport(final JsonCommand jsonCommand) {
        try {
            // validate the request to create a new data export entity
            this.exportDataValidator.validateCreateDataExportRequest(jsonCommand);
            
            final DataExportCreateRequestData dataExportCreateRequestData = this.fromJsonHelper.fromJson(
                    jsonCommand.json(), DataExportCreateRequestData.class);
            final String name = dataExportCreateRequestData.getName();
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
            final DataExport dataExport = DataExport.newInstance(name, baseEntityName, jsonCommand.json(), dataSql);
            final LocalDateTime currentDataTime = new LocalDateTime();
            final String dateTimeString = currentDataTime.toString(
            		DataExportApiConstants.DATA_EXPORT_FILENAME_DATETIME_FORMAT_PATTERN);
            final String filename = name + "_" + dateTimeString;
            
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
        	DataExportCoreDatatable coreDatatable = DataExportCoreDatatable.newInstance(datatableName);
        	
        	if (coreDatatable != null) {
        		DataExportBaseEntity baseEntity = coreDatatable.getBaseEntity();
        		
        		datatableData = DatatableData.create(baseEntity.getTableName(), coreDatatable.getTableName(), 
        				null, 0L, null, false, coreDatatable.getDisplayName());
        		
        		break;
        	} else if (datatable.getRegisteredTableName().equals(datatableName)) {
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
        int referencedTableIndex = 0;
        
        // initialize the SQl statement builder class
        SQL sqlBuilder = new SQL();
        
        for (EntityColumnMetaData column : selectedColumns) {
            String columnName = column.getName();
            DataExportCoreColumn coreColumn = DataExportCoreColumn.newInstanceFromName(columnName);
            
            if (coreColumn != null) {
            	String tableAlias = coreColumn.getReferencedTableName() + referencedTableIndex++;
            	
            	sqlBuilder.SELECT("`" + tableAlias + "`.`" + coreColumn.getReferencedColumnName() + "` as `"
            			+ coreColumn.getLabel() + "`");
            	sqlBuilder.LEFT_OUTER_JOIN("`" + coreColumn.getReferencedTableName() + "` `" + tableAlias 
            			+ "` on `" + tableAlias + "`.`id` = `" + baseEntityName + "`.`"
            					+ coreColumn.getForeignKeyIndexColumnName() + "`");
            } else if (columnName.contains("userid") || columnName.contains("_by")) {
                String tableAlias = "user" + referencedTableIndex++;
                
                sqlBuilder.SELECT("`" + tableAlias + "`.`username` as `" + column.getLabel() + "`");
                sqlBuilder.LEFT_OUTER_JOIN("`m_appuser` `" + tableAlias + "` on `" + tableAlias 
                		+ "`.`id` = `" + baseEntityName + "`.`" + column.getName() + "`");
            } else if (columnName.equals("loan_status_id")) { 
                String tableAlias = "rev" + referencedTableIndex++;
                
                sqlBuilder.SELECT("`" + tableAlias + "`.`enum_value` as `loan status`");
                sqlBuilder.LEFT_OUTER_JOIN("`r_enum_value` `" + tableAlias + "` on `"
                        + tableAlias + "`.`enum_id` = `" + baseEntityName + "`.`" + column.getName()
                        		+ "` and `" + tableAlias + "`.`enum_name` = 'loan_status_id'");
            } else if (columnName.equals("status_enum")) { 
                String tableAlias = "rev" + referencedTableIndex++;
                
                sqlBuilder.SELECT("`" + tableAlias + "`.`enum_value` as `" + column.getLabel() + "`");
                sqlBuilder.LEFT_OUTER_JOIN("`r_enum_value` `" + tableAlias + "` on `"
                        + tableAlias + "`.`enum_id` = `" + baseEntityName + "`.`" + column.getName()
                        		+ "` and `" + tableAlias + "`.`enum_name` = 'status_enum'");
            } else if (columnName.equals("closure_reason_cv_id") || columnName.equals("gender_cv_id") || 
            		columnName.equals("loanpurpose_cv_id")) { 
                String tableAlias = "mcv" + referencedTableIndex++;
                
                sqlBuilder.SELECT("`" + tableAlias + "`.`code_value` as `" + column.getLabel() + "`");
                sqlBuilder.LEFT_OUTER_JOIN("`m_code_value` `" + tableAlias + "` on `"
                        + tableAlias + "`.`id` = `" + baseEntityName + "`.`" + column.getName() + "`");
            } else {
            	sqlBuilder.SELECT("`" + baseEntityName + "`.`" + columnName + "` as `" + column.getLabel() + "`");
            } 
        }
        
        if (selectedDatatables.size() > 0) {
        	
            for (DatatableData datatableData : selectedDatatables) {
            	String datatableName = datatableData.getRegisteredTableName();
                DataExportCoreDatatable coreDatatable = DataExportCoreDatatable.newInstance(datatableName);
                String datatableDisplayName = datatableData.getDisplayName();
                String baseEntityReferenceColumn = baseEntityName.concat("_id");
                List<RegisteredTableMetaData> registeredTablesMetaData = this.registeredTableMetaDataRepository.
                		findAllByTableName(datatableName);
                
                if ((coreDatatable != null) && DataExportCoreDatatable.SAVINGS_ACCOUNT_CHARGES.equals(coreDatatable)) {
                	baseEntityReferenceColumn = "savings_account_id";
                }
                
                sqlBuilder.LEFT_OUTER_JOIN("`" + datatableName + "` `" + datatableName + "` on `"
                		+ datatableName + "`.`" + baseEntityReferenceColumn + "` = `" + baseEntityName + "`.`id`");
                
                for (RegisteredTableMetaData metaData : registeredTablesMetaData) {
                    String fieldName = metaData.getFieldName();

                    if (fieldName.contains("_cd_")) {
                        String tableAlias = "mcv" + referencedTableIndex++;
                        
                        sqlBuilder.SELECT("`" + tableAlias + "`.`code_value` as `" + metaData.getLabelName() + "`");
                        sqlBuilder.LEFT_OUTER_JOIN("`m_code_value` `" + tableAlias + "` on `"
                                + tableAlias + "`.`code_value` = `" + metaData.getTableName() + "`.`" + metaData.getFieldName() + "`");
                        
                    } else if (fieldName.contains("userid") || fieldName.endsWith("_by")) {
                        String tableAlias = "user" + referencedTableIndex++;
                        
                        sqlBuilder.SELECT("`" + tableAlias + "`.`username` as `" + metaData.getLabelName() + "`");
                        sqlBuilder.LEFT_OUTER_JOIN("`m_appuser` `" + tableAlias + "` on `"
                                + tableAlias + "`.`id` = `" + baseEntityName + "`.`" + metaData.getFieldName() + "`");
                    } else if (fieldName.equalsIgnoreCase("id")) {
                    	sqlBuilder.SELECT("`" + datatableName + "`.`" + metaData.getFieldName() + "` as `"
                    			+ datatableDisplayName + " id`");
                    } else if (fieldName.equalsIgnoreCase(baseEntityReferenceColumn)) { 
                    	// skip
                    } else {
                    	sqlBuilder.SELECT("`" + datatableName + "`.`" + metaData.getFieldName() + "` as `"
                    			+ metaData.getLabelName() + "`");
                    }
                }
                
                if (coreDatatable != null) {
                	List<EntityColumnMetaData> columnsMetaData = DataExportUtils.getTableColumnsMetaData(
                			coreDatatable.getTableName(), this.jdbcTemplate);
                	
                	String guarantorClientTableAlias = "guarantorClient";
                	String guarantorStaffTableAlias = "guarantorStaff";
                	
                	for (EntityColumnMetaData metaData : columnsMetaData) {
                		String fieldName = metaData.getName();
                		String fieldLabel = metaData.getLabel();
                		
                		if (coreDatatable.equals(DataExportCoreDatatable.GUARANTORS)) {
                			String[] excludeFields = {
                				"loan_id", "type_enum", "entity_id", "address_line_2", "state", "zip", "mobile_number", "comment", "is_active"
                			};
                			
                			if (ArrayUtils.contains(excludeFields, fieldName)) {
                				// skip
                			} else if (fieldName.equalsIgnoreCase("firstname")) {
                				sqlBuilder.SELECT("case when `" + datatableName + "`.`type_enum`=1 then `"
                    					+ guarantorClientTableAlias + "`.`firstname` when `" + datatableName + "`.`type_enum`=2 "
                    							+ "then `" + guarantorStaffTableAlias + "`.`firstname` else `"
                    									+ datatableName + "`.`firstname` end as firstname");
                    		} else if (fieldName.equalsIgnoreCase("lastname")) {
                    			sqlBuilder.SELECT("case when `" + datatableName + "`.`type_enum`=1 then `"
                    					+ guarantorClientTableAlias + "`.`lastname` when `" + datatableName + "`.`type_enum`=2 "
                    							+ "then `" + guarantorStaffTableAlias + "`.`lastname` else `"
                    									+ datatableName + "`.`lastname` end as lastname");
                    		} else if (fieldName.equalsIgnoreCase("client_reln_cv_id")) {
                    			String tableAlias = "mcv" + referencedTableIndex++;
                    			
                    			sqlBuilder.SELECT("`" + tableAlias + "`.`code_value` as `client relationship`");
                    			sqlBuilder.LEFT_OUTER_JOIN("`m_code_value` `" + tableAlias + "` on `"
                                        + tableAlias + "`.`id` = `" + datatableName + "`.`" + fieldName + "`");
                    		} else if (fieldName.equalsIgnoreCase("id")) {
                    			sqlBuilder.SELECT("`" + datatableName + "`.`" + fieldName + "` as `" + datatableDisplayName + " id`");
                    		} else {
                            	sqlBuilder.SELECT("`" + datatableName + "`.`" + fieldName + "` as `" + fieldLabel + "`");
                    		}
                		} else if (coreDatatable.equals(DataExportCoreDatatable.LOAN_COLLATERALS)) {
                			if (fieldName.equalsIgnoreCase("type_cv_id")) {
                    			String tableAlias = "mcv" + referencedTableIndex++;
                    			
                    			sqlBuilder.SELECT("`" + tableAlias + "`.`code_value` as `collateral type`");
                    			sqlBuilder.LEFT_OUTER_JOIN("`m_code_value` `" + tableAlias + "` on `"
                                        + tableAlias + "`.`id` = `" + datatableName + "`.`" + fieldName + "`");
                			} else if (fieldName.equalsIgnoreCase("id")) {
                				sqlBuilder.SELECT("`" + datatableName + "`.`" + fieldName + "` as `" + datatableDisplayName + " id`");
                			} else if (fieldName.equalsIgnoreCase(baseEntityReferenceColumn)) { 
                            	// skip
                            } else {
                            	sqlBuilder.SELECT("`" + datatableName + "`.`" + fieldName + "` as `" + fieldLabel + "`");
                    		}
                		} else if (fieldName.equalsIgnoreCase("charge_id")) {
                			String tableAlias = "charge" + referencedTableIndex++;
                			
                			sqlBuilder.SELECT("`" + tableAlias + "`.`id` as `charge id`, `" + tableAlias
                            		+ "`.`name` as `charge name`");
                			sqlBuilder.LEFT_OUTER_JOIN("`m_charge` `" + tableAlias + "` on `"
                                    + tableAlias + "`.`id` = `" + datatableName + "`.`" + fieldName + "`");
                		} else if (fieldName.equalsIgnoreCase("id")) {
                			sqlBuilder.SELECT("`" + datatableName + "`.`" + fieldName + "` as `" + datatableDisplayName + " id`");
                		} else if (fieldName.equalsIgnoreCase(baseEntityReferenceColumn)) { 
                        	// skip
                        } else {
                        	sqlBuilder.SELECT("`" + datatableName + "`.`" + fieldName + "` as `" + fieldLabel + "`");
                		}
                	}
                	
                	if (coreDatatable.equals(DataExportCoreDatatable.GUARANTORS)) {
                		sqlBuilder.LEFT_OUTER_JOIN("`m_client` `" + guarantorClientTableAlias + "` on `"
                                + guarantorClientTableAlias + "`.`id` = `" + datatableName + "`.`entity_id`");
                		sqlBuilder.LEFT_OUTER_JOIN("`m_staff` `" + guarantorStaffTableAlias + "` on `"
                                + guarantorStaffTableAlias + "`.`id` = `" + datatableName + "`.`entity_id`");
                	}
                }
            }
        }
        
        sqlBuilder.FROM("`" + baseEntityTableName + "` as `" + baseEntityName + "`");
        
        final Iterator<Entry<EntityColumnMetaData, String>> filterEntries = selectedFilters.entrySet().iterator();
        
        while (filterEntries.hasNext()) {
            Entry<EntityColumnMetaData, String> filterEntry = filterEntries.next();
            EntityColumnMetaData columnMetaData = filterEntry.getKey();
            DataExportCoreColumn coreColumn = DataExportCoreColumn.newInstanceFromName(columnMetaData.getName());
            
            if (coreColumn != null) {
            	String tableAlias = coreColumn.getReferencedTableName() + referencedTableIndex++;
            	String foreignKeyIndexColumnName = coreColumn.getForeignKeyIndexColumnName();
            	
            	sqlBuilder.WHERE("`" + tableAlias + "`.`" + coreColumn.getReferencedColumnName() + "` " + filterEntry.getValue());
            	sqlBuilder.LEFT_OUTER_JOIN("`" + coreColumn.getReferencedTableName() + "` `" + tableAlias
            			+ "` on `" + tableAlias + "`.`id` = `" + baseEntityName + "`.`"
            					+ foreignKeyIndexColumnName + "`");
            } else {
            	sqlBuilder.WHERE("`" + baseEntityName + "`.`" + columnMetaData.getName() + "` " + filterEntry.getValue());
            }
        }
        
        return sqlBuilder.toString();
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

	@Override
	public CommandProcessingResult updateDataExport(final Long id, final JsonCommand command) {
		// 26.10.2016 - put on hold for now
		return null;
	}

	@Override
	public CommandProcessingResult deleteDataExport(final Long id) {
		// retrieve entity from database
		final DataExport dataExport = this.dataExportRepository.findOne(id);
		
		// throw exception if entity not found
		if (dataExport == null) {
			throw new DataExportNotFoundException(id);
		}
		
		// delete entity
		dataExport.delete();
		
		// save entity
		this.dataExportRepository.save(dataExport);
		
		return new CommandProcessingResultBuilder().withEntityId(id).build();
	}
}
