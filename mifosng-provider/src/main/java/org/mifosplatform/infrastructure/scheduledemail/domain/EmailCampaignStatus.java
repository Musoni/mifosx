/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.scheduledemail.domain;

public enum EmailCampaignStatus {

    INVALID(0, "emailCampaignStatus.invalid"), //
    PENDING(100, "emailCampaignStatus.pending"), //
    ACTIVE(300, "emailCampaignStatus.active"), //
    CLOSED(600, "emailCampaignStatus.closed");

    private final Integer value;
    private final String code;

    EmailCampaignStatus(Integer value, String code) {
        this.value = value;
        this.code = code;
    }

    public static EmailCampaignStatus fromInt(final Integer statusValue) {

        EmailCampaignStatus enumeration = EmailCampaignStatus.INVALID;
        switch (statusValue) {
            case 100:
                enumeration = EmailCampaignStatus.PENDING;
                break;
            case 300:
                enumeration = EmailCampaignStatus.ACTIVE;
                break;
            case 600:
                enumeration = EmailCampaignStatus.CLOSED;
                break;
        }
        return enumeration;
    }

    public Integer getValue() {
        return value;
    }

    public String getCode() {
        return code;
    }
    public boolean isActive(){
        return this.value.equals(EmailCampaignStatus.ACTIVE.getValue());
    }

    public boolean isPending(){
        return this.value.equals(EmailCampaignStatus.PENDING.getValue());
    }

    public boolean isClosed(){
         return this.value.equals(EmailCampaignStatus.CLOSED.getValue());
    }
}
