/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.scheduledemail.data;

import org.mifosplatform.infrastructure.core.data.EnumOptionData;
import org.mifosplatform.infrastructure.scheduledemail.domain.ScheduledEmailAttachmentFileFormat;
import org.mifosplatform.infrastructure.scheduledemail.domain.ScheduledEmailStretchyReportParamDateOption;

public class ScheduledEmailEnumerations {
    public static EnumOptionData emailAttachementFileFormat(final Integer emailAttachementFileFormatId) {
        return emailAttachementFileFormat(ScheduledEmailAttachmentFileFormat.instance(emailAttachementFileFormatId));
    }
    
    public static EnumOptionData emailAttachementFileFormat(final String emailAttachementFileFormatString) {
        return emailAttachementFileFormat(ScheduledEmailAttachmentFileFormat.instance(emailAttachementFileFormatString));
    }
    
    public static EnumOptionData emailAttachementFileFormat(final ScheduledEmailAttachmentFileFormat emailAttachementFileFormat) {
        EnumOptionData enumOptionData = null;
        
        if (emailAttachementFileFormat != null) {
            enumOptionData = new EnumOptionData(emailAttachementFileFormat.getId().longValue(), emailAttachementFileFormat.getCode(), 
                    emailAttachementFileFormat.getValue());
        }
        
        return enumOptionData;
    }
    
    public static EnumOptionData stretchyReportDateOption(final ScheduledEmailStretchyReportParamDateOption
            reportMailingJobStretchyReportParamDateOption) {
        EnumOptionData enumOptionData = null;
        
        if (reportMailingJobStretchyReportParamDateOption != null) {
            enumOptionData = new EnumOptionData(reportMailingJobStretchyReportParamDateOption.getId().longValue(), 
                    reportMailingJobStretchyReportParamDateOption.getCode(), reportMailingJobStretchyReportParamDateOption.getValue());
        }
        
        return enumOptionData;
    }
}
