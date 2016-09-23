/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.scheduledemail;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class EmailApiConstants {

    public static final String RESOURCE_NAME = "scheduledemail";

    // general
    public static final String localeParamName = "locale";
    public static final String dateFormatParamName = "dateFormat";

    // request parameters
    public static final String idParamName = "id";
    public static final String groupIdParamName = "groupId";
    public static final String clientIdParamName = "clientId";
    public static final String staffIdParamName = "staffId";
    public static final String subjectParamName = "emailSubject";
    public static final String messageParamName = "emailMessage";

    // response parameters
    public static final String statusParamName = "status";

    public static final Set<String> CREATE_REQUEST_DATA_PARAMETERS = new HashSet<>(Arrays.asList(localeParamName,
            dateFormatParamName, groupIdParamName, clientIdParamName, staffIdParamName, messageParamName));

    public static final Set<String> UPDATE_REQUEST_DATA_PARAMETERS = new HashSet<>(Arrays.asList(messageParamName));

    public static final String SMTP_SERVER = "SMTP_SERVER";
    public static final String SMTP_PORT = "SMTP_PORT";
    public static final String SMTP_USERNAME = "SMTP_USERNAME";
    public static final String SMTP_PASSWORD = "SMTP_PASSWORD";

}