/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.portfolio.savings.domain;

import org.mifosplatform.portfolio.charge.domain.Charge;
import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.*;

@Entity
@Table(name="m_savings_product_add_charge_to_existing_accounts")
public class ApplyChargesToExistingSavingsAccount extends AbstractPersistable<Long> {

    @ManyToOne
    @JoinColumn(name = "savings_product_id", nullable = false)
    private SavingsProduct savingsProduct;

    @ManyToOne(optional = false)
    @JoinColumn(name = "charge_id", nullable = false)
    private Charge productCharge;

    @Column(name = "apply_charge_to_existing_savings_account", nullable = false)
    private boolean applyChargeToExistingSavingsAccount;

    protected ApplyChargesToExistingSavingsAccount() {
    }

    public ApplyChargesToExistingSavingsAccount(final SavingsProduct savingsProduct, final Charge productCharge, final boolean applyChargeToExistingSavingsAccount) {
        this.savingsProduct = savingsProduct;
        this.productCharge = productCharge;
        this.applyChargeToExistingSavingsAccount = applyChargeToExistingSavingsAccount;
    }

    public SavingsProduct getSavingsProduct() {
        return this.savingsProduct;
    }

    public Charge getProductCharge() {
        return this.productCharge;
    }

    public boolean isApplyChargeToExistingSavingsAccount() {
        return this.applyChargeToExistingSavingsAccount;
    }

    public void updateSavingsProduct(final SavingsProduct product){
        this.savingsProduct = product;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        ApplyChargesToExistingSavingsAccount that = (ApplyChargesToExistingSavingsAccount) o;

        if (applyChargeToExistingSavingsAccount != that.applyChargeToExistingSavingsAccount) return false;
        if (savingsProduct != null ? !savingsProduct.equals(that.savingsProduct) : that.savingsProduct != null)
            return false;
        return productCharge != null ? productCharge.equals(that.productCharge) : that.productCharge == null;

    }


}
