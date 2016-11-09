/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.scheduledemail.domain;

import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "scheduled_email_configuration")
public class EmailConfiguration extends AbstractPersistable<Long> {
	@Column(name = "name", nullable = false)
    private String name;
	
	@Column(name = "value", nullable = false)
    private String value;
	
	/** 
	 * EmailConfiguration constructor
	 **/
	protected EmailConfiguration() {}
	
	/** 
	 * EmailConfiguration constructor
	 **/
	public EmailConfiguration(String name, String value) {
		this.name = name;
		this.value = value;
	}
	
	public void setValue(String value) {
		this.value = value;
	}

	public String getName() {
		return this.name;
	}

	public String getValue() {
		return this.value;
	}
}
