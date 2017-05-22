/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.data;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class DataExportEntityColumnName {
	public static final String TRANSFER_TO_OFFICE_ID = "transfer_to_office_id";
	public static final String VERSION = "version";
	public static final String IMAGE_ID = "image_id";
	public static final String ACCOUNT_TYPE_ENUM = "account_type_enum";
	public static final String DEPOSIT_TYPE_ENUM = "deposit_type_enum";
	public static final String SUB_STATUS = "sub_status";
	public static final String EXTERNAL_ID = "external_id";
	public static final String OFFICE_ID = "office_id";
	public static final String IS_ACCCOUNT_TRANSFER = "is_account_transfer";
	public static final String UNRECOGNIZED_INCOME_PORTION = "unrecognized_income_portion";
	public static final String SUSPENDED_INTEREST_PORTION_DERIVED = "suspended_interest_portion_derived";
	public static final String SUSPENDED_FEE_CHARGES_PORTION_DERIVED = "suspended_fee_charges_portion_derived";
	public static final String SUSPENDED_PENALTY_CHARGES_PORTION_DERIVED = "suspended_penalty_charges_portion_derived";
	public static final String OUTSTANDING_LOAN_BALANCE_DERIVED = "outstanding_loan_balance_derived";
	public static final String RECOVERED_PORTION_DERIVED = "recovered_portion_derived";
	public static final String PAYMENT_DETAIL_ID = "payment_detail_id";
	public static final String APPUSER_ID = "appuser_id";
	public static final String IS_REVERSED = "is_reversed";
	public static final String CREATED_DATE = "created_date";
	public static final String TRANSACTION_TYPE_ENUM = "transaction_type_enum";
	public static final String OVERDRAFT_AMOUNT_DERIVED = "overdraft_amount_derived";
	public static final String BALANCE_END_DATE_DERIVED = "balance_end_date_derived";
	public static final String BALANCE_NUMBER_OF_DAYS_DERIVED = "balance_number_of_days_derived";
	public static final String RUNNING_BALANCE_DERIVED = "running_balance_derived";
	public static final String CUMULATIVE_BALANCE_DERIVED = "cumulative_balance_derived";
	public static final String SAVINGS_ACCOUNT_ID = "savings_account_id";
	public static final String LOAN_ID = "loan_id";
	public static final String AMOUNT = "amount";
	public static final String ID = "id";
	public static final String TRANSACTION_ID = "transaction_id";
	public static final String FROMDATE = "fromdate";
	public static final String DUEDATE = "duedate";
	public static final String INSTALLMENT = "installment";
	public static final String PRINCIPAL_AMOUNT = "principal_amount";
	public static final String PRINCIPAL_COMPLETED_DERIVED = "principal_completed_derived";
	public static final String PRINCIPAL_WRITTENOFF_DERIVED = "principal_writtenoff_derived";
	public static final String INTEREST_AMOUNT = "interest_amount";
	public static final String INTEREST_COMPLETED_DERIVED = "interest_completed_derived";
	public static final String INTEREST_WRITTENOFF_DERIVED = "interest_writtenoff_derived";
	public static final String INTEREST_WAIVED_DERIVED = "interest_waived_derived";
	public static final String ACCRUAL_INTEREST_DERIVED = "accrual_interest_derived";
	public static final String SUSPENDED_INTEREST_DERIVED = "suspended_interest_derived";
	public static final String FEE_CHARGES_AMOUNT = "fee_charges_amount";
	public static final String FEE_CHARGES_COMPLETED_DERIVED = "fee_charges_completed_derived";
	public static final String FEE_CHARGES_WRITTENOFF_DERIVED = "fee_charges_writtenoff_derived";
	public static final String FEE_CHARGES_WAIVED_DERIVED = "fee_charges_waived_derived";
	public static final String ACCRUAL_FEE_CHARGES_DERIVED = "accrual_fee_charges_derived";
	public static final String SUSPENDED_FEE_CHARGES_DERIVED = "suspended_fee_charges_derived";
	public static final String PENALTY_CHARGES_AMOUNT = "penalty_charges_amount";
	public static final String PENALTY_CHARGES_COMPLETED_DERIVED = "penalty_charges_completed_derived";
	public static final String PENALTY_CHARGES_WRITTEN_DERIVED = "penalty_charges_writtenoff_derived";
	public static final String PENALTY_CHARGES_WAIVED_DERIVED = "penalty_charges_waived_derived";
	public static final String ACCRUAL_PENALTY_CHARGES_DERIVED = "accrual_penalty_charges_derived";
	public static final String SUSPENDED_PENALTY_CHARGES_DERIVED = "suspended_penalty_charges_derived";
	public static final String TOTAL_PAID_IN_ADVANCE_DERIVED = "total_paid_in_advance_derived";
	public static final String TOTAL_PAID_LATE_DERIVED = "total_paid_late_derived";
	public static final String COMPLETED_DERIVED = "completed_derived";
	public static final String OBLIGATIONS_MET_ON_DATE = "obligations_met_on_date";
	public static final String CREATED_BY_ID = "createdby_id";
	public static final String LAST_MODIFIED_DATE = "lastmodified_date";
	public static final String LAST_MODIFIED_BY_ID = "lastmodifiedby_id";
	public static final String RECALCULATED_INTEREST_COMPONENT = "recalculated_interest_component";
	public static final String PRINCIPAL_PORTION_DERIVED = "principal_portion_derived";
	public static final String INTEREST_PORTION_DERIVED = "interest_portion_derived";
	public static final String FEE_CHARGES_PORTION_DERIVED = "fee_charges_portion_derived";
	public static final String PENALTY_CHARGES_PORTION_DERIVED = "penalty_charges_portion_derived";
	public static final String OVERPAYMENT_PORTION_DERIVED = "overpayment_portion_derived";
	public static final String OBLIGATION_MET_ON_DATE = "obligations_met_on_date";
	public static final String TRANSACTION_DATE = "transaction_date";
	public static final String ACTIVATION_DATE = "activation_date";
	public static final String FULL_NAME = "fullname";
	
	public static final Set<String> COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS = new HashSet<>(Arrays.asList(
			TRANSFER_TO_OFFICE_ID, VERSION, IMAGE_ID, ACCOUNT_TYPE_ENUM, DEPOSIT_TYPE_ENUM, SUB_STATUS, FULL_NAME, TRANSACTION_ID));
}
