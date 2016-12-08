/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.data;

import org.apache.commons.lang.StringUtils;

public enum MysqlDataType {
	UNKNOWN("UNKNOWN", MysqlDataTypeCategory.UNKNOWN),
	
	// =========================================================
	// ========== Numeric Datatypes =============
	CHAR("CHAR", MysqlDataTypeCategory.STRING), 
	VARCHAR("VARCHAR", MysqlDataTypeCategory.STRING),
	TINYTEXT("TINYTEXT", MysqlDataTypeCategory.STRING),
	TEXT("TEXT", MysqlDataTypeCategory.STRING),
	MEDIUMTEXT("MEDIUMTEXT", MysqlDataTypeCategory.STRING),
	LONGTEXT("LONGTEXT", MysqlDataTypeCategory.STRING),
	BINARY("BINARY", MysqlDataTypeCategory.STRING),
	VARBINARY("VARBINARY", MysqlDataTypeCategory.STRING),
	// =========================================================
	
	// =========================================================
	// ========== Numeric Datatypes =============
	BIT("BIT", MysqlDataTypeCategory.NUMERIC), 
	TINYINT("TINYINT", MysqlDataTypeCategory.NUMERIC),
	SMALLINT("SMALLINT", MysqlDataTypeCategory.NUMERIC),
	MEDIUMINT("MEDIUMINT", MysqlDataTypeCategory.NUMERIC),
	INT("INT", MysqlDataTypeCategory.NUMERIC),
	INTEGER("INTEGER", MysqlDataTypeCategory.NUMERIC),
	BIGINT("BIGINT", MysqlDataTypeCategory.NUMERIC),
	DECIMAL("DECIMAL", MysqlDataTypeCategory.NUMERIC),
	DEC("DEC", MysqlDataTypeCategory.NUMERIC), 
	NUMERIC("NUMERIC", MysqlDataTypeCategory.NUMERIC),
	FIXED("FIXED", MysqlDataTypeCategory.NUMERIC),
	FLOAT("FLOAT", MysqlDataTypeCategory.NUMERIC),
	DOUBLE("DOUBLE", MysqlDataTypeCategory.NUMERIC),
	DOUBLE_PRECISION("DOUBLE PRECISION", MysqlDataTypeCategory.NUMERIC),
	REAL("REAL", MysqlDataTypeCategory.NUMERIC),
	BOOL("BOOL", MysqlDataTypeCategory.NUMERIC),
	BOOLEAN("BOOLEAN", MysqlDataTypeCategory.NUMERIC),
	// =========================================================
	
	// =========================================================
	// ========== Date/Time Datatypes =============
	DATE("DATE", MysqlDataTypeCategory.DATE_TIME), 
	DATETIME("DATETIME", MysqlDataTypeCategory.DATE_TIME),
	TIMESTAMP("TIMESTAMP", MysqlDataTypeCategory.DATE_TIME),
	TIME("TIME", MysqlDataTypeCategory.DATE_TIME),
	YEAR("YEAR", MysqlDataTypeCategory.DATE_TIME),
	// =========================================================
	
	// =========================================================
	// ========== Large Object (LOB) Datatypes =============
	TINYBLOB("TINYBLOB", MysqlDataTypeCategory.LARGE_OBJECT), 
	BLOB("BLOB", MysqlDataTypeCategory.LARGE_OBJECT),
	MEDIUMBLOB("MEDIUMBLOB", MysqlDataTypeCategory.LARGE_OBJECT);
	// =========================================================
	
	final String name;
	final MysqlDataTypeCategory category;
	
	/**
	 * @param name
	 * @param category
	 */
	private MysqlDataType(final String name, final MysqlDataTypeCategory category) {
		this.name = name;
		this.category = category;
	}
	
	/**
	 * Creates a new {@link MysqlDataType} object using the provided name
	 * 
	 * @param name mysql data type name
	 * @return {@link MysqlDataType} object
	 */
	public static MysqlDataType newInstance(final String name) {
		MysqlDataType newInstance = UNKNOWN;
		
		for (MysqlDataType mysqlDataType : MysqlDataType.values()) {
			if (StringUtils.equalsIgnoreCase(mysqlDataType.name, name)) {
				newInstance = mysqlDataType;
				
				break;
			}
		}
		
		return newInstance;
	}
	
	/**
	 * Checks if it is a numeric data type
	 * 
	 * @return boolean
	 */
	public boolean isNumeric() {
		return this.category.equals(MysqlDataTypeCategory.NUMERIC);
	}
	
	/**
	 * Checks if it is a string data type
	 * 
	 * @return boolean
	 */
	public boolean isString() {
		return this.category.equals(MysqlDataTypeCategory.STRING);
	}
	
	/**
	 * Checks if it is a boolean data type
	 * 
	 * @return boolean
	 */
	public boolean isBoolean() {
		return (this.equals(BOOL) || this.equals(BOOLEAN));
	}
	
	/**
	 * Checks if it is a date time data type
	 * 
	 * @return boolean
	 */
	public boolean isDateTime() {
		return this.category.equals(MysqlDataTypeCategory.DATE_TIME);
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the category
	 */
	public MysqlDataTypeCategory getCategory() {
		return category;
	}
}
