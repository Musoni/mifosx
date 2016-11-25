/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.portfolio.loanaccount.guarantor.data;


import org.joda.time.LocalDate;

import java.math.BigDecimal;

public class GuarantorInterestAllocationData {

    @SuppressWarnings("unused")
    private final Long id;

    @SuppressWarnings("unused")
    private final BigDecimal amountAllocated;

    @SuppressWarnings("unused")
    private final Long savingsId;

    private final LocalDate submittedOnDate;


    public GuarantorInterestAllocationData(final Long id, final BigDecimal amount, final Long savingsId, final LocalDate submittedOnDate) {
        this.id = id;
        this.amountAllocated = amount;
        this.savingsId = savingsId;
        this.submittedOnDate = submittedOnDate;
    }

    public static GuarantorInterestAllocationData createNew (final Long id, final BigDecimal amount, final Long savingsId, final LocalDate submittedOnDate){
        return new GuarantorInterestAllocationData(id,amount,savingsId,submittedOnDate);
    }

    public Long getId() {return this.id;}

    public BigDecimal getAmount() {return this.amountAllocated;}

    public Long getSavingsId() {return this.savingsId;}

    public LocalDate getSubmittedOnDate() {return this.submittedOnDate;}
}
