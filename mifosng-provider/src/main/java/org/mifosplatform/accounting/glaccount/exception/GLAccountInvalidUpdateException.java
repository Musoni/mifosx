/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.accounting.glaccount.exception;

import org.mifosplatform.infrastructure.core.exception.AbstractPlatformDomainRuleException;

/**
 * A {@link RuntimeException} thrown when an error is encountered during
 * updating a GL Account
 */
public class GLAccountInvalidUpdateException extends AbstractPlatformDomainRuleException {

    /*** Enum of reasons for invalid delete **/
    public static enum GL_ACCOUNT_INVALID_UPDATE_REASON {
        TRANSANCTIONS_LOGGED,
        DISABLED_IN_USE;

        public String errorMessage() {
            if (name().toString().equalsIgnoreCase("TRANSANCTIONS_LOGGED")) { return "This Usage of this (detail) GL Account as it already has transactions logged against it"; }
            if (name().toString().equalsIgnoreCase("DISABLED_IN_USE")) { return "This GL Account cannot be disabled as it is still used in products"; }
            return name().toString();
        }

        public String errorCode() {
            if (name().toString().equalsIgnoreCase("TRANSANCTIONS_LOGGED")) { return "error.msg.glaccount.glcode.invalid.update.transactions.logged"; }
            if (name().toString().equalsIgnoreCase("DISABLED_IN_USE")) { return "error.msg.glaccount.disabled.invalid.update.in.use"; }
            return name().toString();
        }
    }

    public GLAccountInvalidUpdateException(final GL_ACCOUNT_INVALID_UPDATE_REASON reason, final Long glAccountId) {
        super(reason.errorCode(), reason.errorMessage(), glAccountId);
    }
}