/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.api;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class DataExportApiConstants {
    public static final String DATA_EXPORT_URI_PATH_VALUE = "/dataexports";
    

    // base path and format constants
    public static final String MYSQL_DATE_FORMAT = "yyyy-MM-dd";
    public static final String MYSQL_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String WINDOWS_END_OF_LINE_CHARACTER = "\r\n";
    public static final String UNIX_END_OF_LINE_CHARACTER = "\n";
    public static final String XLS_FILE_CONTENT_TYPE = "text/xlsx; charset=utf-8";
    public static final String CSV_FILE_CONTENT_TYPE = "text/csv; charset=utf-8";
    public static final String XML_FILE_CONTENT_TYPE = "text/xml; charset=utf-8";
    public static final String CSV_FILE_FORMAT = "csv";
    public static final String XML_FILE_FORMAT = "xml";
    public static final String XLS_FILE_FORMAT = "xlsx";

    public static final String DATA_EXPORT_FILENAME_DATETIME_FORMAT_PATTERN = "yyyyMMddHHmmss";

    public static final String DATA_EXPORT_ENTITY_NAME = "DATAEXPORT";
    
    // common API resource request parameter constants
    public static final String LOCALE_PARAM_NAME = "locale";
    public static final String DATE_FORMAT_PARAM_NAME = "dateFormat";
    
    // request parameters
    public static final String NAME_PARAM_NAME = "name";
    public static final String BASE_ENTITY_NAME_PARAM_NAME = "baseEntityName";
    public static final String COLUMNS_PARAM_NAME = "columns";
    public static final String DATATABLES_PARAM_NAME = "datatables";
    public static final String FILTERS_PARAM_NAME = "filters";
    public static final String FILE_FORMAT_PARAM_NAME = "fileFormat";
    
    // set of supported parameter strings for API create data export request
    public static final Set<String> CREATE_DATA_EXPORT_REQUEST_PARAMETERS = new HashSet<>(Arrays.asList(LOCALE_PARAM_NAME, 
            DATE_FORMAT_PARAM_NAME, BASE_ENTITY_NAME_PARAM_NAME, DATATABLES_PARAM_NAME, FILTERS_PARAM_NAME, 
            COLUMNS_PARAM_NAME, NAME_PARAM_NAME));
    
    public static final String USER_CREATED_DATATABLE_NAME_PREFIX = "cct_";
    public static final String MUSONI_SYSTEM_DATATABLE_NAME_PREFIX = "ml_";
}
