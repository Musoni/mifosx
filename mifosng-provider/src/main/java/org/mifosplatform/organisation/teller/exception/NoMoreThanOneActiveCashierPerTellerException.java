/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.organisation.teller.exception;

import org.mifosplatform.infrastructure.core.exception.AbstractPlatformDomainRuleException;


public class NoMoreThanOneActiveCashierPerTellerException extends AbstractPlatformDomainRuleException{

    private static final String ERROR_MESSAGE_CODE = "error.msg.teller.no.more.than.one.active.cashier";
    private static final String DEFAULT_ERROR_MESSAGE = "Cannot have more than one active Cashier for this teller";

    public NoMoreThanOneActiveCashierPerTellerException(Long tellerId) {
        super(ERROR_MESSAGE_CODE, DEFAULT_ERROR_MESSAGE, tellerId);
    }

}
