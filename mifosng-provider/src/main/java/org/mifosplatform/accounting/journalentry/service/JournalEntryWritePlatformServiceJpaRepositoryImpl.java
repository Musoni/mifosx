/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.accounting.journalentry.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.emory.mathcs.backport.java.util.Arrays;
import org.apache.commons.lang.StringUtils;
import org.joda.time.LocalDate;
import org.mifosplatform.accounting.closure.bookoffincomeandexpense.data.SingleDebitOrCreditEntry;
import org.mifosplatform.accounting.closure.domain.GLClosure;
import org.mifosplatform.accounting.closure.domain.GLClosureRepository;
import org.mifosplatform.accounting.common.AccountingConstants;
import org.mifosplatform.accounting.financialactivityaccount.domain.FinancialActivityAccount;
import org.mifosplatform.accounting.financialactivityaccount.domain.FinancialActivityAccountRepositoryWrapper;
import org.mifosplatform.accounting.glaccount.data.GLAccountDataForLookup;
import org.mifosplatform.accounting.glaccount.domain.GLAccount;
import org.mifosplatform.accounting.glaccount.domain.GLAccountRepository;
import org.mifosplatform.accounting.glaccount.domain.GLAccountType;
import org.mifosplatform.accounting.glaccount.exception.GLAccountNotFoundException;
import org.mifosplatform.accounting.glaccount.service.GLAccountReadPlatformService;
import org.mifosplatform.accounting.journalentry.api.JournalEntryJsonInputParams;
import org.mifosplatform.accounting.journalentry.command.JournalEntryCommand;
import org.mifosplatform.accounting.journalentry.command.SingleDebitOrCreditEntryCommand;
import org.mifosplatform.accounting.journalentry.data.ClientTransactionDTO;
import org.mifosplatform.accounting.journalentry.data.LoanDTO;
import org.mifosplatform.accounting.journalentry.data.SavingsDTO;
import org.mifosplatform.accounting.journalentry.domain.JournalEntry;
import org.mifosplatform.accounting.journalentry.domain.JournalEntryRepository;
import org.mifosplatform.accounting.journalentry.domain.JournalEntryType;
import org.mifosplatform.accounting.journalentry.exception.JournalEntriesNotFoundException;
import org.mifosplatform.accounting.journalentry.exception.JournalEntryInvalidException;
import org.mifosplatform.accounting.journalentry.exception.JournalEntryInvalidException.GL_JOURNAL_ENTRY_INVALID_REASON;
import org.mifosplatform.accounting.journalentry.serialization.JournalEntryCommandFromApiJsonDeserializer;
import org.mifosplatform.accounting.provisioning.domain.LoanProductProvisioningEntry;
import org.mifosplatform.accounting.provisioning.domain.ProvisioningEntry;
import org.mifosplatform.accounting.rule.domain.AccountingRule;
import org.mifosplatform.accounting.rule.domain.AccountingRuleRepository;
import org.mifosplatform.accounting.rule.exception.AccountingRuleNotFoundException;
import org.mifosplatform.infrastructure.core.api.JsonCommand;
import org.mifosplatform.infrastructure.core.data.ApiParameterError;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResult;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResultBuilder;
import org.mifosplatform.infrastructure.core.data.DataValidatorBuilder;
import org.mifosplatform.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.mifosplatform.infrastructure.core.exception.PlatformApiDataValidationException;
import org.mifosplatform.infrastructure.core.exception.PlatformDataIntegrityException;
import org.mifosplatform.infrastructure.security.service.PlatformSecurityContext;
import org.mifosplatform.organisation.office.domain.Office;
import org.mifosplatform.organisation.office.domain.OfficeRepository;
import org.mifosplatform.organisation.office.domain.OrganisationCurrencyRepositoryWrapper;
import org.mifosplatform.organisation.office.exception.OfficeNotFoundException;
import org.mifosplatform.portfolio.client.domain.ClientTransaction;
import org.mifosplatform.portfolio.paymentdetail.domain.PaymentDetail;
import org.mifosplatform.portfolio.paymentdetail.service.PaymentDetailWritePlatformService;
import org.mifosplatform.useradministration.domain.AppUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
public class JournalEntryWritePlatformServiceJpaRepositoryImpl implements JournalEntryWritePlatformService {

    private final static Logger logger = LoggerFactory.getLogger(JournalEntryWritePlatformServiceJpaRepositoryImpl.class);

    private final GLClosureRepository glClosureRepository;
    private final GLAccountRepository glAccountRepository;
    private final JournalEntryRepository glJournalEntryRepository;
    private final OfficeRepository officeRepository;
    private final AccountingProcessorForLoanFactory accountingProcessorForLoanFactory;
    private final AccountingProcessorForSavingsFactory accountingProcessorForSavingsFactory;
    private final AccountingProcessorHelper helper;
    private final JournalEntryCommandFromApiJsonDeserializer fromApiJsonDeserializer;
    private final AccountingRuleRepository accountingRuleRepository;
    private final GLAccountReadPlatformService glAccountReadPlatformService;
    private final OrganisationCurrencyRepositoryWrapper organisationCurrencyRepository;
    private final PlatformSecurityContext context;
    private final PaymentDetailWritePlatformService paymentDetailWritePlatformService;
    private final FinancialActivityAccountRepositoryWrapper financialActivityAccountRepositoryWrapper;
    private final CashBasedAccountingProcessorForClientTransactions accountingProcessorForClientTransactions;

    @Autowired
    public JournalEntryWritePlatformServiceJpaRepositoryImpl(final GLClosureRepository glClosureRepository,
            final JournalEntryRepository glJournalEntryRepository, final OfficeRepository officeRepository,
            final GLAccountRepository glAccountRepository, final JournalEntryCommandFromApiJsonDeserializer fromApiJsonDeserializer,
            final AccountingProcessorHelper accountingProcessorHelper, final AccountingRuleRepository accountingRuleRepository,
            final AccountingProcessorForLoanFactory accountingProcessorForLoanFactory,
            final AccountingProcessorForSavingsFactory accountingProcessorForSavingsFactory,
            final GLAccountReadPlatformService glAccountReadPlatformService,
            final OrganisationCurrencyRepositoryWrapper organisationCurrencyRepository, final PlatformSecurityContext context,
            final PaymentDetailWritePlatformService paymentDetailWritePlatformService,
            final FinancialActivityAccountRepositoryWrapper financialActivityAccountRepositoryWrapper,
            final CashBasedAccountingProcessorForClientTransactions accountingProcessorForClientTransactions) {
        this.glClosureRepository = glClosureRepository;
        this.officeRepository = officeRepository;
        this.glJournalEntryRepository = glJournalEntryRepository;
        this.fromApiJsonDeserializer = fromApiJsonDeserializer;
        this.glAccountRepository = glAccountRepository;
        this.accountingProcessorForLoanFactory = accountingProcessorForLoanFactory;
        this.accountingProcessorForSavingsFactory = accountingProcessorForSavingsFactory;
        this.helper = accountingProcessorHelper;
        this.accountingRuleRepository = accountingRuleRepository;
        this.glAccountReadPlatformService = glAccountReadPlatformService;
        this.organisationCurrencyRepository = organisationCurrencyRepository;
        this.context = context;
        this.paymentDetailWritePlatformService = paymentDetailWritePlatformService;
        this.financialActivityAccountRepositoryWrapper = financialActivityAccountRepositoryWrapper;
        this.accountingProcessorForClientTransactions = accountingProcessorForClientTransactions;
    }

    @Transactional
    @Override
    public CommandProcessingResult createJournalEntry(final JsonCommand command) {
        try {
            final JournalEntryCommand jec = this.fromApiJsonDeserializer.commandFromApiJson(command.json());
            jec.validateForCreate();
            final Boolean multipleCreditOffices = jec.hasMultipleOffices(JournalEntryJsonInputParams.CREDITS.getValue());
            final Boolean multipleDebitOffices = jec.hasMultipleOffices(JournalEntryJsonInputParams.DEBITS.getValue());
            final Long accountRuleId = command.longValueOfParameterNamed(JournalEntryJsonInputParams.ACCOUNTING_RULE.getValue());
            final String currencyCode = command.stringValueOfParameterNamed(JournalEntryJsonInputParams.CURRENCY_CODE.getValue());
            final Date transactionDate = command.DateValueOfParameterNamed(JournalEntryJsonInputParams.TRANSACTION_DATE.getValue());
            final String referenceNumber = command.stringValueOfParameterNamed(JournalEntryJsonInputParams.REFERENCE_NUMBER.getValue());

            if(multipleCreditOffices && multipleDebitOffices){
                throw new JournalEntryInvalidException(GL_JOURNAL_ENTRY_INVALID_REASON.INVALID_DEBIT_OR_CREDIT_OFFICES);
            }

            /** Capture payment details **/
            final Map<String, Object> changes = new LinkedHashMap<>();
            final PaymentDetail paymentDetail = this.paymentDetailWritePlatformService.createAndPersistPaymentDetail(command, changes);

            if(jec.isInterBranch()){
                // if journal entry command involves multiple offices, retrieve control account and split up into multiple journal entries
                FinancialActivityAccount controlAccount =
                        this.financialActivityAccountRepositoryWrapper.findByFinancialActivityTypeWithNotFoundDetection(AccountingConstants.FINANCIAL_ACTIVITY.INTERBRANCH_CONTROL.getValue());
                final List<SingleDebitOrCreditEntryCommand> finalCreditList = new ArrayList<>();
                final List<SingleDebitOrCreditEntryCommand> finalDebitList = new ArrayList<>();

                if(multipleCreditOffices){
                    final Long officeId = jec.getDebits()[0].getOfficeId();
                    final Office office = this.officeRepository.findOne(officeId);
                    if(office == null){ throw new OfficeNotFoundException(officeId); }
                    final String transactionId = generateTransactionId(officeId);
                    final Map<Long,List<SingleDebitOrCreditEntryCommand>> creditOfficeMap = new HashMap<>();
                    finalDebitList.addAll(Arrays.asList(jec.getDebits()));
                    BigDecimal creditControlAmount = BigDecimal.ZERO;

                    for(SingleDebitOrCreditEntryCommand credit: jec.getCredits()){
                        final List<SingleDebitOrCreditEntryCommand> differentOfficeCredits;
                        if(creditOfficeMap.containsKey(credit.getOfficeId())){
                            differentOfficeCredits = creditOfficeMap.get(credit.getOfficeId());
                        }else{
                            differentOfficeCredits = new ArrayList<>();
                        }
                        differentOfficeCredits.add(credit);
                        creditOfficeMap.put(credit.getOfficeId(),differentOfficeCredits);
                    }

                    for(Map.Entry<Long,List<SingleDebitOrCreditEntryCommand>> entry : creditOfficeMap.entrySet()){
                        if(entry.getKey().equals(officeId)){
                            finalCreditList.addAll(entry.getValue());
                        }else{
                            BigDecimal amount = BigDecimal.ZERO;

                            for(SingleDebitOrCreditEntryCommand creditEntryCommand : entry.getValue()){
                                amount = amount.add(creditEntryCommand.getAmount());
                                creditControlAmount = creditControlAmount.add(creditEntryCommand.getAmount());
                            }

                            final SingleDebitOrCreditEntryCommand controlDebit =
                                    new SingleDebitOrCreditEntryCommand(null,controlAccount.getGlAccount().getId(),amount,null,entry.getKey());

                            finalCreditList.addAll(entry.getValue());
                            finalDebitList.add(controlDebit);
                        }
                    }

                    final SingleDebitOrCreditEntryCommand controlCredit =
                            new SingleDebitOrCreditEntryCommand(null,controlAccount.getGlAccount().getId(),creditControlAmount,null,officeId);
                    finalCreditList.add(controlCredit);

                    SingleDebitOrCreditEntryCommand[] finalCredits = finalCreditList.toArray(new SingleDebitOrCreditEntryCommand[finalCreditList.size()]);
                    SingleDebitOrCreditEntryCommand[] finalDebits = finalDebitList.toArray(new SingleDebitOrCreditEntryCommand[finalDebitList.size()]);

                    final JournalEntryCommand journalEntryCommand = new JournalEntryCommand(jec.getCurrencyCode(),jec.getTransactionDate(),jec.getComments(),
                            finalCredits,finalDebits,jec.getReferenceNumber(),jec.getAccountingRuleId(),jec.getAmount(),jec.getPaymentTypeId(),
                            jec.getAccountNumber(),jec.getCheckNumber(),jec.getReceiptNumber(),jec.getBankNumber(),jec.getRoutingCode());

                    createJournalEntryForSingleOffice(journalEntryCommand,accountRuleId,currencyCode,transactionDate,referenceNumber,paymentDetail,transactionId);

                    return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withTransactionId(transactionId).build();
                }else{
                    final Long officeId = jec.getCredits()[0].getOfficeId();
                    final Office office = this.officeRepository.findOne(officeId);
                    if(office == null){ throw new OfficeNotFoundException(officeId); }
                    final String transactionId = generateTransactionId(officeId);
                    final Map<Long,List<SingleDebitOrCreditEntryCommand>> debitOfficeMap = new HashMap<>();
                    finalCreditList.addAll(Arrays.asList(jec.getCredits()));
                    BigDecimal debitControlAmount = BigDecimal.ZERO;

                    for(SingleDebitOrCreditEntryCommand debit: jec.getDebits()){
                        final List<SingleDebitOrCreditEntryCommand> differentOfficeDebits;
                        if(debitOfficeMap.containsKey(debit.getOfficeId())){
                            differentOfficeDebits = debitOfficeMap.get(debit.getOfficeId());
                        }else{
                            differentOfficeDebits = new ArrayList<>();
                        }
                        differentOfficeDebits.add(debit);
                        debitOfficeMap.put(debit.getOfficeId(),differentOfficeDebits);
                    }

                    for(Map.Entry<Long,List<SingleDebitOrCreditEntryCommand>> entry : debitOfficeMap.entrySet()){
                        if(entry.getKey().equals(officeId)){
                            finalDebitList.addAll(entry.getValue());
                        }else{
                            BigDecimal amount = BigDecimal.ZERO;

                            for(SingleDebitOrCreditEntryCommand debitEntryCommand : entry.getValue()){
                                amount = amount.add(debitEntryCommand.getAmount());
                                debitControlAmount = debitControlAmount.add(debitEntryCommand.getAmount());
                            }

                            final SingleDebitOrCreditEntryCommand controlCredit =
                                    new SingleDebitOrCreditEntryCommand(null,controlAccount.getGlAccount().getId(),amount,null,entry.getKey());

                            finalDebitList.addAll(entry.getValue());
                            finalCreditList.add(controlCredit);
                        }
                    }

                    final SingleDebitOrCreditEntryCommand controlDebit =
                            new SingleDebitOrCreditEntryCommand(null,controlAccount.getGlAccount().getId(),debitControlAmount,null,officeId);
                    finalDebitList.add(controlDebit);

                    SingleDebitOrCreditEntryCommand[] finalCredits = finalCreditList.toArray(new SingleDebitOrCreditEntryCommand[finalCreditList.size()]);
                    SingleDebitOrCreditEntryCommand[] finalDebits = finalDebitList.toArray(new SingleDebitOrCreditEntryCommand[finalDebitList.size()]);

                    final JournalEntryCommand journalEntryCommand = new JournalEntryCommand(jec.getCurrencyCode(),jec.getTransactionDate(),jec.getComments(),
                            finalCredits,finalDebits,jec.getReferenceNumber(),jec.getAccountingRuleId(),jec.getAmount(),jec.getPaymentTypeId(),
                            jec.getAccountNumber(),jec.getCheckNumber(),jec.getReceiptNumber(),jec.getBankNumber(),jec.getRoutingCode());

                    createJournalEntryForSingleOffice(journalEntryCommand,accountRuleId,currencyCode,transactionDate,referenceNumber,paymentDetail,transactionId);

                    return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withTransactionId(transactionId).build();
                }
            }else {
                // check office is valid and set transaction id
                final Long officeId = jec.getCredits()[0].getOfficeId();

                final Office office = this.officeRepository.findOne(officeId);
                if (office == null) {
                    throw new OfficeNotFoundException(officeId);
                }
                final String transactionId = generateTransactionId(officeId);

                createJournalEntryForSingleOffice(jec,accountRuleId,currencyCode,transactionDate,referenceNumber,paymentDetail,transactionId);

                return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withOfficeId(officeId)
                        .withTransactionId(transactionId).build();
            }
        } catch (final DataIntegrityViolationException dve) {
            handleJournalEntryDataIntegrityIssues(dve);
            return null;
        }
    }

    private void createJournalEntryForSingleOffice(final JournalEntryCommand journalEntryCommand, final Long accountRuleId,
                                                   final String currencyCode, final Date transactionDate, final String referenceNumber,
                                                   final PaymentDetail paymentDetail, final String transactionId){

        validateBusinessRulesForJournalEntries(journalEntryCommand);

        /** Save these Journal entries **/

        if (accountRuleId != null) {

            final AccountingRule accountingRule = this.accountingRuleRepository.findOne(accountRuleId);
            if (accountingRule == null) {
                throw new AccountingRuleNotFoundException(accountRuleId);
            }

            if (accountingRule.getAccountToCredit() == null) {
                if (journalEntryCommand.getCredits() == null) {
                    throw new JournalEntryInvalidException(
                            GL_JOURNAL_ENTRY_INVALID_REASON.NO_DEBITS_OR_CREDITS, null, null, null);
                }
                if (journalEntryCommand.getDebits() != null) {
                    checkDebitOrCreditAccountsAreValid(accountingRule, journalEntryCommand.getCredits(),
                            journalEntryCommand.getDebits());
                    checkDebitAndCreditAmounts(journalEntryCommand.getCredits(), journalEntryCommand.getDebits());
                }

                saveAllDebitOrCreditEntries(journalEntryCommand, paymentDetail, currencyCode, transactionDate,
                        journalEntryCommand.getCredits(), JournalEntryType.CREDIT, referenceNumber, transactionId);
            } else {
                final GLAccount creditAccountHead = accountingRule.getAccountToCredit();
                validateGLAccountForTransaction(creditAccountHead);
                validateDebitOrCreditArrayForExistingGLAccount(creditAccountHead, journalEntryCommand.getCredits());
                saveAllDebitOrCreditEntries(journalEntryCommand, paymentDetail, currencyCode, transactionDate,
                        journalEntryCommand.getCredits(), JournalEntryType.CREDIT, referenceNumber, transactionId);
            }

            if (accountingRule.getAccountToDebit() == null) {
                if (journalEntryCommand.getDebits() == null) {
                    throw new JournalEntryInvalidException(
                            GL_JOURNAL_ENTRY_INVALID_REASON.NO_DEBITS_OR_CREDITS, null, null, null);
                }
                if (journalEntryCommand.getCredits() != null) {
                    checkDebitOrCreditAccountsAreValid(accountingRule, journalEntryCommand.getCredits(),
                            journalEntryCommand.getDebits());
                    checkDebitAndCreditAmounts(journalEntryCommand.getCredits(), journalEntryCommand.getDebits());
                }

                saveAllDebitOrCreditEntries(journalEntryCommand, paymentDetail, currencyCode, transactionDate,
                        journalEntryCommand.getDebits(), JournalEntryType.DEBIT, referenceNumber, transactionId);
            } else {
                final GLAccount debitAccountHead = accountingRule.getAccountToDebit();
                validateGLAccountForTransaction(debitAccountHead);
                validateDebitOrCreditArrayForExistingGLAccount(debitAccountHead, journalEntryCommand.getDebits());
                saveAllDebitOrCreditEntries(journalEntryCommand, paymentDetail, currencyCode, transactionDate,
                        journalEntryCommand.getDebits(), JournalEntryType.DEBIT, referenceNumber, transactionId);
            }
        } else {

            saveAllDebitOrCreditEntries(journalEntryCommand, paymentDetail, currencyCode, transactionDate,
                    journalEntryCommand.getDebits(), JournalEntryType.DEBIT, referenceNumber, transactionId);

            saveAllDebitOrCreditEntries(journalEntryCommand, paymentDetail, currencyCode, transactionDate,
                    journalEntryCommand.getCredits(), JournalEntryType.CREDIT, referenceNumber, transactionId);

        }
    }

    private void validateDebitOrCreditArrayForExistingGLAccount(final GLAccount glaccount,
            final SingleDebitOrCreditEntryCommand[] creditOrDebits) {
        /**
         * If a glaccount is assigned for a rule the credits or debits array
         * should have only one entry and it must be same as existing account
         */
        if (creditOrDebits.length != 1) { throw new JournalEntryInvalidException(
                GL_JOURNAL_ENTRY_INVALID_REASON.INVALID_DEBIT_OR_CREDIT_ACCOUNTS, null, null, null); }
        for (final SingleDebitOrCreditEntryCommand creditOrDebit : creditOrDebits) {
            if (!glaccount.getId().equals(creditOrDebit.getGlAccountId())) { throw new JournalEntryInvalidException(
                    GL_JOURNAL_ENTRY_INVALID_REASON.INVALID_DEBIT_OR_CREDIT_ACCOUNTS, null, null, null); }
        }
    }

    @SuppressWarnings("null")
    private void checkDebitOrCreditAccountsAreValid(final AccountingRule accountingRule, final SingleDebitOrCreditEntryCommand[] credits,
            final SingleDebitOrCreditEntryCommand[] debits) {
        // Validate the debit and credit arrays are appropriate accounts
        List<GLAccountDataForLookup> allowedCreditGLAccounts = new ArrayList<>();
        List<GLAccountDataForLookup> allowedDebitGLAccounts = new ArrayList<>();
        final SingleDebitOrCreditEntryCommand[] validCredits = new SingleDebitOrCreditEntryCommand[credits.length];
        final SingleDebitOrCreditEntryCommand[] validDebits = new SingleDebitOrCreditEntryCommand[debits.length];

        if (credits != null && credits.length > 0) {
            allowedCreditGLAccounts = this.glAccountReadPlatformService.retrieveAccountsByTagId(accountingRule.getId(),
                    JournalEntryType.CREDIT.getValue());
            for (final GLAccountDataForLookup accountDataForLookup : allowedCreditGLAccounts) {
                for (int i = 0; i < credits.length; i++) {
                    final SingleDebitOrCreditEntryCommand credit = credits[i];
                    if (credit.getGlAccountId().equals(accountDataForLookup.getId())) {
                        validCredits[i] = credit;
                    }
                }
            }
            if (credits.length != validCredits.length) { throw new RuntimeException("Invalid credits"); }
        }

        if (debits != null && debits.length > 0) {
            allowedDebitGLAccounts = this.glAccountReadPlatformService.retrieveAccountsByTagId(accountingRule.getId(),
                    JournalEntryType.DEBIT.getValue());
            for (final GLAccountDataForLookup accountDataForLookup : allowedDebitGLAccounts) {
                for (int i = 0; i < debits.length; i++) {
                    final SingleDebitOrCreditEntryCommand debit = debits[i];
                    if (debit.getGlAccountId().equals(accountDataForLookup.getId())) {
                        validDebits[i] = debit;
                    }
                }
            }
            if (debits.length != validDebits.length) { throw new RuntimeException("Invalid debits"); }
        }
    }

    private void checkDebitAndCreditAmounts(final SingleDebitOrCreditEntryCommand[] credits, final SingleDebitOrCreditEntryCommand[] debits) {
        // sum of all debits must be = sum of all credits
        BigDecimal creditsSum = BigDecimal.ZERO;
        BigDecimal debitsSum = BigDecimal.ZERO;
        for (final SingleDebitOrCreditEntryCommand creditEntryCommand : credits) {
            if (creditEntryCommand.getAmount() == null || creditEntryCommand.getGlAccountId() == null) { throw new JournalEntryInvalidException(
                    GL_JOURNAL_ENTRY_INVALID_REASON.DEBIT_CREDIT_ACCOUNT_OR_AMOUNT_EMPTY, null, null, null); }
            creditsSum = creditsSum.add(creditEntryCommand.getAmount());
        }
        for (final SingleDebitOrCreditEntryCommand debitEntryCommand : debits) {
            if (debitEntryCommand.getAmount() == null || debitEntryCommand.getGlAccountId() == null) { throw new JournalEntryInvalidException(
                    GL_JOURNAL_ENTRY_INVALID_REASON.DEBIT_CREDIT_ACCOUNT_OR_AMOUNT_EMPTY, null, null, null); }
            debitsSum = debitsSum.add(debitEntryCommand.getAmount());
        }
        if (creditsSum.compareTo(debitsSum) != 0) { throw new JournalEntryInvalidException(
                GL_JOURNAL_ENTRY_INVALID_REASON.DEBIT_CREDIT_SUM_MISMATCH, null, null, null); }
    }

    private void validateGLAccountForTransaction(final GLAccount creditOrDebitAccountHead) {
        /***
         * validate that the account allows manual adjustments and is not
         * disabled
         **/
        if (creditOrDebitAccountHead.isDisabled()) {
            throw new JournalEntryInvalidException(GL_JOURNAL_ENTRY_INVALID_REASON.GL_ACCOUNT_DISABLED, null,
                    creditOrDebitAccountHead.getName(), creditOrDebitAccountHead.getGlCode());
        } else if (!creditOrDebitAccountHead.isManualEntriesAllowed()) { throw new JournalEntryInvalidException(
                GL_JOURNAL_ENTRY_INVALID_REASON.GL_ACCOUNT_MANUAL_ENTRIES_NOT_PERMITTED, null, creditOrDebitAccountHead.getName(),
                creditOrDebitAccountHead.getGlCode()); }
    }

    @Transactional
    @Override
    public CommandProcessingResult revertJournalEntry(final JsonCommand command) {
        // is the transaction Id valid
        //final List<JournalEntry> journalEntryList = this.glJournalEntryRepository.findAll();

        final List<JournalEntry> journalEntries = this.glJournalEntryRepository.findUnReversedManualJournalEntriesByTransactionId(command
                .getTransactionId());
        
        String reversalComment = null;
        
        if (command.hasParameter(JournalEntryJsonInputParams.COMMENTS.getValue())) {
            reversalComment = command.stringValueOfParameterNamed(JournalEntryJsonInputParams.COMMENTS.getValue());
        }

        if (journalEntries.size() <= 1) { throw new JournalEntriesNotFoundException(command.getTransactionId()); }
        final String reversalTransactionId = revertJournalEntry(journalEntries, reversalComment);
        return new CommandProcessingResultBuilder().withTransactionId(reversalTransactionId).build();
    }

    public String revertJournalEntry(final List<JournalEntry> journalEntries, String reversalComment) {
        final Long officeId = journalEntries.get(0).getOffice().getId();
        final String reversalTransactionId = generateTransactionId(officeId);
        final boolean manualEntry = true;

        final boolean useDefaultComment = StringUtils.isBlank(reversalComment);

        validateCommentForReversal(reversalComment);

        for (final JournalEntry journalEntry : journalEntries) {
            JournalEntry reversalJournalEntry;
            if (useDefaultComment) {
                reversalComment = "Reversal entry for Journal Entry with Entry Id  :" + journalEntry.getId() + " and transaction Id "
                        + journalEntry.getTransactionId();
            }
            if (journalEntry.isDebitEntry()) {
                reversalJournalEntry = JournalEntry.createNew(journalEntry.getOffice(), journalEntry.getPaymentDetails(),
                        journalEntry.getGlAccount(), journalEntry.getCurrencyCode(), reversalTransactionId, manualEntry,
                        journalEntry.getTransactionDate(), JournalEntryType.CREDIT, journalEntry.getAmount(), reversalComment, null, null,
                        journalEntry.getReferenceNumber(), journalEntry.getLoanTransaction(), journalEntry.getSavingsTransaction(),
                        journalEntry.getClientTransaction());
            } else {
                reversalJournalEntry = JournalEntry.createNew(journalEntry.getOffice(), journalEntry.getPaymentDetails(),
                        journalEntry.getGlAccount(), journalEntry.getCurrencyCode(), reversalTransactionId, manualEntry,
                        journalEntry.getTransactionDate(), JournalEntryType.DEBIT, journalEntry.getAmount(), reversalComment, null, null,
                        journalEntry.getReferenceNumber(), journalEntry.getLoanTransaction(), journalEntry.getSavingsTransaction(),
                        journalEntry.getClientTransaction());
            }
            // save the reversal entry
            this.glJournalEntryRepository.saveAndFlush(reversalJournalEntry);
            journalEntry.setReversed(true);
            journalEntry.setReversalJournalEntry(reversalJournalEntry);
            // save the updated journal entry
            this.glJournalEntryRepository.saveAndFlush(journalEntry);
        }
        return reversalTransactionId;
    }

    @Override
    public String revertProvisioningJournalEntries(final Date reversalTransactionDate, final Long entityId, final Integer entityType) {
        List<JournalEntry> journalEntries = this.glJournalEntryRepository.findProvisioningJournalEntriesByEntityId(entityId, entityType);
        final String reversalTransactionId = journalEntries.get(0).getTransactionId();
        for (final JournalEntry journalEntry : journalEntries) {
            JournalEntry reversalJournalEntry;
            String reversalComment = "Reversal entry for Journal Entry with Entry Id  :" + journalEntry.getId() + " and transaction Id "
                    + journalEntry.getTransactionId();
            if (journalEntry.isDebitEntry()) {
                reversalJournalEntry = JournalEntry.createNew(journalEntry.getOffice(), journalEntry.getPaymentDetails(),
                        journalEntry.getGlAccount(), journalEntry.getCurrencyCode(), journalEntry.getTransactionId(), Boolean.FALSE,
                        reversalTransactionDate, JournalEntryType.CREDIT, journalEntry.getAmount(), reversalComment,
                        journalEntry.getEntityType(), journalEntry.getEntityId(), journalEntry.getReferenceNumber(),
                        journalEntry.getLoanTransaction(), journalEntry.getSavingsTransaction(), journalEntry.getClientTransaction());
            } else {
                reversalJournalEntry = JournalEntry.createNew(journalEntry.getOffice(), journalEntry.getPaymentDetails(),
                        journalEntry.getGlAccount(), journalEntry.getCurrencyCode(), journalEntry.getTransactionId(), Boolean.FALSE,
                        reversalTransactionDate, JournalEntryType.DEBIT, journalEntry.getAmount(), reversalComment,
                        journalEntry.getEntityType(), journalEntry.getEntityId(), journalEntry.getReferenceNumber(),
                        journalEntry.getLoanTransaction(), journalEntry.getSavingsTransaction(), journalEntry.getClientTransaction());
            }
            // save the reversal entry
            this.glJournalEntryRepository.save(reversalJournalEntry);
            journalEntry.setReversalJournalEntry(reversalJournalEntry);
            // save the updated journal entry
            this.glJournalEntryRepository.save(journalEntry);
        }
        return reversalTransactionId;

    }

    @Override
    public String createProvisioningJournalEntries(ProvisioningEntry provisioningEntry) {
        Collection<LoanProductProvisioningEntry> provisioningEntries = provisioningEntry.getLoanProductProvisioningEntries();
        Map<OfficeCurrencyKey, List<LoanProductProvisioningEntry>> officeMap = new HashMap<>();

        for (LoanProductProvisioningEntry entry : provisioningEntries) {
            OfficeCurrencyKey key = new OfficeCurrencyKey(entry.getOffice(), entry.getCurrencyCode());
            if (officeMap.containsKey(key)) {
                List<LoanProductProvisioningEntry> list = officeMap.get(key);
                list.add(entry);
            } else {
                List<LoanProductProvisioningEntry> list = new ArrayList<>();
                list.add(entry);
                officeMap.put(key, list);
            }
        }

        Set<OfficeCurrencyKey> officeSet = officeMap.keySet();
        Map<GLAccount, BigDecimal> liabilityMap = new HashMap<>();
        Map<GLAccount, BigDecimal> expenseMap = new HashMap<>();

        for (OfficeCurrencyKey key : officeSet) {
            liabilityMap.clear();
            expenseMap.clear();
            List<LoanProductProvisioningEntry> entries = officeMap.get(key);
            for (LoanProductProvisioningEntry entry : entries) {
                if (liabilityMap.containsKey(entry.getLiabilityAccount())) {
                    BigDecimal amount = liabilityMap.get(entry.getLiabilityAccount());
                    amount = amount.add(entry.getReservedAmount());
                    liabilityMap.put(entry.getLiabilityAccount(), amount);
                } else {
                    BigDecimal amount = BigDecimal.ZERO.add(entry.getReservedAmount());
                    liabilityMap.put(entry.getLiabilityAccount(), amount);
                }

                if (expenseMap.containsKey(entry.getExpenseAccount())) {
                    BigDecimal amount = expenseMap.get(entry.getExpenseAccount());
                    amount = amount.add(entry.getReservedAmount());
                    expenseMap.put(entry.getExpenseAccount(), amount);
                } else {
                    BigDecimal amount = BigDecimal.ZERO.add(entry.getReservedAmount());
                    expenseMap.put(entry.getExpenseAccount(), amount);
                }
            }
            createJournalEnry(provisioningEntry.getCreatedDate(), provisioningEntry.getId(), key.office, key.currency, liabilityMap, expenseMap);
        }
        return "P"+provisioningEntry.getId() ;
    }
    
    private void createJournalEnry(Date transactionDate, Long entryId, Office office, String currencyCode, Map<GLAccount, BigDecimal> liabilityMap,
            Map<GLAccount, BigDecimal> expenseMap) {
        Set<GLAccount> liabilityAccounts = liabilityMap.keySet();
        for (GLAccount account : liabilityAccounts) {
            this.helper.createProvisioningCreditJournalEntry(transactionDate,entryId, office, currencyCode, account,
                    liabilityMap.get(account));
        }
        Set<GLAccount> expenseAccounts = expenseMap.keySet();
        for (GLAccount account : expenseAccounts) {
            this.helper.createProvisioningDebitJournalEntry(transactionDate,entryId, office, currencyCode, account,
                    expenseMap.get(account));
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult reconcileJournalEntry(final JsonCommand command) {
        // is the transaction Id valid

                final List<JournalEntry> journalEntries = this.glJournalEntryRepository.findByIsReconciledFalseAndTransactionId(command
                .getTransactionId());

        if (journalEntries.size() <= 1) { throw new JournalEntriesNotFoundException(command.getTransactionId()); }


        for (final JournalEntry journalEntry : journalEntries) {

            journalEntry.setIsReconciled(true);
            // save the updated journal entry
            this.glJournalEntryRepository.saveAndFlush(journalEntry);
        }
        return new CommandProcessingResultBuilder().withTransactionId(command.getTransactionId()).build();
    }

    private void validateCommentForReversal(final String reversalComment) {
        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();

        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("GLJournalEntry");

        baseDataValidator.reset().parameter("comments").value(reversalComment).notExceedingLengthOf(500);

        if (!dataValidationErrors.isEmpty()) { throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist",
                "Validation errors exist.", dataValidationErrors); }
    }


    @Transactional
    @Override
    public CommandProcessingResult batchReconciliationJournalEntry(final JsonCommand command) {

        final String[] transactionIds = command.arrayValueOfParameterNamed("transactionId");

        final List<JournalEntry> journalEntries = this.glJournalEntryRepository.findByIsReconciledFalseAndTransactionIdIn(transactionIds);

        if (journalEntries.size() <= 1) { throw new JournalEntriesNotFoundException(transactionIds); }


        for (final JournalEntry journalEntry : journalEntries) {

            journalEntry.setIsReconciled(true);
            // save the updated journal entry
            this.glJournalEntryRepository.saveAndFlush(journalEntry);
        }
        return new CommandProcessingResultBuilder().withTransactionId(command.getTransactionId()).build();
    }



    @Transactional
    @Override
    public void createJournalEntriesForLoan(final Map<String, Object> accountingBridgeData) {

        final boolean cashBasedAccountingEnabled = (Boolean) accountingBridgeData.get("cashBasedAccountingEnabled");
        final boolean upfrontAccrualBasedAccountingEnabled = (Boolean) accountingBridgeData.get("upfrontAccrualBasedAccountingEnabled");
        final boolean periodicAccrualBasedAccountingEnabled = (Boolean) accountingBridgeData.get("periodicAccrualBasedAccountingEnabled");

        if (cashBasedAccountingEnabled || upfrontAccrualBasedAccountingEnabled || periodicAccrualBasedAccountingEnabled) {
            final LoanDTO loanDTO = this.helper.populateLoanDtoFromMap(accountingBridgeData, cashBasedAccountingEnabled,
                    upfrontAccrualBasedAccountingEnabled, periodicAccrualBasedAccountingEnabled);
            final AccountingProcessorForLoan accountingProcessorForLoan = this.accountingProcessorForLoanFactory
                    .determineProcessor(loanDTO);
            accountingProcessorForLoan.createJournalEntriesForLoan(loanDTO);
        }
    }

    @Transactional
    @Override
    public void createJournalEntriesForSavings(final Map<String, Object> accountingBridgeData) {

        final boolean cashBasedAccountingEnabled = (Boolean) accountingBridgeData.get("cashBasedAccountingEnabled");
        final boolean accrualBasedAccountingEnabled = (Boolean) accountingBridgeData.get("accrualBasedAccountingEnabled");

        if (cashBasedAccountingEnabled || accrualBasedAccountingEnabled) {
            final SavingsDTO savingsDTO = this.helper.populateSavingsDtoFromMap(accountingBridgeData, cashBasedAccountingEnabled,
                    accrualBasedAccountingEnabled);
            final AccountingProcessorForSavings accountingProcessorForSavings = this.accountingProcessorForSavingsFactory
                    .determineProcessor(savingsDTO);
            accountingProcessorForSavings.createJournalEntriesForSavings(savingsDTO);
        }

    }

    private void validateBusinessRulesForJournalEntries(final JournalEntryCommand command) {
        /** check if date of Journal entry is valid ***/
        final LocalDate entryLocalDate = command.getTransactionDate();
        final Date transactionDate = entryLocalDate.toDateTimeAtStartOfDay().toDate();
        // shouldn't be in the future
        final Date todaysDate = new Date();
        if (transactionDate.after(todaysDate)) { throw new JournalEntryInvalidException(GL_JOURNAL_ENTRY_INVALID_REASON.FUTURE_DATE,
                transactionDate, null, null); }
        // shouldn't be before an accounting closure
        final GLClosure latestGLClosure = command.getOfficeId()!= null ? this.glClosureRepository.getLatestGLClosureByBranch(command.getOfficeId()) : null;
        if (latestGLClosure != null) {
            if (latestGLClosure.getClosingDate().after(transactionDate) || latestGLClosure.getClosingDate().equals(transactionDate)) { throw new JournalEntryInvalidException(
                    GL_JOURNAL_ENTRY_INVALID_REASON.ACCOUNTING_CLOSED, latestGLClosure.getClosingDate(), null, null); }
        }

        /*** check if credits and debits are valid **/
        final SingleDebitOrCreditEntryCommand[] credits = command.getCredits();
        final SingleDebitOrCreditEntryCommand[] debits = command.getDebits();

        // atleast one debit or credit must be present
        if (credits == null || credits.length <= 0 || debits == null || debits.length <= 0) { throw new JournalEntryInvalidException(
                GL_JOURNAL_ENTRY_INVALID_REASON.NO_DEBITS_OR_CREDITS, null, null, null); }

        checkDebitAndCreditAmounts(credits, debits);
    }

    private void saveAllDebitOrCreditEntries(final JournalEntryCommand command, final PaymentDetail paymentDetail,
            final String currencyCode, final Date transactionDate, final SingleDebitOrCreditEntryCommand[] singleDebitOrCreditEntryCommands,
            final JournalEntryType type, final String referenceNumber, final String transactionId) {
        final boolean manualEntry = true;

        for (final SingleDebitOrCreditEntryCommand singleDebitOrCreditEntryCommand : singleDebitOrCreditEntryCommands) {
            final GLAccount glAccount = this.glAccountRepository.findOne(singleDebitOrCreditEntryCommand.getGlAccountId());
            if (glAccount == null) { throw new GLAccountNotFoundException(singleDebitOrCreditEntryCommand.getGlAccountId()); }
            final Long officeId = singleDebitOrCreditEntryCommand.getOfficeId();
            final Office office = this.officeRepository.findOne(officeId);
            if(office == null){ throw new OfficeNotFoundException(officeId); }

            validateGLAccountForTransaction(glAccount);

            String comments = command.getComments();
            if (!StringUtils.isBlank(singleDebitOrCreditEntryCommand.getComments())) {
                comments = singleDebitOrCreditEntryCommand.getComments();
            }

            /** Validate current code is appropriate **/
            this.organisationCurrencyRepository.findOneWithNotFoundDetection(currencyCode);

            final ClientTransaction clientTransaction = null;
            final JournalEntry glJournalEntry = JournalEntry.createNew(office, paymentDetail, glAccount, currencyCode, transactionId,
                    manualEntry, transactionDate, type, singleDebitOrCreditEntryCommand.getAmount(), comments, null, null, referenceNumber,
                    null, null, clientTransaction);
            this.glJournalEntryRepository.saveAndFlush(glJournalEntry);
        }
    }

    /**
     * TODO: Need a better implementation with guaranteed uniqueness (but not a
     * long UUID)...maybe something tied to system clock..
     */
    private String generateTransactionId(final Long officeId) {
        final AppUser user = this.context.authenticatedUser();
        final Long time = System.currentTimeMillis();
        final String uniqueVal = String.valueOf(time) + user.getId() + officeId;
        final String transactionId = Long.toHexString(Long.parseLong(uniqueVal));
        return transactionId;
    }

    private void handleJournalEntryDataIntegrityIssues(final DataIntegrityViolationException dve) {
        final Throwable realCause = dve.getMostSpecificCause();
        logger.error(dve.getMessage(), dve);
        throw new PlatformDataIntegrityException("error.msg.glJournalEntry.unknown.data.integrity.issue",
                "Unknown data integrity issue with resource Journal Entry: " + realCause.getMessage());
    }

    @Transactional
    @Override
    public CommandProcessingResult defineOpeningBalance(final JsonCommand command) {
        try {
            final JournalEntryCommand journalEntryCommand = this.fromApiJsonDeserializer.commandFromApiJson(command.json());
            journalEntryCommand.validateForCreate();

            final FinancialActivityAccount financialActivityAccountId = this.financialActivityAccountRepositoryWrapper
                    .findByFinancialActivityTypeWithNotFoundDetection(300);
            final Long contraId = financialActivityAccountId.getGlAccount().getId();
            if (contraId == null) { throw new GeneralPlatformDomainRuleException(
                    "error.msg.financial.activity.mapping.opening.balance.contra.account.cannot.be.null",
                    "office-opening-balances-contra-account value can not be null", "office-opening-balances-contra-account"); }

            validateJournalEntriesArePostedBefore(contraId);

            // check office is valid
            final Long officeId = command.longValueOfParameterNamed(JournalEntryJsonInputParams.OFFICE_ID.getValue());
            final Office office = this.officeRepository.findOne(officeId);
            if (office == null) { throw new OfficeNotFoundException(officeId); }

            final String currencyCode = command.stringValueOfParameterNamed(JournalEntryJsonInputParams.CURRENCY_CODE.getValue());

            validateBusinessRulesForJournalEntries(journalEntryCommand);

            /**
             * revert old journal entries
             */
            final List<String> transactionIdsToBeReversed = this.glJournalEntryRepository.findNonReversedContraTansactionIds(contraId,
                    officeId);
            for (String transactionId : transactionIdsToBeReversed) {
                final List<JournalEntry> journalEntries = this.glJournalEntryRepository
                        .findUnReversedManualJournalEntriesByTransactionId(transactionId);
                revertJournalEntry(journalEntries, "defining opening balance");
            }

            /** Set a transaction Id and save these Journal entries **/
            final Date transactionDate = command.DateValueOfParameterNamed(JournalEntryJsonInputParams.TRANSACTION_DATE.getValue());
            final String transactionId = generateTransactionId(officeId);

            saveAllDebitOrCreditOpeningBalanceEntries(journalEntryCommand, office, currencyCode, transactionDate,
                    journalEntryCommand.getDebits(), transactionId, JournalEntryType.DEBIT, contraId);

            saveAllDebitOrCreditOpeningBalanceEntries(journalEntryCommand, office, currencyCode, transactionDate,
                    journalEntryCommand.getCredits(), transactionId, JournalEntryType.CREDIT, contraId);

            return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withOfficeId(officeId)
                    .withTransactionId(transactionId).build();
        } catch (final DataIntegrityViolationException dve) {
            handleJournalEntryDataIntegrityIssues(dve);
            return null;
        }
    }

    private void debitOrCreditEntriesForIncomeAndExpenseBooking(final JournalEntryCommand command, final Office office, final PaymentDetail paymentDetail,
            final String currencyCode, final Date transactionDate, final SingleDebitOrCreditEntryCommand[] singleDebitOrCreditEntryCommands,
            final String transactionId, final JournalEntryType type, final String referenceNumber){
        final boolean manualEntry = false;

        for (final SingleDebitOrCreditEntryCommand singleDebitOrCreditEntryCommand : singleDebitOrCreditEntryCommands) {
            final GLAccount glAccount = this.glAccountRepository.findOne(singleDebitOrCreditEntryCommand.getGlAccountId());
            if (glAccount == null) {
                throw new GLAccountNotFoundException(singleDebitOrCreditEntryCommand.getGlAccountId());
            }

            String comments = command.getComments();
            if (!StringUtils.isBlank(singleDebitOrCreditEntryCommand.getComments())) {
                comments = singleDebitOrCreditEntryCommand.getComments();
            }

            /** Validate current code is appropriate **/
            this.organisationCurrencyRepository.findOneWithNotFoundDetection(currencyCode);

            final JournalEntry glJournalEntry = JournalEntry.createNew(office, paymentDetail, glAccount, currencyCode, transactionId,
                    manualEntry, transactionDate, type, singleDebitOrCreditEntryCommand.getAmount(), comments, null, null, referenceNumber,
                    null, null, null);
            this.glJournalEntryRepository.saveAndFlush(glJournalEntry);
        }

    }
    
    @Transactional
    @Override
    public String createJournalEntryForIncomeAndExpenseBookOff(final JournalEntryCommand journalEntryCommand) {
        try{
            journalEntryCommand.validateForCreate();

            // check office is valid
            final Long officeId = journalEntryCommand.getCredits()[0].getOfficeId();
            final Office office = this.officeRepository.findOne(officeId);
            if (office == null) { throw new OfficeNotFoundException(officeId); }

            final String currencyCode = journalEntryCommand.getCurrencyCode();

            validateBusinessRulesForJournalEntries(journalEntryCommand);
            /** Capture payment details **/
            final PaymentDetail paymentDetail =null;

            /** Set a transaction Id and save these Journal entries **/
            final Date transactionDate = journalEntryCommand.getTransactionDate().toDate();
            final String transactionId = generateTransactionId(officeId);
            final String referenceNumber = journalEntryCommand.getReferenceNumber();

            debitOrCreditEntriesForIncomeAndExpenseBooking(journalEntryCommand, office, paymentDetail, currencyCode, transactionDate,
                    journalEntryCommand.getDebits(), transactionId, JournalEntryType.DEBIT, referenceNumber);

            debitOrCreditEntriesForIncomeAndExpenseBooking(journalEntryCommand, office, paymentDetail, currencyCode, transactionDate,
                    journalEntryCommand.getCredits(), transactionId, JournalEntryType.CREDIT, referenceNumber);

            return transactionId;
        }catch (final DataIntegrityViolationException dve) {
            handleJournalEntryDataIntegrityIssues(dve);
            return null;
        }
    }

    private void saveAllDebitOrCreditOpeningBalanceEntries(final JournalEntryCommand command, final Office office,
            final String currencyCode, final Date transactionDate,
            final SingleDebitOrCreditEntryCommand[] singleDebitOrCreditEntryCommands, final String transactionId,
            final JournalEntryType type, final Long contraAccountId) {

        final boolean manualEntry = true;
        final GLAccount contraAccount = this.glAccountRepository.findOne(contraAccountId);
        if (contraAccount == null) { throw new GLAccountNotFoundException(contraAccountId); }
        if (!GLAccountType.fromInt(contraAccount.getType()).isEquityType()) { throw new GeneralPlatformDomainRuleException(
                "error.msg.configuration.opening.balance.contra.account.value.is.invalid.account.type",
                "Global configuration 'office-opening-balances-contra-account' value is not an equity type account", contraAccountId); }
        validateGLAccountForTransaction(contraAccount);
        final JournalEntryType contraType = getContraType(type);
        String comments = command.getComments();

        /** Validate current code is appropriate **/
        this.organisationCurrencyRepository.findOneWithNotFoundDetection(currencyCode);

        for (final SingleDebitOrCreditEntryCommand singleDebitOrCreditEntryCommand : singleDebitOrCreditEntryCommands) {
            final GLAccount glAccount = this.glAccountRepository.findOne(singleDebitOrCreditEntryCommand.getGlAccountId());
            if (glAccount == null) { throw new GLAccountNotFoundException(singleDebitOrCreditEntryCommand.getGlAccountId()); }

            validateGLAccountForTransaction(glAccount);

            if (!StringUtils.isBlank(singleDebitOrCreditEntryCommand.getComments())) {
                comments = singleDebitOrCreditEntryCommand.getComments();
            }

            final ClientTransaction clientTransaction = null;
            final JournalEntry glJournalEntry = JournalEntry.createNew(office, null, glAccount, currencyCode, transactionId, manualEntry,
                    transactionDate, type, singleDebitOrCreditEntryCommand.getAmount(), comments, null, null, null, null, null,
                    clientTransaction);
            this.glJournalEntryRepository.saveAndFlush(glJournalEntry);

            final JournalEntry contraEntry = JournalEntry.createNew(office, null, contraAccount, currencyCode, transactionId, manualEntry,
                    transactionDate, contraType, singleDebitOrCreditEntryCommand.getAmount(), comments, null, null, null, null, null,
                    clientTransaction);
            this.glJournalEntryRepository.saveAndFlush(contraEntry);
        }
    }

    private JournalEntryType getContraType(final JournalEntryType type) {
        final JournalEntryType contraType;
        if (type.isCreditType()) {
            contraType = JournalEntryType.DEBIT;
        } else {
            contraType = JournalEntryType.CREDIT;
        }
        return contraType;
    }

    private void validateJournalEntriesArePostedBefore(final Long contraId) {
        final List<String> transactionIds = this.glJournalEntryRepository.findNonContraTansactionIds(contraId);
        if (!CollectionUtils.isEmpty(transactionIds)) { throw new GeneralPlatformDomainRuleException(
                "error.msg.journalentry.defining.openingbalance.not.allowed",
                "Defining Opening balances not allowed after journal entries posted", transactionIds); }
    }

    @Override
    public void createJournalEntriesForClientTransactions(Map<String, Object> accountingBridgeData) {
        final ClientTransactionDTO clientTransactionDTO = this.helper.populateClientTransactionDtoFromMap(accountingBridgeData);
        accountingProcessorForClientTransactions.createJournalEntriesForClientTransaction(clientTransactionDTO);
    }
    
    private class OfficeCurrencyKey {

        Office office;
        String currency;

        OfficeCurrencyKey(Office office, String currency) {
            this.office = office;
            this.currency = currency;
        }

        @Override
        public boolean equals(Object obj) {
            if (!obj.getClass().equals(this.getClass())) return false;
            OfficeCurrencyKey copy = (OfficeCurrencyKey) obj;
            return this.office.getId() == copy.office.getId() && this.currency.equals(copy.currency);
        }

        @Override
        public int hashCode() {
            return this.office.hashCode() + this.currency.hashCode();
        }
    }
    
    @Transactional
    @Override
    public void revertJournalEntry(final String transactionId) {
        final List<JournalEntry> journalEntries = this.glJournalEntryRepository.findUnReversedJournalEntriesByTransactionId(transactionId);

        if (journalEntries.size() <= 1) { throw new JournalEntriesNotFoundException(transactionId); }
        final Long officeId = journalEntries.get(0).getOffice().getId();
        final String reversalTransactionId = generateTransactionId(officeId);
        final boolean manualEntry = false;
        String reversalComment = null;


        final boolean useDefaultComment = StringUtils.isBlank(reversalComment);

        validateCommentForReversal(reversalComment);

        for (final JournalEntry journalEntry : journalEntries) {
            JournalEntry reversalJournalEntry;
            if (useDefaultComment) {
                reversalComment = "Reversal entry for Journal Entry with Entry Id  :" + journalEntry.getId()
                        + " and transaction Id " + transactionId;
            }
            if (journalEntry.isDebitEntry()) {
                reversalJournalEntry = JournalEntry.createNew(journalEntry.getOffice(), journalEntry.getPaymentDetails(),
                        journalEntry.getGlAccount(), journalEntry.getCurrencyCode(), reversalTransactionId, manualEntry,
                        journalEntry.getTransactionDate(), JournalEntryType.CREDIT, journalEntry.getAmount(), reversalComment, null, null,
                        journalEntry.getReferenceNumber(), journalEntry.getLoanTransaction(), journalEntry.getSavingsTransaction(), 
                        journalEntry.getClientTransaction());
            } else {
                reversalJournalEntry = JournalEntry.createNew(journalEntry.getOffice(), journalEntry.getPaymentDetails(),
                        journalEntry.getGlAccount(), journalEntry.getCurrencyCode(), reversalTransactionId, manualEntry,
                        journalEntry.getTransactionDate(), JournalEntryType.DEBIT, journalEntry.getAmount(), reversalComment, null, null,
                        journalEntry.getReferenceNumber(), journalEntry.getLoanTransaction(), journalEntry.getSavingsTransaction(), 
                        journalEntry.getClientTransaction());
            }
            // save the reversal entry
            this.glJournalEntryRepository.saveAndFlush(reversalJournalEntry);
            journalEntry.setReversed(true);
            journalEntry.setReversalJournalEntry(reversalJournalEntry);
            // save the updated journal entry
            this.glJournalEntryRepository.saveAndFlush(journalEntry);
        }
    }
}
