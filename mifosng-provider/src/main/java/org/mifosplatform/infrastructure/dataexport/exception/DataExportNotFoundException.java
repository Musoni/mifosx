/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.exception;

import org.mifosplatform.infrastructure.core.exception.AbstractPlatformResourceNotFoundException;

public class DataExportNotFoundException extends AbstractPlatformResourceNotFoundException {

    private static final long serialVersionUID = 903969071743344665L;

    public DataExportNotFoundException(final Long id) {
        super("error.msg.data.export.not.found", "Data export with identifier " + id +
                " does not exist", id);
    }
}
