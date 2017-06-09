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

import java.util.List;

public interface DepositAccountOnHoldTransactionRepository extends JpaRepository<DepositAccountOnHoldTransaction, Long>,
        JpaSpecificationExecutor<DepositAccountOnHoldTransaction> {

        @Query("from DepositAccountOnHoldTransaction sa where sa.savingsAccount = :savingsAccount and sa.reversed = false order by sa.transactionDate, sa.createdDate")
        List<DepositAccountOnHoldTransaction> findBySavingsAccountAndReversedFalseOrderByCreatedDateAsc(@Param("savingsAccount") SavingsAccount account );

}
