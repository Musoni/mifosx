/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.portfolio.loanaccount.guarantor.service;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormatter;
import org.mifosplatform.infrastructure.core.data.ApiParameterError;
import org.mifosplatform.infrastructure.core.data.DataValidatorBuilder;
import org.mifosplatform.infrastructure.core.exception.PlatformApiDataValidationException;
import org.mifosplatform.portfolio.account.PortfolioAccountType;
import org.mifosplatform.portfolio.account.data.AccountTransferDTO;
import org.mifosplatform.portfolio.account.domain.AccountTransferDetails;
import org.mifosplatform.portfolio.account.domain.AccountTransferType;
import org.mifosplatform.portfolio.account.service.AccountTransfersReadPlatformService;
import org.mifosplatform.portfolio.account.service.AccountTransfersWritePlatformService;
import org.mifosplatform.portfolio.common.BusinessEventNotificationConstants.BUSINESS_EVENTS;
import org.mifosplatform.portfolio.common.service.BusinessEventListner;
import org.mifosplatform.portfolio.common.service.BusinessEventNotifierService;
import org.mifosplatform.portfolio.loanaccount.domain.Loan;
import org.mifosplatform.portfolio.loanaccount.domain.LoanStatus;
import org.mifosplatform.portfolio.loanaccount.domain.LoanTransaction;
import org.mifosplatform.portfolio.loanaccount.guarantor.GuarantorConstants;
import org.mifosplatform.portfolio.loanaccount.guarantor.domain.Guarantor;
import org.mifosplatform.portfolio.loanaccount.guarantor.domain.GuarantorFundingDetails;
import org.mifosplatform.portfolio.loanaccount.guarantor.domain.GuarantorFundingRepository;
import org.mifosplatform.portfolio.loanaccount.guarantor.domain.GuarantorFundingTransaction;
import org.mifosplatform.portfolio.loanaccount.guarantor.domain.GuarantorFundingTransactionRepository;
import org.mifosplatform.portfolio.loanaccount.guarantor.domain.GuarantorRepository;
import org.mifosplatform.portfolio.loanproduct.domain.LoanProduct;
import org.mifosplatform.portfolio.loanproduct.domain.LoanProductGuaranteeDetails;
import org.mifosplatform.portfolio.paymentdetail.domain.PaymentDetail;
import org.mifosplatform.portfolio.savings.domain.DepositAccountOnHoldTransaction;
import org.mifosplatform.portfolio.savings.domain.DepositAccountOnHoldTransactionRepository;
import org.mifosplatform.portfolio.savings.domain.SavingsAccount;
import org.mifosplatform.portfolio.savings.domain.SavingsAccountDomainService;
import org.mifosplatform.portfolio.savings.domain.SavingsAccountTransactionSummaryWrapper;
import org.mifosplatform.portfolio.savings.domain.SavingsHelper;
import org.mifosplatform.portfolio.savings.exception.InsufficientAccountBalanceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.AbstractPersistable;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class GuarantorDomainServiceImpl implements GuarantorDomainService {

    private final GuarantorRepository guarantorRepository;
    private final GuarantorFundingRepository guarantorFundingRepository;
    private final GuarantorFundingTransactionRepository guarantorFundingTransactionRepository;
    private final AccountTransfersWritePlatformService accountTransfersWritePlatformService;
    private final BusinessEventNotifierService businessEventNotifierService;
    private final DepositAccountOnHoldTransactionRepository depositAccountOnHoldTransactionRepository;
    private final Map<Long, Long> releaseLoanIds = new HashMap<>(2);
    private final RoundingMode roundingMode = RoundingMode.HALF_EVEN;
    private final SavingsAccountDomainService savingsAccountDomainService;
    private final SavingsAccountTransactionSummaryWrapper savingsAccountTransactionSummaryWrapper;
    private final SavingsHelper savingsHelper;
    private final AccountTransfersReadPlatformService accountTransfersReadPlatformService;

    @Autowired
    public GuarantorDomainServiceImpl(final GuarantorRepository guarantorRepository,
            final GuarantorFundingRepository guarantorFundingRepository,
            final GuarantorFundingTransactionRepository guarantorFundingTransactionRepository,
            final AccountTransfersWritePlatformService accountTransfersWritePlatformService,
            final BusinessEventNotifierService businessEventNotifierService,
            final DepositAccountOnHoldTransactionRepository depositAccountOnHoldTransactionRepository, 
            final SavingsAccountDomainService savingsAccountDomainService, 
            final SavingsAccountTransactionSummaryWrapper savingsAccountTransactionSummaryWrapper, 
            final AccountTransfersReadPlatformService accountTransfersReadPlatformService) {
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
    }

    @PostConstruct
    public void addListners() {
        this.businessEventNotifierService.addBusinessEventPostListners(BUSINESS_EVENTS.LOAN_APPROVED, new ValidateOnBusinessEvent());
        this.businessEventNotifierService.addBusinessEventPostListners(BUSINESS_EVENTS.LOAN_APPROVED, new HoldFundsOnBusinessEvent());
        this.businessEventNotifierService.addBusinessEventPostListners(BUSINESS_EVENTS.LOAN_UNDO_APPROVAL, new UndoAllFundTransactions());
        this.businessEventNotifierService.addBusinessEventPostListners(BUSINESS_EVENTS.LOAN_UNDO_DISBURSAL,
                new ReverseAllFundsOnBusinessEvent());
        this.businessEventNotifierService.addBusinessEventPostListners(BUSINESS_EVENTS.LOAN_UNDO_TRANSACTION,
                new ReverseFundsOnBusinessEvent());
        this.businessEventNotifierService.addBusinessEventPostListners(BUSINESS_EVENTS.LOAN_MAKE_REPAYMENT,
                new ReleaseFundsOnBusinessEvent());
        this.businessEventNotifierService.addBusinessEventPostListners(BUSINESS_EVENTS.LOAN_WRITTEN_OFF, new ReleaseAllFunds());
        this.businessEventNotifierService.addBusinessEventPostListners(BUSINESS_EVENTS.LOAN_UNDO_WRITTEN_OFF,
                new ReverseFundsOnBusinessEvent());
        this.businessEventNotifierService.addBusinessEventPostListners(BUSINESS_EVENTS.LOAN_MAKE_REPAYMENT,
                new SplitInterestIncomeOnBusinessEvent());
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
            BigDecimal minExtGuarantee = principal.multiply(guaranteeData.getMinimumGuaranteeFromGuarantor()).divide(
                    BigDecimal.valueOf(100));

            BigDecimal actualAmount = BigDecimal.ZERO;
            BigDecimal actualSelfAmount = BigDecimal.ZERO;
            BigDecimal actualExtGuarantee = BigDecimal.ZERO;
            for (Guarantor guarantor : existGuarantorList) {
                List<GuarantorFundingDetails> fundingDetails = guarantor.getGuarantorFundDetails();
                for (GuarantorFundingDetails guarantorFundingDetails : fundingDetails) {
                    if (guarantorFundingDetails.getStatus().isActive() || guarantorFundingDetails.getStatus().isWithdrawn()
                            || guarantorFundingDetails.getStatus().isCompleted()) {
                        if (guarantor.isSelfGuarantee()) {
                            actualSelfAmount = actualSelfAmount.add(guarantorFundingDetails.getAmount()).subtract(
                                    guarantorFundingDetails.getAmountTransfered());
                        } else {
                            actualExtGuarantee = actualExtGuarantee.add(guarantorFundingDetails.getAmount()).subtract(
                                    guarantorFundingDetails.getAmountTransfered());
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
                        savingsAccount.getId());
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
                        if(loan.getGuaranteeAmount().compareTo(loan.getPrincpal().getAmount()) == 1){
                            remainingAmount = remainingAmount.multiply(loan.getPrincpal().getAmount()).divide(loan.getGuaranteeAmount(), roundingMode);
                        }
                        AccountTransferDTO accountTransferDTO = new AccountTransferDTO(transactionDate, remainingAmount, fromAccountType,
                                toAccountType, fromAccountId, toAccountId, description, locale, fmt, paymentDetail, fromTransferType,
                                toTransferType, chargeId, loanInstallmentNumber, transferType, accountTransferDetails, noteText,
                                txnExternalId, loan, toSavingsAccount, fromSavingsAccount, isRegularTransaction, isExceptionForBalanceCheck);
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
                    accountTransferDTO.getFromAccountId(), accountTransferDTO.getToAccountId());
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
                        }
                    }
                }
            }
            if (!insufficientBalanceIds.isEmpty()) {
                final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
                final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("loan.guarantor");
                baseDataValidator.reset().failWithCodeNoParameterAddedToErrorCode(GuarantorConstants.GUARANTOR_INSUFFICIENT_BALANCE_ERROR,
                        insufficientBalanceIds);
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

            amountForRelease = amountForRelease.multiply(totalGuaranteeAmount).divide(principal);
            List<DepositAccountOnHoldTransaction> accountOnHoldTransactions = new ArrayList<>();

            BigDecimal amountLeft = calculateAndRelaseGuarantorFunds(externalGuarantorList, guarantorGuarantee, amountForRelease,
                    loanTransaction, accountOnHoldTransactions);

            if (amountLeft.compareTo(BigDecimal.ZERO) == 1) {
                calculateAndRelaseGuarantorFunds(selfGuarantorList, selfGuarantee, amountLeft, loanTransaction, accountOnHoldTransactions);
                externalGuarantorList.addAll(selfGuarantorList);
            }

            if (!externalGuarantorList.isEmpty()) {
                this.depositAccountOnHoldTransactionRepository.save(accountOnHoldTransactions);
                this.guarantorFundingRepository.save(externalGuarantorList);
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
                    roundingMode);
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
        final Loan loan = loanTransaction.getLoan();
        final LoanProduct loanProduct = loan.getLoanProduct();
        final LoanProductGuaranteeDetails loanProductGuaranteeDetails = loanProduct.getLoanProductGuaranteeDetails();
        final LoanStatus loanStatus = LoanStatus.fromInt(loan.getStatus());
        
        // only proceed if the splitInterestAmongGuarantors property is set to 1 and the loan status is 600
        if (loanProductGuaranteeDetails.splitInterestAmongGuarantors() && loanStatus.isClosedObligationsMet()) {
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
                            final PaymentDetail paymentDetail = null;
                            final boolean isAccountTransfer = false;
                            final boolean isRegularTransaction = true;
                            
                            // inject helper classes to savings account class
                            savingsAccount.setHelpers(this.savingsAccountTransactionSummaryWrapper, this.savingsHelper);
                            
                            // call method to handle savings account deposit
                            this.savingsAccountDomainService.handleDeposit(savingsAccount, fmt, transactionDate,
                                    shareOfLoanInterestIncome, paymentDetail, isAccountTransfer, isRegularTransaction);
                        }
                    }
                }
            }
        }
    }

    private class ValidateOnBusinessEvent implements BusinessEventListner {

        @Override
        public void businessEventToBeExecuted(@SuppressWarnings("unused") AbstractPersistable<Long> businessEventEntity) {}

        @Override
        public void businessEventWasExecuted(AbstractPersistable<Long> businessEventEntity) {
            if (businessEventEntity instanceof Loan) {
                Loan loan = (Loan) businessEventEntity;
                validateGuarantorBusinessRules(loan);
            }
        }
    }

    private class HoldFundsOnBusinessEvent implements BusinessEventListner {

        @Override
        public void businessEventToBeExecuted(@SuppressWarnings("unused") AbstractPersistable<Long> businessEventEntity) {}

        @Override
        public void businessEventWasExecuted(AbstractPersistable<Long> businessEventEntity) {
            if (businessEventEntity instanceof Loan) {
                Loan loan = (Loan) businessEventEntity;
                holdGuarantorFunds(loan);
            }
        }
    }

    private class ReleaseFundsOnBusinessEvent implements BusinessEventListner {

        @Override
        public void businessEventToBeExecuted(@SuppressWarnings("unused") AbstractPersistable<Long> businessEventEntity) {}

        @Override
        public void businessEventWasExecuted(AbstractPersistable<Long> businessEventEntity) {
            if (businessEventEntity instanceof LoanTransaction) {
                LoanTransaction loanTransaction = (LoanTransaction) businessEventEntity;
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
        public void businessEventToBeExecuted(@SuppressWarnings("unused") AbstractPersistable<Long> businessEventEntity) {}

        @Override
        public void businessEventWasExecuted(AbstractPersistable<Long> businessEventEntity) {
            if (businessEventEntity instanceof LoanTransaction) {
                LoanTransaction loanTransaction = (LoanTransaction) businessEventEntity;
                List<Long> reersedTransactions = new ArrayList<>(1);
                reersedTransactions.add(loanTransaction.getId());
                reverseTransaction(reersedTransactions);
            }
        }
    }

    private class ReverseAllFundsOnBusinessEvent implements BusinessEventListner {

        @Override
        public void businessEventToBeExecuted(@SuppressWarnings("unused") AbstractPersistable<Long> businessEventEntity) {}

        @Override
        public void businessEventWasExecuted(AbstractPersistable<Long> businessEventEntity) {
            if (businessEventEntity instanceof Loan) {
                Loan loan = (Loan) businessEventEntity;
                List<Long> reersedTransactions = new ArrayList<>(1);
                reersedTransactions.addAll(loan.findExistingTransactionIds());
                reverseTransaction(reersedTransactions);
            }
        }
    }

    private class UndoAllFundTransactions implements BusinessEventListner {

        @Override
        public void businessEventToBeExecuted(@SuppressWarnings("unused") AbstractPersistable<Long> businessEventEntity) {}

        @Override
        public void businessEventWasExecuted(AbstractPersistable<Long> businessEventEntity) {
            if (businessEventEntity instanceof Loan) {
                Loan loan = (Loan) businessEventEntity;
                reverseAllFundTransaction(loan);
            }
        }
    }

    private class ReleaseAllFunds implements BusinessEventListner {

        @Override
        public void businessEventToBeExecuted(@SuppressWarnings("unused") AbstractPersistable<Long> businessEventEntity) {}

        @Override
        public void businessEventWasExecuted(AbstractPersistable<Long> businessEventEntity) {
            if (businessEventEntity instanceof LoanTransaction) {
                LoanTransaction loanTransaction = (LoanTransaction) businessEventEntity;
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
        public void businessEventToBeExecuted(AbstractPersistable<Long> businessEventEntity) { }

        @Override
        public void businessEventWasExecuted(AbstractPersistable<Long> businessEventEntity) {
            if (businessEventEntity instanceof LoanTransaction) {
                LoanTransaction loanTransaction = (LoanTransaction) businessEventEntity;
                splitIncomeInterestAmongGuarantors(loanTransaction);
            }
        }
    }
}
