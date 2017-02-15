/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.data;

public enum DataExportCoreDatatable {
	GUARANTORS("m_guarantor", "Loan Guarantors", DataExportBaseEntity.LOAN),
	LOAN_CHARGES("m_loan_charge", "Loan Charges", DataExportBaseEntity.LOAN), 
	SAVINGS_ACCOUNT_CHARGES("m_savings_account_charge", "Savings Account Charges", DataExportBaseEntity.SAVINGS_ACCOUNT), 
	LOAN_COLLATERALS("m_loan_collateral", "Loan Collateral", DataExportBaseEntity.LOAN);
	
	private final String tableName;
	private final String displayName;
	private final DataExportBaseEntity baseEntity;
	
	/**
	 * @param baseEntity
	 * @param tableName
	 * @param displayName
	 */
	private DataExportCoreDatatable(final String tableName, final String displayName, 
			final DataExportBaseEntity baseEntity) {
		this.baseEntity = baseEntity;
		this.tableName = tableName;
		this.displayName = displayName;
	}
	
	/**
	 * Creates a new {@link DataExportCoreDatatable} object
	 * 
	 * @param tableName
	 * @return {@link DataExportCoreDatatable} enum object
	 */
	public static DataExportCoreDatatable newInstance(final String tableName) {
		DataExportCoreDatatable dataExportCoreDatatable = null;
		
		for (DataExportCoreDatatable datatable : DataExportCoreDatatable.values()) {
			if (datatable.tableName.equalsIgnoreCase(tableName)) {
				dataExportCoreDatatable = datatable;
				
				break;
			}
		}
		
		return dataExportCoreDatatable;
	}

	/**
	 * @return the baseEntity
	 */
	public DataExportBaseEntity getBaseEntity() {
		return baseEntity;
	}

	/**
	 * @return the tableName
	 */
	public String getTableName() {
		return tableName;
	}

	/**
	 * @return the displayName
	 */
	public String getDisplayName() {
		return displayName;
	}
}
