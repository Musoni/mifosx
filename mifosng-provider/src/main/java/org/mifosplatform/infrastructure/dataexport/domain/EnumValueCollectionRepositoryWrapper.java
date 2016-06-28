/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.domain;

import org.mifosplatform.infrastructure.dataexport.exception.EnumValueCollectionNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EnumValueCollectionRepositoryWrapper {

    private final EnumValueCollectionRepository repository;

    @Autowired
    public EnumValueCollectionRepositoryWrapper(final EnumValueCollectionRepository repository){
        this.repository = repository;
    }

    public EnumValueCollection findOneByFieldNameAndId(final String name, Long id){
        final EnumValueCollection enumValueCollection =
                this.repository.findOneByEnumValueCollectionPK(new EnumValueCollectionPK(name,id));
        if(enumValueCollection == null){throw new EnumValueCollectionNotFoundException(name,id);}
        return enumValueCollection;
    }
}
