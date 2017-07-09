/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.portfolio.savings.exception;

import org.mifosplatform.infrastructure.core.exception.AbstractPlatformResourceNotFoundException;

public class SavingsProductInterestRateChartNotFoundException extends AbstractPlatformResourceNotFoundException {


    public SavingsProductInterestRateChartNotFoundException(final Long id) {
        super("error.msg.savings.product.interest.rate.chart.id.invalid", "Savings Product Interest rate chart with identifier " + id
                + " does not exist", id);
    }
}
