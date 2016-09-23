/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.scheduledemail.service;

import com.google.gson.JsonElement;
import org.mifosplatform.infrastructure.core.api.JsonCommand;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResult;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResultBuilder;
import org.mifosplatform.infrastructure.scheduledemail.data.EmailConfigurationData;
import org.mifosplatform.infrastructure.scheduledemail.data.EmailConfigurationValidator;
import org.mifosplatform.infrastructure.scheduledemail.domain.EmailConfiguration;
import org.mifosplatform.infrastructure.scheduledemail.domain.EmailConfigurationRepository;
import org.mifosplatform.infrastructure.scheduledemail.exception.EmailConfigurationSMTPUsernameNotValid;
import org.mifosplatform.infrastructure.security.service.PlatformSecurityContext;
import org.mifosplatform.useradministration.domain.AppUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
public class EmailConfigurationWritePlatformServiceImpl implements EmailConfigurationWritePlatformService {

    private final PlatformSecurityContext context;
    private final EmailConfigurationRepository repository;
    private final EmailConfigurationValidator emailConfigurationValidator;

    @Autowired
    public EmailConfigurationWritePlatformServiceImpl(final PlatformSecurityContext context, final EmailConfigurationRepository repository,
                                                      final EmailConfigurationValidator emailConfigurationValidator) {
        this.context = context;
        this.repository = repository;
        this.emailConfigurationValidator = emailConfigurationValidator;
    }

    @Override
    public CommandProcessingResult update(final JsonCommand command) {

            final AppUser currentUser = this.context.authenticatedUser();

            this.emailConfigurationValidator.validateUpdateConfiguration(command.json());
            final String smtpUsername = command.stringValueOfParameterNamed("SMTP_USERNAME");

            if(!this.emailConfigurationValidator.isValidEmail(smtpUsername)){
                throw new EmailConfigurationSMTPUsernameNotValid(smtpUsername);
            }


            final Map<String,Object> changes = new HashMap<>(4);

            Collection<EmailConfiguration> configurations = this.repository.findAll();
            /**
             *Default SMTP configuration added to flyway
             */
            for (EmailConfiguration config : configurations) {
                if(config.getName() !=null){
                    String value = command.stringValueOfParameterNamed(config.getName());
                    config.setValue(value); changes.put(config.getName(),value);
                    this.repository.saveAndFlush(config);
                }
            }

            return new CommandProcessingResultBuilder() //
                    .with(changes)
                    .build();

    }


}
