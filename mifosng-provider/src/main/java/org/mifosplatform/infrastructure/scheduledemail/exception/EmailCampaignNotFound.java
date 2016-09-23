/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.scheduledemail.exception;

import org.mifosplatform.infrastructure.core.exception.AbstractPlatformResourceNotFoundException;

public class EmailCampaignNotFound extends AbstractPlatformResourceNotFoundException{

    public EmailCampaignNotFound(final Long resourceId) {
        super("error.msg.scheduledemail.campaign.identifier.not.found", "EMAIL_CAMPAIGN with identifier `" + resourceId + "` does not exist", resourceId);
    }
}
