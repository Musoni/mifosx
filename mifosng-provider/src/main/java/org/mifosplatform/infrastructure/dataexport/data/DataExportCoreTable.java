/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.data;

public enum DataExportCoreTable {
	M_OFFICE("m_office"), 
	M_CLIENT("m_client"), 
	M_GROUP("m_group"), 
	M_GROUP_CLIENT("m_group_client"), 
	M_SAVINGS_ACCOUNT("m_savings_account"), 
	M_STAFF("m_staff"), 
	M_CODE_VALUE("m_code_value"), 
	M_CODE("m_code"), 
	M_CHARGE("m_charge");
	
	private String name;

	/**
	 * @param name
	 */
	private DataExportCoreTable(final String name) {
		this.name = name;
	}
	
	/**
	 * Creates a new instance of {@link DataExportCoreTable} object
	 * 
	 * @param name table name
	 * @return {@link DataExportCoreTable} object
	 */
	public static DataExportCoreTable newInstance(final String name) {
		DataExportCoreTable dataExportCoreTable = null;
		
		for (DataExportCoreTable coreTable : DataExportCoreTable.values()) {
			if (coreTable.name.equalsIgnoreCase(name)) {
				dataExportCoreTable = coreTable;
				
				break;
			}
		}
		
		return dataExportCoreTable;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Creates a table alias
	 * 
	 * @param suffix integer that would be appended to the table name
	 * @return
	 */
	public String getAlias(final int suffix) {
		return this.name + suffix;
	}
}
