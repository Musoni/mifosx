/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.data;

import java.util.Map;

public class DataExportCreateRequestData {
    private final String baseEntity;
    private final Map<String, String> filters;
    private final String[] datatables;
    
    /**
     * @param baseEntity
     * @param filters
     * @param datatables
     */
    public DataExportCreateRequestData(final String baseEntity, final Map<String, String> filters,
            final String[] datatables) {
        this.baseEntity = baseEntity;
        this.filters = filters;
        this.datatables = datatables;
    }

    /**
     * @return the baseEntity
     */
    public String getBaseEntity() {
        return baseEntity;
    }

    /**
     * @return the filters
     */
    public Map<String, String> getFilters() {
        return filters;
    }

    /**
     * @return the datatables
     */
    public String[] getDatatables() {
        return datatables;
    }
}
