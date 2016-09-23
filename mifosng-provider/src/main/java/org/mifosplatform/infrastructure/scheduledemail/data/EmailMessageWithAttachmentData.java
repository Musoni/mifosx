/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.scheduledemail.data;

import java.io.File;
import java.util.List;
import java.util.Set;

public class EmailMessageWithAttachmentData {

    private final String to;
    private final String text;
    private final String subject;
    private final List<File> attachments;

    private EmailMessageWithAttachmentData(final String to, final String text, final String subject, final List<File> attachments) {
        this.to = to;
        this.text = text;
        this.subject = subject;
        this.attachments = attachments;
    }


    public static EmailMessageWithAttachmentData createNew (final String to, final String text, final String subject, final List<File> attachments){
        return new EmailMessageWithAttachmentData(to,text,subject,attachments);
    }

    public String getTo() {return this.to;}

    public String getText() {return this.text;}

    public String getSubject() {return this.subject;}

    public List<File> getAttachments() {
        return this.attachments;
    }
}
