/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.accounting.journalentry.command;

import org.joda.time.LocalDate;
import org.mifosplatform.accounting.journalentry.api.JournalEntryJsonInputParams;
import org.mifosplatform.infrastructure.core.data.ApiParameterError;
import org.mifosplatform.infrastructure.core.data.DataValidatorBuilder;
import org.mifosplatform.infrastructure.core.exception.PlatformApiDataValidationException;

import java.math.BigDecimal;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

/**
 * Immutable command for adding an accounting closure
 */
public class JournalEntryCommand {

    private final Long officeId;
    private final LocalDate transactionDate;
    private final String currencyCode;
    private final String comments;
    private final String referenceNumber;
    private final Long accountingRuleId;
    private final BigDecimal amount;
    private final Long paymentTypeId;
    @SuppressWarnings("unused")
    private final String accountNumber;
    @SuppressWarnings("unused")
    private final String checkNumber;
    @SuppressWarnings("unused")
    private final String receiptNumber;
    @SuppressWarnings("unused")
    private final String bankNumber;
    @SuppressWarnings("unused")
    private final String routingCode;

    private final SingleDebitOrCreditEntryCommand[] credits;
    private final SingleDebitOrCreditEntryCommand[] debits;

    public JournalEntryCommand(final String currencyCode, final LocalDate transactionDate, final String comments,
            final SingleDebitOrCreditEntryCommand[] credits, final SingleDebitOrCreditEntryCommand[] debits, final String referenceNumber,
            final Long accountingRuleId, final BigDecimal amount, final Long paymentTypeId, final String accountNumber,
            final String checkNumber, final String receiptNumber, final String bankNumber, final String routingCode) {
        this.currencyCode = currencyCode;
        this.transactionDate = transactionDate;
        this.comments = comments;
        this.credits = credits;
        this.debits = debits;
        this.referenceNumber = referenceNumber;
        this.accountingRuleId = accountingRuleId;
        this.amount = amount;
        this.paymentTypeId = paymentTypeId;
        this.accountNumber = accountNumber;
        this.checkNumber = checkNumber;
        this.receiptNumber = receiptNumber;
        this.bankNumber = bankNumber;
        this.routingCode = routingCode;
        if(hasMultipleOffices("credits") || hasMultipleOffices("debits")){
            this.officeId = null;
        }else{
            this.officeId = this.credits[0].getOfficeId();
        }
    }

    public void validateForCreate() {

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();

        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("GLJournalEntry");

        baseDataValidator.reset().parameter("transactionDate").value(this.transactionDate).notBlank();

        baseDataValidator.reset().parameter("officeId").value(this.officeId).ignoreIfNull().integerGreaterThanZero();

        baseDataValidator.reset().parameter(JournalEntryJsonInputParams.CURRENCY_CODE.getValue()).value(this.currencyCode).notBlank();

        baseDataValidator.reset().parameter("comments").value(this.comments).ignoreIfNull().notExceedingLengthOf(500);

        baseDataValidator.reset().parameter("referenceNumber").value(this.referenceNumber).ignoreIfNull().notExceedingLengthOf(100);

        baseDataValidator.reset().parameter("accountingRule").value(this.accountingRuleId).ignoreIfNull().longGreaterThanZero();

        baseDataValidator.reset().parameter("paymentTypeId").value(this.paymentTypeId).ignoreIfNull().longGreaterThanZero();

        // validation for credit array elements
        if (this.credits != null) {
            if (this.credits.length == 0) {
                validateSingleDebitOrCredit(baseDataValidator, "credits", 0, new SingleDebitOrCreditEntryCommand(null, null, null, null, null));
            } else {
                int i = 0;
                for (final SingleDebitOrCreditEntryCommand credit : this.credits) {
                    validateSingleDebitOrCredit(baseDataValidator, "credits", i, credit);
                    i++;
                }
            }
        }

        // validation for debit array elements
        if (this.debits != null) {
            if (this.debits.length == 0) {
                validateSingleDebitOrCredit(baseDataValidator, "debits", 0, new SingleDebitOrCreditEntryCommand(null, null, null, null, null));
            } else {
                int i = 0;
                for (final SingleDebitOrCreditEntryCommand debit : this.debits) {
                    validateSingleDebitOrCredit(baseDataValidator, "debits", i, debit);
                    i++;
                }
            }
        }
        baseDataValidator.reset().parameter("amount").value(this.amount).ignoreIfNull().zeroOrPositiveAmount();

        if (!dataValidationErrors.isEmpty()) { throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist",
                "Validation errors exist.", dataValidationErrors); }
    }

    /**
     * @param baseDataValidator
     * @param arrayPos
     * @param credit
     */
    private void validateSingleDebitOrCredit(final DataValidatorBuilder baseDataValidator, final String paramSuffix, final int arrayPos,
            final SingleDebitOrCreditEntryCommand credit) {
        baseDataValidator.reset().parameter(paramSuffix + "[" + arrayPos + "].glAccountId").value(credit.getGlAccountId()).notNull()
                .integerGreaterThanZero();
        baseDataValidator.reset().parameter(paramSuffix + "[" + arrayPos + "].officeId").value(credit.getOfficeId()).notNull()
                .integerGreaterThanZero();
        baseDataValidator.reset().parameter(paramSuffix + "[" + arrayPos + "].amount").value(credit.getAmount()).notNull()
                .zeroOrPositiveAmount();
    }

    public boolean hasMultipleOffices(final String paramSuffix){
        return getDebitOrCreditEntryCommandOfficeIds(paramSuffix).size() > 1;
    }

    public boolean isInterBranch(){
        final List<Long> debitOffices = getDebitOrCreditEntryCommandOfficeIds(JournalEntryJsonInputParams.DEBITS.getValue());
        final List<Long> creditOffices = getDebitOrCreditEntryCommandOfficeIds(JournalEntryJsonInputParams.CREDITS.getValue());
        if(debitOffices.size()>1 || creditOffices.size()>1){
            return true;
        }
        if(!debitOffices.get(0).equals(creditOffices.get(0))){
            return true;
        }
        return false;
    }

    private List<Long> getDebitOrCreditEntryCommandOfficeIds(final String paramSuffix){
        final List<Long> officeIds = new ArrayList<>();
        final SingleDebitOrCreditEntryCommand[] debitsOrCredits;

        if(paramSuffix.equals("credits")){
            debitsOrCredits = this.credits;
        }else if(paramSuffix.equals("debits")){
            debitsOrCredits = this.debits;
        }else{
            throw new InvalidParameterException(paramSuffix + " is not a valid parameter name. Accepted parameters are 'credits' or 'debits'");
        }

        if(debitsOrCredits != null && debitsOrCredits.length > 0){
            for(SingleDebitOrCreditEntryCommand debitOrCredit : debitsOrCredits){
                final Long officeId = debitOrCredit.getOfficeId();
                if(!officeIds.contains(officeId)){
                    officeIds.add(officeId);
                }
            }
        }

        return officeIds;
    }

    public Long getOfficeId() {
        return this.officeId;
    }

    public LocalDate getTransactionDate() {
        return this.transactionDate;
    }

    public String getComments() {
        return this.comments;
    }

    public SingleDebitOrCreditEntryCommand[] getCredits() {
        return this.credits;
    }

    public SingleDebitOrCreditEntryCommand[] getDebits() {
        return this.debits;
    }

    public String getReferenceNumber() {
        return this.referenceNumber;
    }

    public Long getAccountingRuleId() {
        return this.accountingRuleId;
    }

    public String getCurrencyCode() {return this.currencyCode;}

    public BigDecimal getAmount() {
        return amount;
    }

    public Long getPaymentTypeId() {
        return paymentTypeId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getCheckNumber() {
        return checkNumber;
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public String getBankNumber() {
        return bankNumber;
    }

    public String getRoutingCode() {
        return routingCode;
    }
}