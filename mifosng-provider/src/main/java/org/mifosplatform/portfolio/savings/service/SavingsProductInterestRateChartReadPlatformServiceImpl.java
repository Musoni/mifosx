/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.portfolio.savings.service;

import org.mifosplatform.portfolio.interestratechart.domain.InterestRateChartFields;
import org.mifosplatform.portfolio.savings.data.InterestRateCharts;
import org.mifosplatform.portfolio.savings.domain.SavingsProductInterestRateChart;
import org.mifosplatform.portfolio.savings.domain.SavingsProductInterestRateChartRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;

@Service
public class SavingsProductInterestRateChartReadPlatformServiceImpl  implements SavingsProductInterestRateChartReadPlatformService{

    final private SavingsProductInterestRateChartRepository savingsProductInterestRateChartRepository;

    @Autowired
    private SavingsProductInterestRateChartReadPlatformServiceImpl(final SavingsProductInterestRateChartRepository savingsProductInterestRateChartRepository) {
        this.savingsProductInterestRateChartRepository = savingsProductInterestRateChartRepository;
    }

    @Override
    public Collection<InterestRateCharts> retrieveOne(Long productId) {
        final Collection<SavingsProductInterestRateChart> interestRateCharts = this.savingsProductInterestRateChartRepository.findBySavingsProduct(productId);
        final Collection<InterestRateCharts> interestRateChartData = new ArrayList<>();
        if(!interestRateCharts.isEmpty()){
            for(SavingsProductInterestRateChart interest : interestRateCharts){
                InterestRateChartFields fields = interest.getChartFields();
                interestRateChartData.add(InterestRateCharts.createNew(interest.getId(),fields.getName(),fields.getDescription(),fields.getFromDate(),fields.getEndDate(),interest.getAnnualInterestRate(),interest.isApplyToExistingSavingsAccount()));
            }
        }
        return interestRateChartData;
    }
}
