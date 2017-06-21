/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.accounting.closure.storeglaccountbalance.data;

import java.math.BigDecimal;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.LocalDate;

public class GLClosureAccountBalanceReportData {
    private final String accountNumber;
    private final LocalDate transactionDate;
    private final LocalDate postedDate;
    private final GLClosureAccountBalanceReportTransactionType transactionType;
    private final BigDecimal amount;
    private final String reference;
    
    /**
     * @param accountNumber
     * @param transactionDate
     * @param postedDate
     * @param transactionType
     * @param amount
     * @param reference
     */
    private GLClosureAccountBalanceReportData(final String accountNumber, final LocalDate transactionDate, 
            final LocalDate postedDate, final GLClosureAccountBalanceReportTransactionType transactionType, 
            final BigDecimal amount, final String reference) {
        this.accountNumber = accountNumber;
        this.transactionDate = transactionDate;
        this.postedDate = postedDate;
        this.transactionType = transactionType;
        this.amount = amount;
        this.reference = reference;
    }
    
    /**
     * Creates a new {@link GLClosureAccountBalanceReportData} object
     * 
     * @param accountNumber
     * @param transactionDate
     * @param postedDate
     * @param amount
     * @param reference
     * @return {@link GLClosureAccountBalanceReportData} object
     */
    public static GLClosureAccountBalanceReportData instance(final String accountNumber, final LocalDate transactionDate, 
            final LocalDate postedDate, final BigDecimal amount, final String reference) {
        return new GLClosureAccountBalanceReportData(accountNumber, transactionDate, postedDate, 
                GLClosureAccountBalanceReportTransactionType.NEW, amount, reference);
    }

    /**
     * @return the accountNumber
     */
    public String getAccountNumber() {
        return accountNumber;
    }

    /**
     * @return the transactionDate
     */
    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    /**
     * @return the postedDate
     */
    public LocalDate getPostedDate() {
        return postedDate;
    }

    /**
     * @return the transactionType
     */
    public GLClosureAccountBalanceReportTransactionType getTransactionType() {
        return transactionType;
    }

    /**
     * @return the amount
     */
    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * @return the reference
     */
    public String getReference() {
        return reference;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
