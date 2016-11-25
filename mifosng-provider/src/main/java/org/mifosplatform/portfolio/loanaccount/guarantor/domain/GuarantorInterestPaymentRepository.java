/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.portfolio.loanaccount.guarantor.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;


public interface GuarantorInterestPaymentRepository  extends JpaRepository<GuarantorInterestPayment, Long>,
        JpaSpecificationExecutor<GuarantorInterestPayment> {

    public static String FIND_ACTIVE_INTEREST_ALLOCATION = "select gI from GuarantorInterestPayment gI inner join gI.interestAllocation m where m.loan.id =:loanId " +
            "and gI.guarantor.id =:guarantorId and m.reversed is false ";


    @Query( FIND_ACTIVE_INTEREST_ALLOCATION)
    List<GuarantorInterestPayment> findByLoanAndReverseFalse(@Param("loanId") Long loanId, @Param("guarantorId") Long guarantorId);


    /*
    select *
    from m_guarantor_loan_interest_payment mg
    inner join m_guarantor_interest_allocation ml on ml.id = mg.guarantor_interest_allocation_id
    where ml.loan_id = 7 and ml.is_reversed is false */

    Collection<GuarantorInterestPayment> findByInterestAllocationOrderByIdDesc(GuarantorInterestAllocation guarantorInterestAllocation);
}
