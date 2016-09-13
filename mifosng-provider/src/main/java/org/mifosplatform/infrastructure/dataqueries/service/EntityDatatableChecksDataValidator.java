/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataqueries.service;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang.StringUtils;
import org.mifosplatform.infrastructure.core.data.ApiParameterError;
import org.mifosplatform.infrastructure.core.data.DataValidatorBuilder;
import org.mifosplatform.infrastructure.core.exception.InvalidJsonException;
import org.mifosplatform.infrastructure.core.exception.PlatformApiDataValidationException;
import org.mifosplatform.infrastructure.core.serialization.FromJsonHelper;
import org.mifosplatform.infrastructure.dataqueries.data.EntityTables;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.*;

@Component
public final class EntityDatatableChecksDataValidator {

    /**
     * The parameters supported for this command.
     */
    private final Set<String> supportedParameters = new HashSet<>(Arrays.asList("entity", "datatableId", "status","systemDefined","productId"));


    private final FromJsonHelper fromApiJsonHelper;

    @Autowired
    public EntityDatatableChecksDataValidator(final FromJsonHelper fromApiJsonHelper) {
        this.fromApiJsonHelper = fromApiJsonHelper;
    }


    public void validateForCreate(final String json) {
        if (StringUtils.isBlank(json)) { throw new InvalidJsonException(); }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, this.supportedParameters);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("entityDatatableChecks");

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final String entity = this.fromApiJsonHelper.extractStringNamed("entity", element);
        baseDataValidator.reset().parameter("entity").value(entity).notBlank().isOneOfTheseStringValues(EntityTables.getEntitiesList());

        final Integer status = this.fromApiJsonHelper.extractIntegerSansLocaleNamed("status", element);
        baseDataValidator.reset().parameter("status").value(status).isOneOfTheseValues((Object[])EntityTables.getStatus(entity));



        final String datatableId = this.fromApiJsonHelper.extractStringNamed("datatableId", element);
        baseDataValidator.reset().parameter("datatableId").value(datatableId).notBlank().integerZeroOrGreater();

        if(this.fromApiJsonHelper.parameterExists("systemDefined",element)){
            final String systemDefined = this.fromApiJsonHelper.extractStringNamed("systemDefined", element);
            baseDataValidator.reset().parameter("systemDefined").value(systemDefined).validateForBooleanValue();
        }

        if(this.fromApiJsonHelper.parameterExists("productId",element)){
            final long loanProductId = this.fromApiJsonHelper.extractLongNamed("productId", element);
            baseDataValidator.reset().parameter("productId").value(loanProductId).integerZeroOrGreater();
        }

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }


    private void throwExceptionIfValidationWarningsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) { throw new PlatformApiDataValidationException(dataValidationErrors); }
    }
}