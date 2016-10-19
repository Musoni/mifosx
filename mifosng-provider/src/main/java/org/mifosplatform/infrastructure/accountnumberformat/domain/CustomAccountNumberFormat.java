/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.accountnumberformat.domain;


import org.mifosplatform.infrastructure.core.api.JsonCommand;
import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;


@Entity
@Table(name="c_custom_account_format")
public class CustomAccountNumberFormat extends AbstractPersistable<Long> {

    @Column(name="custom_padding")
    private String customPattern;
    @Column(name="zero_padding")
    private Integer zeroPadding;


    public static CustomAccountNumberFormat createNewCustomAccountFormat( final JsonCommand command){
        final Integer zeroPadding = command.integerValueOfParameterNamedDefaultToNullIfZero("dafdsaf");
        final String customPadding = command.stringValueOfParameterNamed("dsafda");
        return new CustomAccountNumberFormat(zeroPadding,customPadding);

    }

    private CustomAccountNumberFormat(final Integer zeroPadding, final String customPattern) {
        this.zeroPadding = zeroPadding;
        this.customPattern = customPattern;
    }

    public String getCustomPattern() {return this.customPattern;}

    public Integer getZeroPadding() {return this.zeroPadding;}
}
