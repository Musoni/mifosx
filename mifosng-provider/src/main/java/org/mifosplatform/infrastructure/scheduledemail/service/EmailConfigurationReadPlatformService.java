/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.scheduledemail.service;

import org.mifosplatform.infrastructure.scheduledemail.data.EmailConfigurationData;

import java.util.Collection;

public interface EmailConfigurationReadPlatformService {
	
	Collection<EmailConfigurationData> retrieveAll();

	EmailConfigurationData retrieveOne(String name);
}
