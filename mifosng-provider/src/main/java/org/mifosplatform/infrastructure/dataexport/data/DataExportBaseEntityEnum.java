/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.data;

public enum DataExportBaseEntityEnum {

    CLIENT("client","m_client"),
    GROUP("group","m_group"),
    LOAN("loan","m_loan"),
    SAVINGSACCOUNT("savingsaccount","m_savings_account");

    private String name;

    public String getTablename() {
        return tableName;
    }

    public String getName() {
        return this.name;
    }

    private String tableName;

    DataExportBaseEntityEnum(String name, String tableName){
        this.name = name;
        this.tableName = tableName;
    }

    /**
     * @return true if enum is equal to DataExportBaseEntityEnum.CLIENT, else false
     */
    public boolean isClient(){return this.equals(CLIENT);}

    /**
     * @return true if enum is equal to DataExportBaseEntityEnum.GROUP, else false
     */
    public boolean isGroup(){return this.equals(GROUP);}

    /**
     * @return true if enum is equal to DataExportBaseEntityEnum.LOAN, else false
     */
    public boolean isLoan(){return this.equals(LOAN);}

    /**
     * @return true if enum is equal to DataExportBaseEntityEnum.SAVINGSACCOUNT, else false
     */
    public boolean isSavingsAccount(){return this.equals(SAVINGSACCOUNT);}

    public DataExportBaseEntityEnum fromTable(String tableName){
        DataExportBaseEntityEnum entity;
        switch (tableName){
            case "m_client": entity = CLIENT; break;
            case "m_group": entity = GROUP; break;
            case "m_loan": entity = LOAN; break;
            case "m_savings_account": entity = SAVINGSACCOUNT; break;
            default: entity = null;
        }
        return entity;
    }
}
