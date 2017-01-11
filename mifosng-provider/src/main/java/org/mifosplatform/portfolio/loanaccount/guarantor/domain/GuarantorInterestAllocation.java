/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.portfolio.loanaccount.guarantor.domain;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.joda.time.LocalDate;
import org.mifosplatform.infrastructure.core.service.DateUtils;
import org.mifosplatform.portfolio.loanaccount.domain.Loan;
import org.mifosplatform.portfolio.savings.domain.SavingsAccountTransaction;
import org.mifosplatform.useradministration.domain.AppUser;
import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "m_guarantor_interest_allocation")
public class GuarantorInterestAllocation extends AbstractPersistable<Long> {

    @ManyToOne
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;



    @Column(name = "allocated_interest_paid", scale = 6, precision = 19, nullable = false)
    private BigDecimal allocatedInterestPaid;

    @ManyToOne
    @JoinColumn(name = "submitted_user_id", nullable = true)
    private AppUser submittedUserId;

    @Column(name = "is_reversed", nullable = false)
    private boolean reversed;

    @Temporal(TemporalType.DATE)
    @Column(name = "submitted_on_date")
    private Date submittedOnDate;


    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_on_date", nullable = false)
    private  Date createdDate;

    @LazyCollection(LazyCollectionOption.FALSE)
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "interestAllocation", orphanRemoval = true)
    protected final List<GuarantorInterestPayment> guarantorInterestPayments = new ArrayList<>();


    protected GuarantorInterestAllocation() {
    }

    private GuarantorInterestAllocation(final BigDecimal allocatedInterestPaid,final Loan loan, final AppUser submittedUserId) {
        this.allocatedInterestPaid = allocatedInterestPaid;
        this.loan = loan;
        this.submittedUserId = submittedUserId;
        this.submittedOnDate = DateUtils.getDateOfTenant();
        this.createdDate = new Date();
    }

    public static GuarantorInterestAllocation createNew(final BigDecimal allocatedInterestPaid,final Loan loan, final AppUser submittedUserId){
        return new GuarantorInterestAllocation(allocatedInterestPaid,loan,submittedUserId);
    }

    public BigDecimal getAllocatedInterestPaid() {return this.allocatedInterestPaid;}

    public AppUser getSubmittedUserId() {return this.submittedUserId;}

    public Date getCreatedDate() {return this.createdDate;}

    public Date getSubmittedOnDate() {return this.submittedOnDate;}

    public boolean isReversed() {return this.reversed;}

    public void reverse(){this.reversed = true;}

    public void addGuarantorInterestPayment(final List<GuarantorInterestPayment> guarantorInterestPayments){
        this.guarantorInterestPayments.addAll(guarantorInterestPayments);
    }

    public List<GuarantorInterestPayment> getGuarantorInterestPayments() {
        return this.guarantorInterestPayments;
    }
}
