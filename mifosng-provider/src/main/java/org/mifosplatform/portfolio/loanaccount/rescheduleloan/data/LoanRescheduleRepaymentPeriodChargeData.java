/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.portfolio.loanaccount.rescheduleloan.data;

import org.mifosplatform.portfolio.loanaccount.domain.LoanCharge;

/**
 * Object contains the id and a {@link LoanCharge} object linked to a loan installment
 */
public class LoanRescheduleRepaymentPeriodChargeData {
	private final int installmentNumber;
	private final LoanCharge loanCharge;
	
	/**
	 * @param installmentNumber
	 * @param loanCharge
	 */
	private LoanRescheduleRepaymentPeriodChargeData(final int installmentNumber, final LoanCharge loanCharge) {
		this.installmentNumber = installmentNumber;
		this.loanCharge = loanCharge;
	}
	
	/**
	 * Creates a new {@link LoanRescheduleRepaymentPeriodChargeData} object
	 * 
	 * @param installmentNumber
	 * @param loanCharge
	 * @return {@link LoanRescheduleRepaymentPeriodChargeData} object
	 */
	public static LoanRescheduleRepaymentPeriodChargeData instance(final int installmentNumber, 
			final LoanCharge loanCharge) {
		return new LoanRescheduleRepaymentPeriodChargeData(installmentNumber, loanCharge);
	}

	/**
	 * @return the installmentNumber
	 */
	public int getInstallmentNumber() {
		return installmentNumber;
	}

	/**
	 * @return the loanCharge
	 */
	public LoanCharge getLoanCharge() {
		return loanCharge;
	}
}
