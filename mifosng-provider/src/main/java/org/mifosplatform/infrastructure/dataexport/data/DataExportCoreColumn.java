/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.data;

public enum DataExportCoreColumn {
	BRANCH_NAME("branch_name", "branch name", "VARCHAR", true, "office_id", "name", "m_office", null),
	LOAN_OFFICER_NAME("loan_officer_name", "loan officer name", "VARCHAR", true, "loan_officer_id", "display_name", "m_staff", null),
	FIELD_OFFICER_NAME("field_officer_name", "field officer name", "VARCHAR", true, "field_officer_id", "display_name", "m_staff", null),
	STAFF_NAME("staff_name", "staff name", "VARCHAR", true, "staff_id", "display_name", "m_staff", null),
	GROUP_NAME("group_name", "group name", "VARCHAR", true, "group_id", "display_name", "m_group", null),
	CLIENT_NAME("client_name", "client name", "VARCHAR", true, "client_id", "display_name", "m_client", null),
	LOAN_PRODUCT_NAME("loan_product_name", "product name", "VARCHAR", true, "product_id", "name", "m_product_loan", DataExportBaseEntity.LOAN),
	SAVINGS_PRODUCT_NAME("savings_product_name", "product name", "VARCHAR", true, "product_id", "name", "m_savings_product", DataExportBaseEntity.SAVINGSACCOUNT);
	
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
