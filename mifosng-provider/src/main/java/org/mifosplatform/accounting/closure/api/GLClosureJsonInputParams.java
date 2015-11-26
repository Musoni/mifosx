/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.accounting.closure.api;

import java.util.HashSet;
import java.util.Set;

/***
 * Enum of all parameters passed in while creating/updating a loan product
 ***/
public enum GLClosureJsonInputParams {
    ID("id"), OFFICE_ID("officeId"), CLOSING_DATE("closingDate"), COMMENTS("comments"), LOCALE("locale"), DATE_FORMAT("dateFormat"),
    BOOK_OFF_INCOME_AND_EXPENSE("bookOffIncomeAndExpense"),EQUITY_GL_ACCOUNT_ID("equityGlAccountId"),CURRENCY_CODE("currencyCode");

    private final String value;

    private GLClosureJsonInputParams(final String value) {
        this.value = value;
    }

    private static final Set<String> values = new HashSet<>();
    static {
        for (final GLClosureJsonInputParams type : GLClosureJsonInputParams.values()) {
            values.add(type.value);
        }
    }

    public static Set<String> getAllValues() {
        return values;
    }

    @Override
    public String toString() {
        return name().toString().replaceAll("_", " ");
    }

    public String getValue() {
        return this.value;
    }
}