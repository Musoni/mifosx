/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.scheduledemail.service;

import org.mifosplatform.infrastructure.core.api.JsonCommand;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResult;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResultBuilder;
import org.mifosplatform.infrastructure.core.exception.PlatformDataIntegrityException;
import org.mifosplatform.infrastructure.scheduledemail.data.EmailDataValidator;
import org.mifosplatform.infrastructure.scheduledemail.domain.EmailMessage;
import org.mifosplatform.infrastructure.scheduledemail.domain.EmailMessageAssembler;
import org.mifosplatform.infrastructure.scheduledemail.domain.EmailMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class EmailWritePlatformServiceJpaRepositoryImpl implements EmailWritePlatformService {

    private final static Logger logger = LoggerFactory.getLogger(EmailWritePlatformServiceJpaRepositoryImpl.class);

    private final EmailMessageAssembler assembler;
    private final EmailMessageRepository repository;
    private final EmailDataValidator validator;

    @Autowired
    public EmailWritePlatformServiceJpaRepositoryImpl(final EmailMessageAssembler assembler, final EmailMessageRepository repository,
            final EmailDataValidator validator) {
        this.assembler = assembler;
        this.repository = repository;
        this.validator = validator;
    }

    @Transactional
    @Override
    public CommandProcessingResult create(final JsonCommand command) {

        try {
            this.validator.validateCreateRequest(command);

            final EmailMessage message = this.assembler.assembleFromJson(command);

            // TODO: at this point we also want to fire off request using third
            // party service to send Email.
            // TODO: decision to be made on wheter we 'wait' for response or use
            // 'future/promise' to capture response and update the EmailMessage
            // table
            this.repository.save(message);

            return new CommandProcessingResultBuilder() //
                    .withCommandId(command.commandId()) //
                    .withEntityId(message.getId()) //
                    .build();
        } catch (final DataIntegrityViolationException dve) {
            handleDataIntegrityIssues(command, dve);
            return CommandProcessingResult.empty();
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult update(final Long resourceId, final JsonCommand command) {

        try {
            this.validator.validateUpdateRequest(command);

            final EmailMessage message = this.assembler.assembleFromResourceId(resourceId);
            final Map<String, Object> changes = message.update(command);
            if (!changes.isEmpty()) {
                this.repository.save(message);
            }

            return new CommandProcessingResultBuilder() //
                    .withCommandId(command.commandId()) //
                    .withEntityId(resourceId) //
                    .with(changes) //
                    .build();
        } catch (final DataIntegrityViolationException dve) {
            handleDataIntegrityIssues(command, dve);
            return CommandProcessingResult.empty();
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult delete(final Long resourceId) {

        try {
            final EmailMessage message = this.assembler.assembleFromResourceId(resourceId);
            this.repository.delete(message);
            this.repository.flush();
        } catch (final DataIntegrityViolationException dve) {
            handleDataIntegrityIssues(null, dve);
            return CommandProcessingResult.empty();
        }
        return new CommandProcessingResultBuilder().withEntityId(resourceId).build();
    }

    /*
     * Guaranteed to throw an exception no matter what the data integrity issue
     * is.
     */
    private void handleDataIntegrityIssues(@SuppressWarnings("unused") final JsonCommand command, final DataIntegrityViolationException dve) {
        final Throwable realCause = dve.getMostSpecificCause();

        if (realCause.getMessage().contains("email_address")) { throw new PlatformDataIntegrityException("error.msg.scheduledemail.no.email.address.exists",
                "The group, client or staff provided has no email address.", "id"); }

        logger.error(dve.getMessage(), dve);
        throw new PlatformDataIntegrityException("error.msg.scheduledemail.unknown.data.integrity.issue",
                "Unknown data integrity issue with resource: " + realCause.getMessage());
    }
}