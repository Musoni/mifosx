/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.scheduledemail.domain;

import com.google.gson.JsonElement;
import org.mifosplatform.infrastructure.core.api.JsonCommand;
import org.mifosplatform.infrastructure.core.serialization.FromJsonHelper;
import org.mifosplatform.infrastructure.scheduledemail.EmailApiConstants;
import org.mifosplatform.infrastructure.scheduledemail.exception.EmailNotFoundException;
import org.mifosplatform.organisation.staff.domain.Staff;
import org.mifosplatform.organisation.staff.domain.StaffRepositoryWrapper;
import org.mifosplatform.portfolio.client.domain.Client;
import org.mifosplatform.portfolio.client.domain.ClientRepositoryWrapper;
import org.mifosplatform.portfolio.group.domain.Group;
import org.mifosplatform.portfolio.group.domain.GroupRepositoryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EmailMessageAssembler {

    private final EmailMessageRepository emailMessageRepository;
    private final GroupRepositoryWrapper groupRepository;
    private final ClientRepositoryWrapper clientRepository;
    private final StaffRepositoryWrapper staffRepository;
    private final FromJsonHelper fromApiJsonHelper;

    @Autowired
    public EmailMessageAssembler(final EmailMessageRepository emailMessageRepository, final GroupRepositoryWrapper groupRepositoryWrapper,
                               final ClientRepositoryWrapper clientRepository, final StaffRepositoryWrapper staffRepository,
                               final FromJsonHelper fromApiJsonHelper) {
        this.emailMessageRepository = emailMessageRepository;
        this.groupRepository = groupRepositoryWrapper;
        this.clientRepository = clientRepository;
        this.staffRepository = staffRepository;
        this.fromApiJsonHelper = fromApiJsonHelper;
    }

    public EmailMessage assembleFromJson(final JsonCommand command) {

        final JsonElement element = command.parsedJson();

        String emailAddress = null;

        Group group = null;
        if (this.fromApiJsonHelper.parameterExists(EmailApiConstants.groupIdParamName, element)) {
            final Long groupId = this.fromApiJsonHelper.extractLongNamed(EmailApiConstants.groupIdParamName, element);
            group = this.groupRepository.findOneWithNotFoundDetection(groupId);
        }

        Client client = null;
        if (this.fromApiJsonHelper.parameterExists(EmailApiConstants.clientIdParamName, element)) {
            final Long clientId = this.fromApiJsonHelper.extractLongNamed(EmailApiConstants.clientIdParamName, element);
            client = this.clientRepository.findOneWithNotFoundDetection(clientId);
            emailAddress = client.emailAddress();
        }

        Staff staff = null;
        if (this.fromApiJsonHelper.parameterExists(EmailApiConstants.staffIdParamName, element)) {
            final Long staffId = this.fromApiJsonHelper.extractLongNamed(EmailApiConstants.staffIdParamName, element);
            staff = this.staffRepository.findOneWithNotFoundDetection(staffId);
            emailAddress = staff.emailAddress();
        }

        final String message = this.fromApiJsonHelper.extractStringNamed(EmailApiConstants.messageParamName, element);
        final String emailSubject = this.fromApiJsonHelper.extractStringNamed(EmailApiConstants.subjectParamName, element);

        return EmailMessage.pendingEmail(group, client, staff,null,emailSubject, message,emailAddress,null);
    }

    public EmailMessage assembleFromResourceId(final Long resourceId) {
        final EmailMessage email = this.emailMessageRepository.findOne(resourceId);
        if (email == null) { throw new EmailNotFoundException(resourceId); }
        return email;
    }
}