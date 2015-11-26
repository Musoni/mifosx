/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.accounting.closure.bookoffincomeandexpense.service;

import org.joda.time.LocalDate;
import org.mifosplatform.accounting.closure.bookoffincomeandexpense.data.IncomeAndExpenseBookingData;
import org.mifosplatform.accounting.closure.bookoffincomeandexpense.data.IncomeAndExpenseJournalEntryData;
import org.mifosplatform.accounting.closure.bookoffincomeandexpense.data.JournalEntry;
import org.mifosplatform.accounting.closure.bookoffincomeandexpense.data.SingleDebitOrCreditEntry;
import org.mifosplatform.accounting.closure.command.GLClosureCommand;
import org.mifosplatform.accounting.closure.domain.GLClosure;
import org.mifosplatform.accounting.closure.domain.GLClosureRepository;
import org.mifosplatform.accounting.closure.exception.GLClosureInvalidException;
import org.mifosplatform.accounting.closure.exception.RunningBalanceNotCalculatedException;
import org.mifosplatform.accounting.closure.serialization.GLClosureCommandFromApiJsonDeserializer;
import org.mifosplatform.accounting.closure.service.GLClosureReadPlatformService;
import org.mifosplatform.accounting.glaccount.domain.GLAccount;
import org.mifosplatform.accounting.glaccount.domain.GLAccountRepository;
import org.mifosplatform.accounting.glaccount.exception.GLAccountNotFoundException;
import org.mifosplatform.infrastructure.core.api.JsonQuery;
import org.mifosplatform.infrastructure.core.service.DateUtils;
import org.mifosplatform.organisation.office.domain.Office;
import org.mifosplatform.organisation.office.domain.OfficeRepository;
import org.mifosplatform.organisation.office.exception.OfficeNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class CalculateIncomeAndExpenseBookingImpl implements CalculateIncomeAndExpenseBooking {

    private final GLClosureCommandFromApiJsonDeserializer fromApiJsonDeserializer;
    private final OfficeRepository officeRepository;
    private final GLClosureRepository glClosureRepository;
    private final GLAccountRepository glAccountRepository;
    private final GLClosureReadPlatformService glClosureReadPlatformService;

    @Autowired
    public CalculateIncomeAndExpenseBookingImpl(final GLClosureCommandFromApiJsonDeserializer fromApiJsonDeserializer,
            final OfficeRepository officeRepository,final GLClosureRepository glClosureRepository,
            final GLAccountRepository glAccountRepository,final GLClosureReadPlatformService glClosureReadPlatformService) {
        this.fromApiJsonDeserializer = fromApiJsonDeserializer;
        this.officeRepository = officeRepository;
        this.glClosureRepository = glClosureRepository;
        this.glAccountRepository = glAccountRepository;
        this.glClosureReadPlatformService = glClosureReadPlatformService;
    }

    @Override
    public IncomeAndExpenseBookingData CalculateIncomeAndExpenseBooking(JsonQuery query) {
        final GLClosureCommand closureCommand = this.fromApiJsonDeserializer.commandFromApiJson(query.json());
        closureCommand.validateForCreate();
        final Long officeId = closureCommand.getOfficeId();
        final Office office = this.officeRepository.findOne(officeId);
        if (office == null) { throw new OfficeNotFoundException(officeId); }

        final Date todaysDate = new Date();
        final Date closureDate = closureCommand.getClosingDate().toDate();
        if (closureDate.after(todaysDate)) { throw new GLClosureInvalidException(GLClosureInvalidException.GL_CLOSURE_INVALID_REASON.FUTURE_DATE, closureDate); }
        // shouldn't be before an existing accounting closure
        final GLClosure latestGLClosure = this.glClosureRepository.getLatestGLClosureByBranch(officeId);
        if (latestGLClosure != null) {
            if (latestGLClosure.getClosingDate().after(closureDate)) { throw new GLClosureInvalidException(
                    GLClosureInvalidException.GL_CLOSURE_INVALID_REASON.ACCOUNTING_CLOSED, latestGLClosure.getClosingDate()); }
        }
        final LocalDate incomeAndExpenseBookOffDate = LocalDate.fromDateFields(closureDate);


        IncomeAndExpenseBookingData incomeAndExpenseBookingData = null;

        if(closureCommand.getBookOffIncomeAndExpense()){
            final Long equityGlAccountId = closureCommand.getEquityGlAccountId();

            final GLAccount glAccount= this.glAccountRepository.findOne(equityGlAccountId);

            if(glAccount == null){throw new GLAccountNotFoundException(equityGlAccountId);}

            final List<IncomeAndExpenseJournalEntryData> incomeAndExpenseJournalEntryDataList = this.glClosureReadPlatformService.retrieveAllIncomeAndExpenseJournalEntryData(officeId,incomeAndExpenseBookOffDate);
             incomeAndExpenseBookingData = this.bookOffIncomeAndExpense(incomeAndExpenseJournalEntryDataList, closureCommand, true,glAccount);
        }
        return incomeAndExpenseBookingData;
    }

    private IncomeAndExpenseBookingData bookOffIncomeAndExpense(final List<IncomeAndExpenseJournalEntryData> incomeAndExpenseJournalEntryDataList,
                                                                final GLClosureCommand closureData,final boolean preview,final GLAccount glAccount){
        /* All running balances has to be calculated before booking off income and expense account */
        boolean isRunningBalanceCalculated = true;
        for(final IncomeAndExpenseJournalEntryData incomeAndExpenseData :incomeAndExpenseJournalEntryDataList ){
            if(!incomeAndExpenseData.isRunningBalanceCalculated()){ throw new RunningBalanceNotCalculatedException(incomeAndExpenseData.getOfficeId());}
        }
        BigDecimal debits = BigDecimal.ZERO;
        BigDecimal credits = BigDecimal.ZERO;

        List<SingleDebitOrCreditEntry>  debitsJournalEntry = new ArrayList<>();
        List<SingleDebitOrCreditEntry>  creditsJournalEntry = new ArrayList<>();

        for(final IncomeAndExpenseJournalEntryData incomeAndExpense : incomeAndExpenseJournalEntryDataList){
            if(incomeAndExpense.isIncomeAccountType()){
                if(incomeAndExpense.getOfficeRunningBalance().signum() == 1){
                    debits = debits.add(incomeAndExpense.getOfficeRunningBalance());
                    debitsJournalEntry.add(new SingleDebitOrCreditEntry(incomeAndExpense.getAccountId(),incomeAndExpense.getGlAccountName(),incomeAndExpense.getOfficeRunningBalance(),null));
                }else{
                    credits= credits.add(incomeAndExpense.getOfficeRunningBalance().abs());
                    creditsJournalEntry.add(new SingleDebitOrCreditEntry(incomeAndExpense.getAccountId(),incomeAndExpense.getGlAccountName(),incomeAndExpense.getOfficeRunningBalance().abs(),null));;
                }
            }
            if(incomeAndExpense.isExpenseAccountType()){
                if(incomeAndExpense.getOfficeRunningBalance().signum() == 1){
                    credits = credits.add(incomeAndExpense.getOfficeRunningBalance());
                    creditsJournalEntry.add(new SingleDebitOrCreditEntry(incomeAndExpense.getAccountId(),incomeAndExpense.getGlAccountName(),incomeAndExpense.getOfficeRunningBalance().abs(),null));;
                }else{
                    debits= debits.add(incomeAndExpense.getOfficeRunningBalance().abs());
                    debitsJournalEntry.add(new SingleDebitOrCreditEntry(incomeAndExpense.getAccountId(),incomeAndExpense.getGlAccountName(),incomeAndExpense.getOfficeRunningBalance().abs(),null));
                }
            }
        }
        final LocalDate today = DateUtils.getLocalDateOfTenant();
        final int compare = debits.compareTo(credits);
        BigDecimal difference = BigDecimal.ZERO;
        JournalEntry journalEntry = null;
        if(compare == 1){
            /* book with target gl id on the credit side */
            difference = debits.subtract(credits);
            SingleDebitOrCreditEntry targetBooking = new SingleDebitOrCreditEntry(closureData.getEquityGlAccountId(),glAccount.getName(),difference,null);
            creditsJournalEntry.add(targetBooking);
            journalEntry = new JournalEntry(closureData.getOfficeId(),today.toString(),closureData.getComments(),creditsJournalEntry,debitsJournalEntry,null,false,closureData.getCurrencyCode());
        }else if(compare == -1){
            /* book with target gl id on the debit side*/
            difference = credits.subtract(debits);
            SingleDebitOrCreditEntry targetBooking = new SingleDebitOrCreditEntry(closureData.getEquityGlAccountId(),glAccount.getName(),difference,null);
            debitsJournalEntry.add(targetBooking);
            journalEntry = new JournalEntry(closureData.getOfficeId(),today.toString(),closureData.getComments(),creditsJournalEntry,debitsJournalEntry,null,false,closureData.getCurrencyCode());
        }
        final LocalDate localDate = LocalDate.now();
        List<JournalEntry> journalEntries = new ArrayList<>();
        journalEntries.add(journalEntry);

        return new IncomeAndExpenseBookingData(localDate,closureData.getComments(),journalEntries);
    }
}
