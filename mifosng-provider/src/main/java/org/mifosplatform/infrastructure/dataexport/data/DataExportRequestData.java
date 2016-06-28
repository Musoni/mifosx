/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.data;

import org.mifosplatform.infrastructure.dataexport.api.DataExportApiConstants;
import org.yaml.snakeyaml.constructor.ConstructorException;

import java.security.InvalidParameterException;
import java.util.*;

public class DataExportRequestData {

    // Specify the baseEntity which is used as the base object:
    private DataExportBaseEntityEnum baseEntity;

    // Specify the field names that are displayed in the export file
    private List<String> displayedFieldNames;

    // Contains the various filter queries for each tableName
    private List<DataExportFilter> dataExportFiltersList;

    // List of other (data)tables to retrieve:
    private List<String> exportDatatables;
    private Set<String> supportedParameters;

    public DataExportRequestData(final DataExportBaseEntityEnum baseEntity, final List<DataExportFilter> dataExportFiltersList,
                                 final List<String> exportDatatables, final Set<String> supportedParameters){
        this.baseEntity = baseEntity;
        this.dataExportFiltersList = dataExportFiltersList;
        this.exportDatatables = exportDatatables;
        this.supportedParameters = supportedParameters;
        assignParameters();
    }

    public void addExportDataTable(String datatable){
        this.exportDatatables.add(datatable);
    }

    public void addDataExportFilter(DataExportFilter filter){
        this.dataExportFiltersList.add(filter);
    }

    public DataExportBaseEntityEnum getBaseEntity(){return this.baseEntity;}

    public List<DataExportFilter> getDataExportFiltersList(){return this.dataExportFiltersList;}

    public List<String> getExportDatatables(){return this.exportDatatables;}

    public List<String> getDisplayedFieldNames(){return this.displayedFieldNames;}

    public Set<String> getSupportedParameters(){return this.supportedParameters;}

    private void assignParameters(){
        switch(this.baseEntity){
            case CLIENT:
                this.displayedFieldNames = (displayedFieldNames==null?DataExportApiConstants.CLIENT_FIELD_NAMES:displayedFieldNames);
                break;
            case GROUP:
                this.displayedFieldNames = (displayedFieldNames==null?DataExportApiConstants.GROUP_FIELD_NAMES:displayedFieldNames);
                break;
            case LOAN:
                this.displayedFieldNames = (displayedFieldNames==null?DataExportApiConstants.LOAN_FIELD_NAMES:displayedFieldNames);
                break;
            case SAVINGSACCOUNT:
                this.displayedFieldNames = (displayedFieldNames==null?DataExportApiConstants.SAVINGS_ACCOUNT_FIELD_NAMES:displayedFieldNames);
                break;
            default:
                throw new InvalidParameterException(this.baseEntity.name());
        }
    }
}
