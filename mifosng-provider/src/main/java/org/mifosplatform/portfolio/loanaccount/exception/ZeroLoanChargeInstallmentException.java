/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.mifosplatform.portfolio.loanaccount.exception;

import org.mifosplatform.infrastructure.core.exception.AbstractPlatformDomainRuleException;

public class ZeroLoanChargeInstallmentException extends AbstractPlatformDomainRuleException {

    public ZeroLoanChargeInstallmentException(final Long chargeId) {
        super("error.msg.loan.zero.installment.charge", "Loan charge  with identifier " + chargeId
                + " cannot have total installment as zero.", chargeId);
    }
}
