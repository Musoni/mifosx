/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.domain;

import org.mifosplatform.infrastructure.dataexport.data.DataExportData;
import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Date;

@Entity
@Table(name = "m_data_export")
public class DataExport extends AbstractPersistable<Long> {

    @Column(name="base_entity", nullable = false)
    private String baseEntity;

    @Column(name = "json")
    private String json;

    @Column(name = "data_sql", nullable = false)
    private String sql;

    /**
     * {@link DataExport} protected no-arg constructor
     *
     * (An entity class must have a no-arg public/protected constructor according to the JPA specification)
     **/
    protected DataExport() { }

    /**
     * @param baseEntity
     * @param json
     * @param sql
     */
    private DataExport(final String baseEntity, final String json, final String sql) {
        this.baseEntity = baseEntity;
        this.json = json;
        this.sql = sql;
    }

    /**
     * @param baseEntity
     * @param json
     * @param sql
     * @return {@link DataExport} object
     */
    public static DataExport instance(final String baseEntity, final String json, final String sql) {
        return new DataExport(baseEntity,json,sql);
    }

    public String getBaseEntity() {
        return baseEntity;
    }

    public String getJson() {
        return json;
    }

    public String getSql() {
        return sql;
    }
    
    /**
     * Returns {@link DataExportData} representation of the {@link DataExport} entity
     * 
     * @return {@link DataExportData} object
     */
    public DataExportData toData() {
        return DataExportData.newInstance(this.getId(), baseEntity, json);
    }
}
