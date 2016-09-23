/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.scheduledemail.exception;

import org.mifosplatform.infrastructure.core.exception.AbstractPlatformResourceNotFoundException;

/**
 * A {@link RuntimeException} thrown when a code is not found.
 */
public class EmailNotFoundException extends AbstractPlatformResourceNotFoundException {

    public EmailNotFoundException(final Long resourceId) {
        super("error.msg.scheduledemail.identifier.not.found", "Email with identifier `" + resourceId + "` does not exist", resourceId);
    }
}