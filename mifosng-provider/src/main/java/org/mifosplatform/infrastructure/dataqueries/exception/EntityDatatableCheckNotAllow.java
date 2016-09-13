/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataqueries.exception;

import org.mifosplatform.infrastructure.core.exception.AbstractPlatformResourceNotFoundException;

/**
 * A {@link RuntimeException} thrown when datatable resources are not found.
 */
public class EntityDatatableCheckNotAllow extends AbstractPlatformResourceNotFoundException {

    public EntityDatatableCheckNotAllow( final String entityName) {
        super(
                "error.msg.entity.datatable.check.is.not.allowed",
                "Entity Datatable check is not allow without a loan product id to :entity, because there is already a check attached to the same entity with a loan product id",
                entityName
        );
    }

}