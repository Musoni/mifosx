/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.data;


import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class DataExportFileData {
    private final File file;
    private final String fileName;
    private final String contentType;
    private final InputStream inputStream;
    private static final Logger logger = LoggerFactory.getLogger(DataExportFileData.class);

    /**
     * @param file
     * @param fileName
     * @param contentType
     * @param inputStream
     */
    public DataExportFileData(final File file, final InputStream inputStream, final String fileName, final String contentType) {
        this.file = file;
        this.fileName = fileName;
        this.contentType = contentType;
        this.inputStream = inputStream;
    }

    /**
     * @param file
     * @param fileName
     * @param contentType
     */
    public DataExportFileData(final File file, final String fileName, final String contentType) {
        this(file,null,fileName,contentType);
    }

    /**
     * @param fileName
     * @param contentType
     * @param inputStream
     */
    public DataExportFileData(final InputStream inputStream, final String fileName, final String contentType) {
        this(null,inputStream,fileName,contentType);
    }

    /**
     * @return the file
     */
    public File getFile() {
        return file;
    }

    /**
     * @return the fileName
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @return the contentType
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * @return the inputStream
     */
    public InputStream getInputStream() {
        InputStream inputStream = this.inputStream;

        try {
            if (inputStream == null) {
                inputStream = new FileInputStream(this.file);
            }
        }

        catch (Exception exception) {
            logger.error(exception.getMessage());
        }

        return inputStream;
    }
}
