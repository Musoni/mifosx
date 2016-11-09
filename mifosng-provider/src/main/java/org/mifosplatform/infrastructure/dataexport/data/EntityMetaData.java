/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.data;

import java.util.List;

public class EntityMetaData {
    private final String name;
    private final List<EntityColumnMetaData> columns;
    
    /**
     * @param name
     * @param columns
     */
    private EntityMetaData(final String name, final List<EntityColumnMetaData> columns) {
        this.name = name;
        this.columns = columns;
    }
    
    /**
     * Creates a new {@link EntityMetaData} object
     * 
     * @param name
     * @param columns
     * @return {@link EntityMetaData} object
     */
    public static EntityMetaData newInstance(final String name, final List<EntityColumnMetaData> columns) {
        return new EntityMetaData(name, columns);
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the columns
     */
    public List<EntityColumnMetaData> getColumns() {
        return columns;
    }
}
