/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.accounting.producttoaccountmapping.domain;

import java.util.HashMap;
import java.util.Map;

public enum PortfolioProductType {
    INVALID(0, "accountType.invalid"),LOAN(1, "productType.loan"), SAVING(2, "productType.saving"), CLIENT(3, "productType.client"), PROVISIONING(4, "productType.provisioning"),CASHIERTRANSACTION(5, "productType.cashier");

    private final Integer value;
    private final String code;

    private PortfolioProductType(final Integer value, final String code) {
        this.value = value;
        this.code = code;
    }

    @Override
    public String toString() {
        return name().toString().replaceAll("_", " ");
    }

    public Integer getValue() {
        return this.value;
    }

    public String getCode() {
        return this.code;
    }

    private static final Map<Integer, PortfolioProductType> intToEnumMap = new HashMap<>();

    static {
        for (final PortfolioProductType type : PortfolioProductType.values()) {
            intToEnumMap.put(type.value, type);
        }
    }

    public static PortfolioProductType fromInt(final Integer i) {


         PortfolioProductType enumType = PortfolioProductType.INVALID;

        if(i!=null){

            enumType = intToEnumMap.get(Integer.valueOf(i));
        }

        return enumType;
    }


    public boolean isSavingProduct() {
        return this.value.equals(PortfolioProductType.SAVING.getValue());
    }

    public boolean isLoanProduct() {
        return this.value.equals(PortfolioProductType.LOAN.getValue());
    }

    public boolean isClient() {
        return this.value.equals(PortfolioProductType.CLIENT.getValue());
    }

    public boolean isCashierTransaction() {
        return this.value.equals(PortfolioProductType.CASHIERTRANSACTION.getValue());
    }

}
