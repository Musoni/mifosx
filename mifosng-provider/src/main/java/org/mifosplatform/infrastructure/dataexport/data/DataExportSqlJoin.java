/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.data;

import org.apache.commons.lang.StringUtils;

/**
 * @see https://en.wikipedia.org/wiki/Foreign_key
 */
public class DataExportSqlJoin {
	private String id;
	private final DataExportCoreTable parentTable;
	private final DataExportCoreTable childTable;
	private final String sqlStatement;
	private final String parentTableAlias;
	private final String childTableAlias;
	
	/**
	 * @param id
	 * @param parentTable The table containing the candidate key
	 * @param childTable The table containing the foreign key
	 * @param sqlStatement
	 * @param parentTableAlias
	 * @param childTableAlias
	 */
	private DataExportSqlJoin(final String id, final DataExportCoreTable parentTable, 
			final DataExportCoreTable childTable, final String sqlStatement, 
			final String parentTableAlias, final String childTableAlias) {
		this.id = id;
		this.parentTable = parentTable;
		this.childTable = childTable;
		this.sqlStatement = sqlStatement;
		this.parentTableAlias = parentTableAlias;
		this.childTableAlias = childTableAlias;
	}
	
	/**
	 * Creates a new {@link DataExportSqlJoin} object
	 * 
	 * @param parentTable
	 * @param childTable
	 * @param sqlStatement
	 * @param parentTableAlias
	 * @param childTableAlias
	 * @return {@link DataExportSqlJoin} object
	 */
	public static final DataExportSqlJoin newInstance(final DataExportCoreTable parentTable, 
			final DataExportCoreTable childTable, final String sqlStatement, final String parentTableAlias, 
			final String childTableAlias) {
		final String id = createId(parentTable, childTable);
		
		return new DataExportSqlJoin(id, parentTable, childTable, sqlStatement, 
				parentTableAlias, childTableAlias);
	}
	
	/**
	 * Creates a new {@link DataExportSqlJoin} object
	 * 
	 * @param parentTable
	 * @param childTableName
	 * @param sqlStatement
	 * @param parentTableAlias
	 * @param childTableAlias
	 * @return {@link DataExportSqlJoin} object
	 */
	public static final DataExportSqlJoin newInstance(final DataExportCoreTable parentTable, 
			final String childTableName, final String sqlStatement, final String parentTableAlias, 
			final String childTableAlias) {
		final String id = createId(parentTable.getName(), childTableName);
		
		return new DataExportSqlJoin(id, parentTable, null, sqlStatement, 
				parentTableAlias, childTableAlias);
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return the parentTable
	 */
	public DataExportCoreTable getParentTable() {
		return parentTable;
	}

	/**
	 * @return the childTable
	 */
	public DataExportCoreTable getChildTable() {
		return childTable;
	}

	/**
	 * @return the sqlStatement
	 */
	public String getSqlStatement() {
		return sqlStatement;
	}

	/**
	 * @return the parentTableAlias
	 */
	public String getParentTableAlias() {
		return parentTableAlias;
	}

	/**
	 * @return the childTableAlias
	 */
	public String getChildTableAlias() {
		return childTableAlias;
	}
	
	/**
	 * Creates the object id by concatenating the parentTable and the childTable values
	 * 
	 * @param parentTable The table containing the candidate key
	 * @param childTable The table containing the foreign key
	 * @return id
	 */
	public static String createId(final DataExportCoreTable parentTable, 
			final DataExportCoreTable childTable) {
		return createId(parentTable.getName(), childTable.getName());
	}
	
	/**
	 * Creates an id string by concatenating the parent table name and child table name
	 * 
	 * @param parentTableName
	 * @param childTableName
	 * @return id
	 */
	public static String createId(final String parentTableName, final String childTableName) {
		return parentTableName + "_" + childTableName;
	}
	
	/**
	 * Updates the id of the object
	 * 
	 * @param id
	 */
	public void updateId(final String id) {
		if (StringUtils.isNotBlank(id)) {
			this.id = id;
		}
	}
}
