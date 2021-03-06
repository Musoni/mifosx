/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.scheduledemail.domain;

public enum ScheduledEmailStretchyReportParamDateOption {
    INVALID(0, "scheduledEmailStretchyReportParamDateOption.invalid", "invalid"),
    TODAY(1, "scheduledEmailStretchyReportParamDateOption.today", "today"),
    YESTERDAY(2, "scheduledEmailStretchyReportParamDateOption.yesterday", "yesterday"),
    TOMORROW(3, "scheduledEmailStretchyReportParamDateOption.tomorrow", "tomorrow");

    private String code;
    private String value;
    private Integer id;

    /**
     * @param id
     * @param code
     * @param value
     */
    private ScheduledEmailStretchyReportParamDateOption(final Integer id, final String code, final String value) {
        this.value = value;
        this.code = code;
        this.id = id;
    }
    
    /**
     * @param value
     * @return
     */
    public static ScheduledEmailStretchyReportParamDateOption instance(final String value) {
        ScheduledEmailStretchyReportParamDateOption scheduledEmailStretchyReportParamDateOption = INVALID;
        
        switch (value) {
            case "today":
                scheduledEmailStretchyReportParamDateOption = TODAY;
                break;
                
            case "yesterday":
                scheduledEmailStretchyReportParamDateOption = YESTERDAY;
                break;
                
            case "tomorrow":
                scheduledEmailStretchyReportParamDateOption = TOMORROW;
                break;
        }
        
        return scheduledEmailStretchyReportParamDateOption;
    }
    
    /**
     * @param id
     * @return
     */
    public static ScheduledEmailStretchyReportParamDateOption instance(final Integer id) {
        ScheduledEmailStretchyReportParamDateOption scheduledEmailStretchyReportParamDateOption = INVALID;
        
        switch (id) {
            case 1:
                scheduledEmailStretchyReportParamDateOption = TODAY;
                break;
                
            case 2:
                scheduledEmailStretchyReportParamDateOption = YESTERDAY;
                break;
                
            case 3:
                scheduledEmailStretchyReportParamDateOption = TOMORROW;
                break;
        }
        
        return scheduledEmailStretchyReportParamDateOption;
    }
    
    /**
     * @return the code
     */
    public String getCode() {
        return code;
    }

    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * @return the id
     */
    public Integer getId() {
        return id;
    }
    
    /** 
     * @return list of valid ScheduledEmailAttachmentFileFormat values
     **/
    public static Object[] validValues() {
        return new Object[] { TODAY.value, YESTERDAY.value, TOMORROW.value };
    }
}
