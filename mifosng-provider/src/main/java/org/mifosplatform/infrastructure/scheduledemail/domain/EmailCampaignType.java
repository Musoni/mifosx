/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.scheduledemail.domain;

public enum EmailCampaignType {
    DIRECT(1,"emailCampaignStatusType.direct"),
    SCHEDULE(2,"emailCampaignStatusType.schedule");

    private Integer value;
    private String code;

    EmailCampaignType(Integer value, String code) {
        this.value = value;
        this.code = code;
    }

    public Integer getValue() {
        return value;
    }

    public String getCode() {
        return code;
    }

    public static EmailCampaignType fromInt(final Integer typeValue) {
        EmailCampaignType type = null;
        switch (typeValue) {
            case 1:
                type = DIRECT;
                break;
            case 2:
                type = SCHEDULE;
                break;
        }
        return type;
    }

    public boolean isDirect(){
        return this.value.equals(EmailCampaignType.DIRECT.getValue());
    }

    public boolean isSchedule(){
        return this.value.equals(EmailCampaignType.SCHEDULE.getValue());
    }
}
