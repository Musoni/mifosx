/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.portfolio.account.exception;

import org.mifosplatform.infrastructure.core.exception.AbstractPlatformResourceNotFoundException;
import org.mifosplatform.portfolio.account.domain.AccountAssociationType;


public class AccountAssociationNotFoundException extends AbstractPlatformResourceNotFoundException {

    public AccountAssociationNotFoundException(final Long id, final Integer accountAssociationType) {
        super("error.msg.account.association.id.invalid", "AccountAssociation with identifier " + id + " and type "
                + AccountAssociationType.fromInt(accountAssociationType).getCode() + " does not exist",
                id, accountAssociationType);
    }
}
