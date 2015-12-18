/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.accounting.journalentry.service;

import org.mifosplatform.accounting.journalentry.command.JournalEntryCommand;
import org.mifosplatform.infrastructure.core.api.JsonCommand;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResult;

import java.util.Map;

public interface JournalEntryWritePlatformService {

    CommandProcessingResult createJournalEntry(JsonCommand command);

    CommandProcessingResult revertJournalEntry(JsonCommand command);

    CommandProcessingResult reconcileJournalEntry(JsonCommand command);

    void createJournalEntriesForLoan(Map<String, Object> accountingBridgeData);

    void createJournalEntriesForSavings(Map<String, Object> accountingBridgeData);

    public String createJournalEntryForIncomeAndExpenseBookOff(final JournalEntryCommand journalEntryCommand);

    void revertJournalEntry(String transactionId);

}
