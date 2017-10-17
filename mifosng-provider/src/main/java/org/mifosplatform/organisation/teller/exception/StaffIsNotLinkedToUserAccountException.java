/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.organisation.teller.exception;

import org.mifosplatform.infrastructure.core.exception.AbstractPlatformDomainRuleException;


public class StaffIsNotLinkedToUserAccountException extends AbstractPlatformDomainRuleException{

    private static final String ERROR_MESSAGE_CODE = "error.msg.staff.can.not.be.a.cashier.because.not.linked.to.user.account";
    private static final String DEFAULT_ERROR_MESSAGE = "Cashier cannot be assigned to teller, because he is not linked to a user account";

    public StaffIsNotLinkedToUserAccountException(Long cashierId) {
        super(ERROR_MESSAGE_CODE, DEFAULT_ERROR_MESSAGE,cashierId);
    }

}
