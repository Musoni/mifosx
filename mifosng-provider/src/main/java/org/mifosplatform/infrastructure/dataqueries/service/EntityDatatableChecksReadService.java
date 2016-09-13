/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataqueries.service;

import org.mifosplatform.infrastructure.dataqueries.data.EntityDataTableChecksData;
import org.mifosplatform.infrastructure.dataqueries.data.EntityDataTableChecksTemplateData;

import java.util.List;

public interface EntityDatatableChecksReadService {

    EntityDataTableChecksTemplateData retrieveTemplate();
    List<EntityDataTableChecksData> retrieveAll(Long status,String entity,Long productLoanId);

}