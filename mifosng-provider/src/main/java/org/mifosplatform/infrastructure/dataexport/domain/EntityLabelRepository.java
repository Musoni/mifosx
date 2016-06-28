/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;


public interface EntityLabelRepository extends JpaRepository<EntityLabel, Long>,
        JpaSpecificationExecutor<EntityLabel> {

    Collection<EntityLabel> findAllByTable(String table);

    Collection<EntityLabel> findAllByField(String field);

    Collection<EntityLabel> findAllByJsonParam(String jsonParam);

    EntityLabel findOneByTableAndJsonParam(String table, String jsonParam);

    EntityLabel findOneByTableAndField(String table, String field);
}
