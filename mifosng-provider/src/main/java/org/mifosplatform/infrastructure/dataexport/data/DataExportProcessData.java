/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.data;

import org.joda.time.LocalDate;
import org.mifosplatform.useradministration.data.AppUserData;

/**
 * Immutable object representing a {@link DataExportProcess} entity data
 */
public class DataExportProcessData {
    private final Long id;
    private final DataExportData dataExport;
    private final DataExportProcessStatus processStatus;
    private final String fileName;
    private final AppUserData startedByUser;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final String errorMessage;
    private final int fileDownloadCount;
    
    /**
     * @param id
     * @param dataExport
     * @param processStatus
     * @param fileName
     * @param startedByUser
     * @param startDate
     * @param endDate
     * @param errorMessage
     * @param fileDownloadCount
     */
    private DataExportProcessData(final Long id, final DataExportData dataExport, 
            final DataExportProcessStatus processStatus, final String fileName, 
            final AppUserData startedByUser, final LocalDate startDate, final LocalDate endDate, 
            final String errorMessage, final Integer fileDownloadCount) {
        this.id = id;
        this.dataExport = dataExport;
        this.processStatus = processStatus;
        this.fileName = fileName;
        this.startedByUser = startedByUser;
        this.startDate = startDate;
        this.endDate = endDate;
        this.errorMessage = errorMessage;
        this.fileDownloadCount = fileDownloadCount;
    }
    
    /**
     * Creates a new {@link DataExportProcessData} object
     * 
     * @param id
     * @param dataExport
     * @param processStatus
     * @param fileName
     * @param startedByUser
     * @param startDate
     * @param endDate
     * @param errorMessage
     * @param fileDownloadCount
     * @return {@link DataExportProcessData} object
     */
    public static DataExportProcessData newInstance(final Long id, final DataExportData dataExport, 
            final DataExportProcessStatus processStatus, final String fileName, 
            final AppUserData startedByUser, final LocalDate startDate, final LocalDate endDate, 
            final String errorMessage, final Integer fileDownloadCount) {
        return new DataExportProcessData(id, dataExport, processStatus, fileName, startedByUser, 
                startDate, endDate, errorMessage, fileDownloadCount);
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @return the dataExport
     */
    public DataExportData getDataExport() {
        return dataExport;
    }

    /**
     * @return the processStatus
     */
    public DataExportProcessStatus getProcessStatus() {
        return processStatus;
    }

    /**
     * @return the fileName
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @return the startedByUser
     */
    public AppUserData getStartedByUser() {
        return startedByUser;
    }

    /**
     * @return the startDate
     */
    public LocalDate getStartDate() {
        return startDate;
    }

    /**
     * @return the endDate
     */
    public LocalDate getEndDate() {
        return endDate;
    }

    /**
     * @return the errorMessage
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * @return the fileDownloadCount
     */
    public int getFileDownloadCount() {
        return fileDownloadCount;
    }
}
