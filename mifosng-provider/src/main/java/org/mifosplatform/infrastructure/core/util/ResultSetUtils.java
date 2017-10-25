/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.core.util;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.joda.time.LocalDate;
import org.mifosplatform.infrastructure.core.domain.JdbcSupport;

public class ResultSetUtils {
	/**
	 * Retrieves the value of the designated column in the current row of this ResultSet object as a long 
	 * in the Java programming language suppressing SQLException
	 * 
	 * @param rs
	 * @param columnLabel
	 * @return long value
	 */
	public static Long getLongSuppressSQLException(final ResultSet rs, final String columnLabel) {
		Long columnValue = null;
		
		try {
			columnValue = JdbcSupport.getLong(rs, columnLabel);
			
		} catch (SQLException ex) { }
		
		return columnValue;
	}
	
	/**
	 * Retrieves the value of the designated column in the current row of this ResultSet object as 
	 * an int in the Java programming language suppressing SQLException
	 * 
	 * @param rs
	 * @param columnLabel
	 * @return int value
	 */
	public static Integer getIntegerSuppressSQLException(final ResultSet rs, final String columnLabel) {
		Integer columnValue = null;
		
		try {
			columnValue = JdbcSupport.getInteger(rs, columnLabel);
			
		} catch (SQLException ex) { }
		
		return columnValue;
	}
	
	/**
	 * Retrieves the value of the designated column in the current row of this ResultSet object 
	 * as a boolean in the Java programming language suppressing SQLException
	 * 
	 * @param rs
	 * @param columnLabel
	 * @return boolean value
	 */
	public static boolean getBooleanSuppressSQLException(final ResultSet rs, final String columnLabel) {
		boolean columnValue = false;
		
		try {
			columnValue = rs.getBoolean(columnLabel);
			
		} catch (SQLException ex) { }
		
		return columnValue;
	}
	
	/**
	 * Retrieves the value of the designated column in the current row of this ResultSet object 
	 * as a String in the Java programming language suppressing SQLException
	 * 
	 * @param rs
	 * @param columnLabel
	 * @return string value
	 */
	public static String getStringSuppressSQLException(final ResultSet rs, final String columnLabel) {
		String columnValue = null;
		
		try {
			columnValue = rs.getString(columnLabel);
			
		} catch (SQLException ex) { }
		
		return columnValue;
	}
	
	/**
	 * Retrieves the value of the designated column in the current row of this ResultSet object 
	 * as a Joda LocalDate suppressing SQLException
	 * 
	 * @param rs
	 * @param columnLabel
	 * @return
	 */
	public static LocalDate getLocalDateSuppressSQLException(final ResultSet rs, final String columnLabel) {
		LocalDate columnValue = null;
		
		try {
			columnValue = JdbcSupport.getLocalDate(rs, columnLabel);
			
		} catch (SQLException ex) { }
		
		return columnValue;
	}
}
