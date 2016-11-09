/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.data;

import org.mifosplatform.infrastructure.dataexport.domain.DataExport;

/**
 * Immutable object representing a {@link DataExport} entity data
 */
public class DataExportData {
    private final Long id;
    private final String name;
    private final String baseEntityName;
    private final String userRequestMap;
    private final String filename;
    private final Integer fileDownloadCount;
    private final DataExportTimelineData timeline;
    
    /**
     * @param id
     * @param name
     * @param baseEntityName
     * @param userRequestMap
     * @param fileDownloadCount
     * @param timeline
     * @param filename
     */
    private DataExportData(final Long id, final String name, final String baseEntityName, final String userRequestMap, 
            final Integer fileDownloadCount, final DataExportTimelineData timeline, final String filename) {
        this.id = id;
        this.name = name;
        this.baseEntityName = baseEntityName;
        this.userRequestMap = userRequestMap;
        this.fileDownloadCount = fileDownloadCount;
        this.timeline = timeline;
        this.filename = filename;
    }
    
    /**
     * Creates a new {@link DataExportData} object
     * 
     * @param id
     * @param name
     * @param baseEntityName
     * @param userRequestMap
     * @param fileDownloadCount
     * @param timeline
     * @param filename
     * @return {@link DataExportData} object
     */
    public static DataExportData newInstance(final Long id, final String name, final String baseEntityName, 
    		final String userRequestMap, final Integer fileDownloadCount, final DataExportTimelineData timeline, 
    		final String filename) {
        return new DataExportData(id, name, baseEntityName, userRequestMap, fileDownloadCount, timeline, filename);
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
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
     * @return the fileDownloadCount
     */
    public Integer getFileDownloadCount() {
        return fileDownloadCount;
    }

    /**
     * @return the timeline
     */
    public DataExportTimelineData getTimeline() {
        return timeline;
    }

    /**
     * @return filename
     */
    public String getFilename() {
        return filename;
    }
}
