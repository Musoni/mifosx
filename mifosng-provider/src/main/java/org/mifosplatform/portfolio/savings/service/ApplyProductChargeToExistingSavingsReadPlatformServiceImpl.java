/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.portfolio.savings.service;

import org.mifosplatform.portfolio.savings.data.ApplyProductChargeToExistingSavingsAccountData;
import org.mifosplatform.portfolio.savings.domain.ApplyChargesToExistingSavingsAccount;
import org.mifosplatform.portfolio.savings.domain.ApplyChargesToExistingSavingsAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;

@Service
public class ApplyProductChargeToExistingSavingsReadPlatformServiceImpl implements ApplyProductChargeToExistingSavingsReadPlatformService {

    private final ApplyChargesToExistingSavingsAccountRepository applyChargesToExistingSavingsAccountRepository;

    @Autowired
    private ApplyProductChargeToExistingSavingsReadPlatformServiceImpl(ApplyChargesToExistingSavingsAccountRepository applyChargesToExistingSavingsAccountRepository) {
        this.applyChargesToExistingSavingsAccountRepository = applyChargesToExistingSavingsAccountRepository;
    }

    @Override
    public Collection<ApplyProductChargeToExistingSavingsAccountData> retrieveAll(Long productId) {
        final Collection<ApplyChargesToExistingSavingsAccount> applyChargesToExistingSavingsAccounts = this.applyChargesToExistingSavingsAccountRepository.retrieveBySavingsProduct(productId);

        final Collection<ApplyProductChargeToExistingSavingsAccountData> applyChargeData = new ArrayList<>();
        if(applyChargesToExistingSavingsAccounts != null && applyChargesToExistingSavingsAccounts.size() > 0){
            for(final ApplyChargesToExistingSavingsAccount chargesToExistingSavingsAccount : applyChargesToExistingSavingsAccounts){
                ApplyProductChargeToExistingSavingsAccountData data = new ApplyProductChargeToExistingSavingsAccountData(chargesToExistingSavingsAccount.getProductCharge().getId(),chargesToExistingSavingsAccount.isApplyChargeToExistingSavingsAccount());
                applyChargeData.add(data);
            }
        }
        return applyChargeData;
    }

}
