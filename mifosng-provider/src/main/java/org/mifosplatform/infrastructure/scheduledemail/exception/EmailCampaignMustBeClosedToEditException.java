/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.scheduledemail.exception;


import org.mifosplatform.infrastructure.core.exception.AbstractPlatformDomainRuleException;

public class EmailCampaignMustBeClosedToEditException extends AbstractPlatformDomainRuleException {

    public EmailCampaignMustBeClosedToEditException(final Long resourceId) {
        super("error.msg.scheduledemail.campaign.cannot.be.updated",
                "Campaign with identifier " + resourceId + " cannot be updated as it is not in `Closed` state.", resourceId);
    }
}
