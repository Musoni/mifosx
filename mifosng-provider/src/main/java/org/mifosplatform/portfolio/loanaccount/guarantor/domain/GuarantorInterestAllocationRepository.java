/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.portfolio.loanaccount.guarantor.domain;


import org.mifosplatform.portfolio.loanaccount.domain.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface GuarantorInterestAllocationRepository extends JpaRepository<GuarantorInterestAllocation, Long>,
        JpaSpecificationExecutor<GuarantorInterestAllocation> {


    public static String FIND_ACITVE_INTEREST_ALLOACTION = "from GuarantorInterestAllocation c where c.loan.id = :loanId and c.reversed = false";


    Collection<GuarantorInterestAllocation> findByLoanAndReversedFalseOrderByIdDesc(Loan loan);

    Collection<GuarantorInterestAllocation> findByLoan(Loan loan);

    @Query(FIND_ACITVE_INTEREST_ALLOACTION)
    List<GuarantorInterestAllocation>  findByLoanAndReverseFalse(@Param("loanId") Long loanId);

}
