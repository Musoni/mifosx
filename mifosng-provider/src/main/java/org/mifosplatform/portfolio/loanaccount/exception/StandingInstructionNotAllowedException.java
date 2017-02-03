/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.portfolio.loanaccount.exception;

import org.mifosplatform.infrastructure.core.exception.AbstractPlatformDomainRuleException;

public class StandingInstructionNotAllowedException extends AbstractPlatformDomainRuleException {

    public StandingInstructionNotAllowedException(final Long prouductId) {
        super("error.msg.loan.product.does.not.allow.standingInstruction.", "Loan with product identifier " + prouductId
                + " does not allow standing instruction", prouductId);
    }
}
