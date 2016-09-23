/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.scheduledemail.service;

import org.mifosplatform.infrastructure.scheduledemail.data.EmailBusinessRulesData;
import org.mifosplatform.infrastructure.scheduledemail.data.EmailCampaignData;

import java.util.Collection;

public interface EmailCampaignReadPlatformService {

    Collection<EmailBusinessRulesData> retrieveAll();

    EmailBusinessRulesData retrieveOneTemplate(Long resourceId);

    EmailCampaignData retrieveOne(Long resourceId);

    Collection<EmailCampaignData> retrieveAllCampaign();

    Collection<EmailCampaignData> retrieveAllScheduleActiveCampaign();

}
