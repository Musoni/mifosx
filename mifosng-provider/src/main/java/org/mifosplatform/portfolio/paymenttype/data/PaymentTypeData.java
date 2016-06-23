/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.portfolio.paymenttype.data;

public class PaymentTypeData {

    @SuppressWarnings("unused")
    private Long id;
    @SuppressWarnings("unused")
    private String name;
    @SuppressWarnings("unused")
    private String description;
    @SuppressWarnings("unused")
    private Boolean isCashPayment;
    @SuppressWarnings("unused")
    private Long position;
    @SuppressWarnings("unused")
    private boolean deleted;

    public PaymentTypeData(final Long id, final String name, final String description, final Boolean isCashPayment, 
            final Long position, final boolean deleted) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.isCashPayment = isCashPayment;
        this.position = position;
        this.deleted = deleted;
    }

    public static PaymentTypeData instance(final Long id, final String name, final String description, final Boolean isCashPayment,
            final Long position, final boolean deleted) {
        return new PaymentTypeData(id, name, description, isCashPayment, position, deleted);
    }

    public static PaymentTypeData instance(final Long id, final String name, final boolean deleted) {
        String description = null;
        Boolean isCashPayment = null;
        Long position = null;
        return new PaymentTypeData(id, name, description, isCashPayment, position, deleted);
    }
}
