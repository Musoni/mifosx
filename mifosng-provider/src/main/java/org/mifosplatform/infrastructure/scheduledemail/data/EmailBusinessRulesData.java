/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.scheduledemail.data;

import java.util.Map;

public class EmailBusinessRulesData {

    @SuppressWarnings("unused")
    private final Long reportId;

    @SuppressWarnings("unused")
    private final String reportName;

    @SuppressWarnings("unused")
    private final String reportType;

    @SuppressWarnings("unused")
    private final String reportSubType;

    @SuppressWarnings("unused")
    private final String reportDescription;

    @SuppressWarnings("unused")
    private final Map<String,Object> reportParamName;



    public EmailBusinessRulesData(final Long reportId, final String reportName,final String reportType, final Map<String,Object> reportParamName,
                                  final String reportSubType, final String reportDescription) {
        this.reportId = reportId;
        this.reportName = reportName;
        this.reportType = reportType;
        this.reportParamName = reportParamName;
        this.reportDescription = reportDescription;
        this.reportSubType = reportSubType;
    }


    public static EmailBusinessRulesData instance(final Long reportId, final String reportName, final String reportType, final Map<String,Object> reportParamName,
                                                  final String reportSubType, final String reportDescription){
        return new EmailBusinessRulesData(reportId,reportName,reportType,reportParamName,reportSubType,reportDescription);
    }

    public Map<String, Object> getReportParamName() {
        return reportParamName;
    }

    public String getReportType() {
        return reportType;
    }

    public String getReportName() {
        return reportName;
    }

    public Long getReportId() {
        return reportId;
    }

    public String getReportDescription() {
        return reportDescription;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EmailBusinessRulesData that = (EmailBusinessRulesData) o;

        return reportId != null ? reportId.equals(that.reportId) : that.reportId == null;

    }

    @Override
    public int hashCode() {
        return reportId != null ? reportId.hashCode() : 0;
    }
}
