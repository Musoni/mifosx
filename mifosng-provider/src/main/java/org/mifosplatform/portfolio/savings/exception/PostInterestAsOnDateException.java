/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.portfolio.savings.exception;

import org.mifosplatform.infrastructure.core.exception.AbstractPlatformDomainRuleException;

public class PostInterestAsOnDateException extends AbstractPlatformDomainRuleException {

    public static enum PostInterestAsOnException_TYPE {
        FUTURE_DATE, VALID_DATE, ACTIVATION_DATE, LAST_TRANSACTION_DATE;
        public String errorMessage() {
            if (name().toString().equalsIgnoreCase("FUTURE_DATE")) {
                return "Cannot Post Interest in future Dates";
            } else if (name().toString().equalsIgnoreCase("VALID_DATE")) {
                return "Please Pass a valid date";
            } else if (name().toString().equalsIgnoreCase("ACTIVATION_DATE")) {
                return "Post Interest Date must be after the Activation date";
            } else if (name().toString().equalsIgnoreCase("LAST_TRANSACTION_DATE")) {
                return "Cannot Post Interest before last transaction date";
            }
            return name().toString();
        }

        public String errorCode() {
            if (name().toString().equalsIgnoreCase("FUTURE_DATE")) {
                return "error.msg.futureDate";
            } else if (name().toString().equalsIgnoreCase("VALID_DATE")) {
                return "error.msg.nullDatePassed";
            } else if (name().toString().equalsIgnoreCase("ACTIVATION_DATE")) {
                return "error.msg.before activation date";
            } else if (name().toString().equalsIgnoreCase("LAST_TRANSACTION_DATE")) {
                return "error.msg.countInterest";
            }
            return name().toString();
        }
    }


    public PostInterestAsOnDateException(final PostInterestAsOnException_TYPE reason) {
        super(reason.errorCode(), reason.errorMessage());
    }
}
