/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;


public interface EnumValueCollectionRepository extends JpaRepository<EnumValueCollection, EnumValueCollectionPK>,
        JpaSpecificationExecutor<EnumValueCollection> {

    EnumValueCollection findOneByEnumValueCollectionPK(EnumValueCollectionPK enumValueCollectionPK);
}
