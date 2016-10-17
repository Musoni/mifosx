/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.data;

public enum DataExportCoreColumn {
	BRANCH("office_id", "Branch", "BIGINT", "m_office", "name"),
	LOAN_OFFICER("loan_officer_id", "Loan Officer", "BIGINT", "m_staff", "display_name"),
	GROUP_ID("group_id", "Group ID", "BIGINT", "m_group", "id"),
	GROUP_NAME("group_id", "Group Name", "BIGINT", "m_group", "display_name"),
	CLIENT_ID("client_id", "Client ID", "BIGINT", "m_client", "id"),
	CLIENT_NAME("client_id", "Client Name", "BIGINT", "m_client", "display_name"),
	DATE_OF_BIRTH("date_of_birth", "Date Of Birth", "DATE", "", "date_of_birth"),
	GENDER("gender_cv_id", "Gender", "BIGINT", "m_code_value", "code_value"),
	PHONE_NUMBER("mobile_no", "Phone Number", "VARCHAR", "", "mobile_no");
	
	private final String name;
	private final String label;
	private final String dataType;
	private final String referencedTableName;
	private final String selectExpressionColumnName;
	
	/**
	 * @param name
	 * @param label
	 */
	private DataExportCoreColumn(final String name, final String label, final String dataType, 
			final String referencedTableName, final String selectExpressionColumnName) {
		this.name = name;
		this.label = label;
		this.dataType = dataType;
		this.referencedTableName = referencedTableName;
		this.selectExpressionColumnName = selectExpressionColumnName;
	}
	
	/**
	 * Creates a new {@link DataExportCoreColumn} object
	 * 
	 * @param name name of column
	 * @return {@link DataExportCoreColumn} object
	 */
	public static DataExportCoreColumn newInstance(final String name) {
		DataExportCoreColumn dataExportCoreColumn = null;
		
		for (DataExportCoreColumn column : DataExportCoreColumn.values()) {
			if (column.name.equalsIgnoreCase(name)) {
				dataExportCoreColumn = column;
				
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
	 * @return the dataType
	 */
	public String getDataType() {
		return dataType;
	}

	/**
	 * @return the referencedTableName
	 */
	public String getReferencedTableName() {
		return referencedTableName;
	}

	/**
	 * @return the selectExpressionColumnName
	 */
	public String getSelectExpressionColumnName() {
		return selectExpressionColumnName;
	}
}
