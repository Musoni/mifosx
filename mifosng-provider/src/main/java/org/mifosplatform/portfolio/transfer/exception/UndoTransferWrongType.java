/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.portfolio.transfer.exception;

import org.mifosplatform.infrastructure.core.exception.AbstractPlatformDomainRuleException;

public class UndoTransferWrongType extends AbstractPlatformDomainRuleException {
    public UndoTransferWrongType() {
        super("error.msg.transfer.type.is.group.exception",
                "The undo Transfer is of type Group");
    }
}
