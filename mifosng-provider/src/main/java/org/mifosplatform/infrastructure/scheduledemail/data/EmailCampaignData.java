/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.scheduledemail.data;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.mifosplatform.infrastructure.core.data.EnumOptionData;

public class EmailCampaignData {

    @SuppressWarnings("unused")
    private Long id;
    @SuppressWarnings("unused")
    private final String campaignName;
    @SuppressWarnings("unused")
    private final Integer campaignType;
    @SuppressWarnings("unused")
    private final Long businessRuleId;
    @SuppressWarnings("unused")
    private final String paramValue;
    @SuppressWarnings("unused")
    private final EnumOptionData campaignStatus;
    @SuppressWarnings("unused")
    private final String emailSubject;
    @SuppressWarnings("unused")
    private final String emailMessage;
    @SuppressWarnings("unused")
    private final String emailAttachmentFileFormat;
    @SuppressWarnings("unused")
    private final Long stretchyReportId;
    @SuppressWarnings("unused")
    private final String stretchyReportParamMap;
    @SuppressWarnings("unused")
    private final DateTime nextTriggerDate;
    @SuppressWarnings("unused")
    private final LocalDate lastTriggerDate;
    @SuppressWarnings("unused")
    private final EmailCampaignTimeLine emailCampaignTimeLine;

    @SuppressWarnings("unused")
    private final DateTime recurrenceStartDate;

    private final String recurrence;

    private EmailCampaignData(final Long id, final String campaignName, final Integer campaignType, final Long businessRuleId,
                              final String paramValue, final EnumOptionData campaignStatus, final String emailSubject,
                              final String message, final String emailAttachmentFileFormat, final Long stretchyReportId,
                              final String stretchyReportParamMap, final DateTime nextTriggerDate, final LocalDate lastTriggerDate,
                              final EmailCampaignTimeLine emailCampaignTimeLine, final DateTime recurrenceStartDate, final String recurrence) {
        this.id = id;
        this.campaignName = campaignName;
        this.campaignType = campaignType;
        this.businessRuleId = businessRuleId;
        this.paramValue = paramValue;
        this.campaignStatus =campaignStatus;
        this.emailSubject = emailSubject;
        this.emailMessage = message;
        this.emailAttachmentFileFormat = emailAttachmentFileFormat;
        this.stretchyReportId = stretchyReportId;
        this.stretchyReportParamMap = stretchyReportParamMap;
        if(nextTriggerDate !=null){
            this.nextTriggerDate = nextTriggerDate;
        }else{
            this.nextTriggerDate = null;
        }
        if(lastTriggerDate !=null){
            this.lastTriggerDate = lastTriggerDate;
        }else{
            this.lastTriggerDate = null;
        }
        this.emailCampaignTimeLine =emailCampaignTimeLine;
        this.recurrenceStartDate = recurrenceStartDate;
        this.recurrence  = recurrence;
    }

    public static EmailCampaignData instance(final Long id, final String campaignName, final Integer campaignType, final Long runReportId,
                                             final String paramValue, final EnumOptionData campaignStatus, final String emailSubject,
                                             final String message, final String emailAttachmentFileFormat, final Long stretchyReportId,
                                             final String stretchyReportParamMap, final DateTime nextTriggerDate, final LocalDate lastTriggerDate,
                                             final EmailCampaignTimeLine emailCampaignTimeLine,
                                             final DateTime recurrenceStartDate, final String recurrence){
        return new EmailCampaignData(id,campaignName,campaignType,runReportId,paramValue,
                campaignStatus,emailSubject,message,emailAttachmentFileFormat,stretchyReportId,stretchyReportParamMap,nextTriggerDate,lastTriggerDate,emailCampaignTimeLine,recurrenceStartDate,recurrence);
    }


    public Long getId() {
        return id;
    }

    public String getCampaignName() {
        return this.campaignName;
    }

    public Integer getCampaignType() {
        return this.campaignType;
    }

    public Long getRunReportId() {
        return this.businessRuleId;
    }

    public String getParamValue() {
        return this.paramValue;
    }

    public EnumOptionData getCampaignStatus() {
        return this.campaignStatus;
    }

    public String getEmailSubject() { return this.emailSubject; }

    public String getMessage() {
        return this.emailMessage;
    }

    public DateTime getNextTriggerDate() {
        return this.nextTriggerDate;
    }

    public LocalDate getLastTriggerDate() {
        return this.lastTriggerDate;
    }

    public String getRecurrence() {return this.recurrence;}

    public DateTime getRecurrenceStartDate() {return this.recurrenceStartDate;}
}
