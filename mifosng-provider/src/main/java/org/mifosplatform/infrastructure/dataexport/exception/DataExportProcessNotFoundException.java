/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.exception;

import org.mifosplatform.infrastructure.core.exception.AbstractPlatformResourceNotFoundException;

public class DataExportProcessNotFoundException extends AbstractPlatformResourceNotFoundException {

    private static final long serialVersionUID = 903969071743344665L;

    public DataExportProcessNotFoundException(final Long id) {
        super("error.msg.data.export.process.not.found", "Data export process with identifier " + id +
                " does not exist", id);
    }
}
