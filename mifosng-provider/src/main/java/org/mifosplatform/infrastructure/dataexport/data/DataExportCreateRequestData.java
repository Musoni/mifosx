/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.data;

import java.util.Map;

public class DataExportCreateRequestData {
	private final String name;
    private final String baseEntityName;
    private final Map<String, String> filters;
    private final String[] datatables;
    private final String[] columns;
    
    /**
     * @param name
     * @param baseEntityName
     * @param filters
     * @param datatables
     * @param columns
     */
    public DataExportCreateRequestData(final String name, final String baseEntityName, 
    		final Map<String, String> filters, final String[] datatables, final String[] columns) {
    	this.name = name;
        this.baseEntityName = baseEntityName;
        this.filters = filters;
        this.datatables = datatables;
        this.columns = columns;
    }

    /**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
     * @return the baseEntityName
     */
    public String getBaseEntityName() {
        return baseEntityName;
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

    public String[] getColumns() {
        return columns;
    }
}
