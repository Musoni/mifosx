/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.portfolio.loanaccount.exception;

import org.mifosplatform.infrastructure.core.exception.AbstractPlatformDomainRuleException;

public class GuarantorInterestAllocationException extends AbstractPlatformDomainRuleException {

    public GuarantorInterestAllocationException(final Long loanId) {
        super("error.msg.loan.does.not.support.guarantor.interest.splitting", "Loan with identifier " + loanId + " does not support guarantor interest splitting.",
                loanId);
    }
}
