/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.scheduledemail.domain;

import org.mifosplatform.infrastructure.core.data.EnumOptionData;

public class EmailCampaignStatusEnumerations {
    public static EnumOptionData status(final Integer statusId) {
        return status(EmailCampaignStatus.fromInt(statusId));
    }

    public static EnumOptionData status(final EmailCampaignStatus status) {
        EnumOptionData optionData = new EnumOptionData(EmailCampaignStatus.INVALID.getValue().longValue(),
                EmailCampaignStatus.INVALID.getCode(), "Invalid");
        switch (status) {
            case INVALID:
                optionData = new EnumOptionData(EmailCampaignStatus.INVALID.getValue().longValue(),
                        EmailCampaignStatus.INVALID.getCode(), "Invalid");
                break;
            case PENDING:
                optionData = new EnumOptionData(EmailCampaignStatus.PENDING.getValue().longValue(),
                        EmailCampaignStatus.PENDING.getCode(), "Pending");
                break;
            case ACTIVE:
                optionData = new EnumOptionData(EmailCampaignStatus.ACTIVE.getValue().longValue(), EmailCampaignStatus.ACTIVE.getCode(),
                        "active");
                break;
            case CLOSED:
                optionData = new EnumOptionData(EmailCampaignStatus.CLOSED.getValue().longValue(),
                        EmailCampaignStatus.CLOSED.getCode(), "closed");
                break;

        }

        return optionData;
    }
}
