/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;

import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.mifosplatform.infrastructure.codes.data.CodeValueData;
import org.mifosplatform.infrastructure.codes.service.CodeValueReadPlatformService;
import org.mifosplatform.infrastructure.core.domain.JdbcSupport;
import org.mifosplatform.infrastructure.core.service.RoutingDataSource;
import org.mifosplatform.infrastructure.dataexport.api.DataExportApiConstants;
import org.mifosplatform.infrastructure.dataexport.data.DataExportBaseEntity;
import org.mifosplatform.infrastructure.dataexport.data.DataExportCoreColumn;
import org.mifosplatform.infrastructure.dataexport.data.DataExportCoreDatatable;
import org.mifosplatform.infrastructure.dataexport.data.DataExportCoreTable;
import org.mifosplatform.infrastructure.dataexport.data.DataExportData;
import org.mifosplatform.infrastructure.dataexport.data.DataExportEntityData;
import org.mifosplatform.infrastructure.dataexport.data.DataExportFileData;
import org.mifosplatform.infrastructure.dataexport.data.DataExportFileFormat;
import org.mifosplatform.infrastructure.dataexport.data.DataExportTimelineData;
import org.mifosplatform.infrastructure.dataexport.data.EntityColumnMetaData;
import org.mifosplatform.infrastructure.dataexport.domain.DataExport;
import org.mifosplatform.infrastructure.dataexport.domain.DataExportRepository;
import org.mifosplatform.infrastructure.dataexport.exception.DataExportNotFoundException;
import org.mifosplatform.infrastructure.dataexport.helper.DataExportUtils;
import org.mifosplatform.infrastructure.dataexport.helper.FileHelper;
import org.mifosplatform.infrastructure.dataqueries.data.DatatableData;
import org.mifosplatform.infrastructure.dataqueries.domain.RegisteredTable;
import org.mifosplatform.infrastructure.dataqueries.domain.RegisteredTableRepository;
import org.mifosplatform.useradministration.data.AppUserData;
import org.mifosplatform.useradministration.service.AppUserReadPlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Service;

@Service
public class DataExportReadPlatformServiceImpl implements DataExportReadPlatformService {
   private final JdbcTemplate jdbcTemplate;
   private final RegisteredTableRepository registeredTableRepository;
   private final DataExportRepository dataExportRepository;
   private final CodeValueReadPlatformService codeValueReadPlatformService;
   private final AppUserReadPlatformService appUserReadPlatformService;

    @Autowired
    public DataExportReadPlatformServiceImpl(final RoutingDataSource dataSource, 
            final RegisteredTableRepository registeredTableRepository, 
            final DataExportRepository dataExportRepository, 
            final CodeValueReadPlatformService codeValueReadPlatformService, 
            final AppUserReadPlatformService appUserReadPlatformService) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.registeredTableRepository = registeredTableRepository;
        this.dataExportRepository = dataExportRepository;
        this.codeValueReadPlatformService = codeValueReadPlatformService;
        this.appUserReadPlatformService = appUserReadPlatformService;
    }

    @Override
    public Collection<DataExportData> retrieveAll() {
        final DataExportRowMapper dataExportRowMapper = new DataExportRowMapper();
        final String sql = "select " + dataExportRowMapper.sqlStringBuilder.toString() + " where mde.is_deleted = 0";
        
        return this.jdbcTemplate.query(sql, dataExportRowMapper);
    }

    @Override
    public DataExportData retrieveOne(Long id) {
        try {
            final DataExportRowMapper dataExportRowMapper = new DataExportRowMapper();
            final String sql = "select " + dataExportRowMapper.sqlStringBuilder.toString() + " where mde.id = ? and mde.is_deleted = 0";
            
            return this.jdbcTemplate.queryForObject(sql, dataExportRowMapper, new Object[] { id });
        }
        
        catch (final EmptyResultDataAccessException ex) {
            throw new DataExportNotFoundException(id);
        }
    }

    @Override
    public Response downloadDataExportFile(final Long id, final String fileFormat) {
        try {
            final DataExport dataExport = this.dataExportRepository.findByIdAndDeletedFalse(id);
            
            if (dataExport == null) {
                throw new DataExportNotFoundException(id);
            }
            
            final String sql = dataExport.getDataSql();
            final SqlRowSet sqlRowSet = this.jdbcTemplate.queryForRowSet(sql);
            final DataExportBaseEntity baseEntity = DataExportBaseEntity.fromEntityName(dataExport.getBaseEntityName());
            final DataExportCoreTable coreTable = DataExportCoreTable.newInstance(baseEntity.getTableName());
            
            DataExportFileData dataExportFileData = null;
            
            final DataExportFileFormat dataExportFileFormat = DataExportFileFormat.fromString(fileFormat);
            final String filename = dataExport.getFilename();
            final Collection<CodeValueData> codeValues = this.codeValueReadPlatformService.retrieveAllCodeValues();
            final Collection<AppUserData> appUsers = this.appUserReadPlatformService.retrieveAllUsers();
            final HashMap<Long, CodeValueData> codeValueMap = new HashMap<>();
            final HashMap<Long, AppUserData> appUserMap = new HashMap<>();
            
            for (CodeValueData codeValueData : codeValues) {
            	codeValueMap.put(codeValueData.getId(), codeValueData);
            }
            
            for (AppUserData appUserData : appUsers) {
            	appUserMap.put(appUserData.getId(), appUserData);
            }
            
            switch (dataExportFileFormat) {
            	case XLS:
            		dataExportFileData = FileHelper.createDataExportXlsFile(sqlRowSet, filename, 
            				codeValueMap, appUserMap, coreTable);
            		break;
            
            	default:
            		dataExportFileData = FileHelper.createDataExportCsvFile(sqlRowSet, filename, 
            				codeValueMap, appUserMap, coreTable);
            		break;
            }
            
            int fileDownloadCount = dataExport.getFileDownloadCount();
            
            dataExport.updateFileDownloadCount(++fileDownloadCount);
            
            this.dataExportRepository.save(dataExport);
            
            return Response.ok(dataExportFileData.getInputStream()).
                    header("Content-Disposition", "attachment; filename=\"" + dataExportFileData.getFileName() + "\"").
                    header("Content-Type", dataExportFileData.getContentType()).
                    build();
            
        } catch (Exception exception) {
            exception.printStackTrace();
            
            return Response.serverError().tag(exception.getMessage()).build();
        }
    }
    
    private static final class DataExportRowMapper implements RowMapper<DataExportData> {
        public StringBuilder sqlStringBuilder = new StringBuilder("mde.id, mde.name, mde.base_entity_name as baseEntityName, ").
                append("mde.user_request_map as userRequestMap, mde.file_download_count as fileDownloadCount, ").
                append("cbu.username as createdByUsername, cbu.firstname as createdByFirstname, ").
                append("cbu.lastname as createdByLastname, mde.created_date as createdOnDate, ").
                append("mbu.username as modifiedByUsername, mbu.firstname as modifiedByFirstname, ").
                append("mbu.lastname as modifiedByLastname, mde.lastmodified_date as modifiedOnDate, mde.filename ").
                append("from m_data_export mde ").
                append("left join m_appuser cbu on cbu.id = mde.createdby_id ").
                append("left join m_appuser mbu on mbu.id = mde.lastmodifiedby_id ");
        
        @Override
        public DataExportData mapRow(ResultSet rs, int rowNum) throws SQLException {
            final Long id = rs.getLong("id");
            final String name = rs.getString("name");
            final String baseEntityName = rs.getString("baseEntityName");
            final String userRequestMap = rs.getString("userRequestMap");
            final Integer fileDownloadCount = JdbcSupport.getInteger(rs, "fileDownloadCount");
            final DateTime createdOnDate = JdbcSupport.getDateTime(rs, "createdOnDate");
            final DateTime modifiedOnDate = JdbcSupport.getDateTime(rs, "modifiedOnDate");
            final String createdByUsername = rs.getString("createdByUsername");
            final String createdByFirstname = rs.getString("createdByFirstname");
            final String createdByLastname = rs.getString("createdByLastname");
            final String modifiedByUsername = rs.getString("modifiedByUsername");
            final String modifiedByFirstname = rs.getString("modifiedByFirstname");
            final String modifiedByLastname = rs.getString("modifiedByLastname");
            final DataExportTimelineData timeline = DataExportTimelineData.newInstance(createdByUsername, 
                    createdByFirstname, createdByLastname, createdOnDate, modifiedByUsername, 
                    modifiedByFirstname, modifiedByLastname, modifiedOnDate);
            final String filename = rs.getString("filename");
            
            return DataExportData.newInstance(id, name, baseEntityName, userRequestMap, fileDownloadCount, timeline, 
                    filename);
        }
    }

    @Override
    public DataExportEntityData retrieveTemplate(String baseEntityName) {
        DataExportEntityData dataExportEntityData = null;
        DataExportBaseEntity dataExportBaseEntity = DataExportBaseEntity.fromEntityName(baseEntityName);
        DataExportCoreTable dataExportCoreTable = DataExportCoreTable.newInstance(dataExportBaseEntity.getTableName());
        
        if (dataExportBaseEntity.isValid()) {
            Collection<DatatableData> datatables = new ArrayList<>();
            Collection<EntityColumnMetaData> columns = new ArrayList<>();
            LinkedHashMap<String, EntityColumnMetaData> uniqueColumns = new LinkedHashMap<>();
            
            final Collection<EntityColumnMetaData> nonCoreColumns = DataExportUtils.getTableColumnsMetaData(
            		dataExportBaseEntity.getTableName(), jdbcTemplate);
            final Collection<RegisteredTable> registeredTables = this.registeredTableRepository.
                    findAllByApplicationTableName(dataExportBaseEntity.getTableName());
            
            for (RegisteredTable registeredTable : registeredTables) {
                Long category = (registeredTable.getCategory() != null) ? registeredTable.getCategory().longValue() : null;
                String tableName = registeredTable.getRegisteredTableName();
                
                // only return user created or musoni system datatables (ml_loan_details, etc.)
                if (StringUtils.startsWithAny(tableName, new String[] {
                        DataExportApiConstants.USER_CREATED_DATATABLE_NAME_PREFIX, 
                        DataExportApiConstants.MUSONI_SYSTEM_DATATABLE_NAME_PREFIX})) {
                    DatatableData datatableData = DatatableData.create(registeredTable.getApplicationTableName(), 
                            tableName, null, category, null, registeredTable.isSystemDefined(), 
                            registeredTable.getDisplayName());
                    
                    datatables.add(datatableData);
                }
            }
            
            // add the core datatables to the list of datatables
            for (DataExportCoreDatatable coreDatatable : DataExportCoreDatatable.values()) {
            	DataExportBaseEntity baseEntity = coreDatatable.getBaseEntity();
            	
            	if (dataExportBaseEntity.equals(baseEntity)) {
            		DatatableData datatableData = DatatableData.create(baseEntity.getTableName(), coreDatatable.getTableName(), 
            				null, 0L, null, false, coreDatatable.getDisplayName());
                	
                	datatables.add(datatableData);
            	}
            }
            
            // add the core columns
            for (DataExportCoreColumn coreColumn : DataExportCoreColumn.values()) {
            	if (coreColumn.getBaseEntity() == null || 
            			(coreColumn.getBaseEntity() != null && coreColumn.getBaseEntity().equals(dataExportBaseEntity))) {
            		String columnLabel = DataExportUtils.createHumanReadableTableColumnLabel(coreColumn.getLabel(), 
            				dataExportCoreTable);
            		
            		EntityColumnMetaData metaData = EntityColumnMetaData.newInstance(coreColumn.getName(), 
            				columnLabel, coreColumn.getType(), coreColumn.isNullable());
            		
            		uniqueColumns.put(coreColumn.getName(), metaData);
            	}
            }
            
            // add the non-core columns
            for (EntityColumnMetaData nonCoreColumn : nonCoreColumns) {
            	String columnLabel = DataExportUtils.createHumanReadableTableColumnLabel(nonCoreColumn.getLabel(), 
        				dataExportCoreTable);
            	
            	// update the label property
            	nonCoreColumn.updateLabel(columnLabel);
            	
            	uniqueColumns.put(nonCoreColumn.getName(), nonCoreColumn);
            }
            
            // convert LinkedHashMap to ArrayList
            columns.addAll(uniqueColumns.values());
            
            dataExportEntityData = DataExportEntityData.newInstance(dataExportBaseEntity.getEntityName(), 
                    dataExportBaseEntity.getTableName(), datatables, columns);
        }
        
        return dataExportEntityData;
    }

	@Override
	public Collection<DataExportEntityData> retrieveAllBaseEntities() {
		final Collection<DataExportEntityData> dataExportEntityDataList = new ArrayList<>();
		
		for (DataExportBaseEntity dataExportBaseEntity : DataExportBaseEntity.values()) {
			if (dataExportBaseEntity.isValid()) {
				DataExportEntityData dataExportEntityData = DataExportEntityData.newInstance(
						dataExportBaseEntity.getEntityName(), dataExportBaseEntity.getTableName(), 
						null, null);
				
				dataExportEntityDataList.add(dataExportEntityData);
			}
		}
		
		return dataExportEntityDataList;
	}
}
