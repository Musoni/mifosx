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
import org.mifosplatform.infrastructure.dataexport.data.DataExportCoreTable;
import org.mifosplatform.infrastructure.dataexport.data.DataExportCreateRequestData;
import org.mifosplatform.infrastructure.dataexport.data.DataExportEntityData;
import org.mifosplatform.infrastructure.dataexport.data.DataExportSqlJoin;
import org.mifosplatform.infrastructure.dataexport.data.EntityColumnMetaData;
import org.mifosplatform.infrastructure.dataexport.data.ExportDataValidator;
import org.mifosplatform.infrastructure.dataexport.domain.DataExport;
import org.mifosplatform.infrastructure.dataexport.domain.DataExportRepository;
import org.mifosplatform.infrastructure.dataexport.exception.DataExportNotFoundException;
import org.mifosplatform.infrastructure.dataexport.helper.DataExportUtils;
import org.mifosplatform.infrastructure.dataexport.helper.FileHelper;
import org.mifosplatform.infrastructure.dataexport.jdbc.SQL;
import org.mifosplatform.infrastructure.dataqueries.data.DatatableData;
import org.mifosplatform.infrastructure.dataqueries.domain.RegisteredTableMetaData;
import org.mifosplatform.infrastructure.dataqueries.domain.RegisteredTableMetaDataRepository;
import org.mifosplatform.portfolio.loanaccount.domain.LoanTransactionType;
import org.mifosplatform.portfolio.savings.SavingsAccountTransactionType;
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
            final String filename = FileHelper.sanitizeFilename(name) + "_" + dateTimeString;
            
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
        final HashMap<String, DataExportSqlJoin> sqlJoinMap = new HashMap<>();
        
        // initialize the SQl statement builder class
        SQL sqlBuilder = new SQL();
        
        for (EntityColumnMetaData column : selectedColumns) {
            String columnName = column.getName();
            DataExportCoreColumn coreColumn = DataExportCoreColumn.newInstanceFromName(columnName);
            
            if (coreColumn != null) {
            	this.addCoreColumnSqlToSqlBuilder(dataExportEntityData, coreColumn, sqlBuilder, sqlJoinMap, 
            			true, null);
            	
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
                		findByTableNameOrderByOrderAsc(datatableName);
                
                if ((coreDatatable != null) && DataExportCoreDatatable.SAVINGS_ACCOUNT_CHARGES.equals(coreDatatable)) {
                	baseEntityReferenceColumn = "savings_account_id";
                }
                
                sqlBuilder.LEFT_OUTER_JOIN("`" + datatableName + "` `" + datatableName + "` on `"
                		+ datatableName + "`.`" + baseEntityReferenceColumn + "` = `" + baseEntityName + "`.`id`");
                
                for (RegisteredTableMetaData metaData : registeredTablesMetaData) {
                    String fieldName = metaData.getFieldName();

                    if (fieldName.contains("_cv_")) {
                        String tableAlias = "mcv" + referencedTableIndex++;
                        String columnLabel = datatableDisplayName + " - " + metaData.getLabelName();
                        
                        sqlBuilder.SELECT("`" + tableAlias + "`.`code_value` as `" + columnLabel + "`");
                        sqlBuilder.LEFT_OUTER_JOIN("`m_code_value` `" + tableAlias + "` on `"
                                + tableAlias + "`.`code_value` = `" + metaData.getTableName() + "`.`" + metaData.getFieldName() + "`");
                        
                    } else if (fieldName.contains("_cd_")) {
                        String tableAlias = "mcv" + referencedTableIndex++;
                        String columnLabel = datatableDisplayName + " - " + metaData.getLabelName();
                        
                        sqlBuilder.SELECT("`" + tableAlias + "`.`code_value` as `" + columnLabel + "`");
                        sqlBuilder.LEFT_OUTER_JOIN("`m_code_value` `" + tableAlias + "` on `"
                                + tableAlias + "`.`id` = `" + metaData.getTableName() + "`.`" + metaData.getFieldName() + "`");
                        
                    } else if (fieldName.equalsIgnoreCase("submittedon_date") || fieldName.equalsIgnoreCase("submittedon_userid")) {
                    	// skip
                    } else if (fieldName.contains("userid") || fieldName.endsWith("_by")) {
                    	// the commented lines below were left in the code so if Cameron wants the 
                    	// "submittedon_date" and "submittedon_userid" fields back in
                        /*String tableAlias = "user" + referencedTableIndex++;
                        String columnLabel = datatableDisplayName + " - " + metaData.getLabelName();
                        
                        sqlBuilder.SELECT("`" + tableAlias + "`.`username` as `" + columnLabel + "`");
                        sqlBuilder.LEFT_OUTER_JOIN("`m_appuser` `" + tableAlias + "` on `"
                                + tableAlias + "`.`id` = `" + metaData.getTableName() + "`.`" + metaData.getFieldName() + "`");*/
                    } else if (fieldName.equalsIgnoreCase("id")) {
                    	sqlBuilder.SELECT("`" + datatableName + "`.`" + metaData.getFieldName() + "` as `"
                    			+ datatableDisplayName + " id`");
                    } else if (fieldName.equalsIgnoreCase(baseEntityReferenceColumn)) { 
                    	// skip
                    } else {
                    	String columnLabel = datatableDisplayName + " - " + metaData.getLabelName();
                    	
                    	sqlBuilder.SELECT("`" + datatableName + "`.`" + metaData.getFieldName() + "` as `"
                    			+ columnLabel + "`");
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
                				String columnLabel = datatableDisplayName + " - firstname";
                				
                				sqlBuilder.SELECT("case when `" + datatableName + "`.`type_enum`=1 then `"
                    					+ guarantorClientTableAlias + "`.`firstname` when `" + datatableName + "`.`type_enum`=2 "
                    							+ "then `" + guarantorStaffTableAlias + "`.`firstname` else `"
                    									+ datatableName + "`.`firstname` end as `" + columnLabel + "`");
                    		} else if (fieldName.equalsIgnoreCase("lastname")) {
                    			String columnLabel = datatableDisplayName + " - lastname";
                    			
                    			sqlBuilder.SELECT("case when `" + datatableName + "`.`type_enum`=1 then `"
                    					+ guarantorClientTableAlias + "`.`lastname` when `" + datatableName + "`.`type_enum`=2 "
                    							+ "then `" + guarantorStaffTableAlias + "`.`lastname` else `"
                    									+ datatableName + "`.`lastname` end as `" + columnLabel + "`");
                    		} else if (fieldName.equalsIgnoreCase("client_reln_cv_id")) {
                    			String tableAlias = "mcv" + referencedTableIndex++;
                    			String columnLabel = datatableDisplayName + " - client relationship";
                    			
                    			sqlBuilder.SELECT("`" + tableAlias + "`.`code_value` as `" + columnLabel + "`");
                    			sqlBuilder.LEFT_OUTER_JOIN("`m_code_value` `" + tableAlias + "` on `"
                                        + tableAlias + "`.`id` = `" + datatableName + "`.`" + fieldName + "`");
                    		} else if (fieldName.equalsIgnoreCase("id")) {
                    			String columnLabel = datatableDisplayName + " - id";
                    			
                    			sqlBuilder.SELECT("`" + datatableName + "`.`" + fieldName + "` as `" + columnLabel + "`");
                    		} else {
                    			String columnLabel = datatableDisplayName + " - " + fieldLabel;
                    			
                            	sqlBuilder.SELECT("`" + datatableName + "`.`" + fieldName + "` as `" + columnLabel + "`");
                    		}
                		} else if (coreDatatable.equals(DataExportCoreDatatable.LOAN_COLLATERALS)) {
                			if (fieldName.equalsIgnoreCase("type_cv_id")) {
                    			String tableAlias = "mcv" + referencedTableIndex++;
                    			String columnLabel = datatableDisplayName + " - collateral type";
                    			
                    			sqlBuilder.SELECT("`" + tableAlias + "`.`code_value` as `" + columnLabel + "`");
                    			sqlBuilder.LEFT_OUTER_JOIN("`m_code_value` `" + tableAlias + "` on `"
                                        + tableAlias + "`.`id` = `" + datatableName + "`.`" + fieldName + "`");
                			} else if (fieldName.equalsIgnoreCase("id")) {
                				String columnLabel = datatableDisplayName + " - id";
                				
                				sqlBuilder.SELECT("`" + datatableName + "`.`" + fieldName + "` as `" + columnLabel + "`");
                			} else if (fieldName.equalsIgnoreCase(baseEntityReferenceColumn)) { 
                            	// skip
                            } else {
                            	String columnLabel = datatableDisplayName + " - " + fieldLabel;
                            	
                            	sqlBuilder.SELECT("`" + datatableName + "`.`" + fieldName + "` as `" + columnLabel + "`");
                    		}
                		} else if (fieldName.equalsIgnoreCase("charge_id")) {
                			String tableAlias = "charge" + referencedTableIndex++;
                			String columnLabel = datatableDisplayName + " - charge name";
                			
                			sqlBuilder.SELECT("`" + tableAlias + "`.`id` as `charge id`, `" + tableAlias
                            		+ "`.`name` as `" + columnLabel + "`");
                			sqlBuilder.LEFT_OUTER_JOIN("`m_charge` `" + tableAlias + "` on `"
                                    + tableAlias + "`.`id` = `" + datatableName + "`.`" + fieldName + "`");
                		} else if (fieldName.equalsIgnoreCase("id")) {
                			String columnLabel = datatableDisplayName + " - id";
                			
                			sqlBuilder.SELECT("`" + datatableName + "`.`" + fieldName + "` as `" + columnLabel + "`");
                		} else if (fieldName.equalsIgnoreCase(baseEntityReferenceColumn)) { 
                        	// skip
                        } else {
                        	String columnLabel = datatableDisplayName + " - " + fieldLabel;
                        	
                        	sqlBuilder.SELECT("`" + datatableName + "`.`" + fieldName + "` as `" + columnLabel + "`");
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
            String filterValue = filterEntry.getValue();
            
            if (coreColumn != null) {
            	this.addCoreColumnSqlToSqlBuilder(dataExportEntityData, coreColumn, sqlBuilder, sqlJoinMap, 
            			false, filterValue);
                
            } else {
            	sqlBuilder.WHERE("`" + baseEntityName + "`.`" + columnMetaData.getName() + "` " + filterValue);
            }
        }
        
        return sqlBuilder.toString();
    }
    
    /**
     * Adds a join statements, select statements or where clauses
     * This method prevents the addition of duplicate join statements
     * 
     * @param dataExportEntityData
     * @param coreColumn
     * @param sqlBuilder
     * @param sqlJoinMap
     * @param isSelectStatement
     * @param filterValue
     */
    private void addCoreColumnSqlToSqlBuilder(final DataExportEntityData dataExportEntityData, 
    		final DataExportCoreColumn coreColumn, final SQL sqlBuilder, 
    		final HashMap<String, DataExportSqlJoin> sqlJoinMap, final boolean isSelectStatement, 
    		final String filterValue) {
    	if (coreColumn != null) {
            final String baseEntityName = dataExportEntityData.getEntityName();
            final DataExportBaseEntity baseEntity = DataExportBaseEntity.fromEntityName(baseEntityName);
            int referencedTableIndex = 0;
            String referencedTableName = coreColumn.getReferencedTableName();
        	DataExportCoreTable referencedTable = DataExportCoreTable.newInstance(referencedTableName);
        	String referencedColumnName = coreColumn.getReferencedColumnName();
        	String foreignKeyIndexColumnName = coreColumn.getForeignKeyIndexColumnName();
        	
        	// variables initialized with null values
        	String sqlStatement, mClientTableAlias, mGroupTableAlias, mGroupClientTableAlias, 
        	mOfficeTableAlias, mStaffTableAlias, mLoanTableAlias, mProductLoanTableAlias, 
        	mPaymentDetailTableAlias, mSavingsProductTableAlias, sqlJoinKey, parentTableAlias, 
        	mCodeValueTableAlias, mSavingsAccountTableAlias;
        	DataExportSqlJoin dataExportSqlJoin;
        	
        	switch (baseEntity) {
	    		case CLIENT:
	    			switch (coreColumn) {
	    				case GROUP_NAME:
	    				case GROUP_ID:
	    					mGroupTableAlias = DataExportCoreTable.M_GROUP.getAlias(referencedTableIndex++);
	    					mGroupClientTableAlias = DataExportCoreTable.M_GROUP_CLIENT.
	    							getAlias(referencedTableIndex++);
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_CLIENT, 
	    							DataExportCoreTable.M_GROUP_CLIENT);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	        					// m_client and m_group_client table join
	        					sqlStatement = "`" + DataExportCoreTable.M_GROUP_CLIENT.getName()
	        							+ "` `" + mGroupClientTableAlias + "` on `" + mGroupClientTableAlias
	        									+ "`.`client_id` = `" + baseEntityName + "`.`id`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_CLIENT, 
	        							DataExportCoreTable.M_GROUP_CLIENT, sqlStatement, baseEntityName, 
	        							mGroupClientTableAlias);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_GROUP, 
	    							DataExportCoreTable.M_GROUP_CLIENT);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// m_group_client and m_group table join
	        					sqlStatement = "`" + DataExportCoreTable.M_GROUP.getName() + "` `"
	                        			+ mGroupTableAlias + "` on `" + mGroupTableAlias + "`.`id` = `"
	                							+ mGroupClientTableAlias + "`.`group_id`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_GROUP, 
	        							DataExportCoreTable.M_GROUP_CLIENT, sqlStatement, mGroupTableAlias, 
	        							mGroupClientTableAlias);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					if (isSelectStatement) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	        							+  referencedColumnName + "` as `" + coreColumn.getLabel() + "`";
	        					
	    						// add the select statement
	    						sqlBuilder.SELECT(sqlStatement);
	    						
	    					} else if (filterValue != null) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	    								+ referencedColumnName + "` " + filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    				case BRANCH_NAME:
	    				case STAFF_NAME:
	    				case GENDER:
	    					parentTableAlias = referencedTable.getAlias(referencedTableIndex++);
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(referencedTable, 
	    							DataExportCoreTable.M_CLIENT);
	    					
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// m_office/m_staff and m_client table join
	        					sqlStatement = "`" + referencedTable.getName() + "` `" + parentTableAlias 
	                        			+ "` on `" + parentTableAlias + "`.`id` = `" + baseEntityName + "`.`"
	                					+ foreignKeyIndexColumnName + "`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(referencedTable, 
	        							DataExportCoreTable.M_CLIENT, sqlStatement, parentTableAlias, 
	        							baseEntityName);
	    						
	    						sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					if (isSelectStatement) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	        							+ referencedColumnName + "` as `" + coreColumn.getLabel() + "`";
	    						
	    						// add the select statement
	        					sqlBuilder.SELECT(sqlStatement);
	        					
	    					} else if (filterValue != null) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	    								+ referencedColumnName + "` " + filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    					
	    				case CLIENT_ID:
	    				case CLIENT_NAME:
	    					// =============================================================================
	    					if (isSelectStatement) {
	    						sqlStatement = "`" + baseEntityName + "`.`" + referencedColumnName + "` as `"
	                        			+ coreColumn.getLabel() + "`";
	        					
	    						// add the select statement
	        					sqlBuilder.SELECT(sqlStatement);
	        					
	    					} else if (filterValue != null) {
	    						sqlStatement = "`" + baseEntityName + "`.`" + referencedColumnName + "` " 
	    								+ filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    				case PHONE_NUMBER:
	    				case DATE_OF_BIRTH:
	    					// =============================================================================
	    					if (isSelectStatement) {
	    						sqlStatement = "`" + baseEntityName + "`.`" + coreColumn.getName() + "` as `"
	                        			+ coreColumn.getLabel() + "`";
	        					
	        					// add the select statement
	        					sqlBuilder.SELECT(sqlStatement);
	        					
	    					} else if (filterValue != null) {
	    						sqlStatement = "`" + baseEntityName + "`.`" + coreColumn.getName() + "` " 
	    								+ filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    				default:
	    					// =============================================================================
	    					if (isSelectStatement) {
	    						// add the select statement
	        					sqlStatement = "NULL as `" + coreColumn.getLabel() + "`";
	        					
	        					sqlBuilder.SELECT(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    			}
	    			break;
	    			
	    		case GROUP:
	    			switch (coreColumn) {
	    				case GROUP_NAME:
	    				case GROUP_ID:
	    					// =============================================================================
	    					if (isSelectStatement) {
	    						sqlStatement = "`" + baseEntityName + "`.`" + referencedColumnName + "` as `"
	                        			+ coreColumn.getLabel() + "`";
	        					
	    						// add the select statement
	        					sqlBuilder.SELECT(sqlStatement);
	        					
	    					} else if (filterValue != null) {
	    						sqlStatement = "`" + baseEntityName + "`.`" + referencedColumnName + "` " 
	    								+ filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    				case BRANCH_NAME:
	    				case STAFF_NAME:
	    					parentTableAlias = referencedTable.getAlias(referencedTableIndex++);
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(referencedTable, 
	    							DataExportCoreTable.M_GROUP);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// m_office/m_staff and m_group table join
	        					sqlStatement = "`" + referencedTableName + "` `" + parentTableAlias 
	                        			+ "` on `" + parentTableAlias + "`.`id` = `" + baseEntityName + "`.`"
	                					+ foreignKeyIndexColumnName + "`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(referencedTable, 
	        							DataExportCoreTable.M_GROUP, sqlStatement, parentTableAlias, 
	        							baseEntityName);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					if (isSelectStatement) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	        							+ referencedColumnName + "` as `" + coreColumn.getLabel() + "`";
	    						
	    						// add the select statement
	        					sqlBuilder.SELECT(sqlStatement);
	        					
	    					} else if (filterValue != null) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	    								+ referencedColumnName + "` " + filterValue;
	    						
	    						// add the WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    				default:
	    					// =============================================================================
	    					if (isSelectStatement) {
	    						sqlStatement = "NULL as `" + coreColumn.getLabel() + "`";
	        					
	        					// add the select statement
	        					sqlBuilder.SELECT(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    			}
	    			break;
	    			
	    		case LOAN:
	    			switch (coreColumn) {
	    				case LOAN_OFFICER_NAME:
	    				case CLIENT_NAME:
	    				case CLIENT_ID:
	    					parentTableAlias = referencedTable.getAlias(referencedTableIndex++);
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(referencedTable, 
	    							DataExportCoreTable.M_LOAN);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// m_client/m_staff and m_loan
	    						sqlStatement = "`" + referencedTable.getName() + "` `" + parentTableAlias 
	                        			+ "` on `" + parentTableAlias + "`.`id` = `" + baseEntityName + "`.`"
	                					+ foreignKeyIndexColumnName + "`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(referencedTable, 
	        							DataExportCoreTable.M_LOAN, sqlStatement, parentTableAlias, 
	        							baseEntityName);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					if (isSelectStatement) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	        							+ referencedColumnName + "` as `" + coreColumn.getLabel() + "`";
	    						
	    						// add the select statement
	        					sqlBuilder.SELECT(sqlStatement);
	    						
	    					} else if (filterValue != null) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	        							+ referencedColumnName + "` " + filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    				case GROUP_NAME:
	    				case GROUP_ID:
	    					mGroupTableAlias = DataExportCoreTable.M_GROUP.getAlias(referencedTableIndex++);
	    					mGroupClientTableAlias = DataExportCoreTable.M_GROUP_CLIENT.getAlias(
	    							referencedTableIndex++);
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_LOAN, 
	    							DataExportCoreTable.M_GROUP_CLIENT);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// m_loan and m_group_client table join
	        					sqlStatement = "`" + DataExportCoreTable.M_GROUP_CLIENT.getName()
	        							+ "` `" + mGroupClientTableAlias + "` on `" + mGroupClientTableAlias
	        									+ "`.`client_id` = `" + baseEntityName + "`.`client_id`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_LOAN, 
	        							DataExportCoreTable.M_GROUP_CLIENT, sqlStatement, baseEntityName, 
	        							mGroupClientTableAlias);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_GROUP, 
	    							DataExportCoreTable.M_GROUP_CLIENT);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// m_group_client and m_group table join
	        					sqlStatement = "`" + DataExportCoreTable.M_GROUP.getName() + "` `"
	                        			+ mGroupTableAlias + "` on `" + mGroupTableAlias + "`.`id` = case when "
	                							+ "isnull(`" + baseEntityName + "`.`group_id`) then `"
	                									+ mGroupClientTableAlias + "`.`group_id` else `"
	                											+ baseEntityName + "`.`group_id` end";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_GROUP, 
	        							DataExportCoreTable.M_GROUP_CLIENT, sqlStatement, mGroupTableAlias, 
	        							mGroupClientTableAlias);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					if (isSelectStatement) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	        							+  referencedColumnName + "` as `" + coreColumn.getLabel() + "`";
	        					
	    						// add the select statement
	    						sqlBuilder.SELECT(sqlStatement);
	    						
	    					} else if (filterValue != null) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	    								+ referencedColumnName + "` " + filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    				case BRANCH_NAME:
	    					mClientTableAlias = DataExportCoreTable.M_CLIENT.getAlias(referencedTableIndex++);
	    					mGroupTableAlias = DataExportCoreTable.M_GROUP.getAlias(referencedTableIndex++);
	    					mOfficeTableAlias = DataExportCoreTable.M_OFFICE.getAlias(referencedTableIndex++);
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_CLIENT, 
	    							DataExportCoreTable.M_LOAN);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// m_loan and m_client table join
	        					sqlStatement = "`" + DataExportCoreTable.M_CLIENT.getName() + "` `"
	        							+ mClientTableAlias + "` on `" + mClientTableAlias + "`.`id` = `"
	    								+ baseEntityName + "`.`client_id`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_CLIENT, 
	        							DataExportCoreTable.M_LOAN, sqlStatement, mClientTableAlias, 
	        							baseEntityName);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_GROUP, 
	    							DataExportCoreTable.M_LOAN);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// m_loan and m_group table join
	        					sqlStatement = "`" + DataExportCoreTable.M_GROUP.getName() + "` `"
	                        			+ mGroupTableAlias + "` on `" + mGroupTableAlias + "`.`id` = `"
	                					+ baseEntityName + "`.`group_id`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_GROUP, 
	        							DataExportCoreTable.M_LOAN, sqlStatement, mGroupTableAlias, 
	        							baseEntityName);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_OFFICE, 
	    							DataExportCoreTable.M_LOAN);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// m_office and m_client/m_group table join
	        					sqlStatement = "`" + DataExportCoreTable.M_OFFICE.getName() + "` `"
	                        			+ mOfficeTableAlias + "` on `" + mOfficeTableAlias + "`.`id` = case when "
	                					+ "isnull(`" + baseEntityName + "`.`group_id`) then `"
	                							+ mClientTableAlias + "`.`office_id` else `"
	                									+ mGroupTableAlias + "`.`office_id` end";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_OFFICE, 
	        							DataExportCoreTable.M_LOAN, sqlStatement, mOfficeTableAlias, 
	        							baseEntityName);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					if (isSelectStatement) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	        							+  referencedColumnName + "` as `" + coreColumn.getLabel() + "`";
	        					
	    						// add the select statement
	    						sqlBuilder.SELECT(sqlStatement);
	    						
	    					} else if (filterValue != null) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	    								+ referencedColumnName + "` " + filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    				case STAFF_NAME:
	    					mClientTableAlias = DataExportCoreTable.M_CLIENT.getAlias(referencedTableIndex++);
	    					mGroupTableAlias = DataExportCoreTable.M_GROUP.getAlias(referencedTableIndex++);
	    					mStaffTableAlias = DataExportCoreTable.M_STAFF.getAlias(referencedTableIndex++);
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_CLIENT, 
	    							DataExportCoreTable.M_LOAN);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// m_loan and m_client table join
	        					sqlStatement = "`" + DataExportCoreTable.M_CLIENT.getName() + "` `"
	        							+ mClientTableAlias + "` on `" + mClientTableAlias + "`.`id` = `"
	    								+ baseEntityName + "`.`client_id`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_CLIENT, 
	        							DataExportCoreTable.M_LOAN, sqlStatement, mClientTableAlias, 
	        							baseEntityName);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_GROUP, 
	    							DataExportCoreTable.M_LOAN);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// m_loan and m_group table join
	        					sqlStatement = "`" + DataExportCoreTable.M_GROUP.getName() + "` `"
	                        			+ mGroupTableAlias + "` on `" + mGroupTableAlias + "`.`id` = `"
	                					+ baseEntityName + "`.`group_id`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_GROUP, 
	        							DataExportCoreTable.M_LOAN, sqlStatement, mGroupTableAlias, 
	        							baseEntityName);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_STAFF, 
	    							DataExportCoreTable.M_LOAN);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// m_staff and m_client/m_group table join
	        					sqlStatement = "`" + DataExportCoreTable.M_STAFF.getName() + "` `"
	                    			+ mStaffTableAlias + "` on `" + mStaffTableAlias + "`.`id` = case when "
	            					+ "isnull(`" + baseEntityName + "`.`group_id`) then `"
	            							+ mClientTableAlias + "`.`staff_id` else `"
	            									+ mGroupTableAlias + "`.`staff_id` end";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_STAFF, 
	        							DataExportCoreTable.M_LOAN, sqlStatement, mStaffTableAlias, 
	        							baseEntityName);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	                        
	                        // =============================================================================
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					if (isSelectStatement) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	        							+  referencedColumnName + "` as `" + coreColumn.getLabel() + "`";
	        					
	    						// add the select statement
	    						sqlBuilder.SELECT(sqlStatement);
	    						
	    					} else if (filterValue != null) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	    								+ referencedColumnName + "` " + filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    				case DATE_OF_BIRTH:
	    				case PHONE_NUMBER:
	    					mClientTableAlias = DataExportCoreTable.M_CLIENT.getAlias(referencedTableIndex++);
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_CLIENT, 
	    							DataExportCoreTable.M_LOAN);
	    					
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// m_loan and m_client table join
	        					sqlStatement = "`" + DataExportCoreTable.M_CLIENT.getName() + "` `" + mClientTableAlias 
	                        			+ "` on `" + mClientTableAlias + "`.`id` = `" + baseEntityName 
	                                            + "`.`client_id`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_CLIENT, 
	        							DataExportCoreTable.M_LOAN, sqlStatement, mClientTableAlias, 
	        							baseEntityName);
	    						
	    						sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					if (isSelectStatement) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	        							+ coreColumn.getName() + "` as `" + coreColumn.getLabel() + "`";
	    						
	    						// add the select statement
	        					sqlBuilder.SELECT(sqlStatement);
	        					
	    					} else if (filterValue != null) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	    								+ coreColumn.getName() + "` " + filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					break;
	    				case GENDER:
	    					mClientTableAlias = DataExportCoreTable.M_CLIENT.getAlias(referencedTableIndex++);
	    					mCodeValueTableAlias = referencedTable.getAlias(referencedTableIndex++);
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_CLIENT, 
	    							DataExportCoreTable.M_LOAN);
	    					
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						sqlStatement = "`" + DataExportCoreTable.M_CLIENT.getName() + "` `"
	        							+ mClientTableAlias + "` on `" + mClientTableAlias + "`.`id` = `"
										+ baseEntityName + "`.`client_id`";
	    						
	    						dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_CLIENT, 
	        							DataExportCoreTable.M_LOAN, sqlStatement, mClientTableAlias, 
	        							baseEntityName);
	    						
	    						sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(referencedTable, 
	    							DataExportCoreTable.M_CLIENT);
	    					
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						sqlStatement = "`" + referencedTable.getName() + "` `" + mCodeValueTableAlias + "` on `"
	        	                        + mCodeValueTableAlias + "`.`id` = `" + mClientTableAlias + "`.`" 
	    										+ coreColumn.getName() + "`";
	    						
	    						dataExportSqlJoin = DataExportSqlJoin.newInstance(referencedTable, 
	        							DataExportCoreTable.M_CLIENT, sqlStatement, mCodeValueTableAlias, 
	        							mClientTableAlias);
	    						
	    						sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					if (isSelectStatement) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	    								+ referencedColumnName + "` as `" + coreColumn.getLabel() + "`";
	    						
	    						// add the select statement
	        					sqlBuilder.SELECT(sqlStatement);
	    						
	    					} else if (filterValue != null) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	    								+ referencedColumnName + "` " + filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    				default:
	    					// =============================================================================
	    					if (isSelectStatement) {
	    						// add the select statement
	        					sqlStatement = "NULL as `" + coreColumn.getLabel() + "`";
	        					
	        					sqlBuilder.SELECT(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    			}
	    			break;
	    			
	    		case SAVINGS_ACCOUNT:
	    			switch (coreColumn) {
	    				case CLIENT_NAME:
	    				case CLIENT_ID:
	    					parentTableAlias = referencedTable.getAlias(referencedTableIndex++);
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(referencedTable, 
	    							DataExportCoreTable.M_SAVINGS_ACCOUNT);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// m_client/m_staff and m_savings_account
	    						sqlStatement = "`" + referencedTable.getName() + "` `" + parentTableAlias 
	                        			+ "` on `" + parentTableAlias + "`.`id` = `" + baseEntityName + "`.`"
	                					+ foreignKeyIndexColumnName + "`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(referencedTable, 
	        							DataExportCoreTable.M_SAVINGS_ACCOUNT, sqlStatement, parentTableAlias, 
	        							baseEntityName);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					if (isSelectStatement) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	        							+ referencedColumnName + "` as `" + coreColumn.getLabel() + "`";
	    						
	    						// add the select statement
	        					sqlBuilder.SELECT(sqlStatement);
	    						
	    					} else if (filterValue != null) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	        							+ referencedColumnName + "` " + filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    				case GROUP_NAME:
	    				case GROUP_ID:
	    					mGroupTableAlias = DataExportCoreTable.M_GROUP.getAlias(referencedTableIndex++);
	    					mGroupClientTableAlias = DataExportCoreTable.M_GROUP_CLIENT.getAlias(
	    							referencedTableIndex++);
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_SAVINGS_ACCOUNT, 
	    							DataExportCoreTable.M_GROUP_CLIENT);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// m_loan and m_group_client table join
	        					sqlStatement = "`" + DataExportCoreTable.M_GROUP_CLIENT.getName()
	        							+ "` `" + mGroupClientTableAlias + "` on `" + mGroupClientTableAlias
	        									+ "`.`client_id` = `" + baseEntityName + "`.`client_id`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_SAVINGS_ACCOUNT, 
	        							DataExportCoreTable.M_GROUP_CLIENT, sqlStatement, baseEntityName, 
	        							mGroupClientTableAlias);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_GROUP, 
	    							DataExportCoreTable.M_GROUP_CLIENT);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// m_group_client and m_group table join
	        					sqlStatement = "`" + DataExportCoreTable.M_GROUP.getName() + "` `"
	                        			+ mGroupTableAlias + "` on `" + mGroupTableAlias + "`.`id` = case when "
	                							+ "isnull(`" + baseEntityName + "`.`group_id`) then `"
	                									+ mGroupClientTableAlias + "`.`group_id` else `"
	                											+ baseEntityName + "`.`group_id` end";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_GROUP, 
	        							DataExportCoreTable.M_GROUP_CLIENT, sqlStatement, mGroupTableAlias, 
	        							mGroupClientTableAlias);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					if (isSelectStatement) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	        							+  referencedColumnName + "` as `" + coreColumn.getLabel() + "`";
	        					
	    						// add the select statement
	    						sqlBuilder.SELECT(sqlStatement);
	    						
	    					} else if (filterValue != null) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	    								+ referencedColumnName + "` " + filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    				case BRANCH_NAME:
	    					mClientTableAlias = DataExportCoreTable.M_CLIENT.getAlias(referencedTableIndex++);
	    					mGroupTableAlias = DataExportCoreTable.M_GROUP.getAlias(referencedTableIndex++);
	    					mOfficeTableAlias = DataExportCoreTable.M_OFFICE.getAlias(referencedTableIndex++);
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_CLIENT, 
	    							DataExportCoreTable.M_SAVINGS_ACCOUNT);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// m_loan and m_client table join
	        					sqlStatement = "`" + DataExportCoreTable.M_CLIENT.getName() + "` `"
	        							+ mClientTableAlias + "` on `" + mClientTableAlias + "`.`id` = `"
	    								+ baseEntityName + "`.`client_id`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_CLIENT, 
	        							DataExportCoreTable.M_SAVINGS_ACCOUNT, sqlStatement, mClientTableAlias, 
	        							baseEntityName);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_GROUP, 
	    							DataExportCoreTable.M_SAVINGS_ACCOUNT);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// m_loan and m_group table join
	        					sqlStatement = "`" + DataExportCoreTable.M_GROUP.getName() + "` `"
	                        			+ mGroupTableAlias + "` on `" + mGroupTableAlias + "`.`id` = `"
	                					+ baseEntityName + "`.`group_id`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_GROUP, 
	        							DataExportCoreTable.M_SAVINGS_ACCOUNT, sqlStatement, mGroupTableAlias, 
	        							baseEntityName);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_OFFICE, 
	    							DataExportCoreTable.M_SAVINGS_ACCOUNT);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// m_office and m_client/m_group table join
	        					sqlStatement = "`" + DataExportCoreTable.M_OFFICE.getName() + "` `"
	                        			+ mOfficeTableAlias + "` on `" + mOfficeTableAlias + "`.`id` = case when "
	                					+ "isnull(`" + baseEntityName + "`.`group_id`) then `"
	                							+ mClientTableAlias + "`.`office_id` else `"
	                									+ mGroupTableAlias + "`.`office_id` end";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_OFFICE, 
	        							DataExportCoreTable.M_SAVINGS_ACCOUNT, sqlStatement, mOfficeTableAlias, 
	        							baseEntityName);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					if (isSelectStatement) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	        							+  referencedColumnName + "` as `" + coreColumn.getLabel() + "`";
	        					
	    						// add the select statement
	    						sqlBuilder.SELECT(sqlStatement);
	    						
	    					} else if (filterValue != null) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	    								+ referencedColumnName + "` " + filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    				case STAFF_NAME:
	    					mClientTableAlias = DataExportCoreTable.M_CLIENT.getAlias(referencedTableIndex++);
	    					mGroupTableAlias = DataExportCoreTable.M_GROUP.getAlias(referencedTableIndex++);
	    					mStaffTableAlias = DataExportCoreTable.M_STAFF.getAlias(referencedTableIndex++);
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_CLIENT, 
	    							DataExportCoreTable.M_SAVINGS_ACCOUNT);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// m_loan and m_client table join
	        					sqlStatement = "`" + DataExportCoreTable.M_CLIENT.getName() + "` `"
	        							+ mClientTableAlias + "` on `" + mClientTableAlias + "`.`id` = `"
	    								+ baseEntityName + "`.`client_id`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_CLIENT, 
	        							DataExportCoreTable.M_SAVINGS_ACCOUNT, sqlStatement, mClientTableAlias, 
	        							baseEntityName);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_GROUP, 
	    							DataExportCoreTable.M_SAVINGS_ACCOUNT);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// m_loan and m_group table join
	        					sqlStatement = "`" + DataExportCoreTable.M_GROUP.getName() + "` `"
	                        			+ mGroupTableAlias + "` on `" + mGroupTableAlias + "`.`id` = `"
	                					+ baseEntityName + "`.`group_id`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_GROUP, 
	        							DataExportCoreTable.M_SAVINGS_ACCOUNT, sqlStatement, mGroupTableAlias, 
	        							baseEntityName);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_STAFF, 
	    							DataExportCoreTable.M_SAVINGS_ACCOUNT);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// m_staff and m_client/m_group table join
	        					sqlStatement = "`" + DataExportCoreTable.M_STAFF.getName() + "` `"
	                    			+ mStaffTableAlias + "` on `" + mStaffTableAlias + "`.`id` = case when "
	            					+ "isnull(`" + baseEntityName + "`.`group_id`) then `"
	            							+ mClientTableAlias + "`.`staff_id` else `"
	            									+ mGroupTableAlias + "`.`staff_id` end";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_STAFF, 
	        							DataExportCoreTable.M_SAVINGS_ACCOUNT, sqlStatement, mStaffTableAlias, 
	        							baseEntityName);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	                        
	                        // =============================================================================
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					if (isSelectStatement) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	        							+  referencedColumnName + "` as `" + coreColumn.getLabel() + "`";
	        					
	    						// add the select statement
	    						sqlBuilder.SELECT(sqlStatement);
	    						
	    					} else if (filterValue != null) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	    								+ referencedColumnName + "` " + filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	                    	break;
	    				case DATE_OF_BIRTH:
	    				case PHONE_NUMBER:
	    					mClientTableAlias = DataExportCoreTable.M_CLIENT.getAlias(referencedTableIndex++);
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_CLIENT, 
	    							DataExportCoreTable.M_SAVINGS_ACCOUNT);
	    					
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// m_loan and m_client table join
	        					sqlStatement = "`" + DataExportCoreTable.M_CLIENT.getName() + "` `" + mClientTableAlias 
	                        			+ "` on `" + mClientTableAlias + "`.`id` = `" + baseEntityName 
	                                            + "`.`client_id`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_CLIENT, 
	        							DataExportCoreTable.M_SAVINGS_ACCOUNT, sqlStatement, mClientTableAlias, 
	        							baseEntityName);
	    						
	    						sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					if (isSelectStatement) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	        							+ coreColumn.getName() + "` as `" + coreColumn.getLabel() + "`";
	    						
	    						// add the select statement
	        					sqlBuilder.SELECT(sqlStatement);
	        					
	    					} else if (filterValue != null) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	    								+ coreColumn.getName() + "` " + filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					break;
	    				case GENDER:
	    					mClientTableAlias = DataExportCoreTable.M_CLIENT.getAlias(referencedTableIndex++);
	    					mCodeValueTableAlias = referencedTable.getAlias(referencedTableIndex++);
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_CLIENT, 
	    							DataExportCoreTable.M_SAVINGS_ACCOUNT);
	    					
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						sqlStatement = "`" + DataExportCoreTable.M_CLIENT.getName() + "` `"
	        							+ mClientTableAlias + "` on `" + mClientTableAlias + "`.`id` = `"
										+ baseEntityName + "`.`client_id`";
	    						
	    						dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_CLIENT, 
	        							DataExportCoreTable.M_SAVINGS_ACCOUNT, sqlStatement, mClientTableAlias, 
	        							baseEntityName);
	    						
	    						sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(referencedTable, 
	    							DataExportCoreTable.M_CLIENT);
	    					
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						sqlStatement = "`" + referencedTable.getName() + "` `" + mCodeValueTableAlias + "` on `"
	        	                        + mCodeValueTableAlias + "`.`id` = `" + mClientTableAlias + "`.`" 
	    										+ coreColumn.getName() + "`";
	    						
	    						dataExportSqlJoin = DataExportSqlJoin.newInstance(referencedTable, 
	        							DataExportCoreTable.M_CLIENT, sqlStatement, mCodeValueTableAlias, 
	        							mClientTableAlias);
	    						
	    						sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					if (isSelectStatement) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	    								+ referencedColumnName + "` as `" + coreColumn.getLabel() + "`";
	    						
	    						// add the select statement
	        					sqlBuilder.SELECT(sqlStatement);
	    						
	    					} else if (filterValue != null) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	    								+ referencedColumnName + "` " + filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    				default:
	    					// =============================================================================
	    					if (isSelectStatement) {
	    						// add the select statement
	        					sqlStatement = "NULL as `" + coreColumn.getLabel() + "`";
	        					
	        					sqlBuilder.SELECT(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    			}
	    			break;
	    			
	    		case LOAN_TRANSACTION:
	    			switch (coreColumn) {
	    				case LOAN_TRANSACTION_INTEREST_ACCRUED:
	    					// =============================================================================
	    					if (isSelectStatement) {
	    						sqlStatement = "case when `" + baseEntityName + "`.`interest_portion_derived` "
	        							+ "is not null and `" + baseEntityName + "`.`transaction_type_enum` = "
										+ LoanTransactionType.ACCRUAL.getValue() + " then `" + baseEntityName 
												+ "`.`interest_portion_derived` else NULL end as `"
														+ coreColumn.getLabel() + "`";
	    						
	    						// add the select statement
	        					sqlBuilder.SELECT(sqlStatement);
	    						
	    					} else if (filterValue != null) {
	    						sqlStatement = "case when `" + baseEntityName + "`.`interest_portion_derived` "
	        							+ "is not null and `" + baseEntityName + "`.`transaction_type_enum` = "
										+ LoanTransactionType.ACCRUAL.getValue() + " then `" + baseEntityName 
												+ "`.`interest_portion_derived` else NULL end "
														+ filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    				case LOAN_TRANSACTION_INTEREST_WAIVED:
	    					// =============================================================================
	    					if (isSelectStatement) {
	    						sqlStatement = "case when `" + baseEntityName + "`.`interest_portion_derived` "
	        							+ "is not null and `" + baseEntityName + "`.`transaction_type_enum` = "
										+ LoanTransactionType.WAIVE_INTEREST.getValue() + " then `"
												+ baseEntityName + "`.`interest_portion_derived` "
														+ "else NULL end as `" + coreColumn.getLabel() + "`";
	    						
	    						// add the select statement
	        					sqlBuilder.SELECT(sqlStatement);
	    						
	    					} else if (filterValue != null) {
	    						sqlStatement = "case when `" + baseEntityName + "`.`interest_portion_derived` "
	        							+ "is not null and `" + baseEntityName + "`.`transaction_type_enum` = "
										+ LoanTransactionType.WAIVE_INTEREST.getValue() + " then `"
												+ baseEntityName + "`.`interest_portion_derived` "
														+ "else NULL end " + filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					break;
	    				case LOAN_TRANSACTION_TRANSFER_AMOUNT:
	    					// =============================================================================
	    					if (isSelectStatement) {
	    						sqlStatement = "case when `" + baseEntityName + "`.`transaction_type_enum` = "
										+ LoanTransactionType.APPROVE_TRANSFER.getValue() + " then `" 
												+ baseEntityName + "`.`amount` else NULL end as `"
														+ coreColumn.getLabel() + "`";
	    						
	    						// add the select statement
	        					sqlBuilder.SELECT(sqlStatement);
	    						
	    					} else if (filterValue != null) {
	    						sqlStatement = "case when `" + baseEntityName + "`.`transaction_type_enum` = "
										+ LoanTransactionType.APPROVE_TRANSFER.getValue() + " then `" 
												+ baseEntityName + "`.`amount` else NULL end `"
														+ filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    				case LOAN_TRANSACTION_PRODUCT_SHORT_NAME:
	    				case LOAN_TRANSACTION_PRODUCT_NAME:
	    				case LOAN_TRANSACTION_PRODUCT_ID:
	    					mLoanTableAlias = DataExportCoreTable.M_LOAN.getAlias(referencedTableIndex++);
	    					mProductLoanTableAlias = referencedTable.getAlias(referencedTableIndex++);
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_LOAN, 
	    							DataExportCoreTable.M_LOAN_TRANSACTION);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						sqlStatement = "`" + DataExportCoreTable.M_LOAN.getName() + "` `"
	        							+ mLoanTableAlias + "` on `" + mLoanTableAlias + "`.`id` = `"
												+ baseEntityName + "`.`loan_id`";
	    						dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_LOAN, 
	        							DataExportCoreTable.M_LOAN_TRANSACTION, sqlStatement, mLoanTableAlias, 
	        							baseEntityName);
	    						
	    						sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(referencedTable, 
	    							DataExportCoreTable.M_LOAN);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						sqlStatement = "`" + referencedTable.getName() + "` `" + mProductLoanTableAlias 
	                        			+ "` on `" + mProductLoanTableAlias + "`.`id` = `" + mLoanTableAlias + "`.`"
	                							+ foreignKeyIndexColumnName + "`";
	    						dataExportSqlJoin = DataExportSqlJoin.newInstance(referencedTable, 
	        							DataExportCoreTable.M_LOAN, sqlStatement, mProductLoanTableAlias, 
	        							mLoanTableAlias);
	    						
	    						sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					if (isSelectStatement) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	    								+ referencedColumnName + "` as `" + coreColumn.getLabel() + "`";
	        					
	    						// add the select statement
	        					sqlBuilder.SELECT(sqlStatement);
	        					
	    					} else if (filterValue != null) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	    								+ referencedColumnName + "` " + filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    				case LOAN_TRANSACTION_LOAN_ACCOUNT_NUMBER:
	    					mLoanTableAlias = DataExportCoreTable.M_LOAN.getAlias(referencedTableIndex++);
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_LOAN, 
	    							DataExportCoreTable.M_LOAN_TRANSACTION);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						sqlStatement = "`" + DataExportCoreTable.M_LOAN.getName() + "` `"
	        							+ mLoanTableAlias + "` on `" + mLoanTableAlias + "`.`id` = `"
												+ baseEntityName + "`.`loan_id`";
	    						dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_LOAN, 
	        							DataExportCoreTable.M_LOAN_TRANSACTION, sqlStatement, mLoanTableAlias, 
	        							baseEntityName);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					if (isSelectStatement) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	    								+ referencedColumnName + "` as `" + coreColumn.getLabel() + "`";
	    						
	    						// add the select statement
	        					sqlBuilder.SELECT(sqlStatement);
	        					
	    					} else if (filterValue != null) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	    								+ referencedColumnName + "` " + filterValue;
	    						
	    						// add the WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    				case LOAN_TRANSACTION_PAYMENT_CHANNEL:
	    					mPaymentDetailTableAlias = DataExportCoreTable.M_PAYMENT_DETAIL.getAlias(referencedTableIndex++);
	    					parentTableAlias = referencedTable.getAlias(referencedTableIndex++);
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_PAYMENT_DETAIL, 
	    							DataExportCoreTable.M_LOAN_TRANSACTION);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						sqlStatement = "`" + DataExportCoreTable.M_PAYMENT_DETAIL.getName() + "` `"
	        							+ mPaymentDetailTableAlias + "` on `" + mPaymentDetailTableAlias + "`.`id` = `"
										+ baseEntityName + "`.`payment_detail_id`";
	    						dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_PAYMENT_DETAIL, 
	        							DataExportCoreTable.M_LOAN_TRANSACTION, sqlStatement, mPaymentDetailTableAlias, 
	        							baseEntityName);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(referencedTable, 
	    							DataExportCoreTable.M_PAYMENT_DETAIL);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						sqlStatement = "`" + referencedTable.getName() + "` `" + parentTableAlias 
	                        			+ "` on `" + parentTableAlias + "`.`id` = `" + mPaymentDetailTableAlias + "`.`"
	                							+ foreignKeyIndexColumnName + "`";
	    						dataExportSqlJoin = DataExportSqlJoin.newInstance(referencedTable, 
	        							DataExportCoreTable.M_PAYMENT_DETAIL, sqlStatement, parentTableAlias, 
	        							mPaymentDetailTableAlias);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					if (isSelectStatement) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	        							+ referencedColumnName + "` as `" + coreColumn.getLabel() + "`";
	    						
	    						// add the select statement
	        					sqlBuilder.SELECT(sqlStatement);
	        					
	    					} else if (filterValue != null) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	    								+ referencedColumnName + "` " + filterValue;
	    						
	    						// add the WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    				case LOAN_TRANSACTION_REFERENCE:
	    					mPaymentDetailTableAlias = DataExportCoreTable.M_PAYMENT_DETAIL.getAlias(referencedTableIndex++);
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_PAYMENT_DETAIL, 
	    							DataExportCoreTable.M_LOAN_TRANSACTION);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						sqlStatement = "`" + DataExportCoreTable.M_PAYMENT_DETAIL.getName() + "` `"
	        							+ mPaymentDetailTableAlias + "` on `" + mPaymentDetailTableAlias + "`.`id` = `"
												+ baseEntityName + "`.`payment_detail_id`";
	    						dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_PAYMENT_DETAIL, 
	        							DataExportCoreTable.M_LOAN_TRANSACTION, sqlStatement, mPaymentDetailTableAlias, 
	        							baseEntityName);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					if (isSelectStatement) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	        							+ referencedColumnName + "` as `" + coreColumn.getLabel() + "`";
	    						
	    						// add the select statement
	        					sqlBuilder.SELECT(sqlStatement);
	        					
	    					} else if (filterValue != null) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	    								+ referencedColumnName + "` " + filterValue;
	    						
	    						// add the WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    				case LOAN_OFFICER_NAME:
	    				case CLIENT_NAME:
	    				case CLIENT_ID:
	    					mLoanTableAlias = DataExportCoreTable.M_LOAN.getAlias(referencedTableIndex++);
	    					parentTableAlias = referencedTable.getAlias(referencedTableIndex++);
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_LOAN, 
	    							DataExportCoreTable.M_LOAN_TRANSACTION);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						sqlStatement = "`" + DataExportCoreTable.M_LOAN.getName() + "` `"
	        							+ mLoanTableAlias + "` on `" + mLoanTableAlias + "`.`id` = `"
												+ baseEntityName + "`.`loan_id`";
	    						dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_LOAN, 
	        							DataExportCoreTable.M_LOAN_TRANSACTION, sqlStatement, mLoanTableAlias, 
	        							baseEntityName);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(referencedTable, 
	    							DataExportCoreTable.M_LOAN);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						sqlStatement = "`" + referencedTable.getName() + "` `" + parentTableAlias 
	                        			+ "` on `" + parentTableAlias + "`.`id` = `" + mLoanTableAlias + "`.`"
	                							+ foreignKeyIndexColumnName + "`";
	    						dataExportSqlJoin = DataExportSqlJoin.newInstance(referencedTable, 
	        							DataExportCoreTable.M_LOAN, sqlStatement, parentTableAlias, 
	        							mLoanTableAlias);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					if (isSelectStatement) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	        							+  referencedColumnName + "` as `" + coreColumn.getLabel() + "`";
	        					
	    						// add the select statement
	    						sqlBuilder.SELECT(sqlStatement);
	    						
	    					} else if (filterValue != null) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	    								+ referencedColumnName + "` " + filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    				case GROUP_NAME:
	    				case GROUP_ID:
	    					mLoanTableAlias = DataExportCoreTable.M_LOAN.getAlias(referencedTableIndex++);
	    					mGroupTableAlias = DataExportCoreTable.M_GROUP.getAlias(referencedTableIndex++);
	    					mGroupClientTableAlias = DataExportCoreTable.M_GROUP_CLIENT.getAlias(referencedTableIndex++);
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_LOAN, 
	    							DataExportCoreTable.M_LOAN_TRANSACTION);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// m_loan and m_group_client table join
	        					sqlStatement = "`" + DataExportCoreTable.M_LOAN.getName() + "` `"
	        							+ mLoanTableAlias + "` on `" + mLoanTableAlias + "`.`id` = `"
												+ baseEntityName + "`.`loan_id`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_LOAN, 
	        							DataExportCoreTable.M_LOAN_TRANSACTION, sqlStatement, mLoanTableAlias, 
	        							baseEntityName);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_LOAN, 
	    							DataExportCoreTable.M_GROUP_CLIENT);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// m_loan and m_group_client table join
	        					sqlStatement = "`" + DataExportCoreTable.M_GROUP_CLIENT.getName()
	        							+ "` `" + mGroupClientTableAlias + "` on `" + mGroupClientTableAlias
	        									+ "`.`client_id` = `" + mLoanTableAlias + "`.`client_id`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_LOAN, 
	        							DataExportCoreTable.M_GROUP_CLIENT, sqlStatement, mLoanTableAlias, 
	        							mGroupClientTableAlias);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_GROUP, 
	    							DataExportCoreTable.M_GROUP_CLIENT);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// m_group_client and m_group table join
	        					sqlStatement = "`" + DataExportCoreTable.M_GROUP.getName() + "` `"
	                        			+ mGroupTableAlias + "` on `" + mGroupTableAlias + "`.`id` = case when "
	                							+ "isnull(`" + mLoanTableAlias + "`.`group_id`) then `"
	                									+ mGroupClientTableAlias + "`.`group_id` else `"
	                											+ mLoanTableAlias + "`.`group_id` end";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_GROUP, 
	        							DataExportCoreTable.M_GROUP_CLIENT, sqlStatement, mGroupTableAlias, 
	        							mGroupClientTableAlias);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					if (isSelectStatement) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	        							+  referencedColumnName + "` as `" + coreColumn.getLabel() + "`";
	        					
	    						// add the select statement
	    						sqlBuilder.SELECT(sqlStatement);
	    						
	    					} else if (filterValue != null) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	    								+ referencedColumnName + "` " + filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    				case BRANCH_NAME:
	    					mOfficeTableAlias = DataExportCoreTable.M_OFFICE.getAlias(referencedTableIndex++);
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_OFFICE, 
	    							DataExportCoreTable.M_LOAN_TRANSACTION);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						sqlStatement = "`" + DataExportCoreTable.M_OFFICE.getName() + "` `"
	        							+ mOfficeTableAlias + "` on `" + mOfficeTableAlias + "`.`id` = `"
												+ baseEntityName + "`.`office_id`";
	    						dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_OFFICE, 
	        							DataExportCoreTable.M_LOAN_TRANSACTION, sqlStatement, mOfficeTableAlias, 
	        							baseEntityName);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					if (isSelectStatement) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	        							+  referencedColumnName + "` as `" + coreColumn.getLabel() + "`";
	        					
	    						// add the select statement
	    						sqlBuilder.SELECT(sqlStatement);
	    						
	    					} else if (filterValue != null) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	    								+ referencedColumnName + "` " + filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    				case STAFF_NAME:
	    					mLoanTableAlias = DataExportCoreTable.M_LOAN.getAlias(referencedTableIndex++);
	    					mClientTableAlias = DataExportCoreTable.M_CLIENT.getAlias(referencedTableIndex++);
	    					mGroupTableAlias = DataExportCoreTable.M_GROUP.getAlias(referencedTableIndex++);
	    					mStaffTableAlias = DataExportCoreTable.M_STAFF.getAlias(referencedTableIndex++);
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_LOAN, 
	    							DataExportCoreTable.M_LOAN_TRANSACTION);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// m_loan and m_group_client table join
	        					sqlStatement = "`" + DataExportCoreTable.M_LOAN.getName() + "` `"
	        							+ mLoanTableAlias + "` on `" + mLoanTableAlias + "`.`id` = `"
												+ baseEntityName + "`.`loan_id`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_LOAN, 
	        							DataExportCoreTable.M_LOAN_TRANSACTION, sqlStatement, mLoanTableAlias, 
	        							baseEntityName);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_CLIENT, 
	    							DataExportCoreTable.M_LOAN);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// m_loan and m_group_client table join
	        					sqlStatement = "`" + DataExportCoreTable.M_CLIENT.getName() + "` `"
	        							+ mClientTableAlias + "` on `" + mClientTableAlias + "`.`id` = `"
												+ mLoanTableAlias + "`.`client_id`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_CLIENT, 
	        							DataExportCoreTable.M_LOAN, sqlStatement, mClientTableAlias, 
	        							mLoanTableAlias);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_GROUP, 
	    							DataExportCoreTable.M_LOAN);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// m_loan and m_group_client table join
	        					sqlStatement = "`" + DataExportCoreTable.M_GROUP.getName() + "` `"
	                        			+ mGroupTableAlias + "` on `" + mGroupTableAlias + "`.`id` = `"
	                							+ mLoanTableAlias + "`.`group_id`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_GROUP, 
	        							DataExportCoreTable.M_LOAN, sqlStatement, mGroupTableAlias, 
	        							mLoanTableAlias);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_STAFF, 
	    							DataExportCoreTable.M_LOAN);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// m_group_client and m_group table join
	        					sqlStatement = "`" + DataExportCoreTable.M_STAFF.getName() + "` `"
	                        			+ mStaffTableAlias + "` on `" + mStaffTableAlias + "`.`id` = case when "
	                					+ "isnull(`" + mLoanTableAlias + "`.`group_id`) then `"
	                							+ mClientTableAlias + "`.`staff_id` else `"
	                									+ mGroupTableAlias + "`.`staff_id` end";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_STAFF, 
	        							DataExportCoreTable.M_LOAN, sqlStatement, mStaffTableAlias, 
	        							mLoanTableAlias);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					if (isSelectStatement) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	        							+  referencedColumnName + "` as `" + coreColumn.getLabel() + "`";
	        					
	    						// add the select statement
	    						sqlBuilder.SELECT(sqlStatement);
	    						
	    					} else if (filterValue != null) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	    								+ referencedColumnName + "` " + filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    				case DATE_OF_BIRTH:
	    				case PHONE_NUMBER:
	    					mLoanTableAlias = DataExportCoreTable.M_LOAN.getAlias(referencedTableIndex++);
	    					mClientTableAlias = DataExportCoreTable.M_CLIENT.getAlias(referencedTableIndex++);
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_LOAN, 
	    							DataExportCoreTable.M_LOAN_TRANSACTION);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						sqlStatement = "`" + DataExportCoreTable.M_LOAN.getName() + "` `"
		    							+ mLoanTableAlias + "` on `" + mLoanTableAlias + "`.`id` = `"
    											+ baseEntityName + "`.`loan_id`";
	    						dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_LOAN, 
	        							DataExportCoreTable.M_LOAN_TRANSACTION, sqlStatement, mLoanTableAlias, 
	        							baseEntityName);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_CLIENT, 
	    							DataExportCoreTable.M_LOAN);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						sqlStatement = "`" + DataExportCoreTable.M_CLIENT.getName() + "` `"
	        							+ mClientTableAlias + "` on `" + mClientTableAlias + "`.`id` = `"
												+ mLoanTableAlias + "`.`client_id`";
	    						dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_CLIENT, 
	        							DataExportCoreTable.M_LOAN, sqlStatement, mClientTableAlias, 
	        							mLoanTableAlias);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					if (isSelectStatement) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	        							+  coreColumn.getName() + "` as `" + coreColumn.getLabel() + "`";
	        					
	    						// add the select statement
	    						sqlBuilder.SELECT(sqlStatement);
	    						
	    					} else if (filterValue != null) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	    								+ coreColumn.getName() + "` " + filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    				case GENDER:
	    					mLoanTableAlias = DataExportCoreTable.M_LOAN.getAlias(referencedTableIndex++);
	    					mClientTableAlias = DataExportCoreTable.M_CLIENT.getAlias(referencedTableIndex++);
	    					mCodeValueTableAlias = referencedTable.getAlias(referencedTableIndex++);
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_LOAN, 
	    							DataExportCoreTable.M_LOAN_TRANSACTION);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						sqlStatement = "`" + DataExportCoreTable.M_LOAN.getName() + "` `"
	        							+ mLoanTableAlias + "` on `" + mLoanTableAlias + "`.`id` = `"
												+ baseEntityName + "`.`loan_id`";
	    						dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_LOAN, 
	        							DataExportCoreTable.M_LOAN_TRANSACTION, sqlStatement, mLoanTableAlias, 
	        							baseEntityName);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_CLIENT, 
	    							DataExportCoreTable.M_LOAN);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						sqlStatement = "`" + DataExportCoreTable.M_CLIENT.getName() + "` `"
	        							+ mClientTableAlias + "` on `" + mClientTableAlias + "`.`id` = `"
												+ mLoanTableAlias + "`.`client_id`";
	    						dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_CLIENT, 
	        							DataExportCoreTable.M_LOAN, sqlStatement, mClientTableAlias, 
	        							mLoanTableAlias);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(referencedTable, 
	    							DataExportCoreTable.M_CLIENT);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						sqlStatement = "`" + referencedTable.getName() + "` `" + mCodeValueTableAlias 
										+ "` on `" + mCodeValueTableAlias + "`.`id` = `" + mClientTableAlias 
	        	                        		+ "`.`" + coreColumn.getName() + "`";
	    						dataExportSqlJoin = DataExportSqlJoin.newInstance(referencedTable, 
	        							DataExportCoreTable.M_CLIENT, sqlStatement, mCodeValueTableAlias, 
	        							mClientTableAlias);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					if (isSelectStatement) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	        							+  referencedColumnName + "` as `" + coreColumn.getLabel() + "`";
	        					
	    						// add the select statement
	    						sqlBuilder.SELECT(sqlStatement);
	    						
	    					} else if (filterValue != null) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	    								+ referencedColumnName + "` " + filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    				default:
	    					// =============================================================================
	    					if (isSelectStatement) {
	    						// add the select statement
	        					sqlStatement = "NULL as `" + coreColumn.getLabel() + "`";
	        					
	        					sqlBuilder.SELECT(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    			}
	    			break;
	    		case SAVINGS_ACCOUNT_TRANSACTION:
	    			switch (coreColumn) {
	    				case SAVINGS_TRANSACTION_DEPOSIT:
	    					// =============================================================================
	    					if (isSelectStatement) {
	    						sqlStatement = "case when `" + baseEntityName + "`.`transaction_type_enum` = "
										+ SavingsAccountTransactionType.DEPOSIT.getValue() + " then `" 
	        									+ baseEntityName + "`.`amount` else NULL end as `"
														+ coreColumn.getLabel() + "`";
	    						
	    						// add the select statement
	        					sqlBuilder.SELECT(sqlStatement);
	    						
	    					} else if (filterValue != null) {
	    						sqlStatement = "case when `" + baseEntityName + "`.`transaction_type_enum` = "
										+ SavingsAccountTransactionType.DEPOSIT.getValue() + " then `" 
	        									+ baseEntityName + "`.`amount` else NULL end "
														+ filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    				case SAVINGS_TRANSACTION_CHARGE_APPLIED:
	    					// =============================================================================
	    					if (isSelectStatement) {
	    						sqlStatement = "case when `" + baseEntityName + "`.`transaction_type_enum` = "
										+ SavingsAccountTransactionType.WITHDRAWAL_FEE.getValue() + " or `" 
												+ baseEntityName + "`.`transaction_type_enum` = "
														+ SavingsAccountTransactionType.ANNUAL_FEE.getValue()
																+ " then `" + baseEntityName + "`.`amount` "
																		+ "else NULL end as `" 
																				+ coreColumn.getLabel() + "`";
	    						
	    						// add the select statement
	        					sqlBuilder.SELECT(sqlStatement);
	    						
	    					} else if (filterValue != null) {
	    						sqlStatement = "case when `" + baseEntityName + "`.`transaction_type_enum` = "
										+ SavingsAccountTransactionType.WITHDRAWAL_FEE.getValue() + " or `" 
												+ baseEntityName + "`.`transaction_type_enum` = "
														+ SavingsAccountTransactionType.ANNUAL_FEE.getValue()
																+ " then `" + baseEntityName + "`.`amount` "
																		+ "else NULL end " 
																				+ filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    				case SAVINGS_TRANSACTION_CHARGE_WAIVED:
	    					// =============================================================================
	    					if (isSelectStatement) {
	    						sqlStatement = "case when `" + baseEntityName + "`.`transaction_type_enum` = "
										+ SavingsAccountTransactionType.WAIVE_CHARGES.getValue() + " then `" 
	        									+ baseEntityName + "`.`amount` else NULL end as `"
														+ coreColumn.getLabel() + "`";
	    						
	    						// add the select statement
	        					sqlBuilder.SELECT(sqlStatement);
	    						
	    					} else if (filterValue != null) {
	    						sqlStatement = "case when `" + baseEntityName + "`.`transaction_type_enum` = "
										+ SavingsAccountTransactionType.WAIVE_CHARGES.getValue() + " then `" 
	        									+ baseEntityName + "`.`amount` else NULL end "
														+ filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    				case SAVINGS_TRANSACTION_TRANSFER_AMOUNT:
	    					// =============================================================================
	    					if (isSelectStatement) {
	    						sqlStatement = "case when `" + baseEntityName + "`.`transaction_type_enum` = "
										+ SavingsAccountTransactionType.APPROVE_TRANSFER.getValue() + " then `" 
	        									+ baseEntityName + "`.`amount` else NULL end as `"
														+ coreColumn.getLabel() + "`";
	    						
	    						// add the select statement
	        					sqlBuilder.SELECT(sqlStatement);
	    						
	    					} else if (filterValue != null) {
	    						sqlStatement = "case when `" + baseEntityName + "`.`transaction_type_enum` = "
										+ SavingsAccountTransactionType.APPROVE_TRANSFER.getValue() + " then `" 
	        									+ baseEntityName + "`.`amount` else NULL end "
														+ filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    				case SAVINGS_TRANSACTION_PRODUCT_SHORT_NAME:
	    				case SAVINGS_TRANSACTION_PRODUCT_NAME:
	    				case SAVINGS_TRANSACTION_PRODUCT_ID:
	    					mSavingsAccountTableAlias = DataExportCoreTable.M_SAVINGS_ACCOUNT.getAlias(referencedTableIndex++);
	    					mSavingsProductTableAlias = referencedTable.getAlias(referencedTableIndex++);
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_SAVINGS_ACCOUNT, 
	    							DataExportCoreTable.M_SAVINGS_ACCOUNT_TRANSACTION);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						sqlStatement = "`" + DataExportCoreTable.M_SAVINGS_ACCOUNT.getName() + "` `"
	        							+ mSavingsAccountTableAlias + "` on `" + mSavingsAccountTableAlias + "`.`id` = `"
												+ baseEntityName + "`.`savings_account_id`";
	    						dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_SAVINGS_ACCOUNT, 
	        							DataExportCoreTable.M_SAVINGS_ACCOUNT_TRANSACTION, sqlStatement, mSavingsAccountTableAlias, 
	        							baseEntityName);
	    						
	    						sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(referencedTable, 
	    							DataExportCoreTable.M_SAVINGS_ACCOUNT);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						sqlStatement = "`" + referencedTable.getName() + "` `" + mSavingsProductTableAlias 
	                        			+ "` on `" + mSavingsProductTableAlias + "`.`id` = `" + mSavingsAccountTableAlias + "`.`"
	                							+ foreignKeyIndexColumnName + "`";
	    						dataExportSqlJoin = DataExportSqlJoin.newInstance(referencedTable, 
	        							DataExportCoreTable.M_SAVINGS_ACCOUNT, sqlStatement, mSavingsProductTableAlias, 
	        							mSavingsAccountTableAlias);
	    						
	    						sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					if (isSelectStatement) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	    								+ referencedColumnName + "` as `" + coreColumn.getLabel() + "`";
	        					
	    						// add the select statement
	        					sqlBuilder.SELECT(sqlStatement);
	        					
	    					} else if (filterValue != null) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	    								+ referencedColumnName + "` " + filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    				case SAVINGS_TRANSACTION_ACCOUNT_NUMBER:
	    					mSavingsAccountTableAlias = DataExportCoreTable.M_SAVINGS_ACCOUNT.getAlias(referencedTableIndex++);
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_SAVINGS_ACCOUNT, 
	    							DataExportCoreTable.M_SAVINGS_ACCOUNT_TRANSACTION);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						sqlStatement = "`" + DataExportCoreTable.M_SAVINGS_ACCOUNT.getName() + "` `"
	        							+ mSavingsAccountTableAlias + "` on `" + mSavingsAccountTableAlias + "`.`id` = `"
												+ baseEntityName + "`.`savings_account_id`";
	    						dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_SAVINGS_ACCOUNT, 
	        							DataExportCoreTable.M_SAVINGS_ACCOUNT_TRANSACTION, sqlStatement, mSavingsAccountTableAlias, 
	        							baseEntityName);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					if (isSelectStatement) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	    								+ referencedColumnName + "` as `" + coreColumn.getLabel() + "`";
	    						
	    						// add the select statement
	        					sqlBuilder.SELECT(sqlStatement);
	        					
	    					} else if (filterValue != null) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	    								+ referencedColumnName + "` " + filterValue;
	    						
	    						// add the WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    				case SAVINGS_TRANSACTION_PAYMENT_CHANNEL:
	    					mPaymentDetailTableAlias = DataExportCoreTable.M_PAYMENT_DETAIL.getAlias(referencedTableIndex++);
	    					parentTableAlias = referencedTable.getAlias(referencedTableIndex++);
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_PAYMENT_DETAIL, 
	    							DataExportCoreTable.M_SAVINGS_ACCOUNT_TRANSACTION);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						sqlStatement = "`" + DataExportCoreTable.M_PAYMENT_DETAIL.getName() + "` `"
	        							+ mPaymentDetailTableAlias + "` on `" + mPaymentDetailTableAlias + "`.`id` = `"
										+ baseEntityName + "`.`payment_detail_id`";
	    						dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_PAYMENT_DETAIL, 
	        							DataExportCoreTable.M_SAVINGS_ACCOUNT_TRANSACTION, sqlStatement, mPaymentDetailTableAlias, 
	        							baseEntityName);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(referencedTable, 
	    							DataExportCoreTable.M_PAYMENT_DETAIL);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						sqlStatement = "`" + referencedTable.getName() + "` `" + parentTableAlias 
	                        			+ "` on `" + parentTableAlias + "`.`id` = `" + mPaymentDetailTableAlias + "`.`"
	                							+ foreignKeyIndexColumnName + "`";
	    						dataExportSqlJoin = DataExportSqlJoin.newInstance(referencedTable, 
	        							DataExportCoreTable.M_PAYMENT_DETAIL, sqlStatement, parentTableAlias, 
	        							mPaymentDetailTableAlias);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					if (isSelectStatement) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	        							+ referencedColumnName + "` as `" + coreColumn.getLabel() + "`";
	    						
	    						// add the select statement
	        					sqlBuilder.SELECT(sqlStatement);
	        					
	    					} else if (filterValue != null) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	    								+ referencedColumnName + "` " + filterValue;
	    						
	    						// add the WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    				case SAVINGS_TRANSACTION_REFERENCE:
	    					mPaymentDetailTableAlias = DataExportCoreTable.M_PAYMENT_DETAIL.getAlias(referencedTableIndex++);
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_PAYMENT_DETAIL, 
	    							DataExportCoreTable.M_SAVINGS_ACCOUNT_TRANSACTION);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						sqlStatement = "`" + DataExportCoreTable.M_PAYMENT_DETAIL.getName() + "` `"
	        							+ mPaymentDetailTableAlias + "` on `" + mPaymentDetailTableAlias + "`.`id` = `"
												+ baseEntityName + "`.`payment_detail_id`";
	    						dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_PAYMENT_DETAIL, 
	        							DataExportCoreTable.M_SAVINGS_ACCOUNT_TRANSACTION, sqlStatement, mPaymentDetailTableAlias, 
	        							baseEntityName);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					if (isSelectStatement) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	        							+ referencedColumnName + "` as `" + coreColumn.getLabel() + "`";
	    						
	    						// add the select statement
	        					sqlBuilder.SELECT(sqlStatement);
	        					
	    					} else if (filterValue != null) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	    								+ referencedColumnName + "` " + filterValue;
	    						
	    						// add the WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    				case SAVINGS_TRANSACTION_WITHDRAWAL:
	    					// =============================================================================
	    					if (isSelectStatement) {
	    						sqlStatement = "case when `" + baseEntityName + "`.`transaction_type_enum` = "
										+ SavingsAccountTransactionType.WITHDRAWAL.getValue() + " then `" 
	        									+ baseEntityName + "`.`amount` else NULL end as `"
														+ coreColumn.getLabel() + "`";
	    						
	    						// add the select statement
	        					sqlBuilder.SELECT(sqlStatement);
	    						
	    					} else if (filterValue != null) {
	    						sqlStatement = "case when `" + baseEntityName + "`.`transaction_type_enum` = "
										+ SavingsAccountTransactionType.WITHDRAWAL.getValue() + " then `" 
	        									+ baseEntityName + "`.`amount` else NULL end "
														+ filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    				case CLIENT_NAME:
	    				case CLIENT_ID:
	    					mSavingsAccountTableAlias = DataExportCoreTable.M_SAVINGS_ACCOUNT.getAlias(referencedTableIndex++);
	    					parentTableAlias = referencedTable.getAlias(referencedTableIndex++);
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_SAVINGS_ACCOUNT, 
	    							DataExportCoreTable.M_SAVINGS_ACCOUNT_TRANSACTION);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						sqlStatement = "`" + DataExportCoreTable.M_SAVINGS_ACCOUNT.getName() + "` `"
	        							+ mSavingsAccountTableAlias + "` on `" + mSavingsAccountTableAlias + "`.`id` = `"
												+ baseEntityName + "`.`savings_account_id`";
	    						dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_SAVINGS_ACCOUNT, 
	        							DataExportCoreTable.M_SAVINGS_ACCOUNT_TRANSACTION, sqlStatement, mSavingsAccountTableAlias, 
	        							baseEntityName);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(referencedTable, 
	    							DataExportCoreTable.M_SAVINGS_ACCOUNT);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						sqlStatement = "`" + referencedTable.getName() + "` `" + parentTableAlias 
	                        			+ "` on `" + parentTableAlias + "`.`id` = `" + mSavingsAccountTableAlias + "`.`"
	                							+ foreignKeyIndexColumnName + "`";
	    						dataExportSqlJoin = DataExportSqlJoin.newInstance(referencedTable, 
	        							DataExportCoreTable.M_SAVINGS_ACCOUNT, sqlStatement, parentTableAlias, 
	        							mSavingsAccountTableAlias);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					if (isSelectStatement) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	        							+  referencedColumnName + "` as `" + coreColumn.getLabel() + "`";
	        					
	    						// add the select statement
	    						sqlBuilder.SELECT(sqlStatement);
	    						
	    					} else if (filterValue != null) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	    								+ referencedColumnName + "` " + filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    				case GROUP_NAME:
	    				case GROUP_ID:
	    					mSavingsAccountTableAlias = DataExportCoreTable.M_SAVINGS_ACCOUNT.getAlias(referencedTableIndex++);
	    					mGroupTableAlias = DataExportCoreTable.M_GROUP.getAlias(referencedTableIndex++);
	    					mGroupClientTableAlias = DataExportCoreTable.M_GROUP_CLIENT.getAlias(referencedTableIndex++);
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_SAVINGS_ACCOUNT, 
	    							DataExportCoreTable.M_SAVINGS_ACCOUNT_TRANSACTION);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// m_loan and m_group_client table join
	        					sqlStatement = "`" + DataExportCoreTable.M_SAVINGS_ACCOUNT.getName() + "` `"
	        							+ mSavingsAccountTableAlias + "` on `" + mSavingsAccountTableAlias + "`.`id` = `"
												+ baseEntityName + "`.`savings_account_id`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_SAVINGS_ACCOUNT, 
	        							DataExportCoreTable.M_SAVINGS_ACCOUNT_TRANSACTION, sqlStatement, mSavingsAccountTableAlias, 
	        							baseEntityName);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_SAVINGS_ACCOUNT, 
	    							DataExportCoreTable.M_GROUP_CLIENT);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// m_loan and m_group_client table join
	        					sqlStatement = "`" + DataExportCoreTable.M_GROUP_CLIENT.getName()
	        							+ "` `" + mGroupClientTableAlias + "` on `" + mGroupClientTableAlias
	        									+ "`.`client_id` = `" + mSavingsAccountTableAlias + "`.`client_id`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_SAVINGS_ACCOUNT, 
	        							DataExportCoreTable.M_GROUP_CLIENT, sqlStatement, mSavingsAccountTableAlias, 
	        							mGroupClientTableAlias);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_GROUP, 
	    							DataExportCoreTable.M_GROUP_CLIENT);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// m_group_client and m_group table join
	        					sqlStatement = "`" + DataExportCoreTable.M_GROUP.getName() + "` `"
	                        			+ mGroupTableAlias + "` on `" + mGroupTableAlias + "`.`id` = case when "
	                							+ "isnull(`" + mSavingsAccountTableAlias + "`.`group_id`) then `"
	                									+ mGroupClientTableAlias + "`.`group_id` else `"
	                											+ mSavingsAccountTableAlias + "`.`group_id` end";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_GROUP, 
	        							DataExportCoreTable.M_GROUP_CLIENT, sqlStatement, mGroupTableAlias, 
	        							mGroupClientTableAlias);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					if (isSelectStatement) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	        							+  referencedColumnName + "` as `" + coreColumn.getLabel() + "`";
	        					
	    						// add the select statement
	    						sqlBuilder.SELECT(sqlStatement);
	    						
	    					} else if (filterValue != null) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	    								+ referencedColumnName + "` " + filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    				case BRANCH_NAME:
	    					mOfficeTableAlias = DataExportCoreTable.M_OFFICE.getAlias(referencedTableIndex++);
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_OFFICE, 
	    							DataExportCoreTable.M_SAVINGS_ACCOUNT_TRANSACTION);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						sqlStatement = "`" + DataExportCoreTable.M_OFFICE.getName() + "` `"
	        							+ mOfficeTableAlias + "` on `" + mOfficeTableAlias + "`.`id` = `"
												+ baseEntityName + "`.`office_id`";
	    						dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_OFFICE, 
	        							DataExportCoreTable.M_SAVINGS_ACCOUNT_TRANSACTION, sqlStatement, mOfficeTableAlias, 
	        							baseEntityName);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					if (isSelectStatement) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	        							+  referencedColumnName + "` as `" + coreColumn.getLabel() + "`";
	        					
	    						// add the select statement
	    						sqlBuilder.SELECT(sqlStatement);
	    						
	    					} else if (filterValue != null) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	    								+ referencedColumnName + "` " + filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    				case STAFF_NAME:
	    					mSavingsAccountTableAlias = DataExportCoreTable.M_SAVINGS_ACCOUNT.getAlias(referencedTableIndex++);
	    					mClientTableAlias = DataExportCoreTable.M_CLIENT.getAlias(referencedTableIndex++);
	    					mGroupTableAlias = DataExportCoreTable.M_GROUP.getAlias(referencedTableIndex++);
	    					mStaffTableAlias = DataExportCoreTable.M_STAFF.getAlias(referencedTableIndex++);
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_SAVINGS_ACCOUNT, 
	    							DataExportCoreTable.M_SAVINGS_ACCOUNT_TRANSACTION);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// m_loan and m_group_client table join
	        					sqlStatement = "`" + DataExportCoreTable.M_SAVINGS_ACCOUNT.getName() + "` `"
	        							+ mSavingsAccountTableAlias + "` on `" + mSavingsAccountTableAlias + "`.`id` = `"
												+ baseEntityName + "`.`savings_account_id`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_SAVINGS_ACCOUNT, 
	        							DataExportCoreTable.M_SAVINGS_ACCOUNT_TRANSACTION, sqlStatement, mSavingsAccountTableAlias, 
	        							baseEntityName);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_CLIENT, 
	    							DataExportCoreTable.M_SAVINGS_ACCOUNT);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// m_loan and m_group_client table join
	        					sqlStatement = "`" + DataExportCoreTable.M_CLIENT.getName() + "` `"
	        							+ mClientTableAlias + "` on `" + mClientTableAlias + "`.`id` = `"
												+ mSavingsAccountTableAlias + "`.`client_id`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_CLIENT, 
	        							DataExportCoreTable.M_SAVINGS_ACCOUNT, sqlStatement, mClientTableAlias, 
	        							mSavingsAccountTableAlias);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_GROUP, 
	    							DataExportCoreTable.M_SAVINGS_ACCOUNT);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// m_loan and m_group_client table join
	        					sqlStatement = "`" + DataExportCoreTable.M_GROUP.getName() + "` `"
	                        			+ mGroupTableAlias + "` on `" + mGroupTableAlias + "`.`id` = `"
	                							+ mSavingsAccountTableAlias + "`.`group_id`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_GROUP, 
	        							DataExportCoreTable.M_SAVINGS_ACCOUNT, sqlStatement, mGroupTableAlias, 
	        							mSavingsAccountTableAlias);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_STAFF, 
	    							DataExportCoreTable.M_SAVINGS_ACCOUNT);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// m_group_client and m_group table join
	        					sqlStatement = "`" + DataExportCoreTable.M_STAFF.getName() + "` `"
	                        			+ mStaffTableAlias + "` on `" + mStaffTableAlias + "`.`id` = case when "
	                					+ "isnull(`" + mSavingsAccountTableAlias + "`.`group_id`) then `"
	                							+ mClientTableAlias + "`.`staff_id` else `"
	                									+ mGroupTableAlias + "`.`staff_id` end";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_STAFF, 
	        							DataExportCoreTable.M_SAVINGS_ACCOUNT, sqlStatement, mStaffTableAlias, 
	        							mSavingsAccountTableAlias);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					if (isSelectStatement) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	        							+  referencedColumnName + "` as `" + coreColumn.getLabel() + "`";
	        					
	    						// add the select statement
	    						sqlBuilder.SELECT(sqlStatement);
	    						
	    					} else if (filterValue != null) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	    								+ referencedColumnName + "` " + filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    				case DATE_OF_BIRTH:
	    				case PHONE_NUMBER:
	    					mSavingsAccountTableAlias = DataExportCoreTable.M_SAVINGS_ACCOUNT.getAlias(referencedTableIndex++);
	    					mClientTableAlias = DataExportCoreTable.M_CLIENT.getAlias(referencedTableIndex++);
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_SAVINGS_ACCOUNT, 
	    							DataExportCoreTable.M_SAVINGS_ACCOUNT_TRANSACTION);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						sqlStatement = "`" + DataExportCoreTable.M_SAVINGS_ACCOUNT.getName() + "` `"
		    							+ mSavingsAccountTableAlias + "` on `" + mSavingsAccountTableAlias + "`.`id` = `"
    											+ baseEntityName + "`.`savings_account_id`";
	    						dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_SAVINGS_ACCOUNT, 
	        							DataExportCoreTable.M_SAVINGS_ACCOUNT_TRANSACTION, sqlStatement, mSavingsAccountTableAlias, 
	        							baseEntityName);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_CLIENT, 
	    							DataExportCoreTable.M_SAVINGS_ACCOUNT);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						sqlStatement = "`" + DataExportCoreTable.M_CLIENT.getName() + "` `"
	        							+ mClientTableAlias + "` on `" + mClientTableAlias + "`.`id` = `"
												+ mSavingsAccountTableAlias + "`.`client_id`";
	    						dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_CLIENT, 
	        							DataExportCoreTable.M_SAVINGS_ACCOUNT, sqlStatement, mClientTableAlias, 
	        							mSavingsAccountTableAlias);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					if (isSelectStatement) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	        							+  coreColumn.getName() + "` as `" + coreColumn.getLabel() + "`";
	        					
	    						// add the select statement
	    						sqlBuilder.SELECT(sqlStatement);
	    						
	    					} else if (filterValue != null) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	    								+ coreColumn.getName() + "` " + filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    				case GENDER:
	    					mSavingsAccountTableAlias = DataExportCoreTable.M_SAVINGS_ACCOUNT.getAlias(referencedTableIndex++);
	    					mClientTableAlias = DataExportCoreTable.M_CLIENT.getAlias(referencedTableIndex++);
	    					mCodeValueTableAlias = referencedTable.getAlias(referencedTableIndex++);
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_SAVINGS_ACCOUNT, 
	    							DataExportCoreTable.M_SAVINGS_ACCOUNT_TRANSACTION);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						sqlStatement = "`" + DataExportCoreTable.M_SAVINGS_ACCOUNT.getName() + "` `"
	        							+ mSavingsAccountTableAlias + "` on `" + mSavingsAccountTableAlias + "`.`id` = `"
												+ baseEntityName + "`.`savings_account_id`";
	    						dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_SAVINGS_ACCOUNT, 
	        							DataExportCoreTable.M_SAVINGS_ACCOUNT_TRANSACTION, sqlStatement, mSavingsAccountTableAlias, 
	        							baseEntityName);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_CLIENT, 
	    							DataExportCoreTable.M_SAVINGS_ACCOUNT);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						sqlStatement = "`" + DataExportCoreTable.M_CLIENT.getName() + "` `"
	        							+ mClientTableAlias + "` on `" + mClientTableAlias + "`.`id` = `"
												+ mSavingsAccountTableAlias + "`.`client_id`";
	    						dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_CLIENT, 
	        							DataExportCoreTable.M_SAVINGS_ACCOUNT, sqlStatement, mClientTableAlias, 
	        							mSavingsAccountTableAlias);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(referencedTable, 
	    							DataExportCoreTable.M_CLIENT);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						sqlStatement = "`" + referencedTable.getName() + "` `" + mCodeValueTableAlias 
										+ "` on `" + mCodeValueTableAlias + "`.`id` = `" + mClientTableAlias 
	        	                        		+ "`.`" + coreColumn.getName() + "`";
	    						dataExportSqlJoin = DataExportSqlJoin.newInstance(referencedTable, 
	        							DataExportCoreTable.M_CLIENT, sqlStatement, mCodeValueTableAlias, 
	        							mClientTableAlias);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					if (isSelectStatement) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	        							+  referencedColumnName + "` as `" + coreColumn.getLabel() + "`";
	        					
	    						// add the select statement
	    						sqlBuilder.SELECT(sqlStatement);
	    						
	    					} else if (filterValue != null) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	    								+ referencedColumnName + "` " + filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    				default:
	    					// =============================================================================
	    					if (isSelectStatement) {
	    						// add the select statement
	        					sqlStatement = "NULL as `" + coreColumn.getLabel() + "`";
	        					
	        					sqlBuilder.SELECT(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    			}
	    			break;
	    		case LOAN_REPAYMENT_SCHEDULE:
	    			switch (coreColumn) {
	    				case REPAYMENT_SCHEDULE_TOTAL_EXPECTED:
	    					// =============================================================================
	    					if (isSelectStatement) {
	    						sqlStatement = "(ifnull(`" + baseEntityName + "`.`principal_amount`, 0) + "
	    								+ "ifnull(`" + baseEntityName + "`.`interest_amount`, 0) + "
	    										+ "ifnull(`" + baseEntityName + "`.`fee_charges_amount`, 0) + "
	    												+ "ifnull(`" + baseEntityName + "`.`penalty_charges_amount`, 0)) "
	    														+ "as `" + coreColumn.getLabel() + "`";
	    						
	    						// add the select statement
	        					sqlBuilder.SELECT(sqlStatement);
	    						
	    					} else if (filterValue != null) {
	    						sqlStatement = "(ifnull(`" + baseEntityName + "`.`principal_amount`, 0) + "
	    								+ "ifnull(`" + baseEntityName + "`.`interest_amount`, 0) + "
	    										+ "ifnull(`" + baseEntityName + "`.`fee_charges_amount`, 0) + "
	    												+ "ifnull(`" + baseEntityName + "`.`penalty_charges_amount`, 0)) "
	    														+ filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    				case REPAYMENT_SCHEDULE_INTEREST_EXPECTED:
	    				case REPAYMENT_SCHEDULE_FEES_EXPECTED:
	    				case REPAYMENT_SCHEDULE_PENALTIES_EXPECTED:
	    				case REPAYMENT_SCHEDULE_PRINCIPAL_EXPECTED:
	    					// =============================================================================
	    					if (isSelectStatement) {
	    						sqlStatement = "`" + baseEntityName + "`.`" + coreColumn.getName() + "` "
	    								+ "as `" + coreColumn.getLabel() + "`";
	    						
	    						// add the select statement
	        					sqlBuilder.SELECT(sqlStatement);
	    						
	    					} else if (filterValue != null) {
	    						sqlStatement = "`" + baseEntityName + "`.`" + coreColumn.getName() + "` "
	    								+ filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					break;
		    			case STAFF_NAME:
		    				mLoanTableAlias = DataExportCoreTable.M_LOAN.getAlias(referencedTableIndex++);
	    					mClientTableAlias = DataExportCoreTable.M_CLIENT.getAlias(referencedTableIndex++);
	    					mGroupTableAlias = DataExportCoreTable.M_GROUP.getAlias(referencedTableIndex++);
	    					mStaffTableAlias = DataExportCoreTable.M_STAFF.getAlias(referencedTableIndex++);
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_LOAN, 
	    							DataExportCoreTable.M_LOAN_REPAYMENT_SCHEDULE);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	        					sqlStatement = "`" + DataExportCoreTable.M_LOAN.getName() + "` `"
	        							+ mLoanTableAlias + "` on `" + mLoanTableAlias + "`.`id` = `"
												+ baseEntityName + "`.`loan_id`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_LOAN, 
	        							DataExportCoreTable.M_LOAN_REPAYMENT_SCHEDULE, sqlStatement, mLoanTableAlias, 
	        							baseEntityName);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_CLIENT, 
	    							DataExportCoreTable.M_LOAN);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// m_loan and m_client table join
	        					sqlStatement = "`" + DataExportCoreTable.M_CLIENT.getName() + "` `"
	        							+ mClientTableAlias + "` on `" + mClientTableAlias + "`.`id` = `"
	    										+ mLoanTableAlias + "`.`client_id`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_CLIENT, 
	        							DataExportCoreTable.M_LOAN, sqlStatement, mClientTableAlias, 
	        							mLoanTableAlias);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_GROUP, 
	    							DataExportCoreTable.M_LOAN);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// m_loan and m_group table join
	        					sqlStatement = "`" + DataExportCoreTable.M_GROUP.getName() + "` `"
	                        			+ mGroupTableAlias + "` on `" + mGroupTableAlias + "`.`id` = `"
	                					+ baseEntityName + "`.`group_id`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_GROUP, 
	        							DataExportCoreTable.M_LOAN, sqlStatement, mGroupTableAlias, 
	        							baseEntityName);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_STAFF, 
	    							DataExportCoreTable.M_LOAN);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// m_staff and m_client/m_group table join
	        					sqlStatement = "`" + DataExportCoreTable.M_STAFF.getName() + "` `"
	                    			+ mStaffTableAlias + "` on `" + mStaffTableAlias + "`.`id` = case when "
	            					+ "isnull(`" + baseEntityName + "`.`group_id`) then `"
	            							+ mClientTableAlias + "`.`staff_id` else `"
	            									+ mGroupTableAlias + "`.`staff_id` end";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_STAFF, 
	        							DataExportCoreTable.M_LOAN, sqlStatement, mStaffTableAlias, 
	        							baseEntityName);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	                        
	                        // =============================================================================
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					if (isSelectStatement) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	        							+  referencedColumnName + "` as `" + coreColumn.getLabel() + "`";
	        					
	    						// add the select statement
	    						sqlBuilder.SELECT(sqlStatement);
	    						
	    					} else if (filterValue != null) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	    								+ referencedColumnName + "` " + filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
		    			case BRANCH_NAME:
		    				mLoanTableAlias = DataExportCoreTable.M_LOAN.getAlias(referencedTableIndex++);
		    				mClientTableAlias = DataExportCoreTable.M_CLIENT.getAlias(referencedTableIndex++);
	    					mGroupTableAlias = DataExportCoreTable.M_GROUP.getAlias(referencedTableIndex++);
	    					mOfficeTableAlias = DataExportCoreTable.M_OFFICE.getAlias(referencedTableIndex++);
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_LOAN, 
	    							DataExportCoreTable.M_LOAN_REPAYMENT_SCHEDULE);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	        					sqlStatement = "`" + DataExportCoreTable.M_LOAN.getName() + "` `"
	        							+ mLoanTableAlias + "` on `" + mLoanTableAlias + "`.`id` = `"
												+ baseEntityName + "`.`loan_id`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_LOAN, 
	        							DataExportCoreTable.M_LOAN_REPAYMENT_SCHEDULE, sqlStatement, mLoanTableAlias, 
	        							baseEntityName);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_CLIENT, 
	    							DataExportCoreTable.M_LOAN);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// m_loan and m_client table join
	        					sqlStatement = "`" + DataExportCoreTable.M_CLIENT.getName() + "` `"
	        							+ mClientTableAlias + "` on `" + mClientTableAlias + "`.`id` = `"
	    										+ mLoanTableAlias + "`.`client_id`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_CLIENT, 
	        							DataExportCoreTable.M_LOAN, sqlStatement, mClientTableAlias, 
	        							mLoanTableAlias);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_GROUP, 
	    							DataExportCoreTable.M_LOAN);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// m_loan and m_group table join
	        					sqlStatement = "`" + DataExportCoreTable.M_GROUP.getName() + "` `"
	                        			+ mGroupTableAlias + "` on `" + mGroupTableAlias + "`.`id` = `"
	                					+ mLoanTableAlias + "`.`group_id`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_GROUP, 
	        							DataExportCoreTable.M_LOAN, sqlStatement, mGroupTableAlias, 
	        							mLoanTableAlias);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_OFFICE, 
	    							DataExportCoreTable.M_LOAN);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// m_office and m_client/m_group table join
	        					sqlStatement = "`" + DataExportCoreTable.M_OFFICE.getName() + "` `"
	                        			+ mOfficeTableAlias + "` on `" + mOfficeTableAlias + "`.`id` = case when "
	                					+ "isnull(`" + mLoanTableAlias + "`.`group_id`) then `"
	                							+ mClientTableAlias + "`.`office_id` else `"
	                									+ mGroupTableAlias + "`.`office_id` end";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_OFFICE, 
	        							DataExportCoreTable.M_LOAN, sqlStatement, mOfficeTableAlias, 
	        							mLoanTableAlias);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					if (isSelectStatement) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	        							+  referencedColumnName + "` as `" + coreColumn.getLabel() + "`";
	        					
	    						// add the select statement
	    						sqlBuilder.SELECT(sqlStatement);
	    						
	    					} else if (filterValue != null) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	    								+ referencedColumnName + "` " + filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
		    			case GROUP_NAME:
	    				case GROUP_ID:
	    					mLoanTableAlias = DataExportCoreTable.M_LOAN.getAlias(referencedTableIndex++);
	    					mGroupTableAlias = DataExportCoreTable.M_GROUP.getAlias(referencedTableIndex++);
	    					mGroupClientTableAlias = DataExportCoreTable.M_GROUP_CLIENT.getAlias(referencedTableIndex++);
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_LOAN, 
	    							DataExportCoreTable.M_LOAN_REPAYMENT_SCHEDULE);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	        					sqlStatement = "`" + DataExportCoreTable.M_LOAN.getName() + "` `"
	        							+ mLoanTableAlias + "` on `" + mLoanTableAlias + "`.`id` = `"
												+ baseEntityName + "`.`loan_id`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_LOAN, 
	        							DataExportCoreTable.M_LOAN_REPAYMENT_SCHEDULE, sqlStatement, mLoanTableAlias, 
	        							baseEntityName);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_LOAN, 
	    							DataExportCoreTable.M_GROUP_CLIENT);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// m_loan and m_group_client table join
	        					sqlStatement = "`" + DataExportCoreTable.M_GROUP_CLIENT.getName()
	        							+ "` `" + mGroupClientTableAlias + "` on `" + mGroupClientTableAlias
	        									+ "`.`client_id` = `" + mLoanTableAlias + "`.`client_id`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_LOAN, 
	        							DataExportCoreTable.M_GROUP_CLIENT, sqlStatement, mLoanTableAlias, 
	        							mGroupClientTableAlias);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_GROUP, 
	    							DataExportCoreTable.M_GROUP_CLIENT);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// m_group_client and m_group table join
	        					sqlStatement = "`" + DataExportCoreTable.M_GROUP.getName() + "` `"
	                        			+ mGroupTableAlias + "` on `" + mGroupTableAlias + "`.`id` = case when "
	                							+ "isnull(`" + mLoanTableAlias + "`.`group_id`) then `"
	                									+ mGroupClientTableAlias + "`.`group_id` else `"
	                											+ mLoanTableAlias + "`.`group_id` end";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_GROUP, 
	        							DataExportCoreTable.M_GROUP_CLIENT, sqlStatement, mGroupTableAlias, 
	        							mGroupClientTableAlias);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					if (isSelectStatement) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	        							+  referencedColumnName + "` as `" + coreColumn.getLabel() + "`";
	        					
	    						// add the select statement
	    						sqlBuilder.SELECT(sqlStatement);
	    						
	    					} else if (filterValue != null) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	    								+ referencedColumnName + "` " + filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
		    			case LOAN_OFFICER_NAME:
	    				case CLIENT_NAME:
	    				case CLIENT_ID:
	    					mLoanTableAlias = DataExportCoreTable.M_LOAN.getAlias(referencedTableIndex++);
	    					parentTableAlias = referencedTable.getAlias(referencedTableIndex++);
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_LOAN, 
	    							DataExportCoreTable.M_LOAN_REPAYMENT_SCHEDULE);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						sqlStatement = "`" + DataExportCoreTable.M_LOAN.getName() + "` `"
	        							+ mLoanTableAlias + "` on `" + mLoanTableAlias + "`.`id` = `"
												+ baseEntityName + "`.`loan_id`";
	    						dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_LOAN, 
	        							DataExportCoreTable.M_LOAN_REPAYMENT_SCHEDULE, sqlStatement, mLoanTableAlias, 
	        							baseEntityName);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(referencedTable, 
	    							DataExportCoreTable.M_LOAN);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						sqlStatement = "`" + referencedTable.getName() + "` `" + parentTableAlias 
	                        			+ "` on `" + parentTableAlias + "`.`id` = `" + mLoanTableAlias + "`.`"
	                							+ foreignKeyIndexColumnName + "`";
	    						dataExportSqlJoin = DataExportSqlJoin.newInstance(referencedTable, 
	        							DataExportCoreTable.M_LOAN, sqlStatement, parentTableAlias, 
	        							mLoanTableAlias);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					if (isSelectStatement) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	        							+  referencedColumnName + "` as `" + coreColumn.getLabel() + "`";
	        					
	    						// add the select statement
	    						sqlBuilder.SELECT(sqlStatement);
	    						
	    					} else if (filterValue != null) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	    								+ referencedColumnName + "` " + filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
		    			case DATE_OF_BIRTH:
	    				case PHONE_NUMBER:
	    					mLoanTableAlias = DataExportCoreTable.M_LOAN.getAlias(referencedTableIndex++);
	    					mClientTableAlias = DataExportCoreTable.M_CLIENT.getAlias(referencedTableIndex++);
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_LOAN, 
	    							DataExportCoreTable.M_LOAN_REPAYMENT_SCHEDULE);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						sqlStatement = "`" + DataExportCoreTable.M_LOAN.getName() + "` `"
		    							+ mLoanTableAlias + "` on `" + mLoanTableAlias + "`.`id` = `"
												+ baseEntityName + "`.`loan_id`";
	    						dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_LOAN, 
	        							DataExportCoreTable.M_LOAN_REPAYMENT_SCHEDULE, sqlStatement, mLoanTableAlias, 
	        							baseEntityName);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_CLIENT, 
	    							DataExportCoreTable.M_LOAN);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						sqlStatement = "`" + DataExportCoreTable.M_CLIENT.getName() + "` `"
	        							+ mClientTableAlias + "` on `" + mClientTableAlias + "`.`id` = `"
												+ mLoanTableAlias + "`.`client_id`";
	    						dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_CLIENT, 
	        							DataExportCoreTable.M_LOAN, sqlStatement, mClientTableAlias, 
	        							mLoanTableAlias);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					if (isSelectStatement) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	        							+  coreColumn.getName() + "` as `" + coreColumn.getLabel() + "`";
	        					
	    						// add the select statement
	    						sqlBuilder.SELECT(sqlStatement);
	    						
	    					} else if (filterValue != null) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	    								+ coreColumn.getName() + "` " + filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
		    			case GENDER:
	    					mLoanTableAlias = DataExportCoreTable.M_LOAN.getAlias(referencedTableIndex++);
	    					mClientTableAlias = DataExportCoreTable.M_CLIENT.getAlias(referencedTableIndex++);
	    					mCodeValueTableAlias = referencedTable.getAlias(referencedTableIndex++);
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_LOAN, 
	    							DataExportCoreTable.M_LOAN_REPAYMENT_SCHEDULE);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						sqlStatement = "`" + DataExportCoreTable.M_LOAN.getName() + "` `"
	        							+ mLoanTableAlias + "` on `" + mLoanTableAlias + "`.`id` = `"
												+ baseEntityName + "`.`loan_id`";
	    						dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_LOAN, 
	        							DataExportCoreTable.M_LOAN_REPAYMENT_SCHEDULE, sqlStatement, mLoanTableAlias, 
	        							baseEntityName);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_CLIENT, 
	    							DataExportCoreTable.M_LOAN);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						sqlStatement = "`" + DataExportCoreTable.M_CLIENT.getName() + "` `"
	        							+ mClientTableAlias + "` on `" + mClientTableAlias + "`.`id` = `"
												+ mLoanTableAlias + "`.`client_id`";
	    						dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_CLIENT, 
	        							DataExportCoreTable.M_LOAN, sqlStatement, mClientTableAlias, 
	        							mLoanTableAlias);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(referencedTable, 
	    							DataExportCoreTable.M_CLIENT);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						sqlStatement = "`" + referencedTable.getName() + "` `" + mCodeValueTableAlias 
										+ "` on `" + mCodeValueTableAlias + "`.`id` = `" + mClientTableAlias 
	        	                        		+ "`.`" + coreColumn.getName() + "`";
	    						dataExportSqlJoin = DataExportSqlJoin.newInstance(referencedTable, 
	        							DataExportCoreTable.M_CLIENT, sqlStatement, mCodeValueTableAlias, 
	        							mClientTableAlias);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					if (isSelectStatement) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	        							+  referencedColumnName + "` as `" + coreColumn.getLabel() + "`";
	        					
	    						// add the select statement
	    						sqlBuilder.SELECT(sqlStatement);
	    						
	    					} else if (filterValue != null) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	    								+ referencedColumnName + "` " + filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
		    			case REPAYMENT_SCHEDULE_PRODUCT_SHORT_NAME:
	    				case REPAYMENT_SCHEDULE_PRODUCT_NAME:
	    				case REPAYMENT_SCHEDULE_PRODUCT_ID:
	    					mLoanTableAlias = DataExportCoreTable.M_LOAN.getAlias(referencedTableIndex++);
	    					mProductLoanTableAlias = referencedTable.getAlias(referencedTableIndex++);
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_LOAN, 
	    							DataExportCoreTable.M_LOAN_REPAYMENT_SCHEDULE);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						sqlStatement = "`" + DataExportCoreTable.M_LOAN.getName() + "` `"
	        							+ mLoanTableAlias + "` on `" + mLoanTableAlias + "`.`id` = `"
												+ baseEntityName + "`.`loan_id`";
	    						dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_LOAN, 
	        							DataExportCoreTable.M_LOAN_REPAYMENT_SCHEDULE, sqlStatement, mLoanTableAlias, 
	        							baseEntityName);
	    						
	    						sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(referencedTable, 
	    							DataExportCoreTable.M_LOAN);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						sqlStatement = "`" + referencedTable.getName() + "` `" + mProductLoanTableAlias 
	                        			+ "` on `" + mProductLoanTableAlias + "`.`id` = `" + mLoanTableAlias + "`.`"
	                							+ foreignKeyIndexColumnName + "`";
	    						dataExportSqlJoin = DataExportSqlJoin.newInstance(referencedTable, 
	        							DataExportCoreTable.M_LOAN, sqlStatement, mProductLoanTableAlias, 
	        							mLoanTableAlias);
	    						
	    						sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					if (isSelectStatement) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	    								+ referencedColumnName + "` as `" + coreColumn.getLabel() + "`";
	        					
	    						// add the select statement
	        					sqlBuilder.SELECT(sqlStatement);
	        					
	    					} else if (filterValue != null) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	    								+ referencedColumnName + "` " + filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    				default:
	    					// =============================================================================
	    					if (isSelectStatement) {
	    						// add the select statement
	        					sqlStatement = "NULL as `" + coreColumn.getLabel() + "`";
	        					
	        					sqlBuilder.SELECT(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    			}
	    			break;
				default:
					break;
	    	}
    	}
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
	public CommandProcessingResult updateDataExport(final Long id, final JsonCommand jsonCommand) {
		try {
			// retrieve entity from database
			final DataExport dataExport = this.dataExportRepository.findOne(id);

			// throw exception if entity not found
			if (dataExport == null) {
				throw new DataExportNotFoundException(id);
			}

			if(!jsonCommand.json().equals(dataExport.getUserRequestMap())) {
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

				dataExport.update(name, baseEntityName, jsonCommand.json(), dataSql);

				// save the new data export entity
				this.dataExportRepository.save(dataExport);
			}

			return new CommandProcessingResultBuilder()
					.withCommandId(jsonCommand.commandId())
					.withEntityId(dataExport.getId())
					.build();
		} catch (final DataIntegrityViolationException dve) {
			handleDataIntegrityIssues(jsonCommand, dve);

			return CommandProcessingResult.empty();
		}
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
