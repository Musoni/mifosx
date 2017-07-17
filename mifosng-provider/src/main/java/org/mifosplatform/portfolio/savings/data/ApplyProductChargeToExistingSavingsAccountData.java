/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.portfolio.savings.data;

public class ApplyProductChargeToExistingSavingsAccountData {

    private final Long chargeId;

    private final boolean addChargeToExistingSavingsAccount;


    public ApplyProductChargeToExistingSavingsAccountData(Long chargeId, boolean addChargeToExistingSavingsAccount) {
        this.chargeId = chargeId;
        this.addChargeToExistingSavingsAccount = addChargeToExistingSavingsAccount;
    }

    public Long getChargeId() {
        return this.chargeId;
    }

    public boolean isAddChargeToExistingSavingsAccount() {
        return this.addChargeToExistingSavingsAccount;
    }
}
