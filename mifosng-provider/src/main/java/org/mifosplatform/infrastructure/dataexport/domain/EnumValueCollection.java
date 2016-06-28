/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.domain;

import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.*;


@Entity
@Table(name = "r_enum_value")
public class EnumValueCollection {

    @EmbeddedId
    private EnumValueCollectionPK enumValueCollectionPK;

    @Column(name = "enum_message_property", nullable = false)
    private String descriptionMessage;

    @Column(name = "enum_value", nullable = false)
    private String value;

    @Column(name = "enum_type", nullable = false)
    private Integer type;

    /**
     * {@link EnumValueCollection} protected no-arg constructor
     *
     * (An entity class must have a no-arg public/protected constructor according to the JPA specification)
     **/
    protected EnumValueCollection(){}

    /**
     * @param enumValueCollectionPK
     * @param descriptionMessage
     * @param value
     * @param type
     */
    private EnumValueCollection(final EnumValueCollectionPK enumValueCollectionPK, final String descriptionMessage,
                                final String value, final Integer type){
        this.enumValueCollectionPK = enumValueCollectionPK;
        this.descriptionMessage = descriptionMessage;
        this.value = value;
        this.type = type;
    }

    /**
     * @param enumValueCollectionPK
     * @param descriptionMessage
     * @param value
     * @param type
     */
    public EnumValueCollection instance(final EnumValueCollectionPK enumValueCollectionPK, final String descriptionMessage,
                                        final String value, final Integer type){
        return new EnumValueCollection(enumValueCollectionPK, descriptionMessage, value, type);
    }

    public EnumValueCollectionPK getEnumValueCollectionPK() {return enumValueCollectionPK;}

    public String getDescriptionMessage() {return descriptionMessage;}

    public String getValue() {return value;}

    public Integer getType() {return type;}
}
