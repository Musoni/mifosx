/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.data;

public class EntityColumnMetaData {
    private final String name;
    private String label;
    private final String type;
    private final boolean isNullable;
    
    /**
     * @param name
     * @param label
     * @param type
     * @param isNullable
     */
    private EntityColumnMetaData(final String name, final String label, final String type, 
            final boolean isNullable) {
        this.name = name;
        this.label = label;
        this.type = type;
        this.isNullable = isNullable;
    }
    
    /**
     * Creates a new {@link EntityColumnMetaData} object
     * 
     * @param name
     * @param label
     * @param type
     * @param isNullable
     * @return {@link EntityColumnMetaData} object
     */
    public static EntityColumnMetaData newInstance(final String name, final String label, final String type, 
            final boolean isNullable) {
        return new EntityColumnMetaData(name, label, type, isNullable);
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @return the isNullable
     */
    public boolean isNullable() {
        return isNullable;
    }
    
    /**
     * Updates the value of the label property
     * 
     * @param label
     */
    public void updateLabel(final String label) {
    	this.label = label;
    }
}
