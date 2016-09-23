/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataqueries.data;

import java.io.Serializable;

/**
 * Immutable data object for role data.
 */
public class EntityDataTableChecksData implements Serializable {

    private final long id;
    private final String entity;
    private final Long  status;
    private final String  datatableName;
    private final String displayName;
    private final boolean systemDefined;
    private final Long order;
    private final Long productId;
    private final String productName;


     public EntityDataTableChecksData(final long id, final String entity, final long status, final String datatableName, final boolean systemDefined,final String displayName,final Long loanProductId, final String productName) {
        this.id = id;
        this.entity =entity;
        this.status =status;
         this.datatableName = datatableName;
         this.systemDefined = systemDefined;
         this.order = id;
         this.displayName = displayName;
         this.productId = loanProductId;
         this.productName = productName;
    }


}