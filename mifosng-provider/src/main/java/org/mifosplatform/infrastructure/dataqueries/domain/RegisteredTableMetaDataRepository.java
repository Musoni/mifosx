/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataqueries.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface RegisteredTableMetaDataRepository  extends JpaRepository<RegisteredTableMetaData, Long>, JpaSpecificationExecutor<RegisteredTableMetaData> {
    RegisteredTableMetaData findOneByTableNameAndFieldName(String tableName,String fieldName);

    /**
     * Find by table name ordering the resultset by order ascending
     * 
     * @param tableName datatable table name
     * @return List of {@link RegisteredTableMetaData} objects
     */
    List<RegisteredTableMetaData> findByTableNameOrderByOrderAsc(final String tableName);
}
