/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.data;

/**
 * TODO - Add an id property that will uniquely identifier each core column 
 */
public enum DataExportCoreColumn {
	BRANCH_NAME("branch_name", "branch name", "VARCHAR", true, "office_id", "name", "m_office", null),
	LOAN_OFFICER_NAME("loan_officer_name", "loan officer name", "VARCHAR", true, "loan_officer_id", "display_name", "m_staff", null),
	GROUP_NAME("group_name", "group name", "VARCHAR", true, "group_id", "display_name", "m_group", null),
	GROUP_ID("group_id", "group id", "BIGINT", true, "group_id", "id", "m_group", null),
	CLIENT_NAME("client_name", "client name", "VARCHAR", true, "client_id", "display_name", "m_client", null),
	CLIENT_ID("client_id", "client id", "BIGINT", true, "client_id", "id", "m_client", null),
	DATE_OF_BIRTH("date_of_birth", "date of birth", "DATE", true, null, null, null, null),
	GENDER("gender_cv_id", "gender", "VARCHAR", true, "gender_cv_id", "code_value", "m_code_value", null),
	PHONE_NUMBER("mobile_no", "phone number", "VARCHAR", true, null, null, null, null),
	STAFF_NAME("staff_name", "staff name", "VARCHAR", true, "staff_id", "display_name", "m_staff", null),
	LOAN_TRANSACTION_CREATED_BY("loan_transaction_created_by", "created by", "VARCHAR", true, "appuser_id", "username", "m_appuser", DataExportBaseEntity.LOAN_TRANSACTION),
	LOAN_TRANSACTION_INTEREST_ACCRUED("loan_transaction_interest_accrued", "interest accrued", "DECIMAL", true, null, null, null, DataExportBaseEntity.LOAN_TRANSACTION),
	LOAN_TRANSACTION_INTEREST_WAIVED("loan_transaction_interest_waived", "interest waived", "DECIMAL", true, null, null, null, DataExportBaseEntity.LOAN_TRANSACTION),
	LOAN_TRANSACTION_TRANSFER_AMOUNT("loan_transaction_transfer_amount", "transfer amount", "DECIMAL", true, null, null, null, DataExportBaseEntity.LOAN_TRANSACTION),
	LOAN_TRANSACTION_PRODUCT_SHORT_NAME("loan_transaction_product_short_name", "product short name", "VARCHAR", true, "product_id", "short_name", "m_product_loan", DataExportBaseEntity.LOAN_TRANSACTION),
	LOAN_TRANSACTION_PRODUCT_NAME("loan_transaction_product_name", "product name", "VARCHAR", true, "product_id", "name", "m_product_loan", DataExportBaseEntity.LOAN_TRANSACTION),
	LOAN_TRANSACTION_PRODUCT_ID("loan_transaction_product_id", "product id", "BIGINT", true, "product_id", "id", "m_product_loan", DataExportBaseEntity.LOAN_TRANSACTION),
	LOAN_TRANSACTION_LOAN_ACCOUNT_NUMBER("loan_transaction_loan_account_number", "account number", "VARCHAR", true, "loan_id", "account_no", "m_loan", DataExportBaseEntity.LOAN_TRANSACTION),
	LOAN_TRANSACTION_PAYMENT_CHANNEL("loan_transaction_payment_channel", "payment channel", "VARCHAR", true, "payment_type_id", "value", "m_payment_type", DataExportBaseEntity.LOAN_TRANSACTION),
	LOAN_TRANSACTION_REFERENCE("loan_transaction_reference", "reference", "VARCHAR", true, "payment_detail_id", "receipt_number", "m_payment_detail", DataExportBaseEntity.LOAN_TRANSACTION),
	LOAN_TRANSACTION_TOTAL_REPAID("loan_transaction_total_repaid", "total repaid", "DECIMAL", true, null, null, null, DataExportBaseEntity.LOAN_TRANSACTION),
	LOAN_TRANSACTION_PRINCIPAL_REPAID("principal_portion_derived", "principal repaid", "DECIMAL", true, null, null, null, DataExportBaseEntity.LOAN_TRANSACTION),
	LOAN_TRANSACTION_INTEREST_REPAID("interest_portion_derived", "interest repaid", "DECIMAL", true, null, null, null, DataExportBaseEntity.LOAN_TRANSACTION),
	LOAN_TRANSACTION_FEES_REPAID("fee_charges_portion_derived", "fees repaid", "DECIMAL", true, null, null, null, DataExportBaseEntity.LOAN_TRANSACTION),
	LOAN_TRANSACTION_PENALTIES_REPAID("penalty_charges_portion_derived", "penalties repaid", "DECIMAL", true, null, null, null, DataExportBaseEntity.LOAN_TRANSACTION),
	LOAN_TRANSACTION_OVERPAYMENT_REPAID("overpayment_portion_derived", "overpayment repaid", "DECIMAL", true, null, null, null, DataExportBaseEntity.LOAN_TRANSACTION),
	LOAN_TRANSACTION_TOTAL_RECOVERED(DataExportEntityColumnName.RECOVERED_PORTION_DERIVED, "total recovered", "DECIMAL", true, null, null, null, DataExportBaseEntity.LOAN_TRANSACTION),
	SAVINGS_TRANSACTION_CREATED_BY("savings_transaction_created_by", "created by", "VARCHAR", true, "appuser_id", "username", "m_appuser", DataExportBaseEntity.SAVINGS_ACCOUNT_TRANSACTION),
	SAVINGS_TRANSACTION_DEPOSIT("savings_transaction_deposit", "deposit", "DECIMAL", true, null, null, null, DataExportBaseEntity.SAVINGS_ACCOUNT_TRANSACTION),
	SAVINGS_TRANSACTION_WITHDRAWAL("savings_transaction_withdrawal", "withdrawal", "DECIMAL", true, null, null, null, DataExportBaseEntity.SAVINGS_ACCOUNT_TRANSACTION),
	SAVINGS_TRANSACTION_INTEREST_POSTING("savings_transaction_interest_posting", "interest posting", "DECIMAL", true, null, null, null, DataExportBaseEntity.SAVINGS_ACCOUNT_TRANSACTION),
	SAVINGS_TRANSACTION_CHARGE_APPLIED("savings_transaction_charge_applied", "charges applied", "DECIMAL", true, null, null, null, DataExportBaseEntity.SAVINGS_ACCOUNT_TRANSACTION),
	SAVINGS_TRANSACTION_CHARGE_WAIVED("savings_transaction_charge_waived", "charges waived", "DECIMAL", true, null, null, null, DataExportBaseEntity.SAVINGS_ACCOUNT_TRANSACTION),
	SAVINGS_TRANSACTION_TRANSFER_AMOUNT("savings_transaction_transfer_amount", "transfer amount", "DECIMAL", true, null, null, null, DataExportBaseEntity.SAVINGS_ACCOUNT_TRANSACTION),
	SAVINGS_TRANSACTION_PRODUCT_SHORT_NAME("savings_transaction_product_short_name", "product short name", "VARCHAR", true, "product_id", "short_name", "m_savings_product", DataExportBaseEntity.SAVINGS_ACCOUNT_TRANSACTION),
	SAVINGS_TRANSACTION_PRODUCT_NAME("savings_transaction_product_name", "product name", "VARCHAR", true, "product_id", "name", "m_savings_product", DataExportBaseEntity.SAVINGS_ACCOUNT_TRANSACTION),
	SAVINGS_TRANSACTION_PRODUCT_ID("savings_transaction_product_id", "product id", "BIGINT", true, "product_id", "id", "m_savings_product", DataExportBaseEntity.SAVINGS_ACCOUNT_TRANSACTION),
	SAVINGS_TRANSACTION_ACCOUNT_NUMBER("savings_transaction_account_number", "account number", "VARCHAR", true, "savings_account_id", "account_no", "m_savings_account", DataExportBaseEntity.SAVINGS_ACCOUNT_TRANSACTION),
	SAVINGS_TRANSACTION_PAYMENT_CHANNEL("savings_transaction_payment_channel", "payment channel", "VARCHAR", true, "payment_type_id", "value", "m_payment_type", DataExportBaseEntity.SAVINGS_ACCOUNT_TRANSACTION),
	SAVINGS_TRANSACTION_REFERENCE("savings_transaction_reference", "reference", "VARCHAR", true, "payment_detail_id", "receipt_number", "m_payment_detail", DataExportBaseEntity.SAVINGS_ACCOUNT_TRANSACTION),
	REPAYMENT_SCHEDULE_LOAN_ACCOUNT_NUMBER("repayment_schedule_loan_account_number", "account number", "VARCHAR", true, "loan_id", "account_no", "m_loan", DataExportBaseEntity.LOAN_REPAYMENT_SCHEDULE),
	REPAYMENT_SCHEDULE_PRODUCT_SHORT_NAME("repayment_schedule_product_short_name", "product short name", "VARCHAR", true, "product_id", "short_name", "m_product_loan", DataExportBaseEntity.LOAN_REPAYMENT_SCHEDULE),
	REPAYMENT_SCHEDULE_PRODUCT_NAME("repayment_schedule_product_name", "product name", "VARCHAR", true, "product_id", "name", "m_product_loan", DataExportBaseEntity.LOAN_REPAYMENT_SCHEDULE),
	REPAYMENT_SCHEDULE_PRODUCT_ID("repayment_schedule_product_id", "product id", "BIGINT", true, "product_id", "id", "m_product_loan", DataExportBaseEntity.LOAN_REPAYMENT_SCHEDULE),
	REPAYMENT_SCHEDULE_TOTAL_EXPECTED("repayment_schedule_total_expected", "total expected", "DECIMAL", true, null, null, null, DataExportBaseEntity.LOAN_REPAYMENT_SCHEDULE),
	REPAYMENT_SCHEDULE_PRINCIPAL_EXPECTED("repayment_schedule_principal_amount", "principal expected", "DECIMAL", true, null, null, null, DataExportBaseEntity.LOAN_REPAYMENT_SCHEDULE),
	REPAYMENT_SCHEDULE_INTEREST_EXPECTED("repayment_schedule_interest_amount", "interest expected", "DECIMAL", true, null, null, null, DataExportBaseEntity.LOAN_REPAYMENT_SCHEDULE),
	REPAYMENT_SCHEDULE_FEES_EXPECTED("repayment_schedule_fee_charges_amount", "fees expected", "DECIMAL", true, null, null, null, DataExportBaseEntity.LOAN_REPAYMENT_SCHEDULE),
	REPAYMENT_SCHEDULE_PENALTIES_EXPECTED("repayment_schedule_penalty_charges_amount", "penalties expected", "DECIMAL", true, null, null, null, DataExportBaseEntity.LOAN_REPAYMENT_SCHEDULE),
	REPAYMENT_SCHEDULE_PRINCIPAL_OUTSTANDING("repayment_schedule_principal_outstanding", "principal outstanding", "DECIMAL", true, null, null, null, DataExportBaseEntity.LOAN_REPAYMENT_SCHEDULE),
	REPAYMENT_SCHEDULE_INTEREST_OUTSTANDING("repayment_schedule_interest_outstanding", "interest outstanding", "DECIMAL", true, null, null, null, DataExportBaseEntity.LOAN_REPAYMENT_SCHEDULE),
	REPAYMENT_SCHEDULE_PENALTIES_OUTSTANDING("repayment_schedule_penalties_outstanding", "penalties outstanding", "DECIMAL", true, null, null, null, DataExportBaseEntity.LOAN_REPAYMENT_SCHEDULE),
	REPAYMENT_SCHEDULE_FEES_OUTSTANDING("repayment_schedule_fees_outstanding", "fees outstanding", "DECIMAL", true, null, null, null, DataExportBaseEntity.LOAN_REPAYMENT_SCHEDULE),
	REPAYMENT_SCHEDULE_TOTAL_OUTSTANDING("repayment_schedule_total_outstanding", "total outstanding", "DECIMAL", true, null, null, null, DataExportBaseEntity.LOAN_REPAYMENT_SCHEDULE),
	LOAN_ARREARS_DATE("overdue_since_date_derived", "arrears date", "DATE", true, "id", "overdue_since_date_derived", "m_loan_arrears_aging", DataExportBaseEntity.LOAN),
	LOAN_ARREARS_DAYS("arrears_days", "arrears days", "INTEGER", true, "id", "overdue_since_date_derived", "m_loan_arrears_aging", DataExportBaseEntity.LOAN),
	LOAN_ARREARS_AMOUNT("total_overdue_derived", "arrears amount", "DECIMAL", true, "id", "total_overdue_derived", "m_loan_arrears_aging", DataExportBaseEntity.LOAN),
	GROUP_LOAN_MEMBER_ALLOCATION_LOAN_ACCOUNT_NUMBER("group_loan_member_allocation_loan_account_number", "loan account number", "VARCHAR", true, "loan_id", "account_no", "m_loan", DataExportBaseEntity.GROUP_LOAN_MEMBER_ALLOCATION);
	
	private String name;
	private String label;
	private String type;
	private boolean nullable;
	private String foreignKeyIndexColumnName;
	private String referencedColumnName;
	private String referencedTableName;
	private DataExportBaseEntity baseEntity;
	
	/**
	 * @param name
	 * @param label
	 * @param type
	 * @param nullable
	 * @param foreignKeyIndexColumnName
	 * @param referencedTableName
	 * @param baseEntity
	 */
	private DataExportCoreColumn(final String name, final String label, 
			final String type, final boolean nullable, final String foreignKeyIndexColumnName, 
			final String referencedColumnName, final String referencedTableName, 
			final DataExportBaseEntity baseEntity) {
		this.name = name;
		this.label = label;
		this.type = type;
		this.nullable = nullable;
		this.foreignKeyIndexColumnName = foreignKeyIndexColumnName;
		this.referencedColumnName = referencedColumnName;
		this.referencedTableName = referencedTableName;
		this.baseEntity = baseEntity;
	}
	
	/**
	 * Creates a new {@link DataExportCoreColumn} object with name similar to the one provided
	 * 
	 * @param name column name
	 * @return {@link DataExportCoreColumn} object
	 */
	public static DataExportCoreColumn newInstanceFromName(final String name) {
		DataExportCoreColumn dataExportCoreColumn = null;
		
		for (DataExportCoreColumn coreColumn : DataExportCoreColumn.values()) {
			if (coreColumn.name.equalsIgnoreCase(name)) {
				dataExportCoreColumn = coreColumn;
				
				break;
			}
		}
		
		return dataExportCoreColumn;
	}
	
	/**
	 * Creates a new {@link DataExportCoreColumn} object with foreign key index column name similar 
	 * to the one provided
	 * 
	 * @param name column name
	 * @return {@link DataExportCoreColumn} object
	 */
	public static DataExportCoreColumn newInstanceFromForeignKeyIndexColumnName(final String name) {
		DataExportCoreColumn dataExportCoreColumn = null;
		
		for (DataExportCoreColumn coreColumn : DataExportCoreColumn.values()) {
			if (coreColumn.foreignKeyIndexColumnName.equalsIgnoreCase(name)) {
				dataExportCoreColumn = coreColumn;
				
				break;
			}
		}
		
		return dataExportCoreColumn;
	}
	
	/**
	 * Creates a new {@link DataExportCoreColumn} object with foreign key index column name similar 
	 * to the one provided
	 * 
	 * @param name column name
	 * @param baseEntity {@link DataExportBaseEntity} object
	 * @return {@link DataExportCoreColumn} object
	 */
	public static DataExportCoreColumn newInstanceFromForeignKeyIndexColumnName(final String name, 
			final DataExportBaseEntity baseEntity) {
		DataExportCoreColumn dataExportCoreColumn = null;
		
		for (DataExportCoreColumn coreColumn : DataExportCoreColumn.values()) {
			if (coreColumn.foreignKeyIndexColumnName.equalsIgnoreCase(name) && (baseEntity == coreColumn.baseEntity)) {
				dataExportCoreColumn = coreColumn;
				
				break;
			}
		}
		
		return dataExportCoreColumn;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the label
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @return the nullable
	 */
	public boolean isNullable() {
		return nullable;
	}

	/**
	 * @return the foreignKeyIndexColumnName
	 */
	public String getForeignKeyIndexColumnName() {
		return foreignKeyIndexColumnName;
	}

	/**
	 * @return the referencedColumnName
	 */
	public String getReferencedColumnName() {
		return referencedColumnName;
	}

	/**
	 * @return the referencedTableName
	 */
	public String getReferencedTableName() {
		return referencedTableName;
	}

	/**
	 * @return the baseEntity
	 */
	public DataExportBaseEntity getBaseEntity() {
		return baseEntity;
	}
}
