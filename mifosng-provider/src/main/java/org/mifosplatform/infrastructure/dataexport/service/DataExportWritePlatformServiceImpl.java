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
import org.apache.commons.lang.mutable.MutableInt;
import org.apache.commons.lang3.StringUtils;
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
     * @param registeredTableMetaDataRepository
     * @param exportDataValidator
     * @param fromJsonHelper
     * @param dataExportRepository
     * @param dataExportReadPlatformService
	 * @param dataSource
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
        final HashMap<String, DataExportSqlJoin> sqlJoinMap = new HashMap<>();
        final MutableInt aliasPostfixNumber = new MutableInt(0);
        
        // initialize the SQl statement builder class
        final SQL sqlBuilder = new SQL();
        
        for (EntityColumnMetaData column : selectedColumns) {
            this.addCoreColumnSqlToSqlBuilder(dataExportEntityData, column, sqlBuilder, sqlJoinMap, 
        			true, null, aliasPostfixNumber);
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
                
                if (coreDatatable != null) {
                	List<EntityColumnMetaData> columnsMetaData = DataExportUtils.getTableColumnsMetaData(
                			coreDatatable.getTableName(), this.jdbcTemplate);
                	
                	for (EntityColumnMetaData metaData : columnsMetaData) {
                		String fieldName = metaData.getName();
                		String fieldLabel = metaData.getLabel();
                		
                		if (coreDatatable.equals(DataExportCoreDatatable.GUARANTORS)) {
                			DataExportSqlJoin dataExportSqlJoin;
                    		String sqlStatement;
                    		
                    		// =============================================================================
        					String sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_CLIENT, 
        							DataExportCoreTable.M_GUARANTOR);
        					
        					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
        						// increment the alias postfix number
    	    					aliasPostfixNumber.increment();
                    			
                    			String mClientAlias = DataExportCoreTable.M_CLIENT.getAlias(aliasPostfixNumber.intValue());
        						
        						sqlStatement = "`" + DataExportCoreTable.M_CLIENT.getName() + "` `" + mClientAlias + "` on `"
                                        + mClientAlias + "`.`id` = `" + datatableName + "`.`entity_id`";
        						
        						dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_CLIENT, 
            							DataExportCoreTable.M_GUARANTOR, sqlStatement, mClientAlias, 
            							datatableName);
        						
        						sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
        						
        						// add the join to the map
            					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
        					}
        					// =============================================================================
        					
        					// =============================================================================
        					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_STAFF, 
        							DataExportCoreTable.M_GUARANTOR);
        					
        					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
        						// increment the alias postfix number
    	    					aliasPostfixNumber.increment();
    	    					
    	    					String mStaffAlias = DataExportCoreTable.M_STAFF.getAlias(aliasPostfixNumber.intValue());
        						
        						sqlStatement = "`" + DataExportCoreTable.M_STAFF.getName() + "` `" + mStaffAlias + "` on `"
                                        + mStaffAlias + "`.`id` = `" + datatableName + "`.`entity_id`";
        						
        						dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_STAFF, 
            							DataExportCoreTable.M_GUARANTOR, sqlStatement, mStaffAlias, 
            							datatableName);
        						
        						sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
        						
        						// add the join to the map
            					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
        					}
        					// =============================================================================
                			
                			String[] excludeFields = {
                				"loan_id", "address_line_2", "state", "zip", "mobile_number", "comment", "is_active"
                			};
                			
                			if (ArrayUtils.contains(excludeFields, fieldName)) {
                				// skip
                			} else if (fieldName.equalsIgnoreCase("entity_id")) {
                				String columnLabel = datatableDisplayName + " - client id";
                				
                				sqlBuilder.SELECT("case when `" + datatableName + "`.`type_enum`=1 then `"
                    					+ datatableName + "`.`" + fieldName + "` else NULL end as `" + columnLabel + "`");
                			} else if (fieldName.equalsIgnoreCase("type_enum")) {
                				String columnLabel = datatableDisplayName + " - type";
                				
                				sqlBuilder.SELECT("case when `" + datatableName + "`.`type_enum`=1 then 'client' when `"
                						+ datatableName + "`.`type_enum`=2 then 'staff' when `" + datatableName
                								+ "`.`type_enum`=3 then 'external' end as `" + columnLabel + "`");
                				
                			} else if (fieldName.equalsIgnoreCase("firstname") || fieldName.equalsIgnoreCase("lastname")) {
                				String columnLabel = datatableDisplayName + " - " + fieldName;
                				String clientSqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_CLIENT, 
            							DataExportCoreTable.M_GUARANTOR);
                				DataExportSqlJoin clientDataExportSqlJoin = sqlJoinMap.get(clientSqlJoinKey);
                				String staffSqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_STAFF, 
            							DataExportCoreTable.M_GUARANTOR);
                				DataExportSqlJoin staffDataExportSqlJoin = sqlJoinMap.get(staffSqlJoinKey);
                				
                				sqlBuilder.SELECT("case when `" + datatableName + "`.`type_enum`=1 then `"
                    					+ clientDataExportSqlJoin.getParentTableAlias() + "`.`" + fieldName + "` when `" + datatableName + "`.`type_enum`=2 "
                    							+ "then `" + staffDataExportSqlJoin.getParentTableAlias() + "`.`" + fieldName + "` else `"
                    									+ datatableName + "`.`" + fieldName + "` end as `" + columnLabel + "`");
                    		} else {
                    			String columnLabel = datatableDisplayName + " - " + fieldLabel;
                    			
                            	sqlBuilder.SELECT("`" + datatableName + "`.`" + fieldName + "` as `" + columnLabel + "`");
                    		}
                		} else if (coreDatatable.equals(DataExportCoreDatatable.LOAN_COLLATERALS)) {
                			if (fieldName.equalsIgnoreCase(baseEntityReferenceColumn)) { 
                            	// skip
                            } else {
                            	String columnLabel = datatableDisplayName + " - " + fieldLabel;
                            	
                            	sqlBuilder.SELECT("`" + datatableName + "`.`" + fieldName + "` as `" + columnLabel + "`");
                    		}
                		} else if (fieldName.equalsIgnoreCase("charge_id")) {
                			DataExportSqlJoin dataExportSqlJoin;
                    		String sqlStatement;
                    		
                    		// =============================================================================
        					String sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_CHARGE.getName(), 
        							coreDatatable.getTableName());
        					
        					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
        						// increment the alias postfix number
    	    					aliasPostfixNumber.increment();
        						
        						String mChargeAlias = DataExportCoreTable.M_CHARGE.getAlias(aliasPostfixNumber.intValue());
        						
        						sqlStatement = "`" + DataExportCoreTable.M_CHARGE.getName() + "` `" + mChargeAlias + "` on `"
                                        + mChargeAlias + "`.`id` = `" + datatableName + "`.`" + fieldName + "`";
        						
        						dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_CHARGE, 
        								datatableName, sqlStatement, mChargeAlias, datatableName);
        						
        						sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
        						
        						// add the join to the map
            					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
        					}
        					// =============================================================================
        					
        					// =============================================================================
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`id` as `" 
	    							+ datatableDisplayName + " - charge id`, `" + dataExportSqlJoin.getParentTableAlias()
                            		+ "`.`name` as `" + datatableDisplayName + " - charge name`";
        					
    						// add the select statement
    						sqlBuilder.SELECT(sqlStatement);
	    					// =============================================================================
                		} else if (fieldName.equalsIgnoreCase("id")) {
                			String columnLabel = datatableDisplayName + " - " + fieldLabel;
                			
                			sqlBuilder.SELECT("`" + datatableName + "`.`" + fieldName + "` as `" + columnLabel + "`");
                		} else if (fieldName.equalsIgnoreCase(baseEntityReferenceColumn)) { 
                        	// skip
                        } else {
                        	String columnLabel = datatableDisplayName + " - " + fieldLabel;
                        	
                        	sqlBuilder.SELECT("`" + datatableName + "`.`" + fieldName + "` as `" + columnLabel + "`");
                		}
                	}
                	
                } else {
                	for (RegisteredTableMetaData metaData : registeredTablesMetaData) {
                        String fieldName = metaData.getFieldName();

                        if (fieldName.equalsIgnoreCase("submittedon_date") || fieldName.equalsIgnoreCase("submittedon_userid")) {
                        	// skip
                        } else if (fieldName.contains("userid") || fieldName.endsWith("_by")) {
                        	// skip
                        } else if (fieldName.equalsIgnoreCase(baseEntityReferenceColumn)) { 
                        	// skip
                        } else {
                        	String columnLabel = datatableDisplayName + " - " + metaData.getLabelName();
                        	
                        	sqlBuilder.SELECT("`" + datatableName + "`.`" + metaData.getFieldName() + "` as `"
                        			+ columnLabel + "`");
                        }
                    }
                }
            }
        }
        
        sqlBuilder.FROM("`" + baseEntityTableName + "` as `" + baseEntityName + "`");
        
        final Iterator<Entry<EntityColumnMetaData, String>> filterEntries = selectedFilters.entrySet().iterator();
        
        while (filterEntries.hasNext()) {
            Entry<EntityColumnMetaData, String> filterEntry = filterEntries.next();
            EntityColumnMetaData columnMetaData = filterEntry.getKey();
            String filterValue = filterEntry.getValue();
            
            this.addCoreColumnSqlToSqlBuilder(dataExportEntityData, columnMetaData, sqlBuilder, sqlJoinMap, 
        			false, filterValue, aliasPostfixNumber);
        }
        
        return sqlBuilder.toString();
    }
    
    /**
     * Adds a join statements, select statements or where clauses
     * This method prevents the addition of duplicate join statements
     * 
     * @param dataExportEntityData
     * @param aliasPostfixNumber
     * @param sqlBuilder
     * @param sqlJoinMap
     * @param isSelectStatement
     * @param filterValue
     */
    private void addCoreColumnSqlToSqlBuilder(final DataExportEntityData dataExportEntityData,
    		final EntityColumnMetaData columnMetaData, final SQL sqlBuilder, 
    		final HashMap<String, DataExportSqlJoin> sqlJoinMap, final boolean isSelectStatement, 
    		final String filterValue, final MutableInt aliasPostfixNumber) {
    	final String columnName = columnMetaData.getName();
    	final String columnLabel = columnMetaData.getLabel();
    	final DataExportCoreColumn coreColumn = DataExportCoreColumn.newInstanceFromName(columnName);
    	final String baseEntityName = dataExportEntityData.getEntityName();
        final DataExportBaseEntity baseEntity = DataExportBaseEntity.fromEntityName(baseEntityName);
        final DataExportCoreTable baseEntityCoreTable = DataExportCoreTable.newInstance(baseEntity.getTableName());
    	
    	// variables initialized with null values
    	String sqlStatement, mClientTableAlias, mGroupTableAlias, mGroupClientTableAlias, 
    	mOfficeTableAlias, mStaffTableAlias, mLoanTableAlias, mProductLoanTableAlias, 
    	mPaymentDetailTableAlias, mSavingsProductTableAlias, sqlJoinKey, parentTableAlias, 
    	mSavingsAccountTableAlias, mAppUserTableAlias;
    	DataExportSqlJoin dataExportSqlJoin;
    	
    	if (coreColumn != null) {
            String referencedTableName = coreColumn.getReferencedTableName();
        	DataExportCoreTable referencedTable = DataExportCoreTable.newInstance(referencedTableName);
        	String referencedColumnName = coreColumn.getReferencedColumnName();
        	String foreignKeyIndexColumnName = coreColumn.getForeignKeyIndexColumnName();
        	
        	switch (baseEntity) {
	    		case CLIENT:
	    			switch (coreColumn) {
	    				case GROUP_NAME:
	    				case GROUP_ID:
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_CLIENT, 
	    							DataExportCoreTable.M_GROUP_CLIENT);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mGroupClientTableAlias = DataExportCoreTable.M_GROUP_CLIENT.
		    							getAlias(aliasPostfixNumber.intValue());
	    						
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
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_GROUP, 
	    							DataExportCoreTable.M_GROUP_CLIENT);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mGroupTableAlias = DataExportCoreTable.M_GROUP.getAlias(aliasPostfixNumber.intValue());
	    						
	    						// m_group_client and m_group table join
	        					sqlStatement = "`" + DataExportCoreTable.M_GROUP.getName() + "` `"
	                        			+ mGroupTableAlias + "` on `" + mGroupTableAlias + "`.`id` = `"
	                							+ dataExportSqlJoin.getChildTableAlias() + "`.`group_id`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_GROUP, 
	        							DataExportCoreTable.M_GROUP_CLIENT, sqlStatement, mGroupTableAlias, 
	        							dataExportSqlJoin.getChildTableAlias());
	        					
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
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(referencedTable, 
	    							DataExportCoreTable.M_CLIENT);
	    					
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					parentTableAlias = referencedTable.getAlias(aliasPostfixNumber.intValue());
	    						
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
	    				case GENDER:
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
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(referencedTable, 
	    							DataExportCoreTable.M_GROUP);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					parentTableAlias = referencedTable.getAlias(aliasPostfixNumber.intValue());
	    						
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
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(referencedTable, 
	    							DataExportCoreTable.M_LOAN);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					parentTableAlias = referencedTable.getAlias(aliasPostfixNumber.intValue());
	    						
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
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_LOAN, 
	    							DataExportCoreTable.M_GROUP_CLIENT);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mGroupClientTableAlias = DataExportCoreTable.M_GROUP_CLIENT.getAlias(
		    							aliasPostfixNumber.intValue());
	    						
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
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_GROUP, 
	    							DataExportCoreTable.M_GROUP_CLIENT);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mGroupTableAlias = DataExportCoreTable.M_GROUP.getAlias(aliasPostfixNumber.intValue());
	    						
	    						// m_group_client and m_group table join
	        					sqlStatement = "`" + DataExportCoreTable.M_GROUP.getName() + "` `"
	                        			+ mGroupTableAlias + "` on `" + mGroupTableAlias + "`.`id` = case when "
	                							+ "isnull(`" + baseEntityName + "`.`group_id`) then `"
	                									+ dataExportSqlJoin.getChildTableAlias() + "`.`group_id` else `"
	                											+ baseEntityName + "`.`group_id` end";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_GROUP, 
	        							DataExportCoreTable.M_GROUP_CLIENT, sqlStatement, mGroupTableAlias, 
	        							dataExportSqlJoin.getChildTableAlias());
	        					
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
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_CLIENT, 
	    							DataExportCoreTable.M_LOAN);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mClientTableAlias = DataExportCoreTable.M_CLIENT.getAlias(aliasPostfixNumber.intValue());
	    						
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
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mGroupTableAlias = DataExportCoreTable.M_GROUP.getAlias(aliasPostfixNumber.intValue());
	    						
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
	    						sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_CLIENT, 
		    							DataExportCoreTable.M_LOAN);
	    						
	    						DataExportSqlJoin clientLoanSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    						
	    						sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_GROUP, 
		    							DataExportCoreTable.M_LOAN);
	    						
	    						DataExportSqlJoin groupLoanSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    						
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mOfficeTableAlias = DataExportCoreTable.M_OFFICE.getAlias(aliasPostfixNumber.intValue());
	    						
	    						// m_office and m_client/m_group table join
	        					sqlStatement = "`" + DataExportCoreTable.M_OFFICE.getName() + "` `"
	                        			+ mOfficeTableAlias + "` on `" + mOfficeTableAlias + "`.`id` = case when "
	                					+ "isnull(`" + baseEntityName + "`.`group_id`) then `"
	                							+ clientLoanSqlJoin.getParentTableAlias() + "`.`office_id` else `"
	                									+ groupLoanSqlJoin.getParentTableAlias() + "`.`office_id` end";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_OFFICE, 
	        							DataExportCoreTable.M_LOAN, sqlStatement, mOfficeTableAlias, 
	        							baseEntityName);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_OFFICE, 
	    							DataExportCoreTable.M_LOAN);
	    					
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
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_CLIENT, 
	    							DataExportCoreTable.M_LOAN);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mClientTableAlias = DataExportCoreTable.M_CLIENT.getAlias(aliasPostfixNumber.intValue());
	    						
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
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mGroupTableAlias = DataExportCoreTable.M_GROUP.getAlias(aliasPostfixNumber.intValue());
	    						
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
	    						sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_CLIENT, 
		    							DataExportCoreTable.M_LOAN);
	    						
	    						DataExportSqlJoin clientLoanSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    						
	    						sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_GROUP, 
		    							DataExportCoreTable.M_LOAN);
	    						
	    						DataExportSqlJoin groupLoanSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    						
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mStaffTableAlias = DataExportCoreTable.M_STAFF.getAlias(aliasPostfixNumber.intValue());
	    						
	    						// m_staff and m_client/m_group table join
	        					sqlStatement = "`" + DataExportCoreTable.M_STAFF.getName() + "` `"
	                    			+ mStaffTableAlias + "` on `" + mStaffTableAlias + "`.`id` = case when "
	            					+ "isnull(`" + baseEntityName + "`.`group_id`) then `"
	            							+ clientLoanSqlJoin.getParentTableAlias() + "`.`staff_id` else `"
	            									+ groupLoanSqlJoin.getParentTableAlias() + "`.`staff_id` end";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_STAFF, 
	        							DataExportCoreTable.M_LOAN, sqlStatement, mStaffTableAlias, 
	        							baseEntityName);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	                        
	                        // =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_STAFF, 
	    							DataExportCoreTable.M_LOAN);
	    					
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
	    				case GENDER:
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_CLIENT, 
	    							DataExportCoreTable.M_LOAN);
	    					
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mClientTableAlias = DataExportCoreTable.M_CLIENT.getAlias(aliasPostfixNumber.intValue());
	    						
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
						case LOAN_ARREARS_AMOUNT:
						case LOAN_ARREARS_DATE:
							// =============================================================================
							sqlJoinKey = DataExportSqlJoin.createId(referencedTable,
									DataExportCoreTable.M_LOAN);

							// only add the join statement if it hasn't been previously added
							if (!sqlJoinMap.containsKey(sqlJoinKey)) {
								// increment the alias postfix number
								aliasPostfixNumber.increment();

								parentTableAlias = referencedTable.getAlias(aliasPostfixNumber.intValue());

								// m_client/m_staff and m_loan
								sqlStatement = "`" + referencedTable.getName() + "` `" + parentTableAlias
										+ "` on `" + parentTableAlias + "`.`loan_id` = `" + baseEntityName + "`.`"
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
						case LOAN_ARREARS_DAYS:
							// =============================================================================
							sqlJoinKey = DataExportSqlJoin.createId(referencedTable,
									DataExportCoreTable.M_LOAN);

							// only add the join statement if it hasn't been previously added
							if (!sqlJoinMap.containsKey(sqlJoinKey)) {
								// increment the alias postfix number
								aliasPostfixNumber.increment();

								parentTableAlias = referencedTable.getAlias(aliasPostfixNumber.intValue());

								// m_client/m_staff and m_loan
								sqlStatement = "`" + referencedTable.getName() + "` `" + parentTableAlias
										+ "` on `" + parentTableAlias + "`.`loan_id` = `" + baseEntityName + "`.`"
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
								sqlStatement = "DATEDIFF(CURDATE(), `" + dataExportSqlJoin.getParentTableAlias() + "`.`"
										+ referencedColumnName + "`) as `" + coreColumn.getLabel() + "`";

								// add the select statement
								sqlBuilder.SELECT(sqlStatement);

							} else if (filterValue != null) {
								sqlStatement = "DATEDIFF(CURDATE(), `" + dataExportSqlJoin.getParentTableAlias() + "`.`"
										+ referencedColumnName + "`) " + filterValue;

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
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(referencedTable, 
	    							DataExportCoreTable.M_SAVINGS_ACCOUNT);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					parentTableAlias = referencedTable.getAlias(aliasPostfixNumber.intValue());
	    						
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
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_SAVINGS_ACCOUNT, 
	    							DataExportCoreTable.M_GROUP_CLIENT);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mGroupClientTableAlias = DataExportCoreTable.M_GROUP_CLIENT.getAlias(
		    							aliasPostfixNumber.intValue());
	    						
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
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_GROUP, 
	    							DataExportCoreTable.M_GROUP_CLIENT);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mGroupTableAlias = DataExportCoreTable.M_GROUP.getAlias(aliasPostfixNumber.intValue());
	    						
	    						// m_group_client and m_group table join
	        					sqlStatement = "`" + DataExportCoreTable.M_GROUP.getName() + "` `"
	                        			+ mGroupTableAlias + "` on `" + mGroupTableAlias + "`.`id` = case when "
	                							+ "isnull(`" + baseEntityName + "`.`group_id`) then `"
	                									+ dataExportSqlJoin.getChildTableAlias() + "`.`group_id` else `"
	                											+ baseEntityName + "`.`group_id` end";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_GROUP, 
	        							DataExportCoreTable.M_GROUP_CLIENT, sqlStatement, mGroupTableAlias, 
	        							dataExportSqlJoin.getChildTableAlias());
	        					
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
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_CLIENT, 
	    							DataExportCoreTable.M_SAVINGS_ACCOUNT);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mClientTableAlias = DataExportCoreTable.M_CLIENT.getAlias(aliasPostfixNumber.intValue());
	    						
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
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mGroupTableAlias = DataExportCoreTable.M_GROUP.getAlias(aliasPostfixNumber.intValue());
	    						
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
	    						sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_CLIENT, 
		    							DataExportCoreTable.M_SAVINGS_ACCOUNT);
	    						
	    						DataExportSqlJoin clientSavingsAccount = sqlJoinMap.get(sqlJoinKey);
	    						
	    						sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_GROUP, 
		    							DataExportCoreTable.M_SAVINGS_ACCOUNT);
	    						
	    						DataExportSqlJoin groupSavingsAccount = sqlJoinMap.get(sqlJoinKey);
	    						
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mOfficeTableAlias = DataExportCoreTable.M_OFFICE.getAlias(aliasPostfixNumber.intValue());
	    						
	    						// m_office and m_client/m_group table join
	        					sqlStatement = "`" + DataExportCoreTable.M_OFFICE.getName() + "` `"
	                        			+ mOfficeTableAlias + "` on `" + mOfficeTableAlias + "`.`id` = case when "
	                					+ "isnull(`" + baseEntityName + "`.`group_id`) then `"
	                							+ clientSavingsAccount.getParentTableAlias() + "`.`office_id` else `"
	                									+ groupSavingsAccount.getParentTableAlias() + "`.`office_id` end";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_OFFICE, 
	        							DataExportCoreTable.M_SAVINGS_ACCOUNT, sqlStatement, mOfficeTableAlias, 
	        							baseEntityName);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_OFFICE, 
	    							DataExportCoreTable.M_SAVINGS_ACCOUNT);
	    					
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
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_CLIENT, 
	    							DataExportCoreTable.M_SAVINGS_ACCOUNT);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mClientTableAlias = DataExportCoreTable.M_CLIENT.getAlias(aliasPostfixNumber.intValue());
	    						
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
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mGroupTableAlias = DataExportCoreTable.M_GROUP.getAlias(aliasPostfixNumber.intValue());
	    						
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
	    						sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_CLIENT, 
		    							DataExportCoreTable.M_SAVINGS_ACCOUNT);
	    						
	    						DataExportSqlJoin clientSavingsAccount = sqlJoinMap.get(sqlJoinKey);
	    						
	    						sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_GROUP, 
		    							DataExportCoreTable.M_SAVINGS_ACCOUNT);
	    						
	    						DataExportSqlJoin groupSavingsAccount = sqlJoinMap.get(sqlJoinKey);
	    						
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mStaffTableAlias = DataExportCoreTable.M_STAFF.getAlias(aliasPostfixNumber.intValue());
	    						
	    						// m_staff and m_client/m_group table join
	        					sqlStatement = "`" + DataExportCoreTable.M_STAFF.getName() + "` `"
	                    			+ mStaffTableAlias + "` on `" + mStaffTableAlias + "`.`id` = case when "
	            					+ "isnull(`" + baseEntityName + "`.`group_id`) then `"
	            							+ clientSavingsAccount.getParentTableAlias() + "`.`staff_id` else `"
	            									+ groupSavingsAccount.getParentTableAlias() + "`.`staff_id` end";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_STAFF, 
	        							DataExportCoreTable.M_SAVINGS_ACCOUNT, sqlStatement, mStaffTableAlias, 
	        							baseEntityName);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	                        
	                        // =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_STAFF, 
	    							DataExportCoreTable.M_SAVINGS_ACCOUNT);
	    					
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
	    				case GENDER:
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_CLIENT, 
	    							DataExportCoreTable.M_SAVINGS_ACCOUNT);
	    					
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mClientTableAlias = DataExportCoreTable.M_CLIENT.getAlias(aliasPostfixNumber.intValue());
	    						
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
	    				case LOAN_TRANSACTION_TOTAL_REPAID:
	    					// =============================================================================
	    					if (isSelectStatement) {
	    						sqlStatement = "case when `" + baseEntityName + "`.`amount` "
	        							+ "is not null and `" + baseEntityName + "`.`transaction_type_enum` = "
										+ LoanTransactionType.REPAYMENT.getValue() + " then `" + baseEntityName 
												+ "`.`amount` else NULL end as `"
														+ coreColumn.getLabel() + "`";
	    						
	    						// add the select statement
	        					sqlBuilder.SELECT(sqlStatement);
	    						
	    					} else if (filterValue != null) {
	    						sqlStatement = "case when `" + baseEntityName + "`.`amount` "
	        							+ "is not null and `" + baseEntityName + "`.`transaction_type_enum` = "
										+ LoanTransactionType.REPAYMENT.getValue() + " then `" + baseEntityName 
												+ "`.`amount` else NULL end "
														+ filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    				case LOAN_TRANSACTION_PRINCIPAL_REPAID:
	    				case LOAN_TRANSACTION_INTEREST_REPAID:
	    				case LOAN_TRANSACTION_FEES_REPAID:
	    				case LOAN_TRANSACTION_PENALTIES_REPAID:
	    				case LOAN_TRANSACTION_OVERPAYMENT_REPAID:
	    					// =============================================================================
	    					if (isSelectStatement) {
	    						sqlStatement = "case when `" + baseEntityName + "`.`" + coreColumn.getName() + "` "
	        							+ "is not null and `" + baseEntityName + "`.`transaction_type_enum` = "
										+ LoanTransactionType.REPAYMENT.getValue() + " then `" + baseEntityName 
												+ "`.`" + coreColumn.getName() + "` else NULL end as `"
														+ coreColumn.getLabel() + "`";
	    						
	    						// add the select statement
	        					sqlBuilder.SELECT(sqlStatement);
	    						
	    					} else if (filterValue != null) {
	    						sqlStatement = "case when `" + baseEntityName + "`.`" + coreColumn.getName() + "` "
	        							+ "is not null and `" + baseEntityName + "`.`transaction_type_enum` = "
										+ LoanTransactionType.REPAYMENT.getValue() + " then `" + baseEntityName 
												+ "`.`" + coreColumn.getName() + "` else NULL end "
														+ filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
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
	    				case LOAN_TRANSACTION_TOTAL_RECOVERED:
	    					// =============================================================================
	    					if (isSelectStatement) {
	    						sqlStatement = "case when `" + baseEntityName + "`.`transaction_type_enum` = "
										+ LoanTransactionType.RECOVERY_REPAYMENT.getValue() + " then `" 
												+ baseEntityName + "`.`amount` else NULL end as `"
														+ coreColumn.getLabel() + "`";
	    						
	    						// add the select statement
	        					sqlBuilder.SELECT(sqlStatement);
	    						
	    					} else if (filterValue != null) {
	    						sqlStatement = "case when `" + baseEntityName + "`.`transaction_type_enum` = "
										+ LoanTransactionType.RECOVERY_REPAYMENT.getValue() + " then `" 
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
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_LOAN, 
	    							DataExportCoreTable.M_LOAN_TRANSACTION);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mLoanTableAlias = DataExportCoreTable.M_LOAN.getAlias(aliasPostfixNumber.intValue());
	    						
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
	    					
	    					sqlJoinKey = DataExportSqlJoin.createId(referencedTable, 
	    							DataExportCoreTable.M_LOAN);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mProductLoanTableAlias = referencedTable.getAlias(aliasPostfixNumber.intValue());
	    						
	    						sqlStatement = "`" + referencedTable.getName() + "` `" + mProductLoanTableAlias 
	                        			+ "` on `" + mProductLoanTableAlias + "`.`id` = `" + dataExportSqlJoin.getParentTableAlias() + "`.`"
	                							+ foreignKeyIndexColumnName + "`";
	    						dataExportSqlJoin = DataExportSqlJoin.newInstance(referencedTable, 
	        							DataExportCoreTable.M_LOAN, sqlStatement, mProductLoanTableAlias, 
	        							dataExportSqlJoin.getParentTableAlias());
	    						
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
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_LOAN, 
	    							DataExportCoreTable.M_LOAN_TRANSACTION);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mLoanTableAlias = DataExportCoreTable.M_LOAN.getAlias(aliasPostfixNumber.intValue());
	    						
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
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_PAYMENT_DETAIL, 
	    							DataExportCoreTable.M_LOAN_TRANSACTION);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mPaymentDetailTableAlias = DataExportCoreTable.M_PAYMENT_DETAIL.getAlias(aliasPostfixNumber.intValue());
	    						
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
	    					
	    					sqlJoinKey = DataExportSqlJoin.createId(referencedTable, 
	    							DataExportCoreTable.M_PAYMENT_DETAIL);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					parentTableAlias = referencedTable.getAlias(aliasPostfixNumber.intValue());
	    						
	    						sqlStatement = "`" + referencedTable.getName() + "` `" + parentTableAlias 
	                        			+ "` on `" + parentTableAlias + "`.`id` = `" + dataExportSqlJoin.getParentTableAlias() + "`.`"
	                							+ foreignKeyIndexColumnName + "`";
	    						dataExportSqlJoin = DataExportSqlJoin.newInstance(referencedTable, 
	        							DataExportCoreTable.M_PAYMENT_DETAIL, sqlStatement, parentTableAlias, 
	        							dataExportSqlJoin.getParentTableAlias());
	        					
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
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_PAYMENT_DETAIL, 
	    							DataExportCoreTable.M_LOAN_TRANSACTION);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mPaymentDetailTableAlias = DataExportCoreTable.M_PAYMENT_DETAIL.getAlias(aliasPostfixNumber.intValue());
	    						
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
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_LOAN, 
	    							DataExportCoreTable.M_LOAN_TRANSACTION);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mLoanTableAlias = DataExportCoreTable.M_LOAN.getAlias(aliasPostfixNumber.intValue());
	    						
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
	    					
	    					sqlJoinKey = DataExportSqlJoin.createId(referencedTable, 
	    							DataExportCoreTable.M_LOAN);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					parentTableAlias = referencedTable.getAlias(aliasPostfixNumber.intValue());
	    						
	    						sqlStatement = "`" + referencedTable.getName() + "` `" + parentTableAlias 
	                        			+ "` on `" + parentTableAlias + "`.`id` = `" + dataExportSqlJoin.getParentTableAlias() + "`.`"
	                							+ foreignKeyIndexColumnName + "`";
	    						dataExportSqlJoin = DataExportSqlJoin.newInstance(referencedTable, 
	        							DataExportCoreTable.M_LOAN, sqlStatement, parentTableAlias, 
	        							dataExportSqlJoin.getParentTableAlias());
	        					
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
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_LOAN, 
	    							DataExportCoreTable.M_LOAN_TRANSACTION);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mLoanTableAlias = DataExportCoreTable.M_LOAN.getAlias(aliasPostfixNumber.intValue());
	    						
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
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_LOAN, 
	    							DataExportCoreTable.M_GROUP_CLIENT);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mGroupClientTableAlias = DataExportCoreTable.M_GROUP_CLIENT.getAlias(aliasPostfixNumber.intValue());
		    					
		    					// m_loan and m_group_client table join
	        					sqlStatement = "`" + DataExportCoreTable.M_GROUP_CLIENT.getName()
	        							+ "` `" + mGroupClientTableAlias + "` on `" + mGroupClientTableAlias
	        									+ "`.`client_id` = `" + dataExportSqlJoin.getParentTableAlias() + "`.`client_id`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_LOAN, 
	        							DataExportCoreTable.M_GROUP_CLIENT, sqlStatement, dataExportSqlJoin.getParentTableAlias(), 
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
	    						sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_LOAN, 
		    							DataExportCoreTable.M_LOAN_TRANSACTION);
	    						
	    						DataExportSqlJoin loanTransactionSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    						
	    						sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_LOAN, 
		    							DataExportCoreTable.M_GROUP_CLIENT);
	    						
	    						DataExportSqlJoin loanGroupClientSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    						
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mGroupTableAlias = DataExportCoreTable.M_GROUP.getAlias(aliasPostfixNumber.intValue());
	    						
	    						// m_group_client and m_group table join
	        					sqlStatement = "`" + DataExportCoreTable.M_GROUP.getName() + "` `"
	                        			+ mGroupTableAlias + "` on `" + mGroupTableAlias + "`.`id` = case when "
	                							+ "isnull(`" + loanTransactionSqlJoin.getParentTableAlias() + "`.`group_id`) then `"
	                									+ loanGroupClientSqlJoin.getChildTableAlias() + "`.`group_id` else `"
	                											+ loanTransactionSqlJoin.getParentTableAlias() + "`.`group_id` end";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_GROUP, 
	        							DataExportCoreTable.M_GROUP_CLIENT, sqlStatement, mGroupTableAlias, 
	        							loanGroupClientSqlJoin.getChildTableAlias());
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_GROUP, 
	    							DataExportCoreTable.M_GROUP_CLIENT);
	    					
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
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_OFFICE, 
	    							DataExportCoreTable.M_LOAN_TRANSACTION);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mOfficeTableAlias = DataExportCoreTable.M_OFFICE.getAlias(aliasPostfixNumber.intValue());
	    						
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
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_LOAN, 
	    							DataExportCoreTable.M_LOAN_TRANSACTION);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mLoanTableAlias = DataExportCoreTable.M_LOAN.getAlias(aliasPostfixNumber.intValue());
	    						
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
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_CLIENT, 
	    							DataExportCoreTable.M_LOAN);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mClientTableAlias = DataExportCoreTable.M_CLIENT.getAlias(aliasPostfixNumber.intValue());
	    						
	    						// m_loan and m_group_client table join
	        					sqlStatement = "`" + DataExportCoreTable.M_CLIENT.getName() + "` `"
	        							+ mClientTableAlias + "` on `" + mClientTableAlias + "`.`id` = `"
												+ dataExportSqlJoin.getParentTableAlias() + "`.`client_id`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_CLIENT, 
	        							DataExportCoreTable.M_LOAN, sqlStatement, mClientTableAlias, 
	        							dataExportSqlJoin.getParentTableAlias());
	        					
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
	    						sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_LOAN, 
		    							DataExportCoreTable.M_LOAN_TRANSACTION);
	    						
	    						DataExportSqlJoin loanTransactionSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    						
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mGroupTableAlias = DataExportCoreTable.M_GROUP.getAlias(aliasPostfixNumber.intValue());
	    						
	    						// m_loan and m_group_client table join
	        					sqlStatement = "`" + DataExportCoreTable.M_GROUP.getName() + "` `"
	                        			+ mGroupTableAlias + "` on `" + mGroupTableAlias + "`.`id` = `"
	                							+ loanTransactionSqlJoin.getParentTableAlias() + "`.`group_id`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_GROUP, 
	        							DataExportCoreTable.M_LOAN, sqlStatement, mGroupTableAlias, 
	        							loanTransactionSqlJoin.getParentTableAlias());
	        					
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
	    						sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_LOAN, 
		    							DataExportCoreTable.M_LOAN_TRANSACTION);
	    						
	    						DataExportSqlJoin loanTransactionSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    						
	    						sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_CLIENT, 
		    							DataExportCoreTable.M_LOAN);
	    						
	    						DataExportSqlJoin clientLoanSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    						
	    						sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_GROUP, 
		    							DataExportCoreTable.M_LOAN);
	    						
	    						DataExportSqlJoin groupLoanSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    						
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mStaffTableAlias = DataExportCoreTable.M_STAFF.getAlias(aliasPostfixNumber.intValue());
	    						
	    						// m_group_client and m_group table join
	        					sqlStatement = "`" + DataExportCoreTable.M_STAFF.getName() + "` `"
	                        			+ mStaffTableAlias + "` on `" + mStaffTableAlias + "`.`id` = case when "
	                					+ "isnull(`" + loanTransactionSqlJoin.getParentTableAlias() + "`.`group_id`) then `"
	                							+ clientLoanSqlJoin.getParentTableAlias() + "`.`staff_id` else `"
	                									+ groupLoanSqlJoin.getParentTableAlias() + "`.`staff_id` end";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_STAFF, 
	        							DataExportCoreTable.M_LOAN, sqlStatement, mStaffTableAlias, 
	        							loanTransactionSqlJoin.getParentTableAlias());
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_STAFF, 
	    							DataExportCoreTable.M_LOAN);
	    					
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
	    				case GENDER:
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_LOAN, 
	    							DataExportCoreTable.M_LOAN_TRANSACTION);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mLoanTableAlias = DataExportCoreTable.M_LOAN.getAlias(aliasPostfixNumber.intValue());
	    						
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
	    					
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_CLIENT, 
	    							DataExportCoreTable.M_LOAN);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mClientTableAlias = DataExportCoreTable.M_CLIENT.getAlias(aliasPostfixNumber.intValue());
	    						
	    						sqlStatement = "`" + DataExportCoreTable.M_CLIENT.getName() + "` `"
	        							+ mClientTableAlias + "` on `" + mClientTableAlias + "`.`id` = `"
												+ dataExportSqlJoin.getParentTableAlias() + "`.`client_id`";
	    						dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_CLIENT, 
	        							DataExportCoreTable.M_LOAN, sqlStatement, mClientTableAlias, 
	        							dataExportSqlJoin.getParentTableAlias());
	        					
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
						case LOAN_TRANSACTION_CREATED_BY:
							// =============================================================================
							sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_APP_USER,
									DataExportCoreTable.M_LOAN_TRANSACTION);

							// only add the join statement if it hasn't been previously added
							if (!sqlJoinMap.containsKey(sqlJoinKey)) {
								// increment the alias postfix number
								aliasPostfixNumber.increment();

								mAppUserTableAlias = DataExportCoreTable.M_APP_USER.getAlias(aliasPostfixNumber.intValue());

								sqlStatement = "`" + DataExportCoreTable.M_APP_USER.getName() + "` `"
										+ mAppUserTableAlias + "` on `" + mAppUserTableAlias + "`.`id` = `"
										+ baseEntityName + "`.`" + coreColumn.getForeignKeyIndexColumnName() + "` ";
								dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_APP_USER,
										DataExportCoreTable.M_LOAN_TRANSACTION, sqlStatement, mAppUserTableAlias,
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
						case SAVINGS_TRANSACTION_INTEREST_POSTING:
							// =============================================================================
							if (isSelectStatement) {
								sqlStatement = "case when `" + baseEntityName + "`.`transaction_type_enum` = "
										+ SavingsAccountTransactionType.INTEREST_POSTING.getValue() + " then `"
										+ baseEntityName + "`.`amount` else NULL end as `"
										+ coreColumn.getLabel() + "`";

								// add the select statement
								sqlBuilder.SELECT(sqlStatement);

							} else if (filterValue != null) {
								sqlStatement = "case when `" + baseEntityName + "`.`transaction_type_enum` = "
										+ SavingsAccountTransactionType.INTEREST_POSTING.getValue() + " then `"
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
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_SAVINGS_ACCOUNT, 
	    							DataExportCoreTable.M_SAVINGS_ACCOUNT_TRANSACTION);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mSavingsAccountTableAlias = DataExportCoreTable.M_SAVINGS_ACCOUNT.getAlias(aliasPostfixNumber.intValue());
	    						
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
	    					
	    					sqlJoinKey = DataExportSqlJoin.createId(referencedTable, 
	    							DataExportCoreTable.M_SAVINGS_ACCOUNT);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mSavingsProductTableAlias = referencedTable.getAlias(aliasPostfixNumber.intValue());
	    						
	    						sqlStatement = "`" + referencedTable.getName() + "` `" + mSavingsProductTableAlias 
	                        			+ "` on `" + mSavingsProductTableAlias + "`.`id` = `" + dataExportSqlJoin.getParentTableAlias() + "`.`"
	                							+ foreignKeyIndexColumnName + "`";
	    						dataExportSqlJoin = DataExportSqlJoin.newInstance(referencedTable, 
	        							DataExportCoreTable.M_SAVINGS_ACCOUNT, sqlStatement, mSavingsProductTableAlias, 
	        							dataExportSqlJoin.getParentTableAlias());
	    						
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
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_SAVINGS_ACCOUNT, 
	    							DataExportCoreTable.M_SAVINGS_ACCOUNT_TRANSACTION);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mSavingsAccountTableAlias = DataExportCoreTable.M_SAVINGS_ACCOUNT.getAlias(aliasPostfixNumber.intValue());
		    					
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
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_PAYMENT_DETAIL, 
	    							DataExportCoreTable.M_SAVINGS_ACCOUNT_TRANSACTION);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mPaymentDetailTableAlias = DataExportCoreTable.M_PAYMENT_DETAIL.getAlias(aliasPostfixNumber.intValue());
	    						
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
	    					
	    					sqlJoinKey = DataExportSqlJoin.createId(referencedTable, 
	    							DataExportCoreTable.M_PAYMENT_DETAIL);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					parentTableAlias = referencedTable.getAlias(aliasPostfixNumber.intValue());
	    						
	    						sqlStatement = "`" + referencedTable.getName() + "` `" + parentTableAlias 
	                        			+ "` on `" + parentTableAlias + "`.`id` = `" + dataExportSqlJoin.getParentTableAlias() + "`.`"
	                							+ foreignKeyIndexColumnName + "`";
	    						dataExportSqlJoin = DataExportSqlJoin.newInstance(referencedTable, 
	        							DataExportCoreTable.M_PAYMENT_DETAIL, sqlStatement, parentTableAlias, 
	        							dataExportSqlJoin.getParentTableAlias());
	        					
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
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_PAYMENT_DETAIL, 
	    							DataExportCoreTable.M_SAVINGS_ACCOUNT_TRANSACTION);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mPaymentDetailTableAlias = DataExportCoreTable.M_PAYMENT_DETAIL.getAlias(aliasPostfixNumber.intValue());
	    						
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
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_SAVINGS_ACCOUNT, 
	    							DataExportCoreTable.M_SAVINGS_ACCOUNT_TRANSACTION);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mSavingsAccountTableAlias = DataExportCoreTable.M_SAVINGS_ACCOUNT.getAlias(aliasPostfixNumber.intValue());
	    						
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
	    					
	    					sqlJoinKey = DataExportSqlJoin.createId(referencedTable, 
	    							DataExportCoreTable.M_SAVINGS_ACCOUNT);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					parentTableAlias = referencedTable.getAlias(aliasPostfixNumber.intValue());
	    						
	    						sqlStatement = "`" + referencedTable.getName() + "` `" + parentTableAlias 
	                        			+ "` on `" + parentTableAlias + "`.`id` = `" + dataExportSqlJoin.getParentTableAlias() + "`.`"
	                							+ foreignKeyIndexColumnName + "`";
	    						dataExportSqlJoin = DataExportSqlJoin.newInstance(referencedTable, 
	        							DataExportCoreTable.M_SAVINGS_ACCOUNT, sqlStatement, parentTableAlias, 
	        							dataExportSqlJoin.getParentTableAlias());
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(referencedTable, 
	    							DataExportCoreTable.M_SAVINGS_ACCOUNT);
	    					
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
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_SAVINGS_ACCOUNT, 
	    							DataExportCoreTable.M_SAVINGS_ACCOUNT_TRANSACTION);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mSavingsAccountTableAlias = DataExportCoreTable.M_SAVINGS_ACCOUNT.getAlias(aliasPostfixNumber.intValue());
	    						
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
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_SAVINGS_ACCOUNT, 
	    							DataExportCoreTable.M_GROUP_CLIENT);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mGroupClientTableAlias = DataExportCoreTable.M_GROUP_CLIENT.getAlias(aliasPostfixNumber.intValue());
		    					
	    						// m_loan and m_group_client table join
	        					sqlStatement = "`" + DataExportCoreTable.M_GROUP_CLIENT.getName()
	        							+ "` `" + mGroupClientTableAlias + "` on `" + mGroupClientTableAlias
	        									+ "`.`client_id` = `" + dataExportSqlJoin.getParentTableAlias() + "`.`client_id`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_SAVINGS_ACCOUNT, 
	        							DataExportCoreTable.M_GROUP_CLIENT, sqlStatement, dataExportSqlJoin.getParentTableAlias(), 
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
	    						sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_SAVINGS_ACCOUNT, 
		    							DataExportCoreTable.M_GROUP_CLIENT);
	    						
	    						DataExportSqlJoin savingsAccountGroupClientSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    						
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mGroupTableAlias = DataExportCoreTable.M_GROUP.getAlias(aliasPostfixNumber.intValue());
	    						
	    						// m_group_client and m_group table join
	        					sqlStatement = "`" + DataExportCoreTable.M_GROUP.getName() + "` `"
	                        			+ mGroupTableAlias + "` on `" + mGroupTableAlias + "`.`id` = case when "
	                							+ "isnull(`" + savingsAccountGroupClientSqlJoin.getParentTableAlias() + "`.`group_id`) then `"
	                									+ savingsAccountGroupClientSqlJoin.getChildTableAlias() + "`.`group_id` else `"
	                											+ savingsAccountGroupClientSqlJoin.getParentTableAlias() + "`.`group_id` end";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_GROUP, 
	        							DataExportCoreTable.M_GROUP_CLIENT, sqlStatement, mGroupTableAlias, 
	        							savingsAccountGroupClientSqlJoin.getChildTableAlias());
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_GROUP, 
	    							DataExportCoreTable.M_GROUP_CLIENT);
	    					
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
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_OFFICE, 
	    							DataExportCoreTable.M_SAVINGS_ACCOUNT_TRANSACTION);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mOfficeTableAlias = DataExportCoreTable.M_OFFICE.getAlias(aliasPostfixNumber.intValue());
	    						
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
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_SAVINGS_ACCOUNT, 
	    							DataExportCoreTable.M_SAVINGS_ACCOUNT_TRANSACTION);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mSavingsAccountTableAlias = DataExportCoreTable.M_SAVINGS_ACCOUNT.getAlias(aliasPostfixNumber.intValue());
	    						
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
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_CLIENT, 
	    							DataExportCoreTable.M_SAVINGS_ACCOUNT);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mClientTableAlias = DataExportCoreTable.M_CLIENT.getAlias(aliasPostfixNumber.intValue());
	    						
	    						// m_loan and m_group_client table join
	        					sqlStatement = "`" + DataExportCoreTable.M_CLIENT.getName() + "` `"
	        							+ mClientTableAlias + "` on `" + mClientTableAlias + "`.`id` = `"
												+ dataExportSqlJoin.getChildTableAlias() + "`.`client_id`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_CLIENT, 
	        							DataExportCoreTable.M_SAVINGS_ACCOUNT, sqlStatement, mClientTableAlias, 
	        							dataExportSqlJoin.getChildTableAlias());
	        					
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
	    						sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_CLIENT, 
		    							DataExportCoreTable.M_SAVINGS_ACCOUNT);
	    						
	    						DataExportSqlJoin clientSavingsAccountSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    						
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mGroupTableAlias = DataExportCoreTable.M_GROUP.getAlias(aliasPostfixNumber.intValue());
	    						
	    						// m_loan and m_group_client table join
	        					sqlStatement = "`" + DataExportCoreTable.M_GROUP.getName() + "` `"
	                        			+ mGroupTableAlias + "` on `" + mGroupTableAlias + "`.`id` = `"
	                							+ clientSavingsAccountSqlJoin.getChildTableAlias() + "`.`group_id`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_GROUP, 
	        							DataExportCoreTable.M_SAVINGS_ACCOUNT, sqlStatement, mGroupTableAlias, 
	        							clientSavingsAccountSqlJoin.getChildTableAlias());
	        					
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
	    						sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_CLIENT, 
		    							DataExportCoreTable.M_SAVINGS_ACCOUNT);
	    						
	    						DataExportSqlJoin clientSavingsAccountSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    						
	    						sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_GROUP, 
		    							DataExportCoreTable.M_SAVINGS_ACCOUNT);
	    						
	    						DataExportSqlJoin groupSavingsAccountSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    						
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mStaffTableAlias = DataExportCoreTable.M_STAFF.getAlias(aliasPostfixNumber.intValue());
	    						
	    						// m_group_client and m_group table join
	        					sqlStatement = "`" + DataExportCoreTable.M_STAFF.getName() + "` `"
	                        			+ mStaffTableAlias + "` on `" + mStaffTableAlias + "`.`id` = case when "
	                					+ "isnull(`" + clientSavingsAccountSqlJoin.getChildTableAlias() + "`.`group_id`) then `"
	                							+ clientSavingsAccountSqlJoin.getParentTableAlias() + "`.`staff_id` else `"
	                									+ groupSavingsAccountSqlJoin.getParentTableAlias() + "`.`staff_id` end";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_STAFF, 
	        							DataExportCoreTable.M_SAVINGS_ACCOUNT, sqlStatement, mStaffTableAlias, 
	        							clientSavingsAccountSqlJoin.getChildTableAlias());
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_STAFF, 
	    							DataExportCoreTable.M_SAVINGS_ACCOUNT);
	    					
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
	    				case GENDER:
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_SAVINGS_ACCOUNT, 
	    							DataExportCoreTable.M_SAVINGS_ACCOUNT_TRANSACTION);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mSavingsAccountTableAlias = DataExportCoreTable.M_SAVINGS_ACCOUNT.getAlias(aliasPostfixNumber.intValue());
	    						
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
	    					
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_CLIENT, 
	    							DataExportCoreTable.M_SAVINGS_ACCOUNT);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mClientTableAlias = DataExportCoreTable.M_CLIENT.getAlias(aliasPostfixNumber.intValue());
	    						
	    						sqlStatement = "`" + DataExportCoreTable.M_CLIENT.getName() + "` `"
	        							+ mClientTableAlias + "` on `" + mClientTableAlias + "`.`id` = `"
												+ dataExportSqlJoin.getParentTableAlias() + "`.`client_id`";
	    						dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_CLIENT, 
	        							DataExportCoreTable.M_SAVINGS_ACCOUNT, sqlStatement, mClientTableAlias, 
	        							dataExportSqlJoin.getParentTableAlias());
	        					
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
						case SAVINGS_TRANSACTION_CREATED_BY:
							// =============================================================================
							sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_APP_USER,
									DataExportCoreTable.M_SAVINGS_ACCOUNT_TRANSACTION);

							// only add the join statement if it hasn't been previously added
							if (!sqlJoinMap.containsKey(sqlJoinKey)) {
								// increment the alias postfix number
								aliasPostfixNumber.increment();

								mAppUserTableAlias = DataExportCoreTable.M_APP_USER.getAlias(aliasPostfixNumber.intValue());

								sqlStatement = "`" + DataExportCoreTable.M_APP_USER.getName() + "` `"
										+ mAppUserTableAlias + "` on `" + mAppUserTableAlias + "`.`id` = `"
										+ baseEntityName + "`.`" + coreColumn.getForeignKeyIndexColumnName() + "` ";
								dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_APP_USER,
										DataExportCoreTable.M_SAVINGS_ACCOUNT_TRANSACTION, sqlStatement, mAppUserTableAlias,
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
	    			String outstandingInterest = "(ifnull(`" + baseEntityName + "`.`interest_amount`, 0)"
							+ " - ifnull(`" + baseEntityName + "`.`interest_writtenoff_derived`, 0)"
									+ " - ifnull(`" + baseEntityName + "`.`interest_waived_derived`, 0)" 
											+ " - ifnull(`" + baseEntityName + "`.`interest_completed_derived`, 0))";
					
					String outstandingPrincipal = "(ifnull(`" + baseEntityName + "`.`principal_amount`, 0)"
							+ " - ifnull(`" + baseEntityName + "`.`principal_completed_derived`, 0)"
									+ " - ifnull(`" + baseEntityName + "`.`principal_writtenoff_derived`, 0))";
					
					String outstandingFees = "(ifnull(`" + baseEntityName + "`.`fee_charges_amount`, 0)"
							+ " - ifnull(`" + baseEntityName + "`.`fee_charges_completed_derived`, 0)"
									+ " - ifnull(`" + baseEntityName + "`.`fee_charges_writtenoff_derived`, 0)" 
											+ " - ifnull(`" + baseEntityName + "`.`fee_charges_waived_derived`, 0))";
					
					String outstandingPenalties = "(ifnull(`" + baseEntityName + "`.`penalty_charges_amount`, 0)"
							+ " - ifnull(`" + baseEntityName + "`.`penalty_charges_completed_derived`, 0)"
									+ " - ifnull(`" + baseEntityName + "`.`penalty_charges_writtenoff_derived`, 0)" 
											+ " - ifnull(`" + baseEntityName + "`.`penalty_charges_waived_derived`, 0))";
	    			
	    			switch (coreColumn) {
		    			case REPAYMENT_SCHEDULE_TOTAL_OUTSTANDING:
	    					// =============================================================================
	    					if (isSelectStatement) {
	    						sqlStatement = "(" + outstandingInterest + " + " + outstandingPrincipal + " + " + outstandingFees
	    								 + " + " + outstandingPenalties + ") as `" + coreColumn.getLabel() + "`";
	    						
	    						// add the select statement
	        					sqlBuilder.SELECT(sqlStatement);
	    					} else if (filterValue != null) {
	    						sqlStatement = "(" + outstandingInterest + " + " + outstandingPrincipal + " + " + outstandingFees
	    								 + " + " + outstandingPenalties + ")" + filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    				case REPAYMENT_SCHEDULE_PRINCIPAL_OUTSTANDING:
	    					// =============================================================================
	    					if (isSelectStatement) {
	    						sqlStatement = outstandingPrincipal + " as `" + coreColumn.getLabel() + "`";
	    						
	    						// add the select statement
	        					sqlBuilder.SELECT(sqlStatement);
	    					} else if (filterValue != null) {
	    						sqlStatement = outstandingPrincipal + filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    					
	    				case REPAYMENT_SCHEDULE_INTEREST_OUTSTANDING:
	    					// =============================================================================
	    					if (isSelectStatement) {
	    						sqlStatement = outstandingInterest + " as `" + coreColumn.getLabel() + "`";
	    						
	    						// add the select statement
	        					sqlBuilder.SELECT(sqlStatement);
	    					} else if (filterValue != null) {
	    						sqlStatement = outstandingInterest + filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    					
	    				case REPAYMENT_SCHEDULE_FEES_OUTSTANDING:
	    					// =============================================================================
	    					if (isSelectStatement) {
	    						sqlStatement = outstandingFees + " as `" + coreColumn.getLabel() + "`";
	    						
	    						// add the select statement
	        					sqlBuilder.SELECT(sqlStatement);
	    					} else if (filterValue != null) {
	    						sqlStatement = outstandingFees + filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    					
	    				case REPAYMENT_SCHEDULE_PENALTIES_OUTSTANDING:
	    					// =============================================================================
	    					if (isSelectStatement) {
	    						sqlStatement = outstandingPenalties + " as `" + coreColumn.getLabel() + "`";
	    						
	    						// add the select statement
	        					sqlBuilder.SELECT(sqlStatement);
	    					} else if (filterValue != null) {
	    						sqlStatement = outstandingPenalties + filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    			
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
	    					final String actualColumnName = StringUtils.replace(coreColumn.getName(), 
    								"repayment_schedule_", "");
	    					
	    					if (isSelectStatement) {
	    						
	    						sqlStatement = "`" + baseEntityName + "`.`" + actualColumnName + "` "
	    								+ "as `" + coreColumn.getLabel() + "`";
	    						
	    						// add the select statement
	        					sqlBuilder.SELECT(sqlStatement);
	    						
	    					} else if (filterValue != null) {
	    						
	    						sqlStatement = "`" + baseEntityName + "`.`" + actualColumnName + "` "
	    								+ filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					break;
		    			case STAFF_NAME:
		    				// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_LOAN, 
	    							DataExportCoreTable.M_LOAN_REPAYMENT_SCHEDULE);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
			    				
			    				mLoanTableAlias = DataExportCoreTable.M_LOAN.getAlias(aliasPostfixNumber.intValue());
	    						
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
	    					
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					mLoanTableAlias = dataExportSqlJoin.getParentTableAlias();
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_CLIENT, 
	    							DataExportCoreTable.M_LOAN);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mClientTableAlias = DataExportCoreTable.M_CLIENT.getAlias(aliasPostfixNumber.intValue());
	    						
	    						// m_loan and m_client table join
	        					sqlStatement = "`" + DataExportCoreTable.M_CLIENT.getName() + "` `"
	        							+ mClientTableAlias + "` on `" + mClientTableAlias + "`.`id` = `"
	    										+ dataExportSqlJoin.getParentTableAlias() + "`.`client_id`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_CLIENT, 
	        							DataExportCoreTable.M_LOAN, sqlStatement, mClientTableAlias, 
	        							dataExportSqlJoin.getParentTableAlias());
	        					
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
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mGroupTableAlias = DataExportCoreTable.M_GROUP.getAlias(aliasPostfixNumber.intValue());
	    						
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
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_STAFF, 
	    							DataExportCoreTable.M_LOAN);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_CLIENT, 
		    							DataExportCoreTable.M_LOAN);
	    						
	    						DataExportSqlJoin clientLoanSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    						
	    						sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_GROUP, 
		    							DataExportCoreTable.M_LOAN);
	    						
	    						DataExportSqlJoin groupLoanSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    						
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mStaffTableAlias = DataExportCoreTable.M_STAFF.getAlias(aliasPostfixNumber.intValue());
	    						
	    						// m_staff and m_client/m_group table join
	        					sqlStatement = "`" + DataExportCoreTable.M_STAFF.getName() + "` `"
	                    			+ mStaffTableAlias + "` on `" + mStaffTableAlias + "`.`id` = case when "
	            					+ "isnull(`" + mLoanTableAlias + "`.`group_id`) then `"
	            							+ clientLoanSqlJoin.getParentTableAlias() + "`.`staff_id` else `"
	            									+ groupLoanSqlJoin.getParentTableAlias() + "`.`staff_id` end";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_STAFF, 
	        							DataExportCoreTable.M_LOAN, sqlStatement, mStaffTableAlias, 
	        							mLoanTableAlias);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	                        
	                        // =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_STAFF, 
	    							DataExportCoreTable.M_LOAN);
	    					
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
		    				// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_LOAN, 
	    							DataExportCoreTable.M_LOAN_REPAYMENT_SCHEDULE);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
	    						
	    						mLoanTableAlias = DataExportCoreTable.M_LOAN.getAlias(aliasPostfixNumber.intValue());
	    						
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
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_CLIENT, 
	    							DataExportCoreTable.M_LOAN);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mClientTableAlias = DataExportCoreTable.M_CLIENT.getAlias(aliasPostfixNumber.intValue());
	    						
	    						// m_loan and m_client table join
	        					sqlStatement = "`" + DataExportCoreTable.M_CLIENT.getName() + "` `"
	        							+ mClientTableAlias + "` on `" + mClientTableAlias + "`.`id` = `"
	    										+ dataExportSqlJoin.getParentTableAlias() + "`.`client_id`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_CLIENT, 
	        							DataExportCoreTable.M_LOAN, sqlStatement, mClientTableAlias, 
	        							dataExportSqlJoin.getParentTableAlias());
	        					
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
	    						sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_LOAN, 
		    							DataExportCoreTable.M_LOAN_REPAYMENT_SCHEDULE);
	    						
	    						dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    						
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mGroupTableAlias = DataExportCoreTable.M_GROUP.getAlias(aliasPostfixNumber.intValue());
	    						
	    						// m_loan and m_group table join
	        					sqlStatement = "`" + DataExportCoreTable.M_GROUP.getName() + "` `"
	                        			+ mGroupTableAlias + "` on `" + mGroupTableAlias + "`.`id` = `"
	                					+ dataExportSqlJoin.getParentTableAlias() + "`.`group_id`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_GROUP, 
	        							DataExportCoreTable.M_LOAN, sqlStatement, mGroupTableAlias, 
	        							dataExportSqlJoin.getParentTableAlias());
	        					
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
	    						sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_LOAN, 
		    							DataExportCoreTable.M_LOAN_REPAYMENT_SCHEDULE);
	    						
	    						DataExportSqlJoin loanScheduleSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    						
	    						sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_GROUP, 
		    							DataExportCoreTable.M_LOAN);
	    						
	    						DataExportSqlJoin groupLoanSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    						
	    						sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_CLIENT, 
		    							DataExportCoreTable.M_LOAN);
	    						
	    						DataExportSqlJoin clientLoanSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    						
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mOfficeTableAlias = DataExportCoreTable.M_OFFICE.getAlias(aliasPostfixNumber.intValue());
	    						
	    						// m_office and m_client/m_group table join
	        					sqlStatement = "`" + DataExportCoreTable.M_OFFICE.getName() + "` `"
	                        			+ mOfficeTableAlias + "` on `" + mOfficeTableAlias + "`.`id` = case when "
	                					+ "isnull(`" + loanScheduleSqlJoin.getParentTableAlias() + "`.`group_id`) then `"
	                							+ clientLoanSqlJoin.getParentTableAlias() + "`.`office_id` else `"
	                									+ groupLoanSqlJoin.getParentTableAlias() + "`.`office_id` end";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_OFFICE, 
	        							DataExportCoreTable.M_LOAN, sqlStatement, mOfficeTableAlias, 
	        							loanScheduleSqlJoin.getParentTableAlias());
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_OFFICE, 
	    							DataExportCoreTable.M_LOAN);
	    					
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
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_LOAN, 
	    							DataExportCoreTable.M_LOAN_REPAYMENT_SCHEDULE);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mLoanTableAlias = DataExportCoreTable.M_LOAN.getAlias(aliasPostfixNumber.intValue());
	    						
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
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_LOAN, 
	    							DataExportCoreTable.M_GROUP_CLIENT);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mGroupClientTableAlias = DataExportCoreTable.M_GROUP_CLIENT.getAlias(aliasPostfixNumber.intValue());
	    						
	    						// m_loan and m_group_client table join
	        					sqlStatement = "`" + DataExportCoreTable.M_GROUP_CLIENT.getName()
	        							+ "` `" + mGroupClientTableAlias + "` on `" + mGroupClientTableAlias
	        									+ "`.`client_id` = `" + dataExportSqlJoin.getParentTableAlias() + "`.`client_id`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_LOAN, 
	        							DataExportCoreTable.M_GROUP_CLIENT, sqlStatement, dataExportSqlJoin.getParentTableAlias(), 
	        							mGroupClientTableAlias);
	        					
	        					sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_GROUP, 
	    							DataExportCoreTable.M_GROUP_CLIENT);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mGroupTableAlias = DataExportCoreTable.M_GROUP.getAlias(aliasPostfixNumber.intValue());
	    						
	    						// m_group_client and m_group table join
	        					sqlStatement = "`" + DataExportCoreTable.M_GROUP.getName() + "` `"
	                        			+ mGroupTableAlias + "` on `" + mGroupTableAlias + "`.`id` = case when "
	                							+ "isnull(`" + dataExportSqlJoin.getParentTableAlias() + "`.`group_id`) then `"
	                									+ dataExportSqlJoin.getChildTableAlias() + "`.`group_id` else `"
	                											+ dataExportSqlJoin.getParentTableAlias() + "`.`group_id` end";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_GROUP, 
	        							DataExportCoreTable.M_GROUP_CLIENT, sqlStatement, mGroupTableAlias, 
	        							dataExportSqlJoin.getChildTableAlias());
	        					
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
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_LOAN, 
	    							DataExportCoreTable.M_LOAN_REPAYMENT_SCHEDULE);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mLoanTableAlias = DataExportCoreTable.M_LOAN.getAlias(aliasPostfixNumber.intValue());
	    						
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
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					sqlJoinKey = DataExportSqlJoin.createId(referencedTable, 
	    							DataExportCoreTable.M_LOAN);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					parentTableAlias = referencedTable.getAlias(aliasPostfixNumber.intValue());
	    						
	    						sqlStatement = "`" + referencedTable.getName() + "` `" + parentTableAlias 
	                        			+ "` on `" + parentTableAlias + "`.`id` = `" + dataExportSqlJoin.getParentTableAlias() + "`.`"
	                							+ foreignKeyIndexColumnName + "`";
	    						dataExportSqlJoin = DataExportSqlJoin.newInstance(referencedTable, 
	        							DataExportCoreTable.M_LOAN, sqlStatement, parentTableAlias, 
	        							dataExportSqlJoin.getParentTableAlias());
	        					
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
	    				case GENDER:
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_LOAN, 
	    							DataExportCoreTable.M_LOAN_REPAYMENT_SCHEDULE);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mLoanTableAlias = DataExportCoreTable.M_LOAN.getAlias(aliasPostfixNumber.intValue());
	    						
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
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_CLIENT, 
	    							DataExportCoreTable.M_LOAN);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mClientTableAlias = DataExportCoreTable.M_CLIENT.getAlias(aliasPostfixNumber.intValue());
	    						
	    						sqlStatement = "`" + DataExportCoreTable.M_CLIENT.getName() + "` `"
	        							+ mClientTableAlias + "` on `" + mClientTableAlias + "`.`id` = `"
												+ dataExportSqlJoin.getParentTableAlias() + "`.`client_id`";
	    						dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_CLIENT, 
	        							DataExportCoreTable.M_LOAN, sqlStatement, mClientTableAlias, 
	        							dataExportSqlJoin.getParentTableAlias());
	        					
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
		    			case REPAYMENT_SCHEDULE_PRODUCT_SHORT_NAME:
	    				case REPAYMENT_SCHEDULE_PRODUCT_NAME:
	    				case REPAYMENT_SCHEDULE_PRODUCT_ID:
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_LOAN, 
	    							DataExportCoreTable.M_LOAN_REPAYMENT_SCHEDULE);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mLoanTableAlias = DataExportCoreTable.M_LOAN.getAlias(aliasPostfixNumber.intValue());
	    						
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
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					sqlJoinKey = DataExportSqlJoin.createId(referencedTable, 
	    							DataExportCoreTable.M_LOAN);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mProductLoanTableAlias = referencedTable.getAlias(aliasPostfixNumber.intValue());
	    						
	    						sqlStatement = "`" + referencedTable.getName() + "` `" + mProductLoanTableAlias 
	                        			+ "` on `" + mProductLoanTableAlias + "`.`id` = `" + dataExportSqlJoin.getParentTableAlias() + "`.`"
	                							+ foreignKeyIndexColumnName + "`";
	    						dataExportSqlJoin = DataExportSqlJoin.newInstance(referencedTable, 
	        							DataExportCoreTable.M_LOAN, sqlStatement, mProductLoanTableAlias, 
	        							dataExportSqlJoin.getParentTableAlias());
	    						
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
	    				case REPAYMENT_SCHEDULE_LOAN_ACCOUNT_NUMBER:
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_LOAN, 
	    							DataExportCoreTable.M_LOAN_REPAYMENT_SCHEDULE);
	    					
	    					// only add the join statement if it hasn't been previously added
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mLoanTableAlias = DataExportCoreTable.M_LOAN.getAlias(aliasPostfixNumber.intValue());
		    					
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
	    						// add the select statement
	        					sqlStatement = "NULL as `" + coreColumn.getLabel() + "`";
	        					
	        					sqlBuilder.SELECT(sqlStatement);
	    					}
	    					// =============================================================================
	    					break;
	    			}
	    			break;
	    			
	    		case GROUP_LOAN_MEMBER_ALLOCATION:
	    			switch (coreColumn) {
		    			case CLIENT_ID:
		    			case CLIENT_NAME:
		    				// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_CLIENT, 
	    							DataExportCoreTable.M_GROUP_LOAN_MEMBER_ALLOCATION);
	    					
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mClientTableAlias = DataExportCoreTable.M_CLIENT.getAlias(aliasPostfixNumber.intValue());
	    						
	    						// m_group_loan_member_allocation and m_client table join
	        					sqlStatement = "`" + DataExportCoreTable.M_CLIENT.getName() + "` `" + mClientTableAlias 
	                        			+ "` on `" + mClientTableAlias + "`.`id` = `" + baseEntityName 
	                                            + "`.`client_id`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_CLIENT, 
	        							DataExportCoreTable.M_GROUP_LOAN_MEMBER_ALLOCATION, sqlStatement, mClientTableAlias, 
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
	        							+ coreColumn.getReferencedColumnName() + "` as `" + coreColumn.getLabel() + "`";
	    						
	    						// add the select statement
	        					sqlBuilder.SELECT(sqlStatement);
	        					
	    					} else if (filterValue != null) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	    								+ coreColumn.getReferencedColumnName() + "` " + filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
		    				break;
	    			
		    			case DATE_OF_BIRTH:
	    				case PHONE_NUMBER:
	    				case GENDER:
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_CLIENT, 
	    							DataExportCoreTable.M_GROUP_LOAN_MEMBER_ALLOCATION);
	    					
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mClientTableAlias = DataExportCoreTable.M_CLIENT.getAlias(aliasPostfixNumber.intValue());
	    						
	    						// m_group_loan_member_allocation and m_client table join
	        					sqlStatement = "`" + DataExportCoreTable.M_CLIENT.getName() + "` `" + mClientTableAlias 
	                        			+ "` on `" + mClientTableAlias + "`.`id` = `" + baseEntityName 
	                                            + "`.`client_id`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_CLIENT, 
	        							DataExportCoreTable.M_GROUP_LOAN_MEMBER_ALLOCATION, sqlStatement, mClientTableAlias, 
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
	    					
	    				case LOAN_OFFICER_NAME:
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_LOAN, 
	    							DataExportCoreTable.M_GROUP_LOAN_MEMBER_ALLOCATION);
	    					
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mLoanTableAlias = DataExportCoreTable.M_LOAN.getAlias(aliasPostfixNumber.intValue());
	    						
	    						// m_group_loan_member_allocation and m_loan table join
	        					sqlStatement = "`" + DataExportCoreTable.M_LOAN.getName() + "` `" + mLoanTableAlias 
	                        			+ "` on `" + mLoanTableAlias + "`.`id` = `" + baseEntityName 
	                                            + "`.`loan_id`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_LOAN, 
	        							DataExportCoreTable.M_GROUP_LOAN_MEMBER_ALLOCATION, sqlStatement, mLoanTableAlias, 
	        							baseEntityName);
	    						
	    						sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_STAFF, 
	    							DataExportCoreTable.M_LOAN);
	    					
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mStaffTableAlias = DataExportCoreTable.M_STAFF.getAlias(aliasPostfixNumber.intValue());
	    						
	    						// m_loan and m_group table join
	        					sqlStatement = "`" + DataExportCoreTable.M_STAFF.getName() + "` `" + mStaffTableAlias 
	                        			+ "` on `" + mStaffTableAlias + "`.`id` = `" + dataExportSqlJoin.getParentTableAlias() 
	                                            + "`.`loan_officer_id`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_STAFF, 
	        							DataExportCoreTable.M_LOAN, sqlStatement, mStaffTableAlias, 
	        							dataExportSqlJoin.getParentTableAlias());
	    						
	    						sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					if (isSelectStatement) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	        							+ coreColumn.getReferencedColumnName() + "` as `" + coreColumn.getLabel() + "`";
	    						
	    						// add the select statement
	        					sqlBuilder.SELECT(sqlStatement);
	        					
	    					} else if (filterValue != null) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	    								+ coreColumn.getReferencedColumnName() + "` " + filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					break;
	    					
	    				case GROUP_NAME:
	    				case GROUP_ID:
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_LOAN, 
	    							DataExportCoreTable.M_GROUP_LOAN_MEMBER_ALLOCATION);
	    					
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mLoanTableAlias = DataExportCoreTable.M_LOAN.getAlias(aliasPostfixNumber.intValue());
	    						
	    						// m_group_loan_member_allocation and m_loan table join
	        					sqlStatement = "`" + DataExportCoreTable.M_LOAN.getName() + "` `" + mLoanTableAlias 
	                        			+ "` on `" + mLoanTableAlias + "`.`id` = `" + baseEntityName 
	                                            + "`.`loan_id`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_LOAN, 
	        							DataExportCoreTable.M_GROUP_LOAN_MEMBER_ALLOCATION, sqlStatement, mLoanTableAlias, 
	        							baseEntityName);
	    						
	    						sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_GROUP, 
	    							DataExportCoreTable.M_LOAN);
	    					
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mGroupTableAlias = DataExportCoreTable.M_GROUP.getAlias(aliasPostfixNumber.intValue());
	    						
	    						// m_loan and m_group table join
	        					sqlStatement = "`" + DataExportCoreTable.M_GROUP.getName() + "` `" + mGroupTableAlias 
	                        			+ "` on `" + mGroupTableAlias + "`.`id` = `" + dataExportSqlJoin.getParentTableAlias() 
	                                            + "`.`group_id`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_GROUP, 
	        							DataExportCoreTable.M_LOAN, sqlStatement, mGroupTableAlias, 
	        							dataExportSqlJoin.getParentTableAlias());
	    						
	    						sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					if (isSelectStatement) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	        							+ coreColumn.getReferencedColumnName() + "` as `" + coreColumn.getLabel() + "`";
	    						
	    						// add the select statement
	        					sqlBuilder.SELECT(sqlStatement);
	        					
	    					} else if (filterValue != null) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	    								+ coreColumn.getReferencedColumnName() + "` " + filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					break;
	    					
	    				case GROUP_LOAN_MEMBER_ALLOCATION_LOAN_ACCOUNT_NUMBER:
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_LOAN, 
	    							DataExportCoreTable.M_GROUP_LOAN_MEMBER_ALLOCATION);
	    					
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mLoanTableAlias = DataExportCoreTable.M_LOAN.getAlias(aliasPostfixNumber.intValue());
	    						
	    						// m_group_loan_member_allocation and m_loan table join
	        					sqlStatement = "`" + DataExportCoreTable.M_LOAN.getName() + "` `" + mLoanTableAlias 
	                        			+ "` on `" + mLoanTableAlias + "`.`id` = `" + baseEntityName 
	                                            + "`.`loan_id`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_LOAN, 
	        							DataExportCoreTable.M_GROUP_LOAN_MEMBER_ALLOCATION, sqlStatement, mLoanTableAlias, 
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
	        							+ coreColumn.getReferencedColumnName() + "` as `" + coreColumn.getLabel() + "`";
	    						
	    						// add the select statement
	        					sqlBuilder.SELECT(sqlStatement);
	        					
	    					} else if (filterValue != null) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	    								+ coreColumn.getReferencedColumnName() + "` " + filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					break;
	    					
	    				case STAFF_NAME:
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_CLIENT, 
	    							DataExportCoreTable.M_GROUP_LOAN_MEMBER_ALLOCATION);
	    					
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mClientTableAlias = DataExportCoreTable.M_CLIENT.getAlias(aliasPostfixNumber.intValue());
	    						
	    						// m_group_loan_member_allocation and m_client table join
	        					sqlStatement = "`" + DataExportCoreTable.M_CLIENT.getName() + "` `" + mClientTableAlias 
	                        			+ "` on `" + mClientTableAlias + "`.`id` = `" + baseEntityName 
	                                            + "`.`client_id`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_CLIENT, 
	        							DataExportCoreTable.M_GROUP_LOAN_MEMBER_ALLOCATION, sqlStatement, mClientTableAlias, 
	        							baseEntityName);
	    						
	    						sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_STAFF, 
	    							DataExportCoreTable.M_CLIENT);
	    					
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mStaffTableAlias = DataExportCoreTable.M_STAFF.getAlias(aliasPostfixNumber.intValue());
	    						
	    						// m_client and m_staff table join
	        					sqlStatement = "`" + DataExportCoreTable.M_STAFF.getName() + "` `" + mStaffTableAlias 
	                        			+ "` on `" + mStaffTableAlias + "`.`id` = `" + dataExportSqlJoin.getParentTableAlias() 
	                                            + "`.`staff_id`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_STAFF, 
	        							DataExportCoreTable.M_CLIENT, sqlStatement, mStaffTableAlias, 
	        							dataExportSqlJoin.getParentTableAlias());
	    						
	    						sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					if (isSelectStatement) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	        							+ coreColumn.getReferencedColumnName() + "` as `" + coreColumn.getLabel() + "`";
	    						
	    						// add the select statement
	        					sqlBuilder.SELECT(sqlStatement);
	        					
	    					} else if (filterValue != null) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	    								+ coreColumn.getReferencedColumnName() + "` " + filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
	    					break;
	    					
	    				case BRANCH_NAME:
	    					// =============================================================================
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_LOAN, 
	    							DataExportCoreTable.M_GROUP_LOAN_MEMBER_ALLOCATION);
	    					
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mLoanTableAlias = DataExportCoreTable.M_LOAN.getAlias(aliasPostfixNumber.intValue());
	    						
	    						// m_group_loan_member_allocation and m_loan table join
	        					sqlStatement = "`" + DataExportCoreTable.M_LOAN.getName() + "` `" + mLoanTableAlias 
	                        			+ "` on `" + mLoanTableAlias + "`.`id` = `" + baseEntityName 
	                                            + "`.`loan_id`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_LOAN, 
	        							DataExportCoreTable.M_GROUP_LOAN_MEMBER_ALLOCATION, sqlStatement, mLoanTableAlias, 
	        							baseEntityName);
	    						
	    						sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_GROUP, 
	    							DataExportCoreTable.M_LOAN);
	    					
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mGroupTableAlias = DataExportCoreTable.M_GROUP.getAlias(aliasPostfixNumber.intValue());
	    						
	    						// m_loan and m_group table join
	        					sqlStatement = "`" + DataExportCoreTable.M_GROUP.getName() + "` `" + mGroupTableAlias 
	                        			+ "` on `" + mGroupTableAlias + "`.`id` = `" + dataExportSqlJoin.getParentTableAlias() 
	                                            + "`.`group_id`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_GROUP, 
	        							DataExportCoreTable.M_LOAN, sqlStatement, mGroupTableAlias, 
	        							dataExportSqlJoin.getParentTableAlias());
	    						
	    						sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.M_OFFICE, 
	    							DataExportCoreTable.M_GROUP);
	    					
	    					if (!sqlJoinMap.containsKey(sqlJoinKey)) {
	    						// increment the alias postfix number
		    					aliasPostfixNumber.increment();
		    					
		    					mOfficeTableAlias = DataExportCoreTable.M_OFFICE.getAlias(aliasPostfixNumber.intValue());
	    						
	    						// m_group and m_office table join
	        					sqlStatement = "`" + DataExportCoreTable.M_OFFICE.getName() + "` `" + mOfficeTableAlias 
	                        			+ "` on `" + mOfficeTableAlias + "`.`id` = `" + dataExportSqlJoin.getParentTableAlias() 
	                                            + "`.`office_id`";
	        					dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.M_OFFICE, 
	        							DataExportCoreTable.M_GROUP, sqlStatement, mOfficeTableAlias, 
	        							dataExportSqlJoin.getParentTableAlias());
	    						
	    						sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
	    						
	    						// add the join to the map
	        					sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
	    					}
	    					// =============================================================================
	    					
	    					// =============================================================================
	    					dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
	    					
	    					if (isSelectStatement) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	        							+ coreColumn.getReferencedColumnName() + "` as `" + coreColumn.getLabel() + "`";
	    						
	    						// add the select statement
	        					sqlBuilder.SELECT(sqlStatement);
	        					
	    					} else if (filterValue != null) {
	    						sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`" 
	    								+ coreColumn.getReferencedColumnName() + "` " + filterValue;
	    						
	    						// add a WHERE clause
	    						sqlBuilder.WHERE(sqlStatement);
	    					}
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
    	} else if (columnName.equals("loan_status_id")) { 
        	// =============================================================================
    		sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.R_ENUM_VALUE, 
            		baseEntityCoreTable) + "_loan_status_id";
            
            // only add the join statement if it hasn't been previously added
			if (!sqlJoinMap.containsKey(sqlJoinKey)) {
				// increment the alias postfix number
				aliasPostfixNumber.increment();
	        	
	        	String rEnumValuetableAlias = DataExportCoreTable.R_ENUM_VALUE.getAlias(aliasPostfixNumber.intValue());
				
				sqlStatement = "`" + DataExportCoreTable.R_ENUM_VALUE.getName() + "` `" + rEnumValuetableAlias 
						+ "` on `" + rEnumValuetableAlias + "`.`enum_id` = `" + baseEntityName + "`.`" + columnName
								+ "` and `" + rEnumValuetableAlias + "`.`enum_name` = '" + columnName + "'";
				dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.R_ENUM_VALUE, 
						baseEntityCoreTable, sqlStatement, rEnumValuetableAlias, 
						baseEntityName);
				
				// update the DataExportSqlJoin id
				dataExportSqlJoin.updateId(sqlJoinKey);
				
				sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
				
				// add the join to the map
				sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
			}
            // =============================================================================
			
			// =============================================================================
			dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
			
			if (isSelectStatement) {
				sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`enum_value` as `" + columnLabel + "`";
				
				// add the select statement
				sqlBuilder.SELECT(sqlStatement);
				
			} else if (filterValue != null) {
				sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`enum_value`" + filterValue;
				
				// add a WHERE clause
				sqlBuilder.WHERE(sqlStatement);
			}
			// =============================================================================
    	} else if (columnName.equals("loan_type_enum")) { 
        	// =============================================================================
    		sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.R_ENUM_VALUE, 
            		baseEntityCoreTable) + "_loan_type_enum";
            
            // only add the join statement if it hasn't been previously added
			if (!sqlJoinMap.containsKey(sqlJoinKey)) {
				// increment the alias postfix number
				aliasPostfixNumber.increment();
	        	
	        	String rEnumValuetableAlias = DataExportCoreTable.R_ENUM_VALUE.getAlias(aliasPostfixNumber.intValue());
				
				sqlStatement = "`" + DataExportCoreTable.R_ENUM_VALUE.getName() + "` `" + rEnumValuetableAlias 
						+ "` on `" + rEnumValuetableAlias + "`.`enum_id` = `" + baseEntityName + "`.`" + columnName
								+ "` and `" + rEnumValuetableAlias + "`.`enum_name` = '" + columnName + "'";
				dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.R_ENUM_VALUE, 
						baseEntityCoreTable, sqlStatement, rEnumValuetableAlias, 
						baseEntityName);
				
				// update the DataExportSqlJoin id
				dataExportSqlJoin.updateId(sqlJoinKey);
				
				sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
				
				// add the join to the map
				sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
			}
            // =============================================================================
			
			// =============================================================================
			dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
			
			if (isSelectStatement) {
				sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`enum_value` as `" + columnLabel + "`";
				
				// add the select statement
				sqlBuilder.SELECT(sqlStatement);
				
			} else if (filterValue != null) {
				sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`enum_value`" + filterValue;
				
				// add a WHERE clause
				sqlBuilder.WHERE(sqlStatement);
			}
			// =============================================================================
    	} else if (columnName.equals("status_enum")) { 
        	// =============================================================================
    		sqlJoinKey = DataExportSqlJoin.createId(DataExportCoreTable.R_ENUM_VALUE, 
            		baseEntityCoreTable) + "_status_enum";
            
            // only add the join statement if it hasn't been previously added
			if (!sqlJoinMap.containsKey(sqlJoinKey)) {
				// increment the alias postfix number
				aliasPostfixNumber.increment();
	        	
	        	String rEnumValuetableAlias = DataExportCoreTable.R_ENUM_VALUE.getAlias(aliasPostfixNumber.intValue());
				
				sqlStatement = "`" + DataExportCoreTable.R_ENUM_VALUE.getName() + "` `" + rEnumValuetableAlias 
						+ "` on `" + rEnumValuetableAlias + "`.`enum_id` = `" + baseEntityName + "`.`" + columnName
								+ "` and `" + rEnumValuetableAlias + "`.`enum_name` = '" + columnName + "'";
				dataExportSqlJoin = DataExportSqlJoin.newInstance(DataExportCoreTable.R_ENUM_VALUE, 
						baseEntityCoreTable, sqlStatement, rEnumValuetableAlias, 
						baseEntityName);
				
				// update the DataExportSqlJoin id
				dataExportSqlJoin.updateId(sqlJoinKey);
				
				sqlBuilder.LEFT_OUTER_JOIN(sqlStatement);
				
				// add the join to the map
				sqlJoinMap.put(dataExportSqlJoin.getId(), dataExportSqlJoin);
			}
            // =============================================================================
			
			// =============================================================================
			dataExportSqlJoin = sqlJoinMap.get(sqlJoinKey);
			
			if (isSelectStatement) {
				sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`enum_value` as `" + columnLabel + "`";
				
				// add the select statement
				sqlBuilder.SELECT(sqlStatement);
				
			} else if (filterValue != null) {
				sqlStatement = "`" + dataExportSqlJoin.getParentTableAlias() + "`.`enum_value`" + filterValue;
				
				// add a WHERE clause
				sqlBuilder.WHERE(sqlStatement);
			}
			// =============================================================================
        } else {
        	// =============================================================================
			if (isSelectStatement) {
				sqlStatement = "`" + baseEntityName + "`.`" + columnName + "` as `" + columnLabel + "`";
				
				// add the select statement
				sqlBuilder.SELECT(sqlStatement);
				
			} else if (filterValue != null) {
				sqlStatement = "`" + baseEntityName + "`.`" + columnName + "` " + filterValue;
				
				// add a WHERE clause
				sqlBuilder.WHERE(sqlStatement);
			}
			// =============================================================================
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
        
        if (realCause.getMessage().contains(DataExportApiConstants.NAME_PARAM_NAME)) {
            final String name = jsonCommand.stringValueOfParameterNamed(DataExportApiConstants.NAME_PARAM_NAME);
            throw new PlatformDataIntegrityException("error.msg.data.export.duplicate.name", "Data export with name `" + name + "` already exists",
            		DataExportApiConstants.NAME_PARAM_NAME, name);
        }
        
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
