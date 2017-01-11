/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.portfolio.loanaccount.guarantor.domain;


import org.apache.commons.lang.ObjectUtils;
import org.joda.time.LocalDate;
import org.mifosplatform.infrastructure.core.service.DateUtils;
import org.mifosplatform.portfolio.savings.domain.SavingsAccount;
import org.mifosplatform.portfolio.savings.domain.SavingsAccountTransaction;
import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;


@Entity
@Table(name = "m_guarantor_loan_interest_payment")
public class GuarantorInterestPayment extends AbstractPersistable<Long> {

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "guarantor_id", nullable = false)
    private Guarantor guarantor;

    @ManyToOne
    @JoinColumn(name = "guarantor_interest_allocation_id", nullable = false)
    private GuarantorInterestAllocation interestAllocation;


    @Column(name = "deposited_amount", scale = 6, precision = 19, nullable = false)
    private BigDecimal depositedAmount;

    @ManyToOne
    @JoinColumn(name = "savings_account_id", nullable = false)
    private SavingsAccount savingsAccount;

    @ManyToOne
    @JoinColumn(name = "savings_transaction_id", nullable = false)
    private SavingsAccountTransaction savingsAccountTransaction;

    @Column(name = "is_reversed", nullable = false)
    private boolean reversed;

    @Temporal(TemporalType.DATE)
    @Column(name = "submitted_on_date")
    private Date submittedOnDate;


    protected GuarantorInterestPayment() {
    }


    private GuarantorInterestPayment(final Guarantor guarantor, final GuarantorInterestAllocation guarantorInterestAllocation,
                                     final BigDecimal depositedAmount,final SavingsAccount savingsAccount ,final  SavingsAccountTransaction savingsAccountTransaction) {
        this.guarantor = guarantor;
        this.interestAllocation = guarantorInterestAllocation;
        this.depositedAmount = depositedAmount;
        this.savingsAccount = savingsAccount;
        this.savingsAccountTransaction = savingsAccountTransaction;
        this.submittedOnDate = DateUtils.getDateOfTenant();
    }


    public static GuarantorInterestPayment createNew(final Guarantor guarantor, final GuarantorInterestAllocation guarantorInterestAccumulated,
                                                     final BigDecimal depositedAmount,final SavingsAccount savingsAccount,
                                                     final SavingsAccountTransaction savingsAccountTransaction){
        return new GuarantorInterestPayment(guarantor,guarantorInterestAccumulated,depositedAmount,savingsAccount,savingsAccountTransaction);
    }


    public Guarantor getGuarantor() {return this.guarantor;}

    public GuarantorInterestAllocation getInterestAllocation() {
        return this.interestAllocation;
    }

    public BigDecimal getDepositedAmount() {return this.depositedAmount;}

    public SavingsAccountTransaction getSavingsAccountTransaction() {return this.savingsAccountTransaction;}

    public SavingsAccount getSavingsAccount() {return this.savingsAccount;}

    public LocalDate getSubmittedOnDate() {
        return (LocalDate) ObjectUtils.defaultIfNull(new LocalDate(this.submittedOnDate), null);
    }

    public boolean isReversed() {return this.reversed;}

    public void reverse(){this.reversed = true;}
}
