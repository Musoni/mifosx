/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.scheduledemail.exception;

import org.mifosplatform.infrastructure.core.exception.AbstractPlatformDomainRuleException;


public class EmailConfigurationSMTPUsernameNotValid  extends AbstractPlatformDomainRuleException {
    
    public  EmailConfigurationSMTPUsernameNotValid(final String smtpUsername) {
        super("error.msg.scheduledemail.configuration.smtpusername.not.valid",
                "SMTP username configuration with email " + "'"+smtpUsername+"'" + " is not a valid email address ");
    }
}
