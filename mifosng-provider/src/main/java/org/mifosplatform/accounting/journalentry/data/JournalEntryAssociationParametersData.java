/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.accounting.journalentry.data;

public class JournalEntryAssociationParametersData {

    private final boolean transactionDetailsRequired;
    private final boolean runningBalanceRequired;
    private final boolean paymentDetailsRequired;
    private final boolean notesRequired;
    private final boolean glClosureRequired;


    public JournalEntryAssociationParametersData() {
        this.transactionDetailsRequired = false;
        this.runningBalanceRequired = false;
        this.paymentDetailsRequired = false;
        this.notesRequired = false;
        this.glClosureRequired = false;
    }

    public JournalEntryAssociationParametersData(final boolean transactionDetailsRequired, final boolean runningBalanceRequired, final boolean paymentDetails,
                                                 final boolean glClosureRequired) {

        
        Boolean notesRequired = false;
        Boolean paymentDetailsRequired = paymentDetails;
        
        if(transactionDetailsRequired)
        {
            notesRequired = transactionDetailsRequired;
            paymentDetailsRequired = transactionDetailsRequired;
        }
        
        this.notesRequired = notesRequired;
        this.transactionDetailsRequired = transactionDetailsRequired;
        this.paymentDetailsRequired = paymentDetailsRequired;
        this.runningBalanceRequired = runningBalanceRequired;
        this.glClosureRequired = glClosureRequired;

    }

    public boolean isTransactionDetailsRequired() {
        return this.transactionDetailsRequired;
    }

    public boolean isRunningBalanceRequired() {
        return this.runningBalanceRequired;
    }
    
    public boolean isPaymentDetailsRequired() {
        return this.paymentDetailsRequired;
    }
    
    public boolean isNotesRequired() {
        return this.notesRequired;
    }
    public boolean isGlClosureRequired (){ return this.glClosureRequired;}
}
