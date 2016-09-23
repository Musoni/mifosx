/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.scheduledemail.data;

/**
 * Immutable data object representing an Email configuration.
 */
public class EmailConfigurationData {
	@SuppressWarnings("unused")
	private final Long id;
	
	private final String name;
	
    private final String value;
	
	/** 
	 * @return an instance of the EmailConfigurationData class
	 **/
	public static EmailConfigurationData instance(Long id, String name, String value) {
		return new EmailConfigurationData(id, name, value);
	}
	
	/** 
	 * EmailConfigurationData constructor
	 **/
	private EmailConfigurationData(Long id, String name, String value) {
		this.id = id;
		this.name = name;
		this.value = value;
	}

	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getValue() {
		return value;
	}

}
