/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.scheduledemail.service;

import org.mifosplatform.infrastructure.core.service.Page;
import org.mifosplatform.infrastructure.scheduledemail.data.EmailData;

import java.util.Collection;
import java.util.Date;
import java.util.List;

public interface EmailReadPlatformService {

    Collection<EmailData> retrieveAll();

    EmailData retrieveOne(Long resourceId);
    
    Collection<EmailData> retrieveAllPending(Integer limit);
    
    Collection<EmailData> retrieveAllSent(Integer limit);
    
    Collection<EmailData> retrieveAllDelivered(Integer limit);
    
    Collection<EmailData> retrieveAllFailed(Integer limit);

    Page<EmailData> retrieveEmailByStatus(Integer limit, Integer status, Date dateFrom, Date dateTo);
    
    List<Long> retrieveExternalIdsOfAllSent(Integer limit);
}
