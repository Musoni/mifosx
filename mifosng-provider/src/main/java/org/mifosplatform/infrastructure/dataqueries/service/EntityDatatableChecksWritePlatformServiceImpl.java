/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataqueries.service;

import org.mifosplatform.infrastructure.core.api.JsonCommand;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResult;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResultBuilder;
import org.mifosplatform.infrastructure.core.exception.PlatformDataIntegrityException;
import org.mifosplatform.infrastructure.dataqueries.data.DatatableData;
import org.mifosplatform.infrastructure.dataqueries.data.EntityTables;
import org.mifosplatform.infrastructure.dataqueries.domain.EntityDatatableChecks;
import org.mifosplatform.infrastructure.dataqueries.domain.EntityDatatableChecksRepository;
import org.mifosplatform.infrastructure.dataqueries.exception.*;
import org.mifosplatform.infrastructure.security.service.PlatformSecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EntityDatatableChecksWritePlatformServiceImpl implements EntityDatatableChecksWritePlatformService {

    private final static Logger logger = LoggerFactory.getLogger(EntityDatatableChecksWritePlatformServiceImpl.class);

    private final PlatformSecurityContext context;
    private final EntityDatatableChecksDataValidator fromApiJsonDeserializer;
    private final EntityDatatableChecksRepository entityDatatableChecksRepository;
    private final ReadWriteNonCoreDataService readWriteNonCoreDataService;


    @Autowired
    public EntityDatatableChecksWritePlatformServiceImpl(final PlatformSecurityContext context,
                                                         final EntityDatatableChecksDataValidator fromApiJsonDeserializer,
                                                         final EntityDatatableChecksRepository entityDatatableChecksRepository,
                                                         final ReadWriteNonCoreDataService readWriteNonCoreDataService
    )
                                                      {
        this.context = context;
        this.fromApiJsonDeserializer = fromApiJsonDeserializer;
        this.entityDatatableChecksRepository = entityDatatableChecksRepository;
        this.readWriteNonCoreDataService = readWriteNonCoreDataService;


    }

    @Transactional
    @Override
    public CommandProcessingResult createCheck(final JsonCommand command) {

        try {
            this.context.authenticatedUser();

            this.fromApiJsonDeserializer.validateForCreate(command.json());

            // check if the datatable is linked to the entity

            Long datatableId = command.longValueOfParameterNamed("datatableId");
            DatatableData datatableData = this.readWriteNonCoreDataService.retrieveDatatableById(datatableId);

            if(datatableData == null){throw new DatatableNotFoundException(datatableId);}

            final String entity= command.stringValueOfParameterNamed("entity");
            final String foreignKeyColumnName = EntityTables.getForeignKeyColumnNameOnDatatable(entity);
            final boolean columnExist = datatableData.hasColumn(foreignKeyColumnName);

            logger.info(datatableData.getRegisteredTableName()+"has column "+foreignKeyColumnName+" ? "+columnExist);

            if(!columnExist){ throw new EntityDatatableCheckNotSupportedException(datatableData.getRegisteredTableName(), entity);}

            final Long productLoanId = command.longValueOfParameterNamed("productId");
            final Long status = command.longValueOfParameterNamed("status");

            /**
                if the submitted check does not have a product id
                we check if there is already a check with a product Id.
                if it is the case, then one without product id cannot be allow.
                Mainly because a check without product id means the check applies to all product
            **/
            if(productLoanId==null && entity.equals("m_loan")){

                List<EntityDatatableChecks> entityDatatableCheck = this.entityDatatableChecksRepository.findByEntityStatusAndDatatableId(entity,status,datatableId);

                if(!entityDatatableCheck.isEmpty()){

                    throw new EntityDatatableCheckNotAllow(entity);

                }

            }

            final EntityDatatableChecks check = EntityDatatableChecks.fromJson(command);

            this.entityDatatableChecksRepository.saveAndFlush(check);

            return new CommandProcessingResultBuilder() //
                    .withCommandId(command.commandId()) //
                    .withEntityId(check.getId()) //
                    .build();
        } catch (final DataIntegrityViolationException dve) {
            handleReportDataIntegrityIssues(command, dve);
            return CommandProcessingResult.empty();
        }
    }

    public void runTheCheck(final Long entityId,final String entityName,final Long statusCode,String foreignKeyColumn)
    {
        final List<EntityDatatableChecks> tableRequiredBeforeClientActivation= entityDatatableChecksRepository.findByEntityAndStatus(entityName,statusCode);

        if(tableRequiredBeforeClientActivation != null){

            for(EntityDatatableChecks t : tableRequiredBeforeClientActivation){

                final String datatableName = t.getDatatable().getRegisteredTableName();
                final String displayName = t.getDatatable().getDisplayName();
                final Long countEntries = readWriteNonCoreDataService.countDatatableEntries(datatableName,entityId,foreignKeyColumn);

                logger.info("The are "+countEntries+" entries in the table "+ datatableName);
                if(countEntries.intValue()==0){throw new DatatabaleEntryRequiredException(datatableName,displayName);}
            }
        }

    }

    public void runTheCheckForLoan(final Long entityId,final String entityName,final Long statusCode,String foreignKeyColumn,long productLoanId)
    {
        final List<EntityDatatableChecks> tableRequiredBeforAction= entityDatatableChecksRepository.findByEntityStatusAndLoanProduct(entityName, statusCode, productLoanId);

        if(tableRequiredBeforAction != null){

            for(EntityDatatableChecks t : tableRequiredBeforAction){

                final String datatableName = t.getDatatable().getRegisteredTableName();
                final String displayName = t.getDatatable().getDisplayName();
                final Long countEntries = readWriteNonCoreDataService.countDatatableEntries(datatableName,entityId,foreignKeyColumn);

                logger.info("The are "+countEntries+" entries in the table "+ datatableName);
                if(countEntries.intValue()==0){throw new DatatabaleEntryRequiredException(datatableName,displayName);}
            }
        }

    }


    @Transactional
    @Override
    public CommandProcessingResult deleteCheck(final Long entityDatatableCheckId) {

        final EntityDatatableChecks check = this.entityDatatableChecksRepository.findOne(entityDatatableCheckId);
        if (check == null) { throw new EntityDatatableChecksNotFoundException(entityDatatableCheckId); }

        this.entityDatatableChecksRepository.delete(check);

        return new CommandProcessingResultBuilder() //
                .withEntityId(entityDatatableCheckId) //
                .build();
    }

    /*
     * Guaranteed to throw an exception no matter what the data integrity issue
     * is.
     */
    private void handleReportDataIntegrityIssues(final JsonCommand command, final DataIntegrityViolationException dve) {

         final Throwable realCause = dve.getMostSpecificCause();
        if (realCause.getMessage().contains("FOREIGN KEY (`x_registered_table_id`)")) {
            final long datatableId = command.longValueOfParameterNamed("datatableId");
            throw new PlatformDataIntegrityException("error.msg.entityDatatableCheck.foreign.key.constraint", "datatable with id '" + datatableId + "' do not exist",
                    "datatableId", datatableId);
        }

        if(realCause.getMessage().contains("unique_entity_check")){

            final long datatableId = command.longValueOfParameterNamed("datatableId");
            final long status = command.longValueOfParameterNamed("status");
            final String entity = command.stringValueOfParameterNamed("entity");
            throw new PlatformDataIntegrityException("error.msg.entityDatatableCheck.duplicate.entry", "the entity datatable check for status: '" + status + "' and datatable id '"+ datatableId+"' on entity '"+entity+"' already exist",
                    "status","datatableId","entity",status,datatableId,entity);
        }

        logger.error(dve.getMessage(), dve);
        throw new PlatformDataIntegrityException("error.msg.report.unknown.data.integrity.issue",
                "Unknown data integrity issue with resource: " + realCause.getMessage());
    }


}