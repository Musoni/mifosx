/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.scheduledemail.service;


import org.apache.xmlbeans.impl.tool.XMLBean;
import org.mifosplatform.infrastructure.configuration.data.SMTPCredentialsData;
import org.mifosplatform.infrastructure.configuration.service.ExternalServicesPropertiesReadPlatformService;
import org.mifosplatform.infrastructure.scheduledemail.EmailApiConstants;
import org.mifosplatform.infrastructure.scheduledemail.data.EmailMessageWithAttachmentData;
import org.mifosplatform.infrastructure.scheduledemail.domain.EmailConfiguration;
import org.mifosplatform.infrastructure.scheduledemail.domain.EmailConfigurationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.util.List;
import java.util.Properties;
import java.util.Set;

@Service
public class EmailMessageJobEmailServiceImpl implements EmailMessageJobEmailService {

    private final ExternalServicesPropertiesReadPlatformService externalServicesReadPlatformService;
    private static final Logger logger = LoggerFactory.getLogger( EmailMessageJobEmailServiceImpl.class);


    @Autowired
    private EmailMessageJobEmailServiceImpl(final ExternalServicesPropertiesReadPlatformService externalServicesReadPlatformService) {
        this.externalServicesReadPlatformService = externalServicesReadPlatformService;
    }

    @Override
    public void sendEmailWithAttachment(EmailMessageWithAttachmentData emailMessageWithAttachmentData) {
        try{
            SMTPCredentialsData smtpCredentialsData = this.externalServicesReadPlatformService.getSMTPCredentials();

            JavaMailSenderImpl javaMailSenderImpl = new JavaMailSenderImpl();
            javaMailSenderImpl.setHost(smtpCredentialsData.getHost());
            javaMailSenderImpl.setPort(Integer.parseInt(smtpCredentialsData.getPort()));
            javaMailSenderImpl.setUsername(smtpCredentialsData.getUsername());
            javaMailSenderImpl.setPassword(smtpCredentialsData.getPassword());
            javaMailSenderImpl.setJavaMailProperties(this.getJavaMailProperties(smtpCredentialsData.getHost()));

            MimeMessage mimeMessage = javaMailSenderImpl.createMimeMessage();

            // use the true flag to indicate you need a multipart message
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true);

            mimeMessageHelper.setTo(emailMessageWithAttachmentData.getTo());
            mimeMessageHelper.setText(emailMessageWithAttachmentData.getText());
            mimeMessageHelper.setSubject(emailMessageWithAttachmentData.getSubject());
            final List<File> attachments = emailMessageWithAttachmentData.getAttachments();
            if(attachments !=null && attachments.size() > 0){
                for(final File attachment : attachments){
                    if(attachment !=null){
                        mimeMessageHelper.addAttachment(attachment.getName(),attachment);
                    }
                }
            }

            javaMailSenderImpl.send(mimeMessage);

        }catch(Exception e){
            logger.error(e.getMessage(), e);
        }

    }


    private Properties getJavaMailProperties(final String smtpHost) {
        Properties properties = new Properties();
        properties.setProperty("mail.smtp.starttls.enable", "true");
        properties.setProperty("mail.smtp.auth", "true");
        properties.setProperty("mail.smtp.ssl.trust", smtpHost);

        return properties;
    }
}
