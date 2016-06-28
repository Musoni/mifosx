/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.data;

import org.apache.commons.lang3.StringUtils;
import org.mifosplatform.infrastructure.core.data.EnumOptionData;

public enum DataExportProcessStatus {
    INVALID(0, "dataExportProcessStatus.invalid", "invalid"),
    INIT(100, "dataExportProcessStatus.init", "init"),
    PROCESSING(200, "dataExportProcessStatus.processing", "processing"),
    FINISHED_OK(300, "dataExportProcessStatus.finished.ok", "finished ok"),
    FINISHED_WITH_ERRORS(400, "dataExportProcessStatus.finished.with.errors", "finished with errors");

    private final String value;
    private final String code;
    private final Integer id;

    /**
     * @param id
     * @param value
     * @param code
     */
    private DataExportProcessStatus(final Integer id, final String code, final String value) {
        this.id = id;
        this.value = StringUtils.trim(value);
        this.code = StringUtils.trim(code);
    }

    /**
     * @param processStatusEnumValue
     * @return {@link DataExportProcessStatus} object
     */
    public static DataExportProcessStatus instance(final Integer processStatusEnumValue) {
        DataExportProcessStatus dataExportProcessStatus = INVALID;

        switch (processStatusEnumValue) {
            case 100:
                dataExportProcessStatus = INIT;
                break;

            case 200:
                dataExportProcessStatus = PROCESSING;
                break;

            case 300:
                dataExportProcessStatus = FINISHED_OK;
                break;

            case 400:
                dataExportProcessStatus = FINISHED_WITH_ERRORS;
                break;
        }

        return dataExportProcessStatus;
    }

    /**
     * @return the id
     */
    public Integer getId() {
        return id;
    }

    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * @return the code
     */
    public String getCode() {
        return code;
    }

    /**
     * @return true if the enum object is equal to "INVALID", else false
     **/
    public boolean isInvalid() {
        return this.equals(INVALID);
    }

    /**
     * @return true if the enum object is equal to "INIT", else false
     **/
    public boolean isInit() {
        return this.equals(INIT);
    }

    /**
     * @return true if the enum object is equal to "PROCESSING", else false
     **/
    public boolean isProcessing() {
        return this.equals(PROCESSING);
    }

    /**
     * @return true if the enum object is equal to "FINISHED_OK", else false
     **/
    public boolean isFinishedOk() {
        return this.equals(FINISHED_OK);
    }

    /**
     * @return true if the enum object is equal to "FINISHED_WITH_ERRORS", else false
     **/
    public boolean isFinishedWithErrors() {
        return this.equals(FINISHED_WITH_ERRORS);
    }

    /**
     * convert {@link DataExportProcessStatus} object to {@link EnumOptionData} object
     *
     * @return {@link EnumOptionData} object
     */
    public EnumOptionData toEnumOptionData() {
        // convert the integer id to long
        final Long id = (this.id != null) ? this.id.longValue() : null;

        return new EnumOptionData(id, this.code, this.value);
    }
}
