/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.scheduledemail.exception;

import org.mifosplatform.infrastructure.core.exception.AbstractPlatformResourceNotFoundException;

public class EmailBusinessRuleNotFound extends AbstractPlatformResourceNotFoundException {

    public EmailBusinessRuleNotFound(final Long resourceId) {
        super("error.msg.scheduledemail.business.rule.not.found", "Email business rule with identifier `" + resourceId + "` does not exist", resourceId);
    }
}
