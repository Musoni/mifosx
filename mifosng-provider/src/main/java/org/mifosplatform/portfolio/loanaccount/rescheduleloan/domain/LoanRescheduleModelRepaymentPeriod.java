/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.portfolio.loanaccount.rescheduleloan.domain;

import java.math.BigDecimal;

import org.joda.time.LocalDate;
import org.mifosplatform.organisation.monetary.domain.Money;
import org.mifosplatform.portfolio.loanaccount.loanschedule.data.LoanSchedulePeriodData;

public final class LoanRescheduleModelRepaymentPeriod {
    private int number;
    private int oldNumber;
    private LocalDate fromDate;
    private LocalDate dueDate;
    private Money principalCharged;
    private Money interestCharged;
    private Money feeChargesCharged;
    private Money penaltyChargesCharged;
    private Money totalCharged;
    private Money outstandingLoanBalance;
    private boolean isNew;
    
    /**
	 * @param number
	 * @param oldNumber
	 * @param fromDate
	 * @param dueDate
	 * @param principalCharged
	 * @param interestCharged
	 * @param feeChargesCharged
	 * @param penaltyChargesCharged
	 * @param totalCharged
     * @param outstandingLoanBalance
	 * @param isNew
	 */
	private LoanRescheduleModelRepaymentPeriod(final int number, final int oldNumber, 
			final LocalDate fromDate, final LocalDate dueDate, final Money principalCharged, 
            final Money interestCharged, final Money feeChargesCharged, final Money penaltyChargesCharged, 
            final Money totalCharged, final Money outstandingLoanBalance, final boolean isNew) {
		this.number = number;
		this.oldNumber = oldNumber;
		this.fromDate = fromDate;
		this.dueDate = dueDate;
		this.principalCharged = principalCharged;
		this.interestCharged = interestCharged;
		this.feeChargesCharged = feeChargesCharged;
		this.penaltyChargesCharged = penaltyChargesCharged;
		this.totalCharged = totalCharged;
        this.outstandingLoanBalance = outstandingLoanBalance;
		this.isNew = isNew;
	}
    
    /**
	 * Creates a new {@link LoanRescheduleModelRepaymentPeriod} object
	 * 
	 * @param number
	 * @param oldNumber
	 * @param fromDate
	 * @param dueDate
	 * @param principalCharged
	 * @param interestCharged
	 * @param feeChargesCharged
	 * @param penaltyChargesCharged
	 * @param totalCharged
     * @param outstandingLoanBalance
	 * @param isNew
	 * @return {@link LoanRescheduleModelRepaymentPeriod} object
	 */
	public static LoanRescheduleModelRepaymentPeriod instance(final int number, final int oldNumber, 
			final LocalDate fromDate, final LocalDate dueDate, final Money principalCharged, 
			final Money interestCharged, final Money feeChargesCharged, final Money penaltyChargesCharged, 
			final Money totalCharged, final Money outstandingLoanBalance, final boolean isNew) {
		return new LoanRescheduleModelRepaymentPeriod(number, oldNumber, fromDate, dueDate, 
				principalCharged, interestCharged, feeChargesCharged, penaltyChargesCharged, 
                totalCharged, outstandingLoanBalance, isNew);
	}
    
    /**
     * Gets a {@link LoanSchedulePeriodData} representation of the {@link LoanRescheduleModelRepaymentPeriod} object
     */
    public LoanSchedulePeriodData toLoanSchedulePeriodData() {
    	return LoanSchedulePeriodData.repaymentOnlyPeriod(this.number, this.fromDate, this.dueDate, 
    			moneyToBigDecimal(this.getPrincipalCharged()), moneyToBigDecimal(this.getOutstandingLoanBalance()), 
    			moneyToBigDecimal(this.getInterestCharged()), moneyToBigDecimal(this.getFeeChargesCharged()),
    			moneyToBigDecimal(this.getPenaltyChargesCharged()), moneyToBigDecimal(this.getTotalCharged()), 
    			moneyToBigDecimal(this.getTotalInstallmentAmountForPeriod()));
    }

    /**
     * @return principal + interest for the period
     */
	private Money getTotalInstallmentAmountForPeriod() {
		Money amount = this.principalCharged;
		
		if (this.interestCharged != null && amount != null) {
			amount = amount.plus(this.interestCharged);
		}
		
		return amount;
	}
	/**
     * Convert Money object to BigDecimal properly handling null input values
     * 
     * @param moneyValue
     */
    public static BigDecimal moneyToBigDecimal(final Money moneyValue) {
        BigDecimal value = BigDecimal.ZERO;
        
        if (moneyValue != null) {
            value = moneyValue.getAmount();
        }
        
        return value;
    }

	/**
	 * @return the number
	 */
	public int getNumber() {
		return number;
	}

	/**
	 * @return the oldNumber
	 */
	public int getOldNumber() {
		return oldNumber;
	}

	/**
	 * @return the fromDate
	 */
	public LocalDate getFromDate() {
		return fromDate;
	}

	/**
	 * @return the dueDate
	 */
	public LocalDate getDueDate() {
		return dueDate;
	}

	/**
	 * @return the principalCharged
	 */
	public Money getPrincipalCharged() {
		return principalCharged;
	}

	/**
	 * @return the interestCharged
	 */
	public Money getInterestCharged() {
		return interestCharged;
	}

	/**
	 * @return the feeChargesCharged
	 */
	public Money getFeeChargesCharged() {
		return feeChargesCharged;
	}

	/**
	 * @return the penaltyChargesCharged
	 */
	public Money getPenaltyChargesCharged() {
		return penaltyChargesCharged;
	}

	/**
	 * @return the totalCharged
	 */
	public Money getTotalCharged() {
		return totalCharged;
	}

	/**
	 * @return the outstandingLoanBalance
	 */
	public Money getOutstandingLoanBalance() {
		return outstandingLoanBalance;
	}

	/**
	 * @return the isNew
	 */
	public boolean isNew() {
		return isNew;
	}
	
	/**
	 * Updates the number
	 * 
	 * @param number
	 */
	public void updateNumber(final Integer number) {
        this.number = number;
    }

	/**
	 * Updates the oldNumber
	 * 
	 * @param oldNumber
	 */
    public void updateOldNumber(final Integer oldNumber) {
        this.oldNumber = oldNumber;
    }

    /**
     * Updates the fromDate
     * 
     * @param periodFromDate
     */
    public void updateFromDate(final LocalDate fromDate) {
        this.fromDate = fromDate;
    }

    /**
     * Updates the dueDate
     * 
     * @param dueDate
     */
    public void updateDueDate(final LocalDate dueDate) {
        this.dueDate = dueDate;
    }
    
    /**
     * Updates the principalCharged
     * 
     * @param principalCharged
     */
    public void updatePrincipalCharged(final Money principalCharged) {
    	this.principalCharged = principalCharged;
    }
    
    /**
     * Updates the interestCharged
     * 
     * @param interestCharged
     */
    public void updateInterestCharged(final Money interestCharged) {
        this.interestCharged = interestCharged;
    }
    
    /**
     * Updates the feeChargesCharged
     * 
     * @param feeChargesCharged
     */
    public void updateFeeChargesCharged(final Money feeChargesCharged) {
        this.feeChargesCharged = feeChargesCharged;
    }
    
    /**
     * Updates the penaltyChargesCharged
     * 
     * @param penaltyChargesCharged
     */
    public void updatePenaltyChargesCharged(final Money penaltyChargesCharged) {
        this.penaltyChargesCharged = penaltyChargesCharged;
    }

    /**
     * Updates the outstandingLoanBalance
     * 
     * @param outstandingLoanBalance
     */
    public void updateOutstandingLoanBalance(final Money outstandingLoanBalance) {
        this.outstandingLoanBalance = outstandingLoanBalance;
    }
    
    /**
     * Updates the totalCharged
     * 
     * @param totalCharged
     */
    public void updateTotalCharged(final Money totalCharged) {
        this.totalCharged = totalCharged;
    }

    /**
     * updates the isNew boolean
     * 
     * @param isNew
     */
    public void updateIsNew(final boolean isNew) {
        this.isNew = isNew;
    }
}