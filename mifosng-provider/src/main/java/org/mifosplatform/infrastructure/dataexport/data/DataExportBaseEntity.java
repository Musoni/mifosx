/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.data;

public enum DataExportBaseEntity {
    INVALID("invalid", "invalid"),
    CLIENT("client", "m_client"),
    GROUP("group", "m_group"),
    LOAN("loan", "m_loan"),
    SAVINGSACCOUNT("savingsaccount", "m_savings_account");
    
    private String entityName;
    private String tableName;
    
    /**
     * @param entityName
     * @param tableName
     */
    private DataExportBaseEntity(final String entityName, final String tableName) {
        this.entityName = entityName;
        this.tableName = tableName;
    }
    
    /**
     * Creates a new {@link DataExportBaseEntity} object
     * 
     * @param entityName
     * @return {@link DataExportBaseEntity} object
     */
    public static DataExportBaseEntity fromEntityName(final String entityName) {
        DataExportBaseEntity dataExportBaseEntity = INVALID;
        
        if (CLIENT.entityName.equalsIgnoreCase(entityName)) {
            dataExportBaseEntity = CLIENT;
        } else if (GROUP.entityName.equals(entityName)) {
            dataExportBaseEntity = GROUP;
        } else if (LOAN.entityName.equals(entityName)) {
            dataExportBaseEntity = LOAN;
        } else if (SAVINGSACCOUNT.entityName.equals(entityName)) {
            dataExportBaseEntity = SAVINGSACCOUNT;
        }
        
        return dataExportBaseEntity;
    }
    
    /**
     * Creates a new {@link DataExportBaseEntity} object
     * 
     * @param entityName
     * @return {@link DataExportBaseEntity} object
     */
    public static DataExportBaseEntity fromTableName(final String tableName) {
        DataExportBaseEntity dataExportBaseEntity = INVALID;
        
        if (CLIENT.tableName.equalsIgnoreCase(tableName)) {
            dataExportBaseEntity = CLIENT;
        } else if (GROUP.tableName.equals(tableName)) {
            dataExportBaseEntity = GROUP;
        } else if (LOAN.tableName.equals(tableName)) {
            dataExportBaseEntity = LOAN;
        } else if (SAVINGSACCOUNT.tableName.equals(tableName)) {
            dataExportBaseEntity = SAVINGSACCOUNT;
        }
        
        return dataExportBaseEntity;
    }

    /**
     * @return the entityName
     */
    public String getEntityName() {
        return entityName;
    }

    /**
     * @return the tableName
     */
    public String getTableName() {
        return tableName;
    }
    
    /**
     * @return boolean
     */
    public boolean isValid() {
        return !this.isInValid();
    }
    
    /**
     * @return boolean
     */
    public boolean isInValid() {
        return this.equals(INVALID);
    }
}
