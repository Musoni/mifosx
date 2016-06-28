/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.data;

public class DataExportFieldData {
    private final Long id;
    private final String entityTable;
    private final String entityFieldName;
    private final String entityJsonParam;
    private final String entityFieldLabel;
    private final String referenceTable;
    private final String referenceField;
    
    /**
     * @param id
     * @param entityTable
     * @param entityFieldName
     * @param entityJsonParam
     * @param entityFieldLabel
     * @param referenceTable
     * @param referenceField
     */
    private DataExportFieldData(final Long id, final String entityTable, final String entityFieldName, 
            final String entityJsonParam, final String entityFieldLabel, final String referenceTable, 
            final String referenceField) {
        this.id = id;
        this.entityTable = entityTable;
        this.entityFieldName = entityFieldName;
        this.entityJsonParam = entityJsonParam;
        this.entityFieldLabel = entityFieldLabel;
        this.referenceTable = referenceTable;
        this.referenceField = referenceField;
    }
    
    /**
     * Creates a new {@link DataExportFieldData} object
     * 
     * @param id
     * @param entityTable
     * @param entityFieldName
     * @param entityJsonParam
     * @param entityFieldLabel
     * @param referenceTable
     * @param referenceField
     * @return
     */
    public static DataExportFieldData newInstance(final Long id, final String entityTable, 
            final String entityFieldName, final String entityJsonParam, final String entityFieldLabel, 
            final String referenceTable, final String referenceField) {
        return new DataExportFieldData(id, entityTable, entityFieldName, entityJsonParam, 
                entityFieldLabel, referenceTable, referenceField);
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @return the entityTable
     */
    public String getEntityTable() {
        return entityTable;
    }

    /**
     * @return the entityFieldName
     */
    public String getEntityFieldName() {
        return entityFieldName;
    }

    /**
     * @return the entityJsonParam
     */
    public String getEntityJsonParam() {
        return entityJsonParam;
    }

    /**
     * @return the entityFieldLabel
     */
    public String getEntityFieldLabel() {
        return entityFieldLabel;
    }

    /**
     * @return the referenceTable
     */
    public String getReferenceTable() {
        return referenceTable;
    }

    /**
     * @return the referenceField
     */
    public String getReferenceField() {
        return referenceField;
    }
}
