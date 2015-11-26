package org.mifosplatform.accounting.closure.exception;

import org.mifosplatform.infrastructure.core.exception.AbstractPlatformDomainRuleException;

/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
public class RunningBalanceNotCalculatedException extends AbstractPlatformDomainRuleException {
    public RunningBalanceNotCalculatedException(final Long officeId) {
        super("error.msg.running.balance.not.calculated", "Office with identifier " + officeId + " running balance is not calculated");
    }

}
