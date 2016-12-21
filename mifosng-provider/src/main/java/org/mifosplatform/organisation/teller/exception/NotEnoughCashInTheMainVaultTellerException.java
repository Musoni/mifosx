/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.organisation.teller.exception;

import org.mifosplatform.infrastructure.core.exception.AbstractPlatformDomainRuleException;


public class NotEnoughCashInTheMainVaultTellerException extends AbstractPlatformDomainRuleException{

    private static final String ERROR_MESSAGE_CODE = "error.msg.not.enough.cash.in.the.main.vault.account";
    private static final String DEFAULT_ERROR_MESSAGE = "There is not enough cash in the main vault account to allocate to the teller";

    public NotEnoughCashInTheMainVaultTellerException(Long accountId) {
        super(ERROR_MESSAGE_CODE, DEFAULT_ERROR_MESSAGE, accountId);
    }

}
