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

public interface ApplyChargesToExistingSavingsAccountRepository extends JpaRepository<ApplyChargesToExistingSavingsAccount, Long>,
        JpaSpecificationExecutor<ApplyChargesToExistingSavingsAccount> {


    @Query("from ApplyChargesToExistingSavingsAccount sa where sa.savingsProduct.id =:productId")
    Collection<ApplyChargesToExistingSavingsAccount>  retrieveBySavingsProduct(@Param("productId") Long productId);

    @Query("from ApplyChargesToExistingSavingsAccount sa where sa.savingsProduct.id =:productId and sa.productCharge.id =:chargeId")
    ApplyChargesToExistingSavingsAccount findBySavingsProductAndCharge(@Param("productId") Long productId, @Param("chargeId") Long chargeId);
}
