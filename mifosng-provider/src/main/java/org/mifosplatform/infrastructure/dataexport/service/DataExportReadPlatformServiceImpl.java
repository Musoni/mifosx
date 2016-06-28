/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.service;

import org.mifosplatform.infrastructure.codes.domain.CodeValue;
import org.mifosplatform.infrastructure.codes.domain.CodeValueRepository;
import org.mifosplatform.infrastructure.core.service.RoutingDataSource;
import org.mifosplatform.infrastructure.dataexport.api.DataExportApiConstants;
import org.mifosplatform.infrastructure.dataexport.data.*;
import org.mifosplatform.infrastructure.dataexport.domain.*;
import org.mifosplatform.infrastructure.dataexport.exception.EntityMismatchException;
import org.mifosplatform.infrastructure.dataexport.helper.FileHelper;
import org.mifosplatform.infrastructure.dataexport.helper.XmlFileHelper;
import org.mifosplatform.infrastructure.dataqueries.data.DatatableData;
import org.mifosplatform.infrastructure.dataqueries.domain.RegisteredTable;
import org.mifosplatform.infrastructure.dataqueries.domain.RegisteredTableMetaData;
import org.mifosplatform.infrastructure.dataqueries.domain.RegisteredTableMetaDataRepository;
import org.mifosplatform.infrastructure.dataqueries.domain.RegisteredTableRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;
import java.security.InvalidParameterException;
import java.util.*;

@Service
public class DataExportReadPlatformServiceImpl implements DataExportReadPlatformService {
    private final RegisteredTableRepository registeredTableRepository;
    private final RegisteredTableMetaDataRepository registeredTableMetaDataRepository;
    private final JdbcTemplate jdbcTemplate;
    private final DataExportRepository dataExportRepository;
    private final DataExportProcessRepository dataExportProcessRepository;
    private final EnumValueCollectionRepositoryWrapper enumValueCollectionRepositoryWrapper;
    private final EntityLabelRepository entityLabelRepository;
    private final CodeValueRepository codeValueRepository;

    @Autowired
    public DataExportReadPlatformServiceImpl(final RegisteredTableRepository registeredTableRepository, final RoutingDataSource dataSource,
            final DataExportRepository dataExportRepository, final DataExportProcessRepository dataExportProcessRepository,
            final EnumValueCollectionRepositoryWrapper enumValueCollectionRepositoryWrapper, final EntityLabelRepository entityLabelRepository,
            final RegisteredTableMetaDataRepository registeredTableMetaDataRepository, final CodeValueRepository codeValueRepository) {
        this.registeredTableRepository = registeredTableRepository;
        this.dataExportRepository = dataExportRepository;
        this.dataExportProcessRepository = dataExportProcessRepository;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.enumValueCollectionRepositoryWrapper = enumValueCollectionRepositoryWrapper;
        this.entityLabelRepository = entityLabelRepository;
        this.registeredTableMetaDataRepository = registeredTableMetaDataRepository;
        this.codeValueRepository = codeValueRepository;
    }

    @Override
    public DataExportRequestData retrieveDataExportRequestData(final String entity){
        return retrieveDataExportRequestData(entity, new ArrayList<DataExportFilter>(), new ArrayList<String>());
    }

    @Override
    public DataExportRequestData retrieveDataExportRequestData(final String entity,
            final List<DataExportFilter> filters, final List<String> datatables){
        try {
            final DataExportBaseEntityEnum baseEntity;
            final List<String> exportDatatables = new ArrayList<>();

            if (entity != null) {
                baseEntity = DataExportBaseEntityEnum.valueOf(entity.toUpperCase());
            }else{baseEntity = null;}
            if (baseEntity == null || baseEntity.getTablename().isEmpty() || filters == null || datatables == null) {
                throw new InvalidParameterException(entity);
            }

            if(datatables.size()>0){
                List<String> dataTableCheckList = assembleExportDataTables(baseEntity);
                for(String dataTable : datatables){
                    if(dataTableCheckList.contains(dataTable)){
                        exportDatatables.add(dataTable);
                    }
                }
            }

            final Set<String> supportedParameters = new HashSet<>();
            final List<EntityLabel> entityLabels = new ArrayList<>(this.entityLabelRepository.findAllByTable(baseEntity.getTablename()));

            supportedParameters.addAll(DataExportApiConstants.BASIC_SUPPORTED_PARAMETERS);
            for(EntityLabel entityLabel : entityLabels){
                supportedParameters.add(entityLabel.getJsonParam());
            }

            return new DataExportRequestData(baseEntity, filters, exportDatatables, supportedParameters);
        } catch(InvalidParameterException e){return null;}
    }

    @Override
    public Collection<DataExportRequestData> retrieveDataExportRequestDataCollection(){
        Collection<DataExportRequestData> dataExportRequestDatas = new ArrayList<>();
        for(DataExportBaseEntityEnum entity:DataExportBaseEntityEnum.values()){
            dataExportRequestDatas.add(retrieveDataExportRequestData(entity.getName()));
        }
        return dataExportRequestDatas;
    }

    @Override
    public Response downloadDataExportFile(final String entity, final Long dataExportProcessId, final String fileFormat) {

        try {
            final DataExportProcess dataExportProcess = this.dataExportProcessRepository.findOne(dataExportProcessId);
            final DataExport dataExport = dataExportProcess.getDataExport();
            final DataExportRequestData requestData = retrieveDataExportRequestData(entity!=null?entity:dataExport.getBaseEntity());
            if (entity!=null && !entity.equals(dataExport.getBaseEntity())) {
                throw new EntityMismatchException(entity, dataExport.getBaseEntity());
            }

            final DataExportFileFormat dataExportFileFormat = DataExportFileFormat.valueOf(fileFormat.toUpperCase());
            final DataExportFileData dataExportFileData = downloadDataExportFileData(dataExportProcess, requestData, dataExportFileFormat, dataExport.getSql());
            // increase the file download count by one
            Integer fileDownloadCount = dataExportProcess.getFileDownloadCount();
            fileDownloadCount += 1;
            dataExportProcess.updateFileDownloadCount(fileDownloadCount);

            // save the outgoing payment process entity and flush the changes immediately
            dataExportProcessRepository.saveAndFlush(dataExportProcess);

            return Response.ok(dataExportFileData.getInputStream()).
                    header("Content-Disposition", "attachment; filename=\"" + dataExportFileData.getFileName() + "\"").
                    header("Content-Type", dataExportFileData.getContentType())
                    .build();
        }catch(EntityMismatchException eme){return Response.status(Response.Status.BAD_REQUEST).type(eme.getLocalizedMessage()).build();
        }catch(Exception e) {return Response.serverError().tag(e.getMessage()).build();}
    }

    @Override
    public Collection<DataExportEntityData> retrieveDataExportTemplate(final String entityName) {
        List<DataExportBaseEntityEnum> entities = new ArrayList<>();
        Collection<DataExportEntityData> entityDataCollection = new ArrayList<>();

        try {
            if (entityName != null) {
                DataExportBaseEntityEnum entity = DataExportBaseEntityEnum.valueOf(entityName.toUpperCase());
                if (entity == null || entity.getTablename().isEmpty()) {
                    throw new IllegalArgumentException(entityName);
                }
                entities.add(entity);
            } else {throw new IllegalArgumentException();}
        } catch (IllegalArgumentException iae){
            for (DataExportBaseEntityEnum entity : DataExportBaseEntityEnum.values()) {
                entities.add(entity);
            }
        }
        
        for (DataExportBaseEntityEnum entity : entities) {
            Collection<DatatableData> datatables = new ArrayList<>();
            Collection<DataExportFieldData> fields = new ArrayList<>();
            Collection<RegisteredTable> registeredTables = this.registeredTableRepository.
                    findAllByApplicationTableName(entity.getTablename());
            Collection<EntityLabel> entityLabels = this.entityLabelRepository.findAllByTable(entity.getTablename());
            
            for (RegisteredTable registeredTable : registeredTables) {
                Long category = (registeredTable.getCategory() != null) ? registeredTable.getCategory().longValue() : null;
                
                DatatableData datatableData = DatatableData.create(registeredTable.getApplicationTableName(), 
                        registeredTable.getRegisteredTableName(), null, category, null, 
                        registeredTable.isSystemDefined(), registeredTable.getDisplayName());
                
                datatables.add(datatableData);
            }
            
            for (EntityLabel entityLabel : entityLabels) {
                DataExportFieldData dataExportFieldData = DataExportFieldData.newInstance(
                        entityLabel.getId(), entityLabel.getTable(), entityLabel.getField(), entityLabel.getJsonParam(), 
                        entityLabel.getLabel(), entityLabel.getReferenceTable(), entityLabel.getReferenceField());
                
                fields.add(dataExportFieldData);
            }
            
            DataExportEntityData dataExportEntityData = DataExportEntityData.newInstance(
                    entity.name(), entity.getTablename(), datatables, fields);
            
            entityDataCollection.add(dataExportEntityData);
        }

        return entityDataCollection;
    }
    
    private Collection<DatatableData> retrieveEntities(final List<DataExportBaseEntityEnum> entityNames) {
        
        
        return null;
    }

    private List<Map<String, String>> assembleDataTableNames(final List<DataExportBaseEntityEnum> entityNames, final String type){
        List<Map<String,String>> tables = new ArrayList<>();

        for (DataExportBaseEntityEnum entity : entityNames){
            if(type.equals(DataExportApiConstants.DATATABLE_NAME)) {
                List<String> dataTables = assembleExportDataTables(entity);
                for (String dataTable : dataTables) {
                    Map<String, String> table = new HashMap<>();
                    table.put(DataExportApiConstants.ENTITY_NAME, entity.name());
                    table.put(type, dataTable);
                    tables.add(table);
                }
            }
            if(type.equals(DataExportApiConstants.ENTITY_TABLE)){
                Map<String, String> table = new HashMap<>();
                table.put(DataExportApiConstants.ENTITY_NAME, entity.name());
                table.put(type, entity.getTablename());
                tables.add(table);
                if(tables.isEmpty()){throw new RuntimeException("Something went wrong while assembling the entitytables.");}
            }
        }

        return tables;
    }

    private List<String[]> getFileData(final List<Map<String, Object>> rawfileData){
        try {
            final List<String[]> fileData = new ArrayList<>();
            final Integer arraySize = rawfileData.get(0).size();
            final String[] row = new String[arraySize];
            int i = 0;

            for (String key : rawfileData.get(0).keySet()) {
                row[i] = key;
                i++;
            }

            for(Map<String, Object> entry : rawfileData){
                String[] valuerow = new String[arraySize];
                for(i = 0; i < arraySize ; i++){
                    if(entry.get(row[i])!=null) {
                        valuerow[i] = '"' + entry.get(row[i]).toString() + '"';
                    }
                }
                fileData.add(valuerow);
            }

            return fileData;
        } catch (InvalidParameterException ipe){return null;
        } catch (IndexOutOfBoundsException iob){return null;
        } catch (NullPointerException npe){return null;}
    }

    private List<String> assembleExportDataTables(final DataExportBaseEntityEnum entity){
        List<String> exportDataTables = new ArrayList<>();
        for (RegisteredTable registeredTable : this.registeredTableRepository.findAllByApplicationTableName(entity.getTablename())){
            exportDataTables.add(registeredTable.getRegisteredTableName());
        }
        return exportDataTables;
    }

    private DataExportFileData downloadDataExportFileData(final DataExportProcess dataExportProcess,
            final DataExportRequestData requestData, final DataExportFileFormat fileFormat, final String sql) {

        final String fileName = dataExportProcess.getFileName();
        final List<Map<String, Object>> rawfileData = reassignEnumValues(sql, requestData.getBaseEntity());
        final List<String[]> fileData = getFileData(rawfileData);
        DataExportFileData dataExportFileData = null;

        switch (fileFormat) {
            case XLS:
                dataExportFileData = FileHelper.createDataExportXlsFile(rawfileData, fileName);
                break;
            case CSV:
                String[] csvFileHeaders;
                List<String> fieldNames = new ArrayList<>(rawfileData.get(0).keySet());

                csvFileHeaders = fieldNames.toArray(new String[fieldNames.size()]);

                dataExportFileData = FileHelper.createDataExportCsvFile(fileData, fileName,
                        csvFileHeaders);
                break;

            case XML:
                dataExportFileData = FileHelper.createDataExportXmlFile(fileName);

                // write data to XML file
                XmlFileHelper.writeToFile(fileData, dataExportFileData.getFile());
                break;
        }

        return dataExportFileData;
    }

    private List<Map<String, Object>> reassignEnumValues(String sql, DataExportBaseEntityEnum entity) {
        final List<Map<String, Object>> fileData = new ArrayList<>();
        final List<Map<String, Object>> rawfileData = this.jdbcTemplate.queryForList(sql);
        final List<String> cbKeys = findLabelsInSqlWithFieldCharactaristic(sql,"_cb_");
        SqlRowSet rowSet = this.jdbcTemplate.queryForRowSet(sql);

        for (Map<String, Object> entry : rawfileData) {
            for (String key : entry.keySet()) {
                Object value;
                if(cbKeys.contains(key)){
                    value = entry.get(key);
                    StringBuilder toValue = new StringBuilder("");
                    if(value!=null && value.toString().length()>0){
                        String stringValue = value.toString();
                        int index = 0;
                        Long id;
                        CodeValue codeValue;
                        while(stringValue.indexOf(",",index)>0){
                            int newIndex = stringValue.indexOf(",", index);
                            id = Long.valueOf(stringValue.substring(index, newIndex));
                            index = newIndex + 1;
                            codeValue = this.codeValueRepository.findOne(id);
                            toValue.append(codeValue.label() + ", ");
                        }
                        id = Long.valueOf(stringValue.substring(index));
                        codeValue = this.codeValueRepository.findOne(id);
                        toValue.append(codeValue.label());
                        value = toValue.toString();
                    }
                }else {
                    try {
                        EntityLabel entityLabel = this.entityLabelRepository.findOneByTableAndJsonParam(entity.getTablename(), key);
                        if (entityLabel != null) {
                            String fieldName = entityLabel.getField();
                            value = this.enumValueCollectionRepositoryWrapper.findOneByFieldNameAndId(fieldName, Long.valueOf(entry.get(key).toString())).getValue();
                        } else {
                            throw new Exception();
                        }
                    } catch (Exception e) {
                        value = entry.get(key);
                    }
                }
                entry.put(key, value);
            }
            fileData.add(entry);
        }
        return fileData;
    }

    private List<String> findLabelsInSqlWithFieldCharactaristic(String sql, String with){
        List<String> keys = new ArrayList<>();
        int index = 0;

        while (sql.indexOf(with, index) > 0) {
            index = sql.indexOf(with, index) + 1;
            Integer labelStart = sql.indexOf("'", index) + 1;
            Integer labelEnd = sql.indexOf("'", labelStart + 1);
            if (labelStart <= 0 || labelEnd <= 0 || labelEnd <= labelStart) {
                throw new RuntimeException("while loop code in DataExportReadPlatformServiceImpl.findLabelsInSqlWith needs to be reevaluated");
            }
            String key = sql.substring(labelStart, labelEnd);
            keys.add(key);
        }
        return keys;
    }
}
