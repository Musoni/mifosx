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

public interface DashboardMetricsRepository extends JpaRepository<DashboardMetrics, Long>, JpaSpecificationExecutor<DashboardMetrics> {

    @Query("from DashboardMetrics d where d.monthYear = :monthYear and d.staffId =:staffId and d.officeId =:officeId")
    DashboardMetrics findSpecial(@Param("monthYear") String monthYear,@Param("staffId") Long staffId,@Param("officeId") Long officeId);
}