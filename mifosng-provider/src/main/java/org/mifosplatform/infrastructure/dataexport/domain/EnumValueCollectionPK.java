/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.domain;

import javax.persistence.*;
import java.io.Serializable;

@Embeddable
public class EnumValueCollectionPK implements Serializable {

    @Column(name = "enum_name", length = 100)
    private String fieldName;

    @Column(name = "enum_id")
    private Long id;

    public EnumValueCollectionPK(){

    }

    public EnumValueCollectionPK(String fieldName, Long id){
        this.fieldName = fieldName;
        this.id = id;
    }

    public String getFieldName() {return fieldName;}

    public Long getId() {return id;}
}