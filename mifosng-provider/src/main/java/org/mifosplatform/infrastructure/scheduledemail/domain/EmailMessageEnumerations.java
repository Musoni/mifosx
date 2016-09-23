/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.scheduledemail.domain;

import org.mifosplatform.infrastructure.core.data.EnumOptionData;

public class EmailMessageEnumerations {

    public static EnumOptionData status(final Integer statusId) {
        return status(EmailMessageStatusType.fromInt(statusId));
    }

    public static EnumOptionData status(final EmailMessageStatusType status) {
        EnumOptionData optionData = new EnumOptionData(EmailMessageStatusType.INVALID.getValue().longValue(),
                EmailMessageStatusType.INVALID.getCode(), "Invalid");
        switch (status) {
            case INVALID:
                optionData = new EnumOptionData(EmailMessageStatusType.INVALID.getValue().longValue(),
                        EmailMessageStatusType.INVALID.getCode(), "Invalid");
            break;
            case PENDING:
                optionData = new EnumOptionData(EmailMessageStatusType.PENDING.getValue().longValue(),
                        EmailMessageStatusType.PENDING.getCode(), "Pending");
            break;
            case SENT:
                optionData = new EnumOptionData(EmailMessageStatusType.SENT.getValue().longValue(), EmailMessageStatusType.SENT.getCode(),
                        "Sent");
            break;
            case DELIVERED:
                optionData = new EnumOptionData(EmailMessageStatusType.DELIVERED.getValue().longValue(),
                        EmailMessageStatusType.DELIVERED.getCode(), "Delivered");
            break;
            case FAILED:
                optionData = new EnumOptionData(EmailMessageStatusType.FAILED.getValue().longValue(), EmailMessageStatusType.FAILED.getCode(),
                        "Failed");
            break;

        }

        return optionData;
    }
}