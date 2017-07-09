/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.portfolio.savings.data;

import org.joda.time.LocalDate;

import java.math.BigDecimal;

public class InterestRateCharts {

    private Long id;

    private String name;

    private String description;

    private LocalDate fromDate;

    private LocalDate endDate;

    private BigDecimal annualInterestRate;

    private boolean applyToExistingSavingsAccount;


    public InterestRateCharts(final Long id, final String name, final String description, final LocalDate fromDate, final LocalDate endDate,
                              final BigDecimal annualInterestRate, boolean applyToExistingSavingsAccount) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.fromDate = fromDate;
        this.endDate = endDate;
        this.annualInterestRate = annualInterestRate;
        this.applyToExistingSavingsAccount = applyToExistingSavingsAccount;
    }

    public static InterestRateCharts createNew(final Long id , final String name, final String description, final LocalDate fromDate, final LocalDate endDate,
                                               final BigDecimal annualInterestRate, boolean applyToExistingSavingsAccount){
        return new InterestRateCharts(id, name,description,fromDate,endDate,annualInterestRate,applyToExistingSavingsAccount);

    }

    public Long getId() {return id;}

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public BigDecimal getAnnualInterestRate() {
        return annualInterestRate;
    }

    public boolean isApplyToExistingSavingsAccount() {
        return applyToExistingSavingsAccount;
    }
}
