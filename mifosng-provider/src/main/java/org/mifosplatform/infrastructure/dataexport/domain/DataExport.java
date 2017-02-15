/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.domain;

import org.mifosplatform.infrastructure.core.domain.AbstractAuditableCustom;
import org.mifosplatform.useradministration.domain.AppUser;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "m_data_export")
public class DataExport extends AbstractAuditableCustom<AppUser, Long> {
    private static final long serialVersionUID = 4164504837938484160L;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "base_entity_name", nullable = false)
    private String baseEntityName;
    
    @Column(name = "user_request_map", nullable = false)
    private String userRequestMap;
    
    @Column(name = "data_sql", nullable = false)
    private String dataSql;
    
    @Column(name = "filename", nullable = true)
    private String filename;
    
    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;
    
    @Column(name = "file_download_count", nullable = false)
    private Integer fileDownloadCount;
    
    /**
     * {@link DataExport} protected constructor
     */
    protected DataExport() { }

    /**
     * @param name
     * @param baseEntityName
     * @param userRequestMap
     * @param dataSql
     * @param deleted
     * @param fileDownloadCount
     * @param filename
     */
    private DataExport(final String name, final String baseEntityName, final String userRequestMap, 
    		final String dataSql, final boolean deleted, final Integer fileDownloadCount, 
    		final String filename) {
    	this.name = name;
        this.baseEntityName = baseEntityName;
        this.userRequestMap = userRequestMap;
        this.dataSql = dataSql;
        this.deleted = deleted;
        this.fileDownloadCount = fileDownloadCount;
        this.filename = filename;
    }
    
    /**
     * Creates a new instance of the {@link DataExport} object
     * 
     * @param name
     * @param baseEntityName
     * @param userRequestMap
     * @param dataSql
     * @return {@link DataExport} object
     */
    public static DataExport newInstance(final String name, final String baseEntityName, 
    		final String userRequestMap, final String dataSql) {
        return new DataExport(name, baseEntityName, userRequestMap, dataSql, false, 0, null);
    }
    
    /**
     * @return the name
     */
    public String getName() {
    	return name;
    }

    /**
     * @return the baseEntityName
     */
    public String getBaseEntityName() {
        return baseEntityName;
    }

    /**
     * @return the userRequestMap
     */
    public String getUserRequestMap() {
        return userRequestMap;
    }
    
    /**
     * @return the dataSql
     */
    public String getDataSql() {
        return dataSql;
    }

    /**
     * @return the deleted
     */
    public boolean isDeleted() {
        return deleted;
    }
    
    /**
     * @return the fileDownloadCount
     */
    public Integer getFileDownloadCount() {
        return fileDownloadCount;
    }
    
    /**
     * @return the filename
     */
    public String getFilename() {
        return filename;
    }
    
    /**
     * Performs a soft delete of the entity by setting the "deleted" property to true
     */
    public void delete() {
    	this.name = this.name + "_deleted_" + this.getId();
        this.deleted = true;
    }
    
    public void updateFileDownloadCount(final int fileDownloadCount) {
        this.fileDownloadCount = fileDownloadCount;
    }
    
    public void updateFilename(final String filename) {
        this.filename = filename;
    }

    public void update(final String name, final String baseEntityName,
                       final String userRequestMap, final String dataSql) {
        if(name != null && !name.equals(this.name)){ this.name = name; }
        if(baseEntityName != null && !baseEntityName.equals(this.baseEntityName)){ this.baseEntityName = baseEntityName; }
        if(userRequestMap != null && !userRequestMap.equals(this.userRequestMap)){ this.userRequestMap = userRequestMap; }
        if(dataSql != null && !dataSql.equals(this.dataSql)){ this.dataSql = dataSql; }
    }
}
