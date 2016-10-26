/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.scheduledemail.service;

import org.mifosplatform.infrastructure.core.api.JsonCommand;
import org.mifosplatform.infrastructure.core.api.JsonQuery;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResult;
import org.mifosplatform.infrastructure.jobs.exception.JobExecutionException;
import org.mifosplatform.infrastructure.scheduledemail.data.PreviewCampaignMessage;

public interface EmailCampaignWritePlatformService {

    CommandProcessingResult create(JsonCommand command);

    CommandProcessingResult update(Long resourceId, JsonCommand command);

    CommandProcessingResult delete(Long resourceId);

    CommandProcessingResult activateEmailCampaign(Long campaignId, JsonCommand command);

    CommandProcessingResult closeEmailCampaign(Long campaignId, JsonCommand command);

    CommandProcessingResult reactivateEmailCampaign(Long campaignId, JsonCommand command);

    void storeTemplateMessageIntoEmailOutBoundTable() throws JobExecutionException;

    PreviewCampaignMessage previewMessage(JsonQuery query);

    void sendEmailMessage() throws JobExecutionException;

}
