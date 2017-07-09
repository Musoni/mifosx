/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.portfolio.savings.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface SavingsProductInterestRateChartRepository extends JpaRepository<SavingsProductInterestRateChart, Long>,
        JpaSpecificationExecutor<SavingsProductInterestRateChart> {

    public static final String FIND_ALL_INTEREST_RATE_CHART_BY_PRODUCT_ID = "select * from m_savings_product_interest_rate_chart t where t.savings_product_id=:productId";

    @Query(value= FIND_ALL_INTEREST_RATE_CHART_BY_PRODUCT_ID, nativeQuery=true)
    Collection<SavingsProductInterestRateChart> findBySavingsProduct(@Param("productId") Long productId);
}
