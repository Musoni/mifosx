/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataqueries.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EntityDatatableChecksRepository extends JpaRepository<EntityDatatableChecks, Long>, JpaSpecificationExecutor<EntityDatatableChecks> {

    public List<EntityDatatableChecks> findByEntityAndStatus(String entityName,Long status);

    @Query(" from  EntityDatatableChecks t WHERE t.status =:status and t.entity=:entity and t.productLoanId = :productLoanId ")
    public List<EntityDatatableChecks> findByEntityStatusAndLoanProduct(@Param("entity") String entity,@Param("status") Long status,@Param("productLoanId") Long productLoanId);

    @Query(" from  EntityDatatableChecks t WHERE t.status =:status and t.entity=:entity and t.datatableId = :datatableId AND t.productLoanId IS NOT NULL")
    public List<EntityDatatableChecks> findByEntityStatusAndDatatableId(@Param("entity") String entityName,@Param("status") Long status, @Param("datatableId") Long dataTableId);

}
