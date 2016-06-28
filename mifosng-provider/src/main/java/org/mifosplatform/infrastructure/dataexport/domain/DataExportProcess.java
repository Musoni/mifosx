/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.domain;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.mifosplatform.infrastructure.dataexport.data.DataExportProcessData;
import org.mifosplatform.infrastructure.dataexport.data.DataExportProcessStatus;
import org.mifosplatform.useradministration.data.AppUserData;
import org.mifosplatform.useradministration.domain.AppUser;
import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "m_data_export_process")
public class DataExportProcess extends AbstractPersistable<Long> {

    @ManyToOne
    @JoinColumn(name = "data_export_id", nullable = false)
    private DataExport dataExport;

    @Column(name = "process_status", nullable = false)
    private Integer status;

    @Column(name = "file_name")
    private String fileName;

    @ManyToOne
    @JoinColumn(name = "started_by_user_id", nullable = false)
    private AppUser startedByUser;

    @Temporal(TemporalType.DATE)
    @Column(name = "started_date", nullable = false)
    private Date startDate;

    @Temporal(TemporalType.DATE)
    @Column(name = "ended_date")
    private Date endDate;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "file_download_count",nullable = false)
    private Integer fileDownloadCount;

    /**
     * @param dataExport
     * @param fileName
     * @param status
     * @param startedByUser
     * @param startDate
     * @param endDate
     * @param errorMessage
     * @param fileDownloadCount
     */
    private DataExportProcess(final DataExport dataExport, final String fileName, final Integer status, 
            final AppUser startedByUser, final Date startDate, final Date endDate, final String errorMessage, 
            final Integer fileDownloadCount) {
        this.dataExport = dataExport;
        this.fileName = fileName;
        this.status = status;
        this.startedByUser = startedByUser;
        this.startDate = startDate;
        this.endDate = endDate;
        this.errorMessage = errorMessage;
        this.fileDownloadCount = fileDownloadCount;
    }

    /**
     * {@link DataExportProcess} protected no-arg constructor
     *
     * (An entity class must have a no-arg public/protected constructor according to the JPA specification)
     **/
    protected DataExportProcess(){}

    /**
     * @param dataExport
     * @param fileName
     * @param status
     * @param startedByUser
     * @param startDate
     * @param endDate
     * @param errorMessage
     * @param fileDownloadCount
     * @return {@link DataExportProcess} object
     */
    public static DataExportProcess instance(final DataExport dataExport, final String fileName, final Integer status, 
            final AppUser startedByUser, final Date startDate, final Date endDate, final String errorMessage, 
            final Integer fileDownloadCount) {
        return new DataExportProcess(dataExport,fileName,status,startedByUser,startDate,endDate,errorMessage,fileDownloadCount);
    }

    public DataExport getDataExport() {
        return dataExport;
    }

    public Integer getStatus() {
        return status;
    }

    public String getFileName() {
        return fileName;
    }

    /**
     * update the error message property
     *
     * @param errorMessage
     * @return None
     **/
    public void updateErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * update the end date property
     *
     * @param endDate
     * @return None
     **/
    public void updateEndDate(final LocalDateTime endDate) {
        if (endDate != null) {
            this.endDate = endDate.toDate();
        }
    }

    /**
     * updates the fileDownloadCount property
     * @param fileDownloadCount
     * @return None
     */
    public void updateFileDownloadCount(final Integer fileDownloadCount) {
        this.fileDownloadCount = fileDownloadCount;
    }

    /**
     * update the status property
     *
     * @param status
     * @return None
     **/
    public void updateStatus(final Integer status) {
        if (status != null) {
            this.status = status;
        }
    }

    public AppUser getStartedByUser() {
        return startedByUser;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Integer getFileDownloadCount() {
        return fileDownloadCount;
    }
    
    /**
     * Returns a {@link DataExportProcessData} representation of the {@link DataExportProcess} entity
     * 
     * @return {@link DataExportProcessData} object
     */
    public DataExportProcessData toData() {
        final DataExportProcessStatus processStatus = DataExportProcessStatus.instance(status);
        final LocalDate startDate = (this.startDate != null) ? new LocalDate(this.startDate) : null;
        final LocalDate endDate = (this.startDate != null) ? new LocalDate(this.startDate) : null;
        
        AppUserData startedByUser = null;
        
        if (this.startedByUser != null) {
            final Long officeId = (this.startedByUser.getOffice() != null) ? this.startedByUser.getOffice().getId() : null;
            final String officeName = (this.startedByUser.getOffice() != null) ? this.startedByUser.getOffice().getName() : null;
            
            startedByUser = AppUserData.instance(this.startedByUser.getId(), this.startedByUser.getUsername(), 
                    this.startedByUser.getEmail(), officeId, officeName, this.startedByUser.getFirstname(), 
                    this.startedByUser.getLastname(), null, null, null, null, null);
        }
        
        return DataExportProcessData.newInstance(this.getId(), dataExport.toData(), processStatus, 
                fileName, startedByUser, startDate, endDate, errorMessage, fileDownloadCount);
    }
}
