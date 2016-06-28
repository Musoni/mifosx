/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.service;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.mifosplatform.infrastructure.core.domain.JdbcSupport;
import org.mifosplatform.infrastructure.core.service.RoutingDataSource;
import org.mifosplatform.infrastructure.dataexport.api.DataExportApiConstants;
import org.mifosplatform.infrastructure.dataexport.data.DataExportBaseEntity;
import org.mifosplatform.infrastructure.dataexport.data.DataExportData;
import org.mifosplatform.infrastructure.dataexport.data.DataExportEntityData;
import org.mifosplatform.infrastructure.dataexport.data.DataExportTimelineData;
import org.mifosplatform.infrastructure.dataexport.data.EntityColumnMetaData;
import org.mifosplatform.infrastructure.dataexport.exception.DataExportNotFoundException;
import org.mifosplatform.infrastructure.dataqueries.data.DatatableData;
import org.mifosplatform.infrastructure.dataqueries.domain.RegisteredTable;
import org.mifosplatform.infrastructure.dataqueries.domain.RegisteredTableRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class DataExportReadPlatformServiceImpl implements DataExportReadPlatformService {
   private final JdbcTemplate jdbcTemplate;
   private final RegisteredTableRepository registeredTableRepository;

    @Autowired
    public DataExportReadPlatformServiceImpl(final RoutingDataSource dataSource, 
            final RegisteredTableRepository registeredTableRepository) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.registeredTableRepository = registeredTableRepository;
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
    public Response downloadDataExportFile(Long id, String fileFormat) {
        // TODO Auto-generated method stub
        return null;
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
            
            final Collection<EntityColumnMetaData> columns = this.getEntityColumnsMetaData(dataExportBaseEntity.getTableName());
            final Collection<RegisteredTable> registeredTables = this.registeredTableRepository.
                    findAllByApplicationTableName(dataExportBaseEntity.getTableName());
            
            for (RegisteredTable registeredTable : registeredTables) {
                Long category = (registeredTable.getCategory() != null) ? registeredTable.getCategory().longValue() : null;
                String tableName = registeredTable.getRegisteredTableName();
                
                // only return user created and musoni system datatables (ml_loan_details, etc.)
                if (StringUtils.startsWithAny(tableName, new String[] {
                        DataExportApiConstants.USER_CREATED_DATATABLE_NAME_PREFIX, 
                        DataExportApiConstants.MUSONI_SYSTEM_DATATABLE_NAME_PREFIX})) {
                    DatatableData datatableData = DatatableData.create(registeredTable.getApplicationTableName(), 
                            tableName, null, category, null, registeredTable.isSystemDefined(), 
                            registeredTable.getDisplayName());
                    
                    datatables.add(datatableData);
                }
            }
            
            dataExportEntityData = DataExportEntityData.newInstance(dataExportBaseEntity.getEntityName(), 
                    dataExportBaseEntity.getTableName(), datatables, columns);
        }
        
        return dataExportEntityData;
    }
    
    private List<EntityColumnMetaData> getEntityColumnsMetaData(final String tableName) {
        final List<EntityColumnMetaData> entityColumnsMetaData = new ArrayList<>();
        
        try {
            // see - http://dev.mysql.com/doc/refman/5.7/en/limit-optimization.html
            // LIMIT 0 quickly returns an empty set. This can be useful for checking the validity of a query. 
            // It can also be employed to obtain the types of the result columns if you are using a MySQL API 
            // that makes result set metadata available.
            final ResultSetMetaData resultSetMetaData = jdbcTemplate.query("select * from " + tableName + " limit 0", 
                    new ResultSetExtractor<ResultSetMetaData>() {

                @Override
                public ResultSetMetaData extractData(ResultSet rs) throws SQLException, DataAccessException {
                    return rs.getMetaData();
                }
            });
            
            if (resultSetMetaData != null) {
                final int numberOfColumns = resultSetMetaData.getColumnCount();
                
                for (int i = 1; i <= numberOfColumns; i++) {
                    String columnName = resultSetMetaData.getColumnName(i);
                    String columnType = resultSetMetaData.getColumnTypeName(i);
                    Integer columnIsNullable = resultSetMetaData.isNullable(i);
                    boolean isNullable = (columnIsNullable != 0);
                    String columnLabel = this.getEntityColumnLabel(columnName);
                    
                    EntityColumnMetaData entityColumnMetaData = EntityColumnMetaData.newInstance(columnName, 
                            columnLabel, columnType, isNullable);
                    
                    entityColumnsMetaData.add(entityColumnMetaData);
                }
            }
        }
        
        catch (Exception exception) {
            exception.printStackTrace();
        }
        
        return entityColumnsMetaData;
    }
    
    /**
     * Creates a human readable label for the specified column name
     * 
     * @param columnName
     * @return string
     */
    private String getEntityColumnLabel(final String columnName) {
        final int numberOfReplacementStrings = 8;
        final String[] searchList = new String[numberOfReplacementStrings];
        final String[] replacementList = new String[numberOfReplacementStrings];
        
        searchList[0] = "activation_date";
        replacementList[0] = "activation date";
        
        searchList[1] = "_on_userid";
        replacementList[1] = " by user";
        
        searchList[2] = "on_userid";
        replacementList[2] = " by user";
        
        searchList[3] = "_on_date";
        replacementList[3] = " on date";
        
        searchList[4] = "on_date";
        replacementList[4] = " on date";
        
        searchList[5] = "_cv_id";
        replacementList[5] = "";
        
        searchList[6] = "_enum";
        replacementList[6] = "";
        
        searchList[7] = "_";
        replacementList[7] = " ";
        
        return StringUtils.replaceEach(columnName, searchList, replacementList);
    }
}
