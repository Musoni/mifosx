/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.portfolio.loanaccount.guarantor.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;

import javax.annotation.PostConstruct;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormatter;
import org.mifosplatform.accounting.journalentry.service.JournalEntryWritePlatformService;
import org.mifosplatform.infrastructure.configuration.domain.ConfigurationDomainService;
import org.mifosplatform.infrastructure.core.data.ApiParameterError;
import org.mifosplatform.infrastructure.core.data.DataValidatorBuilder;
import org.mifosplatform.infrastructure.core.exception.PlatformApiDataValidationException;
import org.mifosplatform.infrastructure.core.service.DateUtils;
import org.mifosplatform.organisation.monetary.domain.ApplicationCurrency;
import org.mifosplatform.organisation.monetary.domain.ApplicationCurrencyRepositoryWrapper;
import org.mifosplatform.organisation.monetary.domain.MonetaryCurrency;
import org.mifosplatform.organisation.monetary.domain.MoneyHelper;
import org.mifosplatform.portfolio.account.PortfolioAccountType;
import org.mifosplatform.portfolio.account.data.AccountTransferDTO;
import org.mifosplatform.portfolio.account.domain.AccountTransferDetails;
import org.mifosplatform.portfolio.account.domain.AccountTransferType;
import org.mifosplatform.portfolio.account.service.AccountTransfersReadPlatformService;
import org.mifosplatform.portfolio.account.service.AccountTransfersWritePlatformService;
import org.mifosplatform.portfolio.common.BusinessEventNotificationConstants;
import org.mifosplatform.portfolio.common.BusinessEventNotificationConstants.BUSINESS_ENTITY;
import org.mifosplatform.portfolio.common.BusinessEventNotificationConstants.BUSINESS_EVENTS;
import org.mifosplatform.portfolio.common.service.BusinessEventListner;
import org.mifosplatform.portfolio.common.service.BusinessEventNotifierService;
import org.mifosplatform.portfolio.loanaccount.domain.Loan;
import org.mifosplatform.portfolio.loanaccount.domain.LoanStatus;
import org.mifosplatform.portfolio.loanaccount.domain.LoanTransaction;
import org.mifosplatform.portfolio.loanaccount.guarantor.GuarantorConstants;
import org.mifosplatform.portfolio.loanaccount.guarantor.domain.*;
import org.mifosplatform.portfolio.loanproduct.domain.LoanProduct;
import org.mifosplatform.portfolio.loanproduct.domain.LoanProductGuaranteeDetails;
import org.mifosplatform.portfolio.paymentdetail.domain.PaymentDetail;
import org.mifosplatform.portfolio.paymentdetail.domain.PaymentDetailRepository;
import org.mifosplatform.portfolio.paymenttype.domain.PaymentType;
import org.mifosplatform.portfolio.paymenttype.domain.PaymentTypeRepository;
import org.mifosplatform.portfolio.savings.SavingsApiConstants;
import org.mifosplatform.portfolio.savings.domain.*;
import org.mifosplatform.portfolio.savings.exception.InsufficientAccountBalanceException;
import org.mifosplatform.useradministration.domain.AppUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GuarantorDomainServiceImpl implements GuarantorDomainService {

    private final GuarantorRepository guarantorRepository;
    private final GuarantorFundingRepository guarantorFundingRepository;
    private final GuarantorFundingTransactionRepository guarantorFundingTransactionRepository;
    private final AccountTransfersWritePlatformService accountTransfersWritePlatformService;
    private final BusinessEventNotifierService businessEventNotifierService;
    private final DepositAccountOnHoldTransactionRepository depositAccountOnHoldTransactionRepository;
    private final Map<Long, Long> releaseLoanIds = new HashMap<>(2);
    private final SavingsAccountDomainService savingsAccountDomainService;
    private final SavingsAccountTransactionSummaryWrapper savingsAccountTransactionSummaryWrapper;
    private final SavingsHelper savingsHelper;
    private final AccountTransfersReadPlatformService accountTransfersReadPlatformService;
    private final PaymentDetailRepository paymentDetailRepository;
    private final PaymentTypeRepository paymentTypeRepository;
    private final GuarantorInterestAllocationRepository guarantorInterestAllocationRepository;
    private final GuarantorInterestPaymentRepository guarantorInterestPaymentRepository;
    private final JournalEntryWritePlatformService journalEntryWritePlatformService;
    private final ApplicationCurrencyRepositoryWrapper applicationCurrencyRepositoryWrapper;
    private final ConfigurationDomainService configurationDomainService;
    private final SavingsAccountAssembler savingAccountAssembler;

    @Autowired
    public GuarantorDomainServiceImpl(final GuarantorRepository guarantorRepository,
            final GuarantorFundingRepository guarantorFundingRepository,
            final GuarantorFundingTransactionRepository guarantorFundingTransactionRepository,
            final AccountTransfersWritePlatformService accountTransfersWritePlatformService,
            final BusinessEventNotifierService businessEventNotifierService,
            final DepositAccountOnHoldTransactionRepository depositAccountOnHoldTransactionRepository, 
            final SavingsAccountDomainService savingsAccountDomainService, 
            final SavingsAccountTransactionSummaryWrapper savingsAccountTransactionSummaryWrapper, 
            final AccountTransfersReadPlatformService accountTransfersReadPlatformService,
            final PaymentDetailRepository paymentDetailRepository, 
            final PaymentTypeRepository paymentTypeRepository,final GuarantorInterestAllocationRepository guarantorInterestAllocationRepository,
            final GuarantorInterestPaymentRepository guarantorInterestPaymentRepository,
            final JournalEntryWritePlatformService journalEntryWritePlatformService,
            final ApplicationCurrencyRepositoryWrapper  applicationCurrencyRepositoryWrapper,
            final ConfigurationDomainService configurationDomainService,
            final SavingsAccountAssembler savingAccountAssembler) {
        this.guarantorRepository = guarantorRepository;
        this.guarantorFundingRepository = guarantorFundingRepository;
        this.guarantorFundingTransactionRepository = guarantorFundingTransactionRepository;
        this.accountTransfersWritePlatformService = accountTransfersWritePlatformService;
        this.businessEventNotifierService = businessEventNotifierService;
        this.depositAccountOnHoldTransactionRepository = depositAccountOnHoldTransactionRepository;
        this.savingsAccountDomainService = savingsAccountDomainService;
        this.savingsAccountTransactionSummaryWrapper = savingsAccountTransactionSummaryWrapper;
        this.accountTransfersReadPlatformService = accountTransfersReadPlatformService;
        this.savingsHelper = new SavingsHelper(this.accountTransfersReadPlatformService);
        this.paymentDetailRepository = paymentDetailRepository;
        this.paymentTypeRepository = paymentTypeRepository;
        this.guarantorInterestAllocationRepository = guarantorInterestAllocationRepository;
        this.guarantorInterestPaymentRepository = guarantorInterestPaymentRepository;
        this.journalEntryWritePlatformService = journalEntryWritePlatformService;
        this.applicationCurrencyRepositoryWrapper = applicationCurrencyRepositoryWrapper;
        this.configurationDomainService = configurationDomainService;
        this.savingAccountAssembler = savingAccountAssembler;
    }

    @PostConstruct
    public void addListners() {
        this.businessEventNotifierService.addBusinessEventPostListners(BUSINESS_EVENTS.LOAN_APPROVED, new ValidateOnBusinessEvent());
        this.businessEventNotifierService.addBusinessEventPostListners(BUSINESS_EVENTS.LOAN_APPROVED, new HoldFundsOnBusinessEvent());
        this.businessEventNotifierService.addBusinessEventPostListners(BUSINESS_EVENTS.LOAN_UNDO_APPROVAL, new UndoAllFundTransactions());
        this.businessEventNotifierService.addBusinessEventPostListners(BUSINESS_EVENTS.LOAN_UNDO_DISBURSAL,
                new ReverseAllFundsOnBusinessEvent());
        this.businessEventNotifierService.addBusinessEventPostListners(BUSINESS_EVENTS.LOAN_ADJUST_TRANSACTION,
                new AdjustFundsOnBusinessEvent());
        this.businessEventNotifierService.addBusinessEventPostListners(BUSINESS_EVENTS.LOAN_MAKE_REPAYMENT,
                new ReleaseFundsOnBusinessEvent());
        this.businessEventNotifierService.addBusinessEventPostListners(BUSINESS_EVENTS.LOAN_WRITTEN_OFF, new ReleaseAllFunds());
        this.businessEventNotifierService.addBusinessEventPostListners(BUSINESS_EVENTS.LOAN_UNDO_WRITTEN_OFF,
                new ReverseFundsOnBusinessEvent());
        this.businessEventNotifierService.addBusinessEventPostListners(BUSINESS_EVENTS.LOAN_MAKE_REPAYMENT,
                new SplitInterestIncomeOnBusinessEvent());
        this.businessEventNotifierService.addBusinessEventPostListners(BUSINESS_EVENTS.LOAN_WRITTEN_OFF,new SplitInterestIncomeOnBusinessEvent());
    }

    @Override
    public void validateGuarantorBusinessRules(Loan loan) {
        LoanProduct loanProduct = loan.loanProduct();
        BigDecimal principal = loan.getPrincpal().getAmount();
        if (loanProduct.isHoldGuaranteeFundsEnabled()) {
            LoanProductGuaranteeDetails guaranteeData = loanProduct.getLoanProductGuaranteeDetails();
            final List<Guarantor> existGuarantorList = this.guarantorRepository.findByLoan(loan);
            BigDecimal mandatoryAmount = principal.multiply(guaranteeData.getMandatoryGuarantee()).divide(BigDecimal.valueOf(100));
            BigDecimal minSelfAmount = principal.multiply(guaranteeData.getMinimumGuaranteeFromOwnFunds()).divide(BigDecimal.valueOf(100));
            BigDecimal minExtGuarantee = principal.multiply(guaranteeData.getMinimumGuaranteeFromGuarantor())
                    .divide(BigDecimal.valueOf(100));

            BigDecimal actualAmount = BigDecimal.ZERO;
            BigDecimal actualSelfAmount = BigDecimal.ZERO;
            BigDecimal actualExtGuarantee = BigDecimal.ZERO;
            for (Guarantor guarantor : existGuarantorList) {
                List<GuarantorFundingDetails> fundingDetails = guarantor.getGuarantorFundDetails();
                for (GuarantorFundingDetails guarantorFundingDetails : fundingDetails) {
                    if (guarantorFundingDetails.getStatus().isActive() || guarantorFundingDetails.getStatus().isWithdrawn()
                            || guarantorFundingDetails.getStatus().isCompleted()) {
                        if (guarantor.isSelfGuarantee()) {
                            actualSelfAmount = actualSelfAmount.add(guarantorFundingDetails.getAmount())
                                    .subtract(guarantorFundingDetails.getAmountTransfered());
                        } else {
                            actualExtGuarantee = actualExtGuarantee.add(guarantorFundingDetails.getAmount())
                                    .subtract(guarantorFundingDetails.getAmountTransfered());
                        }
                    }
                }
            }

            final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
            final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("loan.guarantor");
            if (actualSelfAmount.compareTo(minSelfAmount) == -1) {
                baseDataValidator.reset().failWithCodeNoParameterAddedToErrorCode(GuarantorConstants.GUARANTOR_SELF_GUARANTEE_ERROR,
                        minSelfAmount);
            }

            if (actualExtGuarantee.compareTo(minExtGuarantee) == -1) {
                baseDataValidator.reset().failWithCodeNoParameterAddedToErrorCode(GuarantorConstants.GUARANTOR_EXTERNAL_GUARANTEE_ERROR,
                        minExtGuarantee);
            }
            actualAmount = actualAmount.add(actualExtGuarantee).add(actualSelfAmount);
            if (actualAmount.compareTo(mandatoryAmount) == -1) {
                baseDataValidator.reset().failWithCodeNoParameterAddedToErrorCode(GuarantorConstants.GUARANTOR_MANDATORY_GUARANTEE_ERROR,
                        mandatoryAmount);
            }

            if (!dataValidationErrors.isEmpty()) { throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist",
                    "Validation errors exist.", dataValidationErrors); }
        }

    }

    /**
     * Method assigns a guarantor to loan and blocks the funds on guarantor's
     * account
     */
    @Override
    public void assignGuarantor(final GuarantorFundingDetails guarantorFundingDetails, final LocalDate transactionDate) {
        if (guarantorFundingDetails.getStatus().isActive()) {
            SavingsAccount savingsAccount = guarantorFundingDetails.getLinkedSavingsAccount();
            savingsAccount.holdFunds(guarantorFundingDetails.getAmount());
            if (savingsAccount.getWithdrawableBalance().compareTo(BigDecimal.ZERO) == -1) {
                final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
                final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("loan.guarantor");
                baseDataValidator.reset().failWithCodeNoParameterAddedToErrorCode(GuarantorConstants.GUARANTOR_INSUFFICIENT_BALANCE_ERROR,
                        savingsAccount.getId(), savingsAccount.getClient().getDisplayName());

                throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist", "Validation errors exist.",
                        dataValidationErrors);
            }
            DepositAccountOnHoldTransaction onHoldTransaction = DepositAccountOnHoldTransaction.hold(savingsAccount,
                    guarantorFundingDetails.getAmount(), transactionDate);
            GuarantorFundingTransaction guarantorFundingTransaction = new GuarantorFundingTransaction(guarantorFundingDetails, null,
                    onHoldTransaction);
            guarantorFundingDetails.addGuarantorFundingTransactions(guarantorFundingTransaction);
            this.depositAccountOnHoldTransactionRepository.save(onHoldTransaction);
        }
    }

    /**
     * Method releases(withdraw) a guarantor from loan and unblocks the funds on
     * guarantor's account
     */
    @Override
    public void releaseGuarantor(final GuarantorFundingDetails guarantorFundingDetails, final LocalDate transactionDate) {
        BigDecimal amoutForWithdraw = guarantorFundingDetails.getAmountRemaining();
        if (amoutForWithdraw.compareTo(BigDecimal.ZERO) == 1 && (guarantorFundingDetails.getStatus().isActive())) {
            SavingsAccount savingsAccount = guarantorFundingDetails.getLinkedSavingsAccount();
            savingsAccount.releaseFunds(amoutForWithdraw);
            DepositAccountOnHoldTransaction onHoldTransaction = DepositAccountOnHoldTransaction.release(savingsAccount, amoutForWithdraw,
                    transactionDate);
            GuarantorFundingTransaction guarantorFundingTransaction = new GuarantorFundingTransaction(guarantorFundingDetails, null,
                    onHoldTransaction);
            guarantorFundingDetails.addGuarantorFundingTransactions(guarantorFundingTransaction);
            guarantorFundingDetails.releaseFunds(amoutForWithdraw);
            guarantorFundingDetails.withdrawFunds(amoutForWithdraw);
            guarantorFundingDetails.getLoanAccount().updateGuaranteeAmount(amoutForWithdraw.negate());
            this.depositAccountOnHoldTransactionRepository.save(onHoldTransaction);
            this.guarantorFundingRepository.save(guarantorFundingDetails);
        }
    }

    /**
     * Method is to recover funds from guarantor's in case loan is unpaid.
     * (Transfers guarantee amount from guarantor's account to loan account and
     * releases guarantor)
     */
    @Override
    public void transaferFundsFromGuarantor(final Loan loan) {
        if (loan.getGuaranteeAmount().compareTo(BigDecimal.ZERO) != 1) { return; }
        final List<Guarantor> existGuarantorList = this.guarantorRepository.findByLoan(loan);
        final boolean isRegularTransaction = true;
        final boolean isExceptionForBalanceCheck = true;
        LocalDate transactionDate = LocalDate.now();
        PortfolioAccountType fromAccountType = PortfolioAccountType.SAVINGS;
        PortfolioAccountType toAccountType = PortfolioAccountType.LOAN;
        final Long toAccountId = loan.getId();
        final String description = "Payment from guarantor savings";
        final Locale locale = null;
        final DateTimeFormatter fmt = null;
        final PaymentDetail paymentDetail = null;
        final Integer fromTransferType = null;
        final Integer toTransferType = null;
        final Long chargeId = null;
        final Integer loanInstallmentNumber = null;
        final Integer transferType = AccountTransferType.LOAN_REPAYMENT.getValue();
        final AccountTransferDetails accountTransferDetails = null;
        final String noteText = null;

        final String txnExternalId = null;
        final SavingsAccount toSavingsAccount = null;

        Long loanId = loan.getId();

        for (Guarantor guarantor : existGuarantorList) {
            final List<GuarantorFundingDetails> fundingDetails = guarantor.getGuarantorFundDetails();
            for (GuarantorFundingDetails guarantorFundingDetails : fundingDetails) {
                if (guarantorFundingDetails.getStatus().isActive()) {
                    final SavingsAccount fromSavingsAccount = guarantorFundingDetails.getLinkedSavingsAccount();
                    final Long fromAccountId = fromSavingsAccount.getId();
                    releaseLoanIds.put(loanId, guarantorFundingDetails.getId());
                    try {
                        BigDecimal remainingAmount = guarantorFundingDetails.getAmountRemaining();
                        if (loan.getGuaranteeAmount().compareTo(loan.getPrincpal().getAmount()) == 1) {
                            remainingAmount = remainingAmount.multiply(loan.getPrincpal().getAmount()).divide(loan.getGuaranteeAmount(),
                                    MoneyHelper.getRoundingMode());
                        }
                        AccountTransferDTO accountTransferDTO = new AccountTransferDTO(transactionDate, remainingAmount, fromAccountType,
                                toAccountType, fromAccountId, toAccountId, description, locale, fmt, paymentDetail, fromTransferType,
                                toTransferType, chargeId, loanInstallmentNumber, transferType, accountTransferDetails, noteText,
                                txnExternalId, loan, toSavingsAccount, fromSavingsAccount, isRegularTransaction,
                                isExceptionForBalanceCheck);
                        transferAmount(accountTransferDTO);
                    } finally {
                        releaseLoanIds.remove(loanId);
                    }
                }
            }
        }

    }

    /**
     * @param accountTransferDTO
     */
    private void transferAmount(final AccountTransferDTO accountTransferDTO) {
        try {
            this.accountTransfersWritePlatformService.transferFunds(accountTransferDTO);
        } catch (final InsufficientAccountBalanceException e) {
            final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
            final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("loan.guarantor");
            baseDataValidator.reset().failWithCodeNoParameterAddedToErrorCode(GuarantorConstants.GUARANTOR_INSUFFICIENT_BALANCE_ERROR,
                    accountTransferDTO.getFromAccountId(), accountTransferDTO.getToAccountId(), accountTransferDTO.getFromSavingsAccount().getClient().getDisplayName());
            throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist", "Validation errors exist.",
                    dataValidationErrors);

        }
    }

    /**
     * Method reverses all blocked fund(both hold and release) transactions.
     * example: reverses all transactions on undo approval of loan account.
     * 
     */
    private void reverseAllFundTransaction(final Loan loan) {

        if (loan.getGuaranteeAmount().compareTo(BigDecimal.ZERO) == 1) {
            final List<Guarantor> existGuarantorList = this.guarantorRepository.findByLoan(loan);
            List<GuarantorFundingDetails> guarantorFundingDetailList = new ArrayList<>();
            for (Guarantor guarantor : existGuarantorList) {
                final List<GuarantorFundingDetails> fundingDetails = guarantor.getGuarantorFundDetails();
                for (GuarantorFundingDetails guarantorFundingDetails : fundingDetails) {
                    guarantorFundingDetails.undoAllTransactions();
                    guarantorFundingDetailList.add(guarantorFundingDetails);
                }
            }

            if (!guarantorFundingDetailList.isEmpty()) {
                loan.setGuaranteeAmount(null);
                this.guarantorFundingRepository.save(guarantorFundingDetailList);
            }
        }
    }

    /**
     * Method holds all guarantor's guarantee amount for a loan account.
     * example: hold funds on approval of loan account.
     * 
     */
    private void holdGuarantorFunds(final Loan loan) {
        if (loan.loanProduct().isHoldGuaranteeFundsEnabled()) {
            final List<Guarantor> existGuarantorList = this.guarantorRepository.findByLoan(loan);
            List<GuarantorFundingDetails> guarantorFundingDetailList = new ArrayList<>();
            List<DepositAccountOnHoldTransaction> onHoldTransactions = new ArrayList<>();
            BigDecimal totalGuarantee = BigDecimal.ZERO;
            List<Long> insufficientBalanceIds = new ArrayList<>();
            List<String> insufficientBalanceClientNames = new ArrayList<>();

            for (Guarantor guarantor : existGuarantorList) {
                final List<GuarantorFundingDetails> fundingDetails = guarantor.getGuarantorFundDetails();
                for (GuarantorFundingDetails guarantorFundingDetails : fundingDetails) {
                    if (guarantorFundingDetails.getStatus().isActive()) {
                        SavingsAccount savingsAccount = guarantorFundingDetails.getLinkedSavingsAccount();
                        savingsAccount.holdFunds(guarantorFundingDetails.getAmount());
                        totalGuarantee = totalGuarantee.add(guarantorFundingDetails.getAmount());
                        DepositAccountOnHoldTransaction onHoldTransaction = DepositAccountOnHoldTransaction.hold(savingsAccount,
                                guarantorFundingDetails.getAmount(), loan.getApprovedOnDate());
                        onHoldTransactions.add(onHoldTransaction);
                        GuarantorFundingTransaction guarantorFundingTransaction = new GuarantorFundingTransaction(guarantorFundingDetails,
                                null, onHoldTransaction);
                        guarantorFundingDetails.addGuarantorFundingTransactions(guarantorFundingTransaction);
                        guarantorFundingDetailList.add(guarantorFundingDetails);
                        if (savingsAccount.getWithdrawableBalance().compareTo(BigDecimal.ZERO) == -1) {
                            insufficientBalanceIds.add(savingsAccount.getId());
                            insufficientBalanceClientNames.add(savingsAccount.getClient().getDisplayName());
                        }
                    }
                }
            }
            if (!insufficientBalanceIds.isEmpty()) {
                final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
                final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("loan.guarantor");
                baseDataValidator.reset().failWithCodeNoParameterAddedToErrorCode(GuarantorConstants.GUARANTOR_INSUFFICIENT_BALANCE_ERROR,
                        insufficientBalanceIds, insufficientBalanceClientNames);
                throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist", "Validation errors exist.",
                        dataValidationErrors);

            }
            loan.setGuaranteeAmount(totalGuarantee);
            if (!guarantorFundingDetailList.isEmpty()) {
                this.depositAccountOnHoldTransactionRepository.save(onHoldTransactions);
                this.guarantorFundingRepository.save(guarantorFundingDetailList);
            }
        }
    }

    /**
     * Method releases all guarantor's guarantee amount(first external guarantee
     * and then self guarantee) for a loan account in the portion of guarantee
     * percentage on a paid principal. example: releases funds on repayments of
     * loan account.
     * 
     */
    private void releaseGuarantorFunds(final LoanTransaction loanTransaction) {
        final Loan loan = loanTransaction.getLoan();
        if (loan.getGuaranteeAmount().compareTo(BigDecimal.ZERO) == 1) {
            final List<Guarantor> existGuarantorList = this.guarantorRepository.findByLoan(loan);
            List<GuarantorFundingDetails> externalGuarantorList = new ArrayList<>();
            List<GuarantorFundingDetails> selfGuarantorList = new ArrayList<>();
            BigDecimal selfGuarantee = BigDecimal.ZERO;
            BigDecimal guarantorGuarantee = BigDecimal.ZERO;
            for (Guarantor guarantor : existGuarantorList) {
                final List<GuarantorFundingDetails> fundingDetails = guarantor.getGuarantorFundDetails();
                for (GuarantorFundingDetails guarantorFundingDetails : fundingDetails) {
                    if (guarantorFundingDetails.getStatus().isActive()) {
                        if (guarantor.isSelfGuarantee()) {
                            selfGuarantorList.add(guarantorFundingDetails);
                            selfGuarantee = selfGuarantee.add(guarantorFundingDetails.getAmountRemaining());
                        } else if (guarantor.isExistingCustomer()) {
                            externalGuarantorList.add(guarantorFundingDetails);
                            guarantorGuarantee = guarantorGuarantee.add(guarantorFundingDetails.getAmountRemaining());
                        }
                    }
                }
            }

            BigDecimal amountForRelease = loanTransaction.getPrincipalPortion();
            BigDecimal totalGuaranteeAmount = loan.getGuaranteeAmount();
            BigDecimal principal = loan.getPrincpal().getAmount();
            if ((amountForRelease != null) && (totalGuaranteeAmount != null)) {
                amountForRelease = amountForRelease.multiply(totalGuaranteeAmount).divide(principal, MoneyHelper.getRoundingMode());
                List<DepositAccountOnHoldTransaction> accountOnHoldTransactions = new ArrayList<>();

                BigDecimal amountLeft = calculateAndRelaseGuarantorFunds(externalGuarantorList, guarantorGuarantee, amountForRelease,
                        loanTransaction, accountOnHoldTransactions);

                if (amountLeft.compareTo(BigDecimal.ZERO) == 1) {
                    calculateAndRelaseGuarantorFunds(selfGuarantorList, selfGuarantee, amountLeft, loanTransaction,
                            accountOnHoldTransactions);
                    externalGuarantorList.addAll(selfGuarantorList);
                }

                if (!externalGuarantorList.isEmpty()) {
                    this.depositAccountOnHoldTransactionRepository.save(accountOnHoldTransactions);
                    this.guarantorFundingRepository.save(externalGuarantorList);
                }
            }
        }

    }

    /**
     * Method releases all guarantor's guarantee amount. example: releases funds
     * on write-off of a loan account.
     * 
     */
    private void releaseAllGuarantors(final LoanTransaction loanTransaction) {
        Loan loan = loanTransaction.getLoan();
        if (loan.getGuaranteeAmount().compareTo(BigDecimal.ZERO) == 1) {
            final List<Guarantor> existGuarantorList = this.guarantorRepository.findByLoan(loan);
            List<GuarantorFundingDetails> saveGuarantorFundingDetails = new ArrayList<>();
            List<DepositAccountOnHoldTransaction> onHoldTransactions = new ArrayList<>();
            for (Guarantor guarantor : existGuarantorList) {
                final List<GuarantorFundingDetails> fundingDetails = guarantor.getGuarantorFundDetails();
                for (GuarantorFundingDetails guarantorFundingDetails : fundingDetails) {
                    BigDecimal amoutForRelease = guarantorFundingDetails.getAmountRemaining();
                    if (amoutForRelease.compareTo(BigDecimal.ZERO) == 1 && (guarantorFundingDetails.getStatus().isActive())) {
                        SavingsAccount savingsAccount = guarantorFundingDetails.getLinkedSavingsAccount();
                        savingsAccount.releaseFunds(amoutForRelease);
                        DepositAccountOnHoldTransaction onHoldTransaction = DepositAccountOnHoldTransaction.release(savingsAccount,
                                amoutForRelease, loanTransaction.getTransactionDate());
                        onHoldTransactions.add(onHoldTransaction);
                        GuarantorFundingTransaction guarantorFundingTransaction = new GuarantorFundingTransaction(guarantorFundingDetails,
                                loanTransaction, onHoldTransaction);
                        guarantorFundingDetails.addGuarantorFundingTransactions(guarantorFundingTransaction);
                        guarantorFundingDetails.releaseFunds(amoutForRelease);
                        saveGuarantorFundingDetails.add(guarantorFundingDetails);

                    }
                }

            }

            if (!saveGuarantorFundingDetails.isEmpty()) {
                this.depositAccountOnHoldTransactionRepository.save(onHoldTransactions);
                this.guarantorFundingRepository.save(saveGuarantorFundingDetails);
            }
        }
    }

    /**
     * Method releases guarantor's guarantee amount on transferring guarantee
     * amount to loan account. example: on recovery of guarantee funds from
     * guarantor's.
     */
    private void completeGuarantorFund(final LoanTransaction loanTransaction) {
        Loan loan = loanTransaction.getLoan();
        GuarantorFundingDetails guarantorFundingDetails = this.guarantorFundingRepository.findOne(releaseLoanIds.get(loan.getId()));
        if (guarantorFundingDetails != null) {
            BigDecimal amountForRelease = loanTransaction.getAmount(loan.getCurrency()).getAmount();
            BigDecimal guarantorGuarantee = amountForRelease;
            List<GuarantorFundingDetails> guarantorList = Arrays.asList(guarantorFundingDetails);
            final List<DepositAccountOnHoldTransaction> accountOnHoldTransactions = new ArrayList<>();
            calculateAndRelaseGuarantorFunds(guarantorList, guarantorGuarantee, amountForRelease, loanTransaction,
                    accountOnHoldTransactions);
            this.depositAccountOnHoldTransactionRepository.save(accountOnHoldTransactions);
            this.guarantorFundingRepository.save(guarantorFundingDetails);
        }
    }

    private BigDecimal calculateAndRelaseGuarantorFunds(List<GuarantorFundingDetails> guarantorList, BigDecimal totalGuaranteeAmount,
            BigDecimal amountForRelease, LoanTransaction loanTransaction,
            final List<DepositAccountOnHoldTransaction> accountOnHoldTransactions) {
        BigDecimal amountLeft = amountForRelease;
        for (GuarantorFundingDetails fundingDetails : guarantorList) {
            BigDecimal guarantorAmount = amountForRelease.multiply(fundingDetails.getAmountRemaining()).divide(totalGuaranteeAmount,
                    MoneyHelper.getRoundingMode());
            if (fundingDetails.getAmountRemaining().compareTo(guarantorAmount) < 1) {
                guarantorAmount = fundingDetails.getAmountRemaining();
            }
            fundingDetails.releaseFunds(guarantorAmount);
            SavingsAccount savingsAccount = fundingDetails.getLinkedSavingsAccount();
            savingsAccount.releaseFunds(guarantorAmount);
            DepositAccountOnHoldTransaction onHoldTransaction = DepositAccountOnHoldTransaction.release(savingsAccount, guarantorAmount,
                    loanTransaction.getTransactionDate());
            accountOnHoldTransactions.add(onHoldTransaction);
            GuarantorFundingTransaction guarantorFundingTransaction = new GuarantorFundingTransaction(fundingDetails, loanTransaction,
                    onHoldTransaction);
            fundingDetails.addGuarantorFundingTransactions(guarantorFundingTransaction);
            amountLeft = amountLeft.subtract(guarantorAmount);
        }
        return amountLeft;
    }

    /**
     * Method reverses the fund release transactions in case of loan transaction
     * reversed
     */
    private void reverseTransaction(final List<Long> loanTransactionIds) {

        List<GuarantorFundingTransaction> fundingTransactions = this.guarantorFundingTransactionRepository
                .fetchGuarantorFundingTransactions(loanTransactionIds);
        for (GuarantorFundingTransaction fundingTransaction : fundingTransactions) {
            fundingTransaction.reverseTransaction();
        }
        if (!fundingTransactions.isEmpty()) {
            this.guarantorFundingTransactionRepository.save(fundingTransactions);
        }
    }
    
    /** 
     * Split fully paid loan interest among the guarantors of the loan
     * 
     * @param loanTransaction -- the make repayment loan transaction object
     * @return None
     **/
    private void splitIncomeInterestAmongGuarantors(final LoanTransaction loanTransaction) {
        final Loan loan = (loanTransaction != null) ? loanTransaction.getLoan() : null;
        final LoanProduct loanProduct = (loan != null) ? loan.getLoanProduct() : null;
        final LoanProductGuaranteeDetails loanProductGuaranteeDetails = (loanProduct != null) ? loanProduct.getLoanProductGuaranteeDetails() : null;
        final LoanStatus loanStatus = (loan != null) ? LoanStatus.fromInt(loan.getStatus()) : null;
        
        // only proceed if the splitInterestAmongGuarantors property is set to 1 and the loan status is 600
        if ((loanProductGuaranteeDetails != null) && (loanStatus != null) && loanProductGuaranteeDetails.splitInterestAmongGuarantors() && loanStatus.isClosedObligationsMet()) {
            final List<Guarantor> guarantors = this.guarantorRepository.findByLoan(loan);
            
            for (Guarantor guarantor : guarantors) {
                if (guarantor.isExistingCustomer()) {
                    final List<GuarantorFundingDetails> guarantorFundingDetails = guarantor.getGuarantorFundDetails();
                    
                    for (GuarantorFundingDetails guarantorFundingDetail : guarantorFundingDetails) {
                        final SavingsAccount savingsAccount = guarantorFundingDetail.getLinkedSavingsAccount();
                        final BigDecimal shareOfLoanInterestIncome = guarantorFundingDetail.calculateShareOfLoanInterestIncome(loan);
                        
                        if (shareOfLoanInterestIncome.compareTo(BigDecimal.ZERO) == 1 && (savingsAccount != null)) {
                            final LocalDate transactionDate = LocalDate.now();
                            final DateTimeFormatter fmt = null;
                           // final PaymentDetail paymentDetail = null;
                            final boolean isAccountTransfer = false;
                            final boolean isRegularTransaction = true;
                            
                            // inject helper classes to savings account class
                            savingsAccount.setHelpers(this.savingsAccountTransactionSummaryWrapper, this.savingsHelper);

                            /*hard coded for temporal fix to add reference. code value inserted in backend db */
                            final String reference = "Interest from Guaranteed Loan";
                            final PaymentType paymentType = this.paymentTypeRepository.findByName("Interest from Guaranteed Loan");
                            final PaymentDetail paymentDetail =  PaymentDetail.generatePaymentDetailWithReference(paymentType, reference);
                            PaymentDetail newPaymentTypeToSave = paymentDetailRepository.save(paymentDetail);
                            final boolean isGuarantorInterestDeposit = true;
                            
                            // call method to handle savings account deposit
                            this.savingsAccountDomainService.handleDeposit(savingsAccount, fmt, transactionDate,
                                    shareOfLoanInterestIncome, newPaymentTypeToSave, isAccountTransfer, isRegularTransaction,isGuarantorInterestDeposit);
                        }
                    }
                }
            }
        }
    }

    private class ValidateOnBusinessEvent implements BusinessEventListner {

        @Override
        public void businessEventToBeExecuted(@SuppressWarnings("unused") Map<BUSINESS_ENTITY, Object> businessEventEntity) {}

        @Override
        public void businessEventWasExecuted(Map<BUSINESS_ENTITY, Object> businessEventEntity) {
            Object entity = businessEventEntity.get(BUSINESS_ENTITY.LOAN);
            if (entity instanceof Loan) {
                Loan loan = (Loan) entity;
                validateGuarantorBusinessRules(loan);
            }
        }
    }

    private class HoldFundsOnBusinessEvent implements BusinessEventListner {

        @Override
        public void businessEventToBeExecuted(@SuppressWarnings("unused") Map<BUSINESS_ENTITY, Object> businessEventEntity) {}

        @Override
        public void businessEventWasExecuted(Map<BUSINESS_ENTITY, Object> businessEventEntity) {
            Object entity = businessEventEntity.get(BUSINESS_ENTITY.LOAN);
            if (entity instanceof Loan) {
                Loan loan = (Loan) entity;
                holdGuarantorFunds(loan);
            }
        }
    }

    private class ReleaseFundsOnBusinessEvent implements BusinessEventListner {

        @Override
        public void businessEventToBeExecuted(@SuppressWarnings("unused") Map<BUSINESS_ENTITY, Object> businessEventEntity) {}

        @Override
        public void businessEventWasExecuted(Map<BUSINESS_ENTITY, Object> businessEventEntity) {
            Object entity = businessEventEntity.get(BUSINESS_ENTITY.LOAN_TRANSACTION);
            if (entity instanceof LoanTransaction) {
                LoanTransaction loanTransaction = (LoanTransaction) entity;
                if (releaseLoanIds.containsKey(loanTransaction.getLoan().getId())) {
                    completeGuarantorFund(loanTransaction);
                } else {
                    releaseGuarantorFunds(loanTransaction);
                }
            }
        }
    }

    private class ReverseFundsOnBusinessEvent implements BusinessEventListner {

        @Override
        public void businessEventToBeExecuted(@SuppressWarnings("unused") Map<BUSINESS_ENTITY, Object> businessEventEntity) {}

        @Override
        public void businessEventWasExecuted(Map<BUSINESS_ENTITY, Object> businessEventEntity) {
            Object entity = businessEventEntity.get(BUSINESS_ENTITY.LOAN_TRANSACTION);
            if (entity instanceof LoanTransaction) {
                LoanTransaction loanTransaction = (LoanTransaction) entity;
                List<Long> reersedTransactions = new ArrayList<>(1);
                reersedTransactions.add(loanTransaction.getId());
                reverseTransaction(reersedTransactions);
            }
        }
    }

    private class AdjustFundsOnBusinessEvent implements BusinessEventListner {

        @Override
        public void businessEventToBeExecuted(@SuppressWarnings("unused") Map<BUSINESS_ENTITY, Object> businessEventEntity) {}

        @Override
        public void businessEventWasExecuted(Map<BUSINESS_ENTITY, Object> businessEventEntity) {
            Object entity = businessEventEntity.get(BUSINESS_ENTITY.LOAN_ADJUSTED_TRANSACTION);
            if (entity instanceof LoanTransaction) {
                LoanTransaction loanTransaction = (LoanTransaction) entity;
                List<Long> reersedTransactions = new ArrayList<>(1);
                reersedTransactions.add(loanTransaction.getId());
                reverseTransaction(reersedTransactions);
            }
            Object transactionentity = businessEventEntity.get(BUSINESS_ENTITY.LOAN_TRANSACTION);
            if (transactionentity != null && transactionentity instanceof LoanTransaction) {
                LoanTransaction loanTransaction = (LoanTransaction) transactionentity;
                releaseGuarantorFunds(loanTransaction);
            }
        }
    }

    private class ReverseAllFundsOnBusinessEvent implements BusinessEventListner {

        @Override
        public void businessEventToBeExecuted(@SuppressWarnings("unused") Map<BUSINESS_ENTITY, Object> businessEventEntity) {}

        @Override
        public void businessEventWasExecuted(Map<BUSINESS_ENTITY, Object> businessEventEntity) {
            Object entity = businessEventEntity.get(BUSINESS_ENTITY.LOAN);
            if (entity instanceof Loan) {
                Loan loan = (Loan) entity;
                List<Long> reersedTransactions = new ArrayList<>(1);
                reersedTransactions.addAll(loan.findExistingTransactionIds());
                reverseTransaction(reersedTransactions);
            }
        }
    }

    private class UndoAllFundTransactions implements BusinessEventListner {

        @Override
        public void businessEventToBeExecuted(@SuppressWarnings("unused") Map<BUSINESS_ENTITY, Object> businessEventEntity) {}

        @Override
        public void businessEventWasExecuted(Map<BUSINESS_ENTITY, Object> businessEventEntity) {
            Object entity = businessEventEntity.get(BUSINESS_ENTITY.LOAN);
            if (entity instanceof Loan) {
                Loan loan = (Loan) entity;
                reverseAllFundTransaction(loan);
            }
        }
    }

    private class ReleaseAllFunds implements BusinessEventListner {

        @Override
        public void businessEventToBeExecuted(@SuppressWarnings("unused") Map<BUSINESS_ENTITY, Object> businessEventEntity) {}

        @Override
        public void businessEventWasExecuted(Map<BUSINESS_ENTITY, Object> businessEventEntity) {
            Object entity = businessEventEntity.get(BUSINESS_ENTITY.LOAN_TRANSACTION);
            if (entity instanceof LoanTransaction) {
                LoanTransaction loanTransaction = (LoanTransaction) entity;
                releaseAllGuarantors(loanTransaction);
            }
        }
    }

    /** 
     * Listens for "BUSINESS_EVENTS.LOAN_MAKE_REPAYMENT" events and calls private method that will split interest income amongs
     * guarantors if the latest repayment pays off the loan
     **/
    private class SplitInterestIncomeOnBusinessEvent implements BusinessEventListner {

        @Override
        public void businessEventToBeExecuted(Map<BUSINESS_ENTITY, Object> businessEventEntity) {
            Object entity = businessEventEntity.get(BUSINESS_ENTITY.LOAN_TRANSACTION);


        }

        @Override
        public void businessEventWasExecuted(Map<BUSINESS_ENTITY, Object> businessEventEntity) {
            Object entity = businessEventEntity.get(BUSINESS_ENTITY.LOAN_TRANSACTION);

            if (entity instanceof LoanTransaction) {
                LoanTransaction loanTransaction = (LoanTransaction) entity;
                Loan loan = (loanTransaction != null) ? loanTransaction.getLoan() : null;
                LoanProduct loanProduct = (loan != null) ? loan.getLoanProduct() : null;
                LoanProductGuaranteeDetails loanProductGuaranteeDetails = (loanProduct != null) ? 
                        loanProduct.getLoanProductGuaranteeDetails() : null;
                
                if (loanProductGuaranteeDetails != null && loanProductGuaranteeDetails.splitInterestAmongGuarantors()) {
                    if(loan.status().isClosedObligationsMet()){
                        splitPartialInterestAmongGuarantors(loan,null);
                    }else if(loan.status().isClosedWrittenOff()){
                        splitPartialInterestAmongGuarantors(loan,null);
                    }
                }
            }

        }
    }


    @Override
    public void splitPartialInterestAmongGuarantors(final Loan loan,final AppUser user) {
        BigDecimal interestPaid = BigDecimal.ZERO;
        final List<LoanTransaction> loanTransactions = loan.getLoanTransactions();

        if(loanTransactions.size() > 0 && !loanTransactions.isEmpty()){
            for (final LoanTransaction loanTransaction : loanTransactions) {
                if (!loanTransaction.isReversed() && (loanTransaction.isRecoveryRepayment() || loanTransaction.isRepayment())) {
                    interestPaid = interestPaid.add(loanTransaction.getInterestPortion(loan.getCurrency()).getAmount());
                }
            }
        }


        /** check if the accumulated guarantor table for the last record compare it with the new interest allocated
            if its more subtract it and find the difference . If its less that means reversal has happen
            reverse those associated with accumulated bookings
         **/

        final Collection<GuarantorInterestAllocation> guarantorInterestAccumulateds = this.guarantorInterestAllocationRepository.findByLoan(loan);
        BigDecimal guarantorInterestAllocated = BigDecimal.ZERO;

        if(!guarantorInterestAccumulateds.isEmpty()  && guarantorInterestAccumulateds.size() > 0){
            for(final GuarantorInterestAllocation guarantorInterestAccumulated : guarantorInterestAccumulateds){
                if(!guarantorInterestAccumulated.isReversed()){
                    guarantorInterestAllocated = guarantorInterestAllocated.add(guarantorInterestAccumulated.getAllocatedInterestPaid());
                }
            }
        }

        if(guarantorInterestAllocated.compareTo(BigDecimal.ZERO) == 1){
            interestPaid = interestPaid.subtract(guarantorInterestAllocated);
        }


        if (interestPaid.compareTo(BigDecimal.ZERO) == 1) {

            final List<Guarantor> guarantors = this.guarantorRepository.findByLoan(loan);
            final LoanProduct loanProduct = (loan != null) ? loan.getLoanProduct() : null;
            final LoanProductGuaranteeDetails loanProductGuaranteeDetails = (loanProduct != null) ? loanProduct.getLoanProductGuaranteeDetails() : null;

            final LoanStatus loanStatus = (loan != null) ? LoanStatus.fromInt(loan.getStatus()) : null;


            if ((loanProductGuaranteeDetails != null) && (loanStatus != null && !loanStatus.isOverpaid()) && loanProductGuaranteeDetails.splitInterestAmongGuarantors()) {

                final GuarantorInterestAllocation guarantorInterestAccumulated = GuarantorInterestAllocation.createNew(interestPaid,loan,user);

                final List<GuarantorInterestPayment> guarantorInterestPaymentList = new ArrayList<>();

                for (Guarantor guarantor : guarantors) {
                    if (guarantor.isExistingCustomer()) {
                        final List<GuarantorFundingDetails> guarantorFundingDetails = guarantor.getGuarantorFundDetails();

                        for (GuarantorFundingDetails guarantorFundingDetail : guarantorFundingDetails) {
                            final SavingsAccount savingsAccount = guarantorFundingDetail.getLinkedSavingsAccount();
                            final BigDecimal shareOfLoanInterestIncome = guarantorFundingDetail.calculateShareOfPartialLoanInterestIncome(loan, interestPaid);

                            if (shareOfLoanInterestIncome.compareTo(BigDecimal.ZERO) == 1 && (savingsAccount != null)) {
                                final LocalDate transactionDate = LocalDate.now();
                                final DateTimeFormatter fmt = null;
                                // final PaymentDetail paymentDetail = null;
                                final boolean isRegularTransaction = true;

                                final boolean isAccountTransfer = false;
                                final boolean isGuarantorInterestDeposit = true;

                                // inject helper classes to savings account class
                                savingsAccount.setHelpers(this.savingsAccountTransactionSummaryWrapper, this.savingsHelper);

                            /*hard coded for temporal fix to add reference. code value inserted in backend db */
                                final String reference = "Interest from Guaranteed Loan";
                                final PaymentType paymentType = this.paymentTypeRepository.findByName("Interest from Guaranteed Loan");
                                final PaymentDetail paymentDetail = PaymentDetail.generatePaymentDetailWithReference(paymentType, reference);
                                PaymentDetail newPaymentTypeToSave = paymentDetailRepository.save(paymentDetail);

                                // call method to handle savings account deposit
                                final SavingsAccountTransaction savingsAccountTransaction =this.savingsAccountDomainService.handleDeposit(savingsAccount, fmt, transactionDate,
                                        shareOfLoanInterestIncome, newPaymentTypeToSave, isAccountTransfer, isRegularTransaction,isGuarantorInterestDeposit);


                                final GuarantorInterestPayment guarantorInterestPayment = GuarantorInterestPayment.createNew(guarantor,guarantorInterestAccumulated,shareOfLoanInterestIncome,savingsAccount,savingsAccountTransaction);
                                guarantorInterestPaymentList.add(guarantorInterestPayment);
                            }
                        }
                    }

                }

                if(!guarantorInterestPaymentList.isEmpty() && guarantorInterestPaymentList.size() > 0){
                    guarantorInterestAccumulated.addGuarantorInterestPayment(guarantorInterestPaymentList);
                    this.guarantorInterestAllocationRepository.save(guarantorInterestAccumulated);
                }

            }

        }else if(interestPaid.compareTo(BigDecimal.ZERO) == -1){
            final Collection<GuarantorInterestAllocation> guarantorInterestAllocations = this.guarantorInterestAllocationRepository.findByLoanAndReversedFalseOrderByIdDesc(loan);


            /**  validate if the all savings transaction **/

            /**  Get all payments made */
            BigDecimal interestPaidToReversed = interestPaid.abs();
            for(final GuarantorInterestAllocation allocation : guarantorInterestAllocations){

                while(interestPaidToReversed.compareTo(BigDecimal.ZERO) == 1 ){
                    final Collection<GuarantorInterestPayment> interestPayments = this.guarantorInterestPaymentRepository.findByInterestAllocationOrderByIdDesc(allocation);
                    for(final GuarantorInterestPayment interestPayment : interestPayments){

                        final SavingsAccount savingsAccount = this.savingAccountAssembler.assembleFrom(interestPayment.getSavingsAccount().getId());


                        final SavingsAccountTransaction savingsAccountTransaction = interestPayment.getSavingsAccountTransaction();
                        final Set<Long> existingTransactionIds = new HashSet<Long>(savingsAccount.findExistingTransactionIds());
                        final Set<Long> existingReversedTransactionIds = new HashSet<Long>(savingsAccount.findExistingReversedTransactionIds());
                        if(savingsAccountTransaction.isGuarantorInterestDeposit() && !savingsAccountTransaction.isReversed()){
                            savingsAccountTransaction.reverse();

                            final LocalDate today = DateUtils.getLocalDateOfTenant();
                            final MathContext mc = new MathContext(15, MoneyHelper.getRoundingMode());
                            final boolean isSavingsInterestPostingAtCurrentPeriodEnd = this.configurationDomainService
                                    .isSavingsInterestPostingAtCurrentPeriodEnd();
                            final Integer financialYearBeginningMonth = this.configurationDomainService.retrieveFinancialYearBeginningMonth();

                            boolean isInterestTransfer = false;
                            LocalDate postInterestOnDate = null;
                            if (savingsAccountTransaction.isPostInterestCalculationRequired()
                                    && savingsAccount.isBeforeLastPostingPeriod(savingsAccountTransaction.transactionLocalDate())) {
                                savingsAccount.postInterest(mc, today, isInterestTransfer, isSavingsInterestPostingAtCurrentPeriodEnd, financialYearBeginningMonth, postInterestOnDate);
                            } else {
                                savingsAccount.calculateInterestUsing(mc, today, isInterestTransfer, isSavingsInterestPostingAtCurrentPeriodEnd,
                                        financialYearBeginningMonth,postInterestOnDate);
                            }
                            List<DepositAccountOnHoldTransaction> depositAccountOnHoldTransactions = null;
                            if(savingsAccount.getOnHoldFunds().compareTo(BigDecimal.ZERO) == 1){
                                depositAccountOnHoldTransactions = this.depositAccountOnHoldTransactionRepository.findBySavingsAccountAndReversedFalseOrderByCreatedDateAsc(savingsAccount);
                            }
                            savingsAccount.validateAccountBalanceDoesNotBecomeNegative(SavingsApiConstants.undoTransactionAction,depositAccountOnHoldTransactions);


                            final MonetaryCurrency currency = interestPayment.getSavingsAccount().getCurrency();
                            final ApplicationCurrency applicationCurrency = this.applicationCurrencyRepositoryWrapper.findOneWithNotFoundDetection(currency);
                            boolean isAccountTransfer = false;
                            final Map<String, Object> accountingBridgeData = savingsAccount.deriveAccountingBridgeData(applicationCurrency.toData(),
                                    existingTransactionIds, existingReversedTransactionIds, isAccountTransfer);
                            this.journalEntryWritePlatformService.createJournalEntriesForSavings(accountingBridgeData);

                            allocation.reverse(); interestPayment.reverse();
                            this.guarantorInterestAllocationRepository.save(allocation);

                            interestPaidToReversed = interestPaidToReversed.subtract(allocation.getAllocatedInterestPaid());
                        }
                    }

                }


            }

        }else{
            // do nothing if the interest paid and the interest accumulated are the same
        }


    }


}
