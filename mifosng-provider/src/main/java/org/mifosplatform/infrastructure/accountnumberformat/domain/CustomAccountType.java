/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.accountnumberformat.domain;


public enum CustomAccountType {
    INVALID(0,"invalid"),LOAN_PRODUCT_SHORT_NAME(1,"loanProductShortName"), STAFF_ID(2,"staffId"),OFFICE_ID(3,"officeId"),
    CLIENT_ID(4,"clientId"),OFFICE_EXTERNAL_ID(5,"officeExternal_id"),CLIENT_TYPE(6,"individual"),
    SAVING_PRODUCT_SHORT_NAME(7,"savingsProductShortName"),LOAN_PRODUCT(8,"loanProduct"),SAVINGS_PRODUCT(9,"savingsProduct"),
    GROUP_TYPE(10,"group_type");

    private final Integer value;
    private final String code;

    private CustomAccountType(Integer value, String code) {
        this.value = value;
        this.code = code;
    }

    public Integer getValue() {return this.value;}

    public String getCode() {return this.code;}

    public static CustomAccountType fromInt(final Integer statusValue){
        CustomAccountType customAccountType = CustomAccountType.INVALID;
        switch(statusValue){
            case 1 :
                customAccountType = CustomAccountType.LOAN_PRODUCT_SHORT_NAME;
                break;
            case 2:
                customAccountType = CustomAccountType.STAFF_ID;
                break;
            case 3 :
                customAccountType =CustomAccountType.OFFICE_ID;
                break;
            case 4:
                customAccountType = CustomAccountType.CLIENT_ID;
                break;
            case 5 :
                customAccountType = CustomAccountType.OFFICE_EXTERNAL_ID;
                break;
            case 6 :
                customAccountType = CustomAccountType.CLIENT_ID;
                break;
            case 7:
                customAccountType = CustomAccountType.SAVING_PRODUCT_SHORT_NAME;
                break;
            case 8:
                customAccountType =CustomAccountType.LOAN_PRODUCT;
                break;
            case 9:
                customAccountType = CustomAccountType.SAVINGS_PRODUCT;
                break;
            case 10:
                customAccountType= CustomAccountType.GROUP_TYPE;
                break;
        }
        return customAccountType;
    }
}
