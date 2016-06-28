/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.domain;

import org.hibernate.annotations.CollectionId;
import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "r_entity_label")
public class EntityLabel extends AbstractPersistable<Long> {

    @Column(name = "entity_table", nullable = false)
    private String table;

    @Column(name = "entity_field_name", nullable = false)
    private String field;

    @Column(name = "entity_json_param", nullable = false)
    private String jsonParam;

    @Column(name = "entity_field_label", nullable = false)
    private String label;

    @Column(name = "refers_to_table")
    private String referenceTable;

    @Column(name = "refers_to_field")
    private String referenceField;


    /**
     * {@link DataExport} protected no-arg constructor
     *
     * (An entity class must have a no-arg public/protected constructor according to the JPA specification)
     **/
    protected EntityLabel() { }

    private EntityLabel(final String table, final String field, final String jsonParam, final String label,
                        final String referenceTable, final String referenceField){
        this.table = table;
        this.field = field;
        this.jsonParam = jsonParam;
        this.label = label;
        this.referenceTable = referenceTable;
        this.referenceField = referenceField;
    }

    public static EntityLabel instance(final String table, final String field, final String jsonParam, final String label,
                                final String referenceTable, final String referenceField){
        return new EntityLabel(table,field,jsonParam,label,referenceTable,referenceField);
    }

    public static EntityLabel instance(final String table, final String field, final String jsonParam, final String label){
        return new EntityLabel(table,field,jsonParam,label,null,null);
    }

    public String getTable() {return table;}

    public String getField() {return field;}

    public String getJsonParam() {return jsonParam;}

    public String getLabel() {return label;}

    public String getReferenceTable() {return referenceTable;}

    public String getReferenceField() {return referenceField;}
}
