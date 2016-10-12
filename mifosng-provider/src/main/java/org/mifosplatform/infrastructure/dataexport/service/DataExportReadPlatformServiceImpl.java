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
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.mifosplatform.infrastructure.core.domain.JdbcSupport;
import org.mifosplatform.infrastructure.core.service.RoutingDataSource;
import org.mifosplatform.infrastructure.dataexport.api.DataExportApiConstants;
import org.mifosplatform.infrastructure.dataexport.data.DataExportBaseEntity;
import org.mifosplatform.infrastructure.dataexport.data.DataExportCoreDatatable;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSetMetaData;
import org.springframework.stereotype.Service;

@Service
public class DataExportReadPlatformServiceImpl implements DataExportReadPlatformService {
   private final JdbcTemplate jdbcTemplate;
   private final RegisteredTableRepository registeredTableRepository;
   private final DataExportRepository dataExportRepository;

    @Autowired
    public DataExportReadPlatformServiceImpl(final RoutingDataSource dataSource, 
            final RegisteredTableRepository registeredTableRepository, 
            final DataExportRepository dataExportRepository) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.registeredTableRepository = registeredTableRepository;
        this.dataExportRepository = dataExportRepository;
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
            final List<String> fileHeaders = new ArrayList<>();
            final List<String[]> csvFileData = new ArrayList<>();
            final List<Map<String, Object>> xlsFileData = new ArrayList<>();
            final SqlRowSet sqlRowSet = this.jdbcTemplate.queryForRowSet(sql);
            final SqlRowSetMetaData sqlRowSetMetaData = sqlRowSet.getMetaData();
            final int columnCount = sqlRowSetMetaData.getColumnCount();
            
            for(int i=1 ; i<=columnCount ; i++) {
                fileHeaders.add(sqlRowSetMetaData.getColumnLabel(i));
            }
            
            while (sqlRowSet.next()) {
                String[] rowData = new String[columnCount];
                Map<String, Object> xlsRowData = new HashMap<>();
                int rowDataIndex = 0;
                
                for(int i=1 ; i<=columnCount ; i++) {
                    String data = StringEscapeUtils.escapeCsv(sqlRowSet.getString(i));
                    Object xlsData = data;
                    rowData[rowDataIndex++] = data;
                    
                    xlsRowData.put(sqlRowSetMetaData.getColumnLabel(i), xlsData);
                }
                
                xlsFileData.add(xlsRowData);
                csvFileData.add(rowData);
            }
            
            final DataExportFileFormat dataExportFileFormat = DataExportFileFormat.fromString(fileFormat);
            final String[] fileHeadersArray = fileHeaders.toArray(new String[fileHeaders.size()]);
            final String filename = dataExport.getFilename();
            DataExportFileData dataExportFileData = FileHelper.createDataExportCsvFile(csvFileData, 
                    filename, fileHeadersArray);
            
            if (dataExportFileFormat.isXls()) {
                dataExportFileData = FileHelper.createDataExportXlsFile(xlsFileData, filename);
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
        public StringBuilder sqlStringBuilder = new StringBuilder("mde.id, mde.base_entity_name as baseEntityName, ").
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
            
            return DataExportData.newInstance(id, baseEntityName, userRequestMap, fileDownloadCount, timeline, 
                    filename);
        }
    }

    @Override
    public DataExportEntityData retrieveTemplate(String baseEntityName) {
        DataExportEntityData dataExportEntityData = null;
        DataExportBaseEntity dataExportBaseEntity = DataExportBaseEntity.fromEntityName(baseEntityName);
        
        if (dataExportBaseEntity.isValid()) {
            Collection<DatatableData> datatables = new ArrayList<>();
            
            final Collection<EntityColumnMetaData> columns = DataExportUtils.getTableColumnsMetaData(
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
            
            // add the core datatables to the list
            for (DataExportCoreDatatable coreDatatable : DataExportCoreDatatable.values()) {
            	DataExportBaseEntity baseEntity = coreDatatable.getBaseEntity();
            	
            	if (dataExportBaseEntity.equals(baseEntity)) {
            		DatatableData datatableData = DatatableData.create(baseEntity.getTableName(), coreDatatable.getTableName(), 
            				null, 0L, null, false, coreDatatable.getDisplayName());
                	
                	datatables.add(datatableData);
            	}
            }
            
            dataExportEntityData = DataExportEntityData.newInstance(dataExportBaseEntity.getEntityName(), 
                    dataExportBaseEntity.getTableName(), datatables, columns);
        }
        
        return dataExportEntityData;
    }
}
