/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.data;

import java.util.List;
import java.util.Map;

public class DataExportTemplateData {
    public List<Map<String, String>> entityNames;
    public List<Map<String, String>> datatableNames;

    public DataExportTemplateData(final List<Map<String, String>> entityNames, List<Map<String, String>> datatableNames){
        this.datatableNames = datatableNames;
        this.entityNames = entityNames;
    }
}
