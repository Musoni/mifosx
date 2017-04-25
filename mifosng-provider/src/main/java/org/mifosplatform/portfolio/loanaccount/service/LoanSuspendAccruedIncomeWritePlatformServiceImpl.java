/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.portfolio.loanaccount.service;


import org.joda.time.LocalDate;
import org.mifosplatform.accounting.journalentry.service.JournalEntryWritePlatformService;
import org.mifosplatform.infrastructure.core.service.DateUtils;
import org.mifosplatform.infrastructure.core.service.RoutingDataSource;
import org.mifosplatform.organisation.monetary.domain.ApplicationCurrencyRepositoryWrapper;
import org.mifosplatform.portfolio.loanaccount.data.LoanChargeData;
import org.mifosplatform.portfolio.loanaccount.data.LoanScheduleSuspendedAccruedIncomeData;
import org.mifosplatform.portfolio.loanaccount.data.LoanTransactionEnumData;
import org.mifosplatform.portfolio.loanaccount.domain.*;
import org.mifosplatform.portfolio.loanproduct.service.LoanEnumerations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class LoanSuspendAccruedIncomeWritePlatformServiceImpl implements LoanSuspendAccruedIncomeWritePlatformService {

    private final static Logger logger = LoggerFactory.getLogger(LoanSuspendAccruedIncomeWritePlatformServiceImpl.class);


    private final LoanReadPlatformService loanReadPlatformService;
    private final LoanChargeReadPlatformService loanChargeReadPlatformService;
    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private final JpaTransactionManager transactionManager;
    private final JournalEntryWritePlatformService journalEntryWritePlatformService;
    private final ApplicationCurrencyRepositoryWrapper applicationCurrencyRepository;
    private final LoanRepaymentScheduleInstallmentRepository repaymentScheduleInstallmentRepository;
    private final LoanRepository loanRepository;




    @Autowired
    public LoanSuspendAccruedIncomeWritePlatformServiceImpl(final LoanReadPlatformService loanReadPlatformService, final LoanChargeReadPlatformService loanChargeReadPlatformService,
            final RoutingDataSource dataSource,final JpaTransactionManager transactionManager,final JournalEntryWritePlatformService journalEntryWritePlatformService,
            final ApplicationCurrencyRepositoryWrapper applicationCurrencyRepository,final LoanRepaymentScheduleInstallmentRepository repaymentScheduleInstallmentRepository,
            final LoanRepository loanRepository) {
        this.loanReadPlatformService = loanReadPlatformService;
        this.loanChargeReadPlatformService = loanChargeReadPlatformService;
        this.dataSource = dataSource;
        this.jdbcTemplate = new JdbcTemplate(this.dataSource);
        this.transactionManager = transactionManager;
        this.journalEntryWritePlatformService = journalEntryWritePlatformService;
        this.applicationCurrencyRepository = applicationCurrencyRepository;
        this.repaymentScheduleInstallmentRepository = repaymentScheduleInstallmentRepository;
        this.loanRepository = loanRepository;

    }



    @Override
    public void suspendAccruedIncome() {
        Collection<LoanScheduleSuspendedAccruedIncomeData> npaLoansToSuspendAccruedIncome = this.loanReadPlatformService.retrieveLoanScheduleForNPASuspendedIncome();
        StringBuilder sb = new StringBuilder();
        Set<Long> loansIds = new HashSet<>();
        Map<Long, Collection<LoanChargeData>> loanChargeMap = new HashMap<>();
        if(npaLoansToSuspendAccruedIncome != null && !npaLoansToSuspendAccruedIncome.isEmpty()){
            for (final LoanScheduleSuspendedAccruedIncomeData accrualData : npaLoansToSuspendAccruedIncome) {
                try {

                    // There is more suspended income to be booked on this loan, therefore make those bookings:
                    if(accrualData.getIsReverse() == false) {

                        if (!loansIds.contains(accrualData.getLoanId())) {
                            addSuspendedIncomeAccounting(accrualData);
                        }
                    }

                    // There has been interest/Fees suspended on this loan, that needs to be unsuspended:
                    if(accrualData.getIsReverse() == true) {

                        if (!loansIds.contains(accrualData.getLoanId())) {
                            reverseSuspendedIncomeAccounting(accrualData);
                        }
                    }


                } catch (Exception e) {
                    loansIds.add(accrualData.getLoanId());
                    Throwable realCause = e;
                    if (e.getCause() != null) {
                        realCause = e.getCause();
                    }
                    sb.append("failed to add accural transaction for repayment with id " + accrualData.getRepaymentScheduleId()
                            + " with message " + realCause.getMessage());
                }
            }

        }

    }

    private void addSuspendedIncomeTransaction(LoanScheduleSuspendedAccruedIncomeData loanScheduleSuspendedAccruedIncomeData,BigDecimal amount,BigDecimal interestPortion,
                                               BigDecimal feePortion, BigDecimal penaltyPortion){
        TransactionStatus transactionStatus = this.transactionManager.getTransaction(new DefaultTransactionDefinition());
        try{
            String transactionSql = "INSERT INTO m_loan_transaction  (loan_id,office_id,is_reversed,transaction_type_enum,transaction_date,amount,suspended_interest_portion_derived,"
                    + "suspended_fee_charges_portion_derived,suspended_penalty_charges_portion_derived, submitted_on_date, is_account_transfer) VALUES (?, ?, 0, ?, ?, ?, ?, ?, ?, ?, 0)";
            this.jdbcTemplate.update(transactionSql, loanScheduleSuspendedAccruedIncomeData.getLoanId(), loanScheduleSuspendedAccruedIncomeData.getOfficeId(),
                    LoanTransactionType.SUSPENDED_ACCRUED_INCOME.getValue(), DateUtils.getLocalDateOfTenant().toDate(), amount, interestPortion, feePortion, penaltyPortion,
                    DateUtils.getDateOfTenant());
            @SuppressWarnings("deprecation")
            final Long transactionId = this.jdbcTemplate.queryForLong("SELECT LAST_INSERT_ID()");

            Map<String, Object> transactionMap = toMapData(transactionId, amount, interestPortion, feePortion, penaltyPortion,
                    loanScheduleSuspendedAccruedIncomeData, DateUtils.getLocalDateOfTenant(),LoanTransactionType.SUSPENDED_ACCRUED_INCOME.getValue());

            String repaymentUpdateSql = "UPDATE m_loan_repayment_schedule SET suspended_interest_derived=?, suspended_fee_charges_derived=?, "
                    + "suspended_penalty_charges_derived=? WHERE  id=?";
            this.jdbcTemplate.update(repaymentUpdateSql, loanScheduleSuspendedAccruedIncomeData.getSuspendedInterest().add(interestPortion), loanScheduleSuspendedAccruedIncomeData.getSuspendedFee().add(feePortion), loanScheduleSuspendedAccruedIncomeData.getSuspendedPenalty().add(penaltyPortion),
                    loanScheduleSuspendedAccruedIncomeData.getRepaymentScheduleId());
            String updateLoan = "UPDATE m_loan  SET is_suspended_income=?  WHERE  id=?";
            boolean isSuspendedIncome = true;
            this.jdbcTemplate.update(updateLoan,isSuspendedIncome, loanScheduleSuspendedAccruedIncomeData.getLoanId());
            final Map<String, Object> accountingBridgeData = deriveAccountingBridgeData(loanScheduleSuspendedAccruedIncomeData, transactionMap);
            this.journalEntryWritePlatformService.createJournalEntriesForLoan(accountingBridgeData);

        }catch(Exception e){
            this.transactionManager.rollback(transactionStatus);
            throw e;
        }
        this.transactionManager.commit(transactionStatus);
    }

    private void reverseSuspendedIncomeTransaction(LoanScheduleSuspendedAccruedIncomeData loanScheduleSuspendedAccruedIncomeData,BigDecimal amount,BigDecimal interestPortion,
                                               BigDecimal feePortion, BigDecimal penaltyPortion){
        TransactionStatus transactionStatus = this.transactionManager.getTransaction(new DefaultTransactionDefinition());
        try{
            String transactionSql = "INSERT INTO m_loan_transaction  (loan_id,office_id,is_reversed,transaction_type_enum,transaction_date,amount,suspended_interest_portion_derived,"
                    + "suspended_fee_charges_portion_derived,suspended_penalty_charges_portion_derived, submitted_on_date, is_account_transfer) VALUES (?, ?, 0, ?, ?, ?, ?, ?, ?, ?, 0)";
            this.jdbcTemplate.update(transactionSql, loanScheduleSuspendedAccruedIncomeData.getLoanId(), loanScheduleSuspendedAccruedIncomeData.getOfficeId(),
                    LoanTransactionType.REVERSE_SUSPENDED_ACCRUED_INCOME.getValue(), DateUtils.getLocalDateOfTenant().toDate(), amount, interestPortion, feePortion, penaltyPortion,
                    DateUtils.getDateOfTenant());
            @SuppressWarnings("deprecation")
            final Long transactionId = this.jdbcTemplate.queryForLong("SELECT LAST_INSERT_ID()");

            Map<String, Object> transactionMap = toMapData(transactionId, amount, interestPortion, feePortion, penaltyPortion,
                    loanScheduleSuspendedAccruedIncomeData, DateUtils.getLocalDateOfTenant(),LoanTransactionType.REVERSE_SUSPENDED_ACCRUED_INCOME.getValue());

            String repaymentUpdateSql = "UPDATE m_loan_repayment_schedule SET suspended_interest_derived=?, suspended_fee_charges_derived=?, "
                    + "suspended_penalty_charges_derived=? WHERE  id=?";
            this.jdbcTemplate.update(repaymentUpdateSql, loanScheduleSuspendedAccruedIncomeData.getSuspendedInterest().subtract(interestPortion), loanScheduleSuspendedAccruedIncomeData.getSuspendedFee().subtract(feePortion), loanScheduleSuspendedAccruedIncomeData.getSuspendedPenalty().subtract(penaltyPortion),
                    loanScheduleSuspendedAccruedIncomeData.getRepaymentScheduleId());
            String updateLoan = "UPDATE m_loan  SET is_suspended_income=?  WHERE  id=?";
            boolean isSuspendedIncome = true;
            this.jdbcTemplate.update(updateLoan,isSuspendedIncome, loanScheduleSuspendedAccruedIncomeData.getLoanId());
            final Map<String, Object> accountingBridgeData = deriveAccountingBridgeData(loanScheduleSuspendedAccruedIncomeData, transactionMap);
            this.journalEntryWritePlatformService.createJournalEntriesForLoan(accountingBridgeData);

        }catch(Exception e){
            this.transactionManager.rollback(transactionStatus);
            throw e;
        }
        this.transactionManager.commit(transactionStatus);
    }

    @Transactional
    private void addSuspendedIncomeAccounting(LoanScheduleSuspendedAccruedIncomeData scheduleAccrualData) throws Exception{

        BigDecimal interestPortion = scheduleAccrualData.getInterestToSuspend();
        BigDecimal feePortion = scheduleAccrualData.getFeesToSuspend() ;
        BigDecimal penaltyPortion = scheduleAccrualData.getPenaltyToSuspend();

        BigDecimal amount = interestPortion.add(feePortion).add(penaltyPortion);

        if (amount.compareTo(BigDecimal.ZERO) == 1) {
            addSuspendedIncomeTransaction(scheduleAccrualData, amount, interestPortion, feePortion, penaltyPortion);
        }

    }

    @Transactional
    private void reverseSuspendedIncomeAccounting(LoanScheduleSuspendedAccruedIncomeData scheduleAccrualData) throws Exception{

        BigDecimal interestPortion = scheduleAccrualData.getInterestToSuspend().abs();
        BigDecimal feePortion = scheduleAccrualData.getFeesToSuspend().abs();
        BigDecimal penaltyPortion = scheduleAccrualData.getPenaltyToSuspend().abs();
        BigDecimal amount = interestPortion.add(feePortion).add(penaltyPortion);

        if (amount.compareTo(BigDecimal.ZERO) == 1) {
            reverseSuspendedIncomeTransaction(scheduleAccrualData, amount, interestPortion, feePortion, penaltyPortion);
        }

    }

    public Map<String, Object> deriveAccountingBridgeData(final LoanScheduleSuspendedAccruedIncomeData loanScheduleSuspendedAccruedIncomeData,
                                                          final Map<String, Object> transactionMap) {

        final Map<String, Object> accountingBridgeData = new LinkedHashMap<>();
        accountingBridgeData.put("loanId", loanScheduleSuspendedAccruedIncomeData.getLoanId());
        accountingBridgeData.put("loanProductId", loanScheduleSuspendedAccruedIncomeData.getLoanProductId());
        accountingBridgeData.put("officeId", loanScheduleSuspendedAccruedIncomeData.getOfficeId());
        accountingBridgeData.put("currency", loanScheduleSuspendedAccruedIncomeData.getCurrencyData());
        accountingBridgeData.put("cashBasedAccountingEnabled", false);
        accountingBridgeData.put("upfrontAccrualBasedAccountingEnabled", false);
        accountingBridgeData.put("periodicAccrualBasedAccountingEnabled", true);
        accountingBridgeData.put("isAccountTransfer", false);

        final List<Map<String, Object>> newLoanTransactions = new ArrayList<>();
        newLoanTransactions.add(transactionMap);

        accountingBridgeData.put("newLoanTransactions", newLoanTransactions);
        return accountingBridgeData;
    }

    public Map<String, Object> toMapData(final Long transactionId, final BigDecimal amount, final BigDecimal interestPortion,
                                         final BigDecimal feePortion, final BigDecimal penaltyPortion, final LoanScheduleSuspendedAccruedIncomeData loanScheduleSuspendedAccruedIncomeData,
                                         final LocalDate accruedTill,final Integer loanTransactionType) {
        final Map<String, Object> thisTransactionData = new LinkedHashMap<>();

        final LoanTransactionEnumData transactionType = LoanEnumerations.transactionType(loanTransactionType);

        thisTransactionData.put("id", transactionId);
        thisTransactionData.put("officeId", loanScheduleSuspendedAccruedIncomeData.getOfficeId());
        thisTransactionData.put("type", transactionType);
        thisTransactionData.put("reversed", false);
        thisTransactionData.put("date", DateUtils.getLocalDateOfTenant());
        thisTransactionData.put("currency", loanScheduleSuspendedAccruedIncomeData.getCurrencyData());
        thisTransactionData.put("amount", amount);
        thisTransactionData.put("principalPortion", null);
        thisTransactionData.put("interestPortion", interestPortion);
        thisTransactionData.put("feeChargesPortion", feePortion);
        thisTransactionData.put("penaltyChargesPortion", penaltyPortion);
        thisTransactionData.put("overPaymentPortion", null);
        thisTransactionData.put("isAccountTransfer", false);




        Map<LoanChargeData, BigDecimal> applicableCharges = loanScheduleSuspendedAccruedIncomeData.getAppliedCharge();
        if (applicableCharges != null && !applicableCharges.isEmpty()) {
            final List<Map<String, Object>> loanChargesPaidData = new ArrayList<>();
                for (Map.Entry<LoanChargeData, BigDecimal> entry : applicableCharges.entrySet()) {
                    LoanChargeData chargeData = entry.getKey();
                final Map<String, Object> loanChargePaidData = new LinkedHashMap<>();
                loanChargePaidData.put("chargeId", chargeData.getChargeId());
                loanChargePaidData.put("isPenalty", chargeData.isPenalty());
                loanChargePaidData.put("loanChargeId", chargeData.getId());
                loanChargePaidData.put("amount", entry.getValue());

                loanChargesPaidData.add(loanChargePaidData);
            }
            thisTransactionData.put("loanChargesPaid", loanChargesPaidData);
        }

        return thisTransactionData;

    }

    @Override
    public void suspendedIncomeOutOfNPA(Loan loan, LoanTransaction newTransaction)
    {
        if(!loan.isNpa())
        {
            return;
        }
        else if (newTransaction.isAnyTypeOfRepayment())
        {
            suspendedIncomeOutOfNPA(loan);
        }

    }


    @Override
    public void suspendedIncomeOutOfNPA(Loan loan) {

        if(loan.isNpa()) {
            this.resetNPAStatus(loan.getId());
        }

        // Only check to unsuspend income when the loan has this enabled:
        if(loan.getLoanProduct().isReverseNPAInterestEnabled()) {

            // First calculate if the loan has become NPA as a result of this action:
            this.updateNPAStatus(loan.getId());

            // Find all instalments that have suspended income on them:
            Collection<LoanScheduleSuspendedAccruedIncomeData> npaLoansToSuspendAccruedIncome = this.loanReadPlatformService.retrieveLoanScheduleForNPASuspendedIncome(loan.getId());
            StringBuilder sb = new StringBuilder();
            Set<Long> loansIds = new HashSet<>();
            Map<Long, Collection<LoanChargeData>> loanChargeMap = new HashMap<>();
            if (npaLoansToSuspendAccruedIncome != null && !npaLoansToSuspendAccruedIncome.isEmpty()) {
                for (final LoanScheduleSuspendedAccruedIncomeData accrualData : npaLoansToSuspendAccruedIncome) {
                    try {

                        // There is more suspended income to be booked on this loan, therefore make those bookings:
                        if (!accrualData.getIsReverse()) {

                            if (!loansIds.contains(accrualData.getLoanId())) {
                                addSuspendedIncomeAccounting(accrualData);
                            }
                        }

                        // There has been interest/Fees suspended on this loan, that needs to be unsuspended:
                        if (accrualData.getIsReverse()) {

                            if (!loansIds.contains(accrualData.getLoanId())) {
                                reverseSuspendedIncomeAccounting(accrualData);
                            }
                        }


                    } catch (Exception e) {
                        loansIds.add(accrualData.getLoanId());
                        Throwable realCause = e;
                        if (e.getCause() != null) {
                            realCause = e.getCause();
                        }
                        sb.append("failed to add accural transaction for repayment with id " + accrualData.getRepaymentScheduleId()
                                + " with message " + realCause.getMessage());
                    }
                }

            }
        }

        if(loan.isNpa()) {
            this.resetNPAStatus(loan.getId());
        }

        this.updateNPAStatus(loan.getId());

    }

    public void updateNPAStatus(Long loanId) {
        final StringBuilder updateSqlBuilder = new StringBuilder(900);

        updateSqlBuilder.append("UPDATE m_loan as ml,");
        updateSqlBuilder.append(" (select loan.id from m_loan_repayment_schedule mr ");
        updateSqlBuilder
                .append(" INNER JOIN  m_loan loan on mr.loan_id = loan.id INNER JOIN m_product_loan mpl on mpl.id = loan.product_id AND mpl.overdue_days_for_npa is not null ");
        updateSqlBuilder.append("WHERE loan.loan_status_id = 300 and mr.completed_derived is false ");
        updateSqlBuilder
                .append(" and mr.duedate < SUBDATE(CURDATE(),INTERVAL  ifnull(mpl.overdue_days_for_npa,0) day) group by loan.id)  as sl ");
        updateSqlBuilder.append("SET ml.is_npa=1 where ml.id=sl.id and ml.id = ?");

        final int result = jdbcTemplate.update(updateSqlBuilder.toString(), loanId);
    }

    public void resetNPAStatus(Long loanId)
    {
        final StringBuilder updateSqlBuilder = new StringBuilder(900);

        updateSqlBuilder.append("UPDATE m_loan ml SET ml.is_npa=0");
        updateSqlBuilder.append(" and ml.id = ?");

        final int result = jdbcTemplate.update(updateSqlBuilder.toString(), loanId);
    }


}
