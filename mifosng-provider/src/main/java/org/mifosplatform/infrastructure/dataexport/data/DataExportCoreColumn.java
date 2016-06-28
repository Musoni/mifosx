/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.data;

public enum DataExportCoreColumn {
	BRANCH("office_id", "Branch", "m_office", "name"),
	LOAN_OFFICER("loan_officer_id", "Loan Officer", "m_staff", "display_name"),
	GROUP_NAME("group_id", "Group Name", "m_group", "display_name"),
	GROUP_ID("group_id", "Group ID", "m_group", "id"),
	CLIENT_NAME("client_id", "Client Name", "m_client", "display_name"),
	CLIENT_ID("client_id", "Client ID", "m_client", "id"),
	DATE_OF_BIRTH("date_of_birth", "Date Of Birth", "", "date_of_birth"),
	GENDER("gender_cv_id", "Gender", "m_code_value", "code_value"),
	PHONE_NUMBER("mobile_no", "Phone Number", "", "mobile_no");
	
	private final String name;
	private final String label;
	private final String referencedTableName;
	private final String selectExpressionColumnName;
	
	/**
	 * @param name
	 * @param label
	 */
	private DataExportCoreColumn(final String name, final String label, 
			final String referencedTableName, final String selectExpressionColumnName) {
		this.name = name;
		this.label = label;
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
	 * @return the referencedTableName
	 */
	public String getReferencedTableName() {
		return referencedTableName;
	}

	public String getSelectExpressionColumnName() {
		return selectExpressionColumnName;
	}
}
