/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.portfolio.loanaccount.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.mifosplatform.accounting.journalentry.exception.JournalEntryInvalidException;
import org.mifosplatform.infrastructure.configuration.domain.ConfigurationDomainService;
import org.mifosplatform.infrastructure.core.data.ApiParameterError;
import org.mifosplatform.infrastructure.core.exception.AbstractPlatformDomainRuleException;
import org.mifosplatform.infrastructure.core.exception.PlatformApiDataValidationException;
import org.mifosplatform.infrastructure.core.service.ThreadLocalContextUtil;
import org.mifosplatform.infrastructure.jobs.annotation.CronTarget;
import org.mifosplatform.infrastructure.jobs.exception.JobExecutionException;
import org.mifosplatform.infrastructure.jobs.service.JobName;
import org.mifosplatform.portfolio.charge.domain.*;
import org.mifosplatform.portfolio.charge.service.ChargeReadPlatformServiceImpl;
import org.mifosplatform.portfolio.common.BusinessEventNotificationConstants;
import org.mifosplatform.portfolio.loanaccount.data.LoanAccountData;
import org.mifosplatform.portfolio.loanaccount.domain.Loan;
import org.mifosplatform.portfolio.loanaccount.domain.LoanCharge;
import org.mifosplatform.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.mifosplatform.portfolio.loanaccount.loanschedule.data.OverdueLoanScheduleData;
import org.mifosplatform.portfolio.loanproduct.domain.LoanProduct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

@Service
public class LoanSchedularServiceImpl implements LoanSchedularService {

    private final static Logger logger = LoggerFactory.getLogger(LoanSchedularServiceImpl.class);
    private final ConfigurationDomainService configurationDomainService;
    private final LoanReadPlatformService loanReadPlatformService;
    private final LoanWritePlatformService loanWritePlatformService;
	private final LoanAssembler loanAssembler;
	private final ChargeReadPlatformServiceImpl chargeReadPlatformService;

    @Autowired
    public LoanSchedularServiceImpl(final ConfigurationDomainService configurationDomainService,
            final LoanReadPlatformService loanReadPlatformService, final LoanWritePlatformService loanWritePlatformService,final LoanAssembler loanAssembler,
									final ChargeReadPlatformServiceImpl chargeReadPlatformService) {
        this.configurationDomainService = configurationDomainService;
        this.loanReadPlatformService = loanReadPlatformService;
        this.loanWritePlatformService = loanWritePlatformService;
		this.loanAssembler = loanAssembler;
		this.chargeReadPlatformService = chargeReadPlatformService;
    }

    @Override
    @CronTarget(jobName = JobName.APPLY_CHARGE_TO_OVERDUE_LOAN_INSTALLMENT)
    public void applyChargeForOverdueLoans() throws JobExecutionException {

        final Long penaltyWaitPeriodValue = this.configurationDomainService.retrievePenaltyWaitPeriod();
		final Long penaltyOnMaturityWaitPeriodValue = this.configurationDomainService.retrievePenaltyOnMaturityWaitPeriod();

        final Boolean backdatePenalties = this.configurationDomainService.isBackdatePenaltiesEnabled();
        final Collection<OverdueLoanScheduleData> overdueLoanScheduledInstallments = this.loanReadPlatformService
                .retrieveAllLoansWithOverdueInstallments(penaltyWaitPeriodValue,backdatePenalties);

        if (!overdueLoanScheduledInstallments.isEmpty()) {
            final StringBuilder sb = new StringBuilder();
            final Map<Long, Collection<OverdueLoanScheduleData>> overdueScheduleData = new HashMap<>();
            for (final OverdueLoanScheduleData overdueInstallment : overdueLoanScheduledInstallments) {
                if (overdueScheduleData.containsKey(overdueInstallment.getLoanId())) {
                    overdueScheduleData.get(overdueInstallment.getLoanId()).add(overdueInstallment);
                } else {
                    Collection<OverdueLoanScheduleData> loanData = new ArrayList<>();
                    loanData.add(overdueInstallment);
                    overdueScheduleData.put(overdueInstallment.getLoanId(), loanData);
                }
            }

            for (final Long loanId : overdueScheduleData.keySet()) {
                try {
                    this.loanWritePlatformService.applyOverdueChargesForLoan(loanId, overdueScheduleData.get(loanId));

                } catch (final PlatformApiDataValidationException e) {
                    final List<ApiParameterError> errors = e.getErrors();
                    for (final ApiParameterError error : errors) {
                        logger.error("Apply Charges due for overdue loans failed for account:" + loanId + " with message "
                                + error.getDeveloperMessage());
                        sb.append("Apply Charges due for overdue loans failed for account:").append(loanId).append(" with message ")
                                .append(error.getDeveloperMessage());
                    }
                } catch (final AbstractPlatformDomainRuleException ex) {
                    logger.error("Apply Charges due for overdue loans failed for account:" + loanId + " with message "
                            + ex.getDefaultUserMessage());
                    sb.append("Apply Charges due for overdue loans failed for account:").append(loanId).append(" with message ")
                            .append(ex.getDefaultUserMessage());
                } catch (Exception e) {
                    Throwable realCause = e;
                    if (e.getCause() != null) {
                        realCause = e.getCause();
                    }
                    logger.error("Apply Charges due for overdue loans failed for account:" + loanId + " with message "
                            + realCause.getMessage());
                    sb.append("Apply Charges due for overdue loans failed for account:").append(loanId).append(" with message ")
                            .append(realCause.getMessage());
                }
            }
            if (sb.length() > 0) { throw new JobExecutionException(sb.toString()); }
        }
    }

	@Override
	@CronTarget(jobName = JobName.APPLY_CHARGE_TO_OVERDUE_ON_MATURITY_LOANS)
	public void applyChargeForOverdueOnMaturityLoans() throws JobExecutionException {

		if(this.chargeReadPlatformService.isOverDueOnMaturityChargeExist(ChargeTimeType.OVERDUE_ON_MATURITY.getValue())){

			final Long penaltyOnMaturityWaitPeriodValue = this.configurationDomainService.retrievePenaltyOnMaturityWaitPeriod();

			final Collection<LoanAccountData> overdueLoans = this.loanReadPlatformService
					.retrieveAllLoansOverdueOnMaturity(penaltyOnMaturityWaitPeriodValue);

			if (!overdueLoans.isEmpty()) {
				final StringBuilder sb = new StringBuilder();

				for (final LoanAccountData loanAccountData : overdueLoans) {

					final Loan overdueLoan = this.loanAssembler.assembleFrom(loanAccountData.getId());

					//this.loanWritePlatformService.addLoanCharge()
					final LoanProduct loanProduct = overdueLoan.getLoanProduct();
					final Collection<Charge> loanProductCharges = loanProduct.getCharges();

					for (Charge loanProductCharge : loanProductCharges) {
						if (loanProductCharge.isOverdueOnMaturity()) {
							final BigDecimal loanPrincipal = overdueLoan.getPrincpal().getAmount();
							final BigDecimal chargeAmount = loanProductCharge.getAmount();
							final ChargeTimeType chargeTimeType = ChargeTimeType.fromInt(loanProductCharge.getChargeTimeType());
							final ChargeCalculationType chargeCalculationType = ChargeCalculationType.fromInt(loanProductCharge.getChargeCalculation());
							final ChargePaymentMode chargePaymentMode = ChargePaymentMode.fromInt(loanProductCharge.getChargePaymentMode());

							LoanCharge loanCharge = new LoanCharge(overdueLoan, loanProductCharge, loanPrincipal, chargeAmount,
									chargeTimeType, chargeCalculationType,overdueLoan.getMaturityDate(), chargePaymentMode,null, BigDecimal.ZERO);
	//
	//						this.businessEventNotifierService.notifyBusinessEventToBeExecuted(BusinessEventNotificationConstants.BUSINESS_EVENTS.LOAN_ADD_CHARGE,
	//								constructEntityMap(BusinessEventNotificationConstants.BUSINESS_ENTITY.LOAN_CHARGE, loanCharge));

							// add the loan charge to the loan
							this.loanWritePlatformService.addLoanCharge(overdueLoan, loanProductCharge, null, loanCharge);
						}
					}

				}

				if (sb.length() > 0) { throw new JobExecutionException(sb.toString()); }
			}
		}
	}

	@Override
	@CronTarget(jobName = JobName.RECALCULATE_INTEREST_FOR_LOAN)
	public void recalculateInterest() throws JobExecutionException {
		Integer maxNumberOfRetries = ThreadLocalContextUtil.getTenant()
				.getConnection().getMaxRetriesOnDeadlock();
		Integer maxIntervalBetweenRetries = ThreadLocalContextUtil.getTenant()
				.getConnection().getMaxIntervalBetweenRetries();
		Collection<Long> loanIds = this.loanReadPlatformService
				.fetchLoansForInterestRecalculation();
		int i = 0;
		if (!loanIds.isEmpty()) {
			final StringBuilder sb = new StringBuilder();
			for (Long loanId : loanIds) {
				logger.info("Loan ID " + loanId);
				Integer numberOfRetries = 0;
				while (numberOfRetries <= maxNumberOfRetries) {
					try {
						numberOfRetries++;
						this.loanWritePlatformService
								.recalculateInterest(loanId);
					} catch (CannotAcquireLockException
							| ObjectOptimisticLockingFailureException exception) {
						logger.info("Recalulate interest job has been retried  "
								+ numberOfRetries + " time(s)");
						/***
						 * Fail if the transaction has been retired for
						 * maxNumberOfRetries
						 **/
						if (numberOfRetries >= maxNumberOfRetries) {
							logger.warn("Recalulate interest job has been retried for the max allowed attempts of "
									+ numberOfRetries
									+ " and will be rolled back");
							sb.append("Recalulate interest job has been retried for the max allowed attempts of "
									+ numberOfRetries
									+ " and will be rolled back");
							break;
						}
						/***
						 * Else sleep for a random time (between 1 to 10
						 * seconds) and continue
						 **/
						try {
							Random random = new Random();
							int randomNum = random
									.nextInt(maxIntervalBetweenRetries + 1);
							Thread.sleep(1000 + (randomNum * 1000));
							numberOfRetries = numberOfRetries + 1;
						} catch (InterruptedException e) {
							sb.append("Interest recalculation for loans failed " + exception.getMessage()) ;
							break;
						}
					} catch (JournalEntryInvalidException je) {
						String exceptionMessage = je.getDefaultUserMessage();
						
						logger.error("Interest recalculation for loans failed for account:"	+ loanId + " with message - \""
								+ exceptionMessage + "\"");
						sb.append("Interest recalculation for loans failed for account:").append(loanId).append(" with message - \"")
                        .append(exceptionMessage + "\"");
					} catch (Exception e) {
						Throwable realCause = e;
						if (e.getCause() != null) {
							realCause = e.getCause();
						}
						logger.error("Interest recalculation for loans failed for account:"	+ loanId + " with message "
								+ realCause.getMessage());
						sb.append("Interest recalculation for loans failed for account:").append(loanId).append(" with message ")
                        .append(realCause.getMessage());
					}
					i++;
				}
				logger.info("Loans count " + i);
			}
			if (sb.length() > 0) {
				throw new JobExecutionException(sb.toString());
			}
		}

	}

}
