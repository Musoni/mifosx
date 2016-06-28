/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.data;

import org.joda.time.DateTime;

public class DataExportTimelineData {
    @SuppressWarnings("unused")
    private final String createdByUsername;
    @SuppressWarnings("unused")
    private final String createdByFirstname;
    @SuppressWarnings("unused")
    private final String createdByLastname;
    @SuppressWarnings("unused")
    private final DateTime createdOnDate;
    @SuppressWarnings("unused")
    private final String modifiedByUsername;
    @SuppressWarnings("unused")
    private final String modifiedByFirstname;
    @SuppressWarnings("unused")
    private final String modifiedByLastname;
    @SuppressWarnings("unused")
    private final DateTime modifiedOnDate;
    
    /**
     * @param createdByUsername
     * @param createdByFirstname
     * @param createdByLastname
     * @param createdOnDate
     * @param modifiedByUsername
     * @param modifiedByFirstname
     * @param modifiedByLastname
     * @param modifiedOnDate
     */
    private DataExportTimelineData(final String createdByUsername, final String createdByFirstname, 
            final String createdByLastname, final DateTime createdOnDate, 
            final String modifiedByUsername, final String modifiedByFirstname,
            final String modifiedByLastname, final DateTime modifiedOnDate) {
        this.createdByUsername = createdByUsername;
        this.createdByFirstname = createdByFirstname;
        this.createdByLastname = createdByLastname;
        this.createdOnDate = createdOnDate;
        this.modifiedByUsername = modifiedByUsername;
        this.modifiedByFirstname = modifiedByFirstname;
        this.modifiedByLastname = modifiedByLastname;
        this.modifiedOnDate = modifiedOnDate;
    }
    
    /**
     * Creates a new instances of the {@link DataExportTimelineData} object
     * 
     * @param createdByUsername
     * @param createdByFirstname
     * @param createdByLastname
     * @param createdOnDate
     * @param modifiedByUsername
     * @param modifiedByFirstname
     * @param modifiedByLastname
     * @param modifiedOnDate
     * @return {@link DataExportTimelineData} object
     */
    public static DataExportTimelineData newInstance(final String createdByUsername, final String createdByFirstname, 
            final String createdByLastname, final DateTime createdOnDate, 
            final String modifiedByUsername, final String modifiedByFirstname,
            final String modifiedByLastname, final DateTime modifiedOnDate) {
        return new DataExportTimelineData(createdByUsername, createdByFirstname, createdByLastname, 
                createdOnDate, modifiedByUsername, modifiedByFirstname, modifiedByLastname, modifiedOnDate);
    }
}
