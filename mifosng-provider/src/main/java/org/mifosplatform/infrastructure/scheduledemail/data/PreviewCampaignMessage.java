/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.scheduledemail.data;

public class PreviewCampaignMessage {

    @SuppressWarnings("unused")
    private final String campaignMessage;

    private final Integer totalNumberOfMessages;

    private final String reportStrethcyParams;

    public PreviewCampaignMessage(String campaignMessage, Integer totalNumberOfMessages,String reportStrethcyParams) {
        this.campaignMessage = campaignMessage;
        this.totalNumberOfMessages = totalNumberOfMessages;
        this.reportStrethcyParams = reportStrethcyParams;
    }

    public String getCampaignMessage() {
        return campaignMessage;
    }

    public Integer getTotalNumberOfMessages() {
        return totalNumberOfMessages;
    }
}
