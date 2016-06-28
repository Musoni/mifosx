/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.exception;

import org.mifosplatform.infrastructure.core.exception.AbstractPlatformResourceNotFoundException;


public class EnumValueCollectionNotFoundException extends AbstractPlatformResourceNotFoundException {

    public EnumValueCollectionNotFoundException(final String fieldName, final Long id){
        super("error.msg.enumvalue.property.invalid","Enum Id `" + id + "` for enum name `" + fieldName + "`", fieldName, id);
    }
}
