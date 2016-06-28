/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.api;

import com.google.common.collect.ImmutableList;

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

    // json parameter and format constants
    public final static String FILE_FORMAT = "fileFormat";
    public final static String DATA_EXPORT_PROCESS_ID = "dataExportProcessId";
    public final static String DATA_EXPORT = "DATAEXPORT";
    public final static String ENTITY_NAME = "entityName";
    public final static String ENTITY_TABLE = "entityTable";
    public final static String DATATABLE_NAME = "datatableName";
    public final static String ENTITY_ID = "id";
    public final static String ENTITY_STATUS = "status";
    public final static String ENTITY_TYPE = "type";
    public final static String ENTITY_OFFICE = "office";
    public final static String ENTITY_STAFF = "staff";
    public final static String ENTITY_CLIENT_ID = "clientId";
    public final static String ENTITY_GROUP_ID = "groupId";
    public final static String ENTITY_SUBMITDATE = "submittedondate";
    public final static String ENTITY_PRINCIPAL = "principal";
    public final static String ENTITY_OUTSTANDING = "outstanding";
    public final static String ENTITY_BALANCE = "balance";
    public final static String SUBMITTEDON_DATE_FORMAT = "dd-MM-yyyy";
    public final static String ENTITY = "entity";
    public static final String DATA_EXPORT_FILENAME_DATETIME_FORMAT_PATTERN = "yyyyMMddHHmmss";

    // field name constants
    public final static String CLIENT_ID = "client_id";
    public final static String GROUP_ID = "group_id";
    public final static String LOAN_ID = "loan_id";
    public final static String SAVINGS_ACCOUNT_ID = "savingsaccount_id";
    public final static String OFFICE_TABLE = "m_office";
    public final static String ACCOUNT_NO = "account_no";
    public final static String EXTERNAL_ID = "external_id";
    public final static String OFFICE_ID = "office_id";
    public final static String OFFICE_NAME = "name";
    public final static String FULL_NAME = "display_name";
    public final static String STATUS = "status_enum";
    public final static String LOAN_STATUS = "loan_status_id";
    public final static String LOAN_TYPE = "loan_type_enum";
    public final static String SAVINGS_PRODUCT_TYPE = "product_id";
    public final static String LOAN_PRINCIPAL = "principal_amount";
    public final static String TOTAL_OUTSTANDING = "total_outstanding_derived";
    public final static String SUBMITTED_ON_DATE = "submittedon_date";
    public final static String MOBILE_NO = "mobile_no";
    public final static String STAFF_ID = "staff_id";
    public final static String ACCOUNT_BALANCE = "account_balance_derived";

    // entity-independent supported parameter list
    public final static ImmutableList<String> BASIC_SUPPORTED_PARAMETERS =
            ImmutableList.of(ENTITY,ENTITY_NAME,DATATABLE_NAME);

    // lists of fields to be entered in the data export for each entity type
    public final static ImmutableList<String> CLIENT_FIELD_NAMES =
            ImmutableList.of(ENTITY_ID,ACCOUNT_NO,FULL_NAME, OFFICE_ID,MOBILE_NO,STATUS, SUBMITTED_ON_DATE);
    public final static ImmutableList<String> GROUP_FIELD_NAMES =
            ImmutableList.of(ENTITY_ID,EXTERNAL_ID,FULL_NAME, OFFICE_ID,STAFF_ID,STATUS, SUBMITTED_ON_DATE);
    public final static ImmutableList<String> LOAN_FIELD_NAMES =
            ImmutableList.of(ENTITY_ID,ACCOUNT_NO,LOAN_TYPE,CLIENT_ID,GROUP_ID,LOAN_STATUS,SUBMITTED_ON_DATE,LOAN_PRINCIPAL,TOTAL_OUTSTANDING);
    public final static ImmutableList<String> SAVINGS_ACCOUNT_FIELD_NAMES =
            ImmutableList.of(ENTITY_ID,ACCOUNT_NO,CLIENT_ID,GROUP_ID,SAVINGS_PRODUCT_TYPE,STATUS, SUBMITTED_ON_DATE, ACCOUNT_BALANCE);
    
    public static final String DATA_EXPORT_ENTITY_NAME = "DATAEXPORT";
    
    // common API resource request parameter constants
    public static final String LOCALE_PARAM_NAME = "locale";
    public static final String DATE_FORMAT_PARAM_NAME = "dateFormat";
    
    // request parameters
    public static final String BASE_ENTITY_NAME_PARAM_NAME = "baseEntityName";
    public static final String COLUMNS_PARAM_NAME = "columns";
    public static final String DATATABLES_PARAM_NAME = "datatables";
    public static final String FILTERS_PARAM_NAME = "filters";
    
    // set of supported parameter strings for API create data export request
    public static final Set<String> CREATE_DATA_EXPORT_REQUEST_PARAMETERS = new HashSet<>(Arrays.asList(LOCALE_PARAM_NAME, 
            DATE_FORMAT_PARAM_NAME, BASE_ENTITY_NAME_PARAM_NAME, DATATABLES_PARAM_NAME, FILTERS_PARAM_NAME, 
            COLUMNS_PARAM_NAME));
    
    public static final String USER_CREATED_DATATABLE_NAME_PREFIX = "cct_";
    public static final String MUSONI_SYSTEM_DATATABLE_NAME_PREFIX = "ml_";
}
