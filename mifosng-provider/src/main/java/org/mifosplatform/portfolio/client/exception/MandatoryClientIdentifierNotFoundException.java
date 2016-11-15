/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.portfolio.client.exception;

import org.mifosplatform.infrastructure.core.exception.AbstractPlatformResourceNotFoundException;


public class MandatoryClientIdentifierNotFoundException extends AbstractPlatformResourceNotFoundException {

    public MandatoryClientIdentifierNotFoundException() {
        super("error.msg.client.does.not.have.all.mandatory.clientIdentifiers", "Client does not have all mandatory Client Identifiers");
    }

    public MandatoryClientIdentifierNotFoundException(final String label) {
        super("error.msg.client.does.not.have." + label.toLowerCase(), "Client does not have " + label + ", which is mandatory");
    }

}
