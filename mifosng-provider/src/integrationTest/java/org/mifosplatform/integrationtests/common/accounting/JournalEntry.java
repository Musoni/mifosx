/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.integrationtests.common.accounting;

public class JournalEntry {

    public enum TransactionType {
        CREDIT("CREDIT"), DEBIT("DEBIT");

        private TransactionType(final String type) {
            this.type = type;
        }

        private final String type;

        @Override
        public String toString() {
            return this.type;
        }
    }

    private final Float transactionAmount;
    private final TransactionType transactionType;
    private final Integer officeId;
    private final Integer accountId;

    public JournalEntry(final float transactionAmount, final TransactionType type, final int officeId, final int accountId){
        this.transactionAmount = transactionAmount;
        this.transactionType = type;
        this.officeId = officeId;
        this.accountId = accountId;
    }

    public JournalEntry(final float transactionAmount, final TransactionType type) {
        this.transactionAmount = transactionAmount;
        this.transactionType = type;
        this.officeId = null;
        this.accountId = null;
    }

    public Float getTransactionAmount() {
        return this.transactionAmount;
    }

    public String getTransactionType() {
        return this.transactionType.toString();
    }

    public Integer getOfficeId() { return this.officeId; }

    public Integer getAccountId() { return this.accountId; }

}
