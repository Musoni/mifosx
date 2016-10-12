/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.domain;

import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DataExportRepository extends JpaRepository<DataExport, Long>, JpaSpecificationExecutor<DataExport> { 
    /**
     * Retrieves entity where "deleted" property is set to 0 by id
     * @param id
     * @return {@link DataExport} entity
     */
    DataExport findByIdAndDeletedFalse(final Long id);
    
    /**
     * Retrieves all entities where "deleted" property is set to 0
     * 
     * @return collection of {@link DataExport} entities
     */
    Collection<DataExport> findByDeletedFalse();
}
