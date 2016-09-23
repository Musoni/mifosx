/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.portfolio.account.exception;

import org.mifosplatform.infrastructure.core.exception.AbstractPlatformDomainRuleException;


public class AccountTransferLoanToLoanException extends AbstractPlatformDomainRuleException {
    public AccountTransferLoanToLoanException(final Long id) {
        super("error.msg.accounttransfer.outstanding.loan.to.loan.not.supported", "Account transfer from an outstanding loan with identifier "+ id  +" to another loan is not supported");
    }

}
