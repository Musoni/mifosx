/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.service;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.mifosplatform.infrastructure.core.api.JsonCommand;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResult;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResultBuilder;
import org.mifosplatform.infrastructure.core.exception.InvalidJsonException;
import org.mifosplatform.infrastructure.core.exception.UnsupportedParameterException;
import org.mifosplatform.infrastructure.core.serialization.FromJsonHelper;
import org.mifosplatform.infrastructure.core.service.DateUtils;
import org.mifosplatform.infrastructure.dataexport.api.DataExportApiConstants;
import org.mifosplatform.infrastructure.dataexport.data.*;
import org.mifosplatform.infrastructure.dataexport.domain.*;
import org.mifosplatform.infrastructure.dataqueries.domain.RegisteredTableMetaData;
import org.mifosplatform.infrastructure.dataqueries.domain.RegisteredTableMetaDataRepository;
import org.mifosplatform.infrastructure.dataqueries.domain.RegisteredTableRepository;
import org.mifosplatform.infrastructure.security.service.PlatformSecurityContext;
import org.mifosplatform.useradministration.domain.AppUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Type;
import java.util.*;

import static org.mifosplatform.infrastructure.dataexport.api.DataExportApiConstants.DATA_EXPORT_FILENAME_DATETIME_FORMAT_PATTERN;

@Service
public class DataExportWritePlatformServiceImpl implements DataExportWritePlatformService {

    private final static Logger logger = LoggerFactory.getLogger(DataExportWritePlatformServiceImpl.class);

    private final PlatformSecurityContext context;
    private final ExportDataValidator fromApiJsonDeserializer;
    private final DataExportReadPlatformService readPlatformService;
    private final FromJsonHelper fromApiJsonHelper;
    private final DataExportRepository dataExportRepository;
    private final DataExportProcessRepository dataExportProcessRepository;
    private final EntityLabelRepository entityLabelRepository;
    private final RegisteredTableRepository registeredTableRepository;
    private final RegisteredTableMetaDataRepository registeredTableMetaDataRepository;

    @Autowired
    public DataExportWritePlatformServiceImpl(final PlatformSecurityContext context, 
            final FromJsonHelper fromApiJsonHelper, 
            final ExportDataValidator fromApiJsonDeserializer,  
            final DataExportRepository dataExportRepository, 
            final DataExportProcessRepository dataExportProcessRepository, 
            final EntityLabelRepository entityLabelRepository, 
            final RegisteredTableMetaDataRepository registeredTableMetaDataRepository, 
            final RegisteredTableRepository registeredTableRepository, 
            final DataExportReadPlatformService readPlatformService) {
        this.context = context;
        this.fromApiJsonHelper = fromApiJsonHelper;
        this.fromApiJsonDeserializer = fromApiJsonDeserializer;
        this.readPlatformService = readPlatformService;
        this.dataExportRepository = dataExportRepository;
        this.dataExportProcessRepository = dataExportProcessRepository;
        this.entityLabelRepository = entityLabelRepository;
        this.registeredTableMetaDataRepository = registeredTableMetaDataRepository;
        this.registeredTableRepository = registeredTableRepository;
    }

    @Transactional
    @Override
    public CommandProcessingResult createDataExport(final JsonCommand command) {

        try {
            final AppUser currentUser = this.context.authenticatedUser();

            this.fromApiJsonDeserializer.validateCreateDataExportRequest(command);

            JsonElement element = command.parsedJson();

            final String entityName = this.fromApiJsonHelper.extractStringNamed(DataExportApiConstants.BASE_ENTITY_PARAM_NAME, 
                    element);
            DataExportRequestData requestData = this.readPlatformService.retrieveDataExportRequestData(entityName);
            final DataExportBaseEntityEnum entity = requestData.getBaseEntity();

            final List<String> exportDataTables = new ArrayList<>();
            final List<DataExportFilter> dataExportFilters = new ArrayList<>();

            final String[] dataTables = this.fromApiJsonHelper.extractArrayNamed(DataExportApiConstants.DATATABLES_PARAM_NAME, 
                    element);

            if (dataTables != null && dataTables.length > 0) {
                for(String dataTable : dataTables){
                    exportDataTables.add(dataTable);
                }
            }
            
            final DataExportCreateRequestData dataExportCreateRequestData = this.fromApiJsonHelper.fromJson(
                    command.json(), DataExportCreateRequestData.class);
            
            final Map<String, String> filters = (dataExportCreateRequestData != null) ? 
                    dataExportCreateRequestData.getFilters() : null;
                    
            if (filters != null) {
                Iterator<Map.Entry<String, String>> iterator = filters.entrySet().iterator();
                
                while (iterator.hasNext()) {
                    Map.Entry<String, String> filter = iterator.next();
                    DataExportFilter dataExportFilter = new DataExportFilter(entity.getTablename(), filter.getValue(), 
                            filter.getKey());
                    
                    dataExportFilters.add(dataExportFilter);
                }
            }

            if(exportDataTables.size() > 0 || dataExportFilters.size() > 0){
                requestData = this.readPlatformService.retrieveDataExportRequestData(entityName,dataExportFilters,exportDataTables);
            }

            final Map<String, List<String>> sqlMap = getBaseEntitySql(requestData);

            final DateTimeFormatter dateStringFormat = DateTimeFormat.forPattern(DataExportApiConstants.SUBMITTEDON_DATE_FORMAT);
            final String submitDate = this.fromApiJsonHelper.extractStringNamed(DataExportApiConstants.ENTITY_SUBMITDATE,element);
            final Date submittedOnDate = (submitDate != null && submitDate.length()>0 ? dateStringFormat.parseLocalDate(submitDate).toDate() : LocalDate.now().toDate());

            final String sql = assembleSqlString(sqlMap);


            final DataExport newDataExport = DataExport.instance(entity.getName(),command.json(),sql);

            this.dataExportRepository.save(newDataExport);

            final LocalDateTime processEndDate = DateUtils.getLocalDateTimeOfTenant();
            final String fileName = processEndDate.toString(DATA_EXPORT_FILENAME_DATETIME_FORMAT_PATTERN);
            final Integer processStatus = DataExportProcessStatus.FINISHED_OK.getId();

            final DataExportProcess dataExportProcess = DataExportProcess.instance(
                    newDataExport, fileName, processStatus, currentUser, submittedOnDate, null, null, 0);

            this.dataExportRepository.saveAndFlush(newDataExport);
            this.dataExportProcessRepository.saveAndFlush(dataExportProcess);

            return new CommandProcessingResultBuilder() //
                    .withCommandId(command.commandId()) //
                    .withEntityId(dataExportProcess.getId()) //
                    .build();

        } catch(final InvalidJsonException ije) {
            logger.error(ije.getMessage());
            
            return CommandProcessingResult.empty();
        } catch(final UnsupportedParameterException upe){
            logger.error(upe.getMessage());
            
            return CommandProcessingResult.empty();
        }
    }

    /*
    * Sorts the data from the DataExportRequestData into a HashMap that's more easily converted into an sql String
    */
    private Map<String, List<String>> getBaseEntitySql (final DataExportRequestData requestData){
        final Map<String,List<String>> sql = new HashMap<>();
        final List<String> select = new ArrayList<>();
        final List<String> from = new ArrayList<>();
        final List<String> where = new ArrayList<>();
        final List<String> group = new ArrayList<>();
        final List<String> order = new ArrayList<>();
        final DataExportBaseEntityEnum entity = requestData.getBaseEntity();
        final List<String> fieldNames = requestData.getDisplayedFieldNames();
        final List<DataExportFilter> dataExportFilters = requestData.getDataExportFiltersList();
        final List<String> exportDataTables = requestData.getExportDatatables();
        final List<EntityLabel> labels = new ArrayList<>();
        final List<String> labelConflictChecklist = new ArrayList<>();
        Integer codeValueTranslations = 0;

        for(String fieldName : fieldNames){
            EntityLabel entityLabel = this.entityLabelRepository.findOneByTableAndField(entity.getTablename(),fieldName);

            if(entityLabel == null){throw new InputMismatchException("No EntityLabel found with entity table " + entity.getTablename() + " and field name " + fieldName + ".");}

            String label = checkForDuplicateLabels(entityLabel.getLabel(),labelConflictChecklist,null);
            labelConflictChecklist.add(label);

            if(fieldName.contains("userid") || fieldName.endsWith("_by")){
                String user = "user" + codeValueTranslations++;
                select.add(user + ".username as " + label);
                from.add("left join m_appuser " + user + " on " + user + ".id = " +
                        entity.getTablename() + "." + fieldName);
            }else {
                if (entityLabel.getReferenceTable() != null) {
                    labels.add(entityLabel);
                } else {
                    select.add(entity.getTablename() + ".`" + fieldName + "` as " + label);
                }
            }
        }

         from.add(entity.getTablename());

        if(labels.size()>0){
            for(EntityLabel label : labels){
                select.add(label.getReferenceTable() + ".`" + label.getReferenceField() + "` as " + label.getLabel());
                from.add("left join " + label.getReferenceTable() + " on " + label.getReferenceTable() + ".`" + DataExportApiConstants.ENTITY_ID
                        + "` = " + entity.getTablename() + ".`" + label.getField() + "`");
            }
        }

        if(dataExportFilters.size()>0){
            for(DataExportFilter filter : dataExportFilters){
                where.add(filter.getSearchQuery());
            }
        }

        if(exportDataTables.size()>0){
            for(String dataTable : exportDataTables){
                String entityReference = entity.getName().concat("_id");
                String prefix = this.registeredTableRepository.findOneByRegisteredTableName(dataTable).getDisplayName();
                from.add("left join " + dataTable + " on " + dataTable + ".`" + entityReference
                        + "` = " + entity.getTablename() + "." + DataExportApiConstants.ENTITY_ID);
                for(RegisteredTableMetaData metaData : this.registeredTableMetaDataRepository.findAllByTableName(dataTable)){
                    String label = checkForDuplicateLabels(metaData.getLabelName(),labelConflictChecklist,prefix);
                    labelConflictChecklist.add(label);
                    String fieldName = metaData.getFieldName();

                    if(fieldName.contains("_cd_")){
                        String mcv = "mcv" + codeValueTranslations++;
                        select.add(mcv + ".code_value as " + label);
                        from.add("left join m_code_value " + mcv + " on " + mcv + ".id = " +
                        metaData.getTableName() + "." + fieldName);
                    } else if(fieldName.contains("userid") || fieldName.endsWith("_by")){
                        String user = "user" + codeValueTranslations++;
                        select.add(user + ".username as " + label);
                        from.add("left join m_appuser " + user + " on " + user + ".id = " +
                                metaData.getTableName() + "." + fieldName);
                    } else{
                        select.add(metaData.getTableName() + ".`" + fieldName + "` as " + label);
                    }
                }
            }
        }

        sql.put("select",select);
        sql.put("from",from);
        sql.put("where",where);
        sql.put("group",group);
        sql.put("order",order);

        return sql;
    }

    private String checkForDuplicateLabels(String label, List<String> checkList, String prefix){
        String newLabel = label.replaceAll("'", "");
        Integer i = 1;

        while(checkList.contains("'" + newLabel + "'")){
            if(prefix!=null && prefix.length()>0 && i == 1){
                newLabel = prefix.replaceAll("'", "") + " " + newLabel;
            }else {
                if (newLabel.endsWith((i.toString()))) {
                    newLabel = newLabel.substring(0, newLabel.length() - 2);
                    i++;
                }
                newLabel += i;
            }
        }

        if(!newLabel.startsWith("'")){
            newLabel = "'" + newLabel;
        }

        if(!newLabel.endsWith("'")){
            newLabel += "'";
        }

        return newLabel;
    }

    /*
    * assembles the data from the sqlMap into an sql String
    */
    private String assembleSqlString (final Map<String, List<String>> sqlMap){
        StringBuilder sql = new StringBuilder("");

        for (Integer i = 0 ; i < sqlMap.keySet().size() ; i++){
            String key;
            switch(i){
                case 0: key = "select"; break;
                case 1: key = "from"; break;
                case 2: key = "where"; break;
                case 3: key = "group"; break;
                case 4: key = "order"; break;
                default: key = null;
            }
            if(sqlMap.get(key).size()>0){sql.append(" " + key + " ");}
            for(String param : sqlMap.get(key)){
                if(key.equals("select") && sqlMap.get(key).indexOf(param)>0){sql.append(", ");}
                if(key.equals("from") && sqlMap.get(key).indexOf(param)>0){sql.append(" ");}
                if(key.equals("where") && sqlMap.get(key).indexOf(param)>0){sql.append(" and ");}
                sql.append(param);
            }
        }

        return sql.toString();
    }
}
