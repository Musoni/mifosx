/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataqueries.data;

import org.mifosplatform.portfolio.loanproduct.data.LoanProductData;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * Immutable data object for role data.
 */
public class EntityDataTableChecksTemplateData implements Serializable {

    private final List<String> entities;
    private final List<DatatableCheckStatusData> status;
    private final List<DatatableCheksData> datatables;
    private final Collection<LoanProductData> loanProductDatas;


     public EntityDataTableChecksTemplateData(final List<String> entities, List<DatatableCheckStatusData> status,List<DatatableCheksData> datatables, Collection<LoanProductData> loanProductDatas) {

        this.entities = entities;
        this.status = status;
        this.datatables =datatables;this.loanProductDatas = loanProductDatas;
    }


}