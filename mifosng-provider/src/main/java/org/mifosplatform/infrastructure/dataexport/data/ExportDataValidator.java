/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.data;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang.StringUtils;
import org.mifosplatform.infrastructure.core.api.JsonCommand;
import org.mifosplatform.infrastructure.core.data.ApiParameterError;
import org.mifosplatform.infrastructure.core.data.DataValidatorBuilder;
import org.mifosplatform.infrastructure.core.exception.InvalidJsonException;
import org.mifosplatform.infrastructure.core.exception.PlatformApiDataValidationException;
import org.mifosplatform.infrastructure.core.serialization.FromJsonHelper;
import org.mifosplatform.infrastructure.dataexport.api.DataExportApiConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class ExportDataValidator {

    private final FromJsonHelper fromJsonHelper;

    @Autowired
    public ExportDataValidator(final FromJsonHelper fromJsonHelper) {
        this.fromJsonHelper = fromJsonHelper;
    }

    /** 
     * validate the request to create a new data export tool
     * 
     * @param jsonCommand -- the JSON command object (instance of the JsonCommand class)
     * @return None
     **/
    public void validateCreateDataExportRequest(final JsonCommand jsonCommand) {
        final String jsonString = jsonCommand.json();
        final JsonElement jsonElement = jsonCommand.parsedJson();

        if (StringUtils.isBlank(jsonString)) { 
            throw new InvalidJsonException(); 
        }
        
        final Type typeToken = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromJsonHelper.checkForUnsupportedParameters(typeToken, jsonString, 
                DataExportApiConstants.CREATE_DATA_EXPORT_REQUEST_PARAMETERS);
        
        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder dataValidatorBuilder = new DataValidatorBuilder(dataValidationErrors).
                resource(StringUtils.lowerCase(DataExportApiConstants.DATA_EXPORT_ENTITY_NAME));
        
        final String name = this.fromJsonHelper.extractStringNamed(
                DataExportApiConstants.NAME_PARAM_NAME, jsonElement);
        dataValidatorBuilder.reset().parameter(DataExportApiConstants.BASE_ENTITY_NAME_PARAM_NAME).value(name).notBlank();
        
        final String baseEntity = this.fromJsonHelper.extractStringNamed(
                DataExportApiConstants.BASE_ENTITY_NAME_PARAM_NAME, jsonElement);
        dataValidatorBuilder.reset().parameter(DataExportApiConstants.BASE_ENTITY_NAME_PARAM_NAME).value(baseEntity).notBlank();
        
        final String[] columns = this.fromJsonHelper.extractArrayNamed(DataExportApiConstants.COLUMNS_PARAM_NAME, 
                jsonElement);
        dataValidatorBuilder.reset().parameter(DataExportApiConstants.COLUMNS_PARAM_NAME).value(columns).notBlank();
        
        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }
    
    /** 
     * throw a PlatformApiDataValidationException exception if there are validation errors
     * 
     * @param dataValidationErrors -- list of ApiParameterError objects
     * @return None
     **/
    private void throwExceptionIfValidationWarningsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) { 
            throw new PlatformApiDataValidationException(dataValidationErrors); 
        }
    }
}
