/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.core.serialization;

import java.lang.reflect.Type;
import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.mifosplatform.infrastructure.core.data.ApiParameterError;
import org.mifosplatform.infrastructure.core.data.DataValidatorBuilder;
import org.mifosplatform.infrastructure.core.exception.InvalidJsonException;
import org.mifosplatform.infrastructure.core.exception.PlatformApiDataValidationException;
import org.mifosplatform.infrastructure.core.exception.PlatformDataIntegrityException;
import org.mifosplatform.infrastructure.dataqueries.data.MetaDataResultSet;
import org.mifosplatform.infrastructure.dataqueries.data.ResultsetColumnHeaderData;
import org.mifosplatform.portfolio.client.api.ClientApiConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.expression.ExpressionParser;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;

@Component
public class DatatableCommandFromApiJsonDeserializer {

    private final static String DATATABLE_NAME_REGEX_PATTERN = "^[a-zA-Z][a-zA-Z0-9\\-_\\s]{0,48}[a-zA-Z0-9]$";
    private final static String DATATABLE_COLUMN_NAME_REGEX_PATTERN = "^[a-zA-Z][a-zA-Z0-9\\-_\\s]{0,}[a-zA-Z0-9]$";
    /**
     * The parameters supported for this command.
     */
    private final Set<String> supportedParametersForCreate = new HashSet<>(Arrays.asList("datatableName", "apptableName", "multiRow",
            "columns", "category", "metaData","displayName","johnDoeColumns"));
    private final Set<String> supportedParametersForCreateColumns = new HashSet<>(Arrays.asList("name", "type", "length",
            "mandatory", "code", "labelName", "order","displayCondition", "formulaExpression","type"));
    private final Set<String> supportedParametersForUpdate = new HashSet<>(Arrays.asList("apptableName", "changeColumns","changeJohnDoeColumns",
            "addColumns","addJohnDoeColumns","dropColumns", "dropJohnDoeColumns","category", "metaData","displayName"));
    private final Set<String> supportedParametersForAddColumns = new HashSet<>(Arrays.asList("name", "type", "length", "mandatory",
            "after", "code", "labelName", "order","displayCondition", "formulaExpression"));
    private final Set<String> supportedParametersForChangeColumns = new HashSet<>(Arrays.asList("name", "newName", "length",
            "mandatory", "after", "code", "newCode", "labelName", "order","displayCondition", "formulaExpression","type"));
    private final Set<String> supportedParametersForDropColumns = new HashSet<>(Arrays.asList("name","type"));
    private final Object[] supportedColumnTypes = { "string", "number", "boolean", "decimal", "date", "datetime", "text", "dropdown","checkbox","signature","image"};
    private final Object[] supportedApptableNames = { "m_loan", "m_savings_account", "m_client", "m_group", "m_center", "m_office",
            "m_savings_product", "m_product_loan" };

    private final static HashMap<String, Object> sampleExpressionValues = new HashMap<String, Object>() {

        {
            put("string", "ABC");
            put("checkbox", "123,123");
            put("number", 1);
            put("boolean", "TRUE");
            put("decimal", 1.00);
            put("date", "2015-01-01");
            put("datetime", "2015-01-01 01:00:00");
            put("text", "ABC DEF");
            put("dropdown", "1");
            put("image", "1");
            put("signature", "1");
        }
    };

    private final FromJsonHelper fromApiJsonHelper;
    private final EvaluationContext expressionContext;

    @Autowired
    public DatatableCommandFromApiJsonDeserializer(final FromJsonHelper fromApiJsonHelper) {
        this.fromApiJsonHelper = fromApiJsonHelper;
        // Initiate expression parser:
        this.expressionContext = new StandardEvaluationContext();
    }

    private void validateType(final DataValidatorBuilder baseDataValidator, final JsonElement column) {
        final String type = this.fromApiJsonHelper.extractStringNamed("type", column);
        baseDataValidator.reset().parameter("type").value(type).notBlank().isOneOfTheseStringValues(this.supportedColumnTypes);

        if (type != null && type.equalsIgnoreCase("String")) {
            if (this.fromApiJsonHelper.parameterExists("length", column)) {
                final String lengthStr = this.fromApiJsonHelper.extractStringNamed("length", column);
                if (lengthStr != null && !StringUtils.isWhitespace(lengthStr) && StringUtils.isNumeric(lengthStr)
                        && StringUtils.isNotBlank(lengthStr)) {
                    final Integer length = Integer.parseInt(lengthStr);
                    baseDataValidator.reset().parameter("length").value(length).positiveAmount();
                } else if (StringUtils.isBlank(lengthStr) || StringUtils.isWhitespace(lengthStr)) {
                    baseDataValidator.reset().parameter("length").failWithCode("must.be.provided.when.type.is.String");
                } else if (!StringUtils.isNumeric(lengthStr)) {
                    baseDataValidator.reset().parameter("length").failWithCode("not.greater.than.zero");
                }
            } else {
                baseDataValidator.reset().parameter("length").failWithCode("must.be.provided.when.type.is.String");
            }
        } else {
            baseDataValidator.reset().parameter("length").mustBeBlankWhenParameterProvidedIs("type", type);
        }

        final String code = this.fromApiJsonHelper.extractStringNamed("code", column);
        if (type != null && ( type.equalsIgnoreCase("Dropdown") || type.equalsIgnoreCase("Checkbox"))) {
            if (code != null) {
                baseDataValidator.reset().parameter("code").value(code).notBlank().matchesRegularExpression(DATATABLE_NAME_REGEX_PATTERN);
            } else {
                baseDataValidator.reset().parameter("code").value(code).cantBeBlankWhenParameterProvidedIs("type", type);
            }
        } else {
            baseDataValidator.reset().parameter("code").value(code).mustBeBlankWhenParameterProvided("type", type);
        }


    }

    public void validateForCreate(final String json) {
        if (StringUtils.isBlank(json)) { throw new InvalidJsonException(); }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, this.supportedParametersForCreate);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("datatable");

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final String datatableName = this.fromApiJsonHelper.extractStringNamed("datatableName", element);
        baseDataValidator.reset().parameter("datatableName").value(datatableName).notBlank().notExceedingLengthOf(50)
                .matchesRegularExpression(DATATABLE_NAME_REGEX_PATTERN);

        final String apptableName = this.fromApiJsonHelper.extractStringNamed("apptableName", element);
        baseDataValidator.reset().parameter("apptableName").value(apptableName).notBlank().notExceedingLengthOf(50)
                .isOneOfTheseValues(this.supportedApptableNames);
        final String fkColumnName = (apptableName != null) ? apptableName.substring(2) + "_id" : "";

        final Boolean multiRow = this.fromApiJsonHelper.extractBooleanNamed("multiRow", element);
        baseDataValidator.reset().parameter("multiRow").value(multiRow).ignoreIfNull().notBlank().isOneOfTheseValues(true, false);

        final JsonArray columns = this.fromApiJsonHelper.extractJsonArrayNamed("columns", element);
        baseDataValidator.reset().parameter("columns").value(columns).notNull().jsonArrayNotEmpty();

        final String categoryIdParameterName = "category";
        if (this.fromApiJsonHelper.parameterExists(categoryIdParameterName, element)) {
            final Long categoryId = this.fromApiJsonHelper.extractLongNamed(categoryIdParameterName, element);
            baseDataValidator.reset().parameter(categoryIdParameterName).value(categoryId).ignoreIfNull().integerGreaterThanZero();
        }

        final Boolean metaData = this.fromApiJsonHelper.extractBooleanNamed("metaData", element);
        baseDataValidator.reset().parameter("metaData").value(metaData).ignoreIfNull().notBlank().isOneOfTheseValues(true, false);

        if (columns != null) {
            for (final JsonElement column : columns) {
                this.fromApiJsonHelper.checkForUnsupportedParameters(column.getAsJsonObject(), this.supportedParametersForCreateColumns);

                final String name = this.fromApiJsonHelper.extractStringNamed("name", column);
                baseDataValidator.reset().parameter("name").value(name).notBlank().isNotOneOfTheseValues("id", fkColumnName)
                        .matchesRegularExpression(DATATABLE_COLUMN_NAME_REGEX_PATTERN);

                validateType(baseDataValidator, column);

                final Boolean mandatory = this.fromApiJsonHelper.extractBooleanNamed("mandatory", column);
                baseDataValidator.reset().parameter("mandatory").value(mandatory).ignoreIfNull().notBlank().isOneOfTheseValues(true, false);
                if(metaData !=null && metaData){
                    final String labelName = this.fromApiJsonHelper.extractStringNamed("labelName",column);
                    baseDataValidator.reset().parameter("labelName").value(labelName).notBlank()
                            .notExceedingLengthOf(300);
                    final Integer order = this.fromApiJsonHelper.extractIntegerSansLocaleNamed("order",column);
                    baseDataValidator.reset().parameter("order").value(order).notNull().integerGreaterThanZero();

                }

                // Add to parser with sample values::
                final String type = this.fromApiJsonHelper.extractStringNamed("type", column);
                this.expressionContext.setVariable(name, sampleExpressionValues.get(type.toLowerCase()));
            }

            // run through all columns again to parse any expressions and test them:
            for (final JsonElement column : columns) {
                // Find the displayCondition:
                final String displayCondition = this.fromApiJsonHelper.extractStringNamed("displayCondition", column);
                if (displayCondition != null && !StringUtils.isWhitespace(displayCondition) && StringUtils.isNotBlank(displayCondition)) {
                    // Try the condition:
                    baseDataValidator.reset().parameter("displayCondition").value(displayCondition).validateBooleanExpression(this.expressionContext);
                }

                // Find the formulaExpressions:
                final String formulaExpression = this.fromApiJsonHelper.extractStringNamed("formulaExpression", column);
                if (formulaExpression != null && !StringUtils.isWhitespace(formulaExpression) && StringUtils.isNotBlank(formulaExpression)) {
                    // Try the condition:
                    baseDataValidator.reset().parameter("formulaExpression").value(formulaExpression).validateObjectExpression(this.expressionContext);
                }

            }


        }

        final String displayName = "displayName";
        if (this.fromApiJsonHelper.parameterExists(displayName, element)) {
            final String displayNameVal = this.fromApiJsonHelper.extractStringNamed(displayName, element);
            baseDataValidator.reset().parameter(displayName).value(displayNameVal).notNull().notBlank();
        }



        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }



    public void validateForUpdate(final String json, final Map<String, ResultsetColumnHeaderData> allColumns) {
        if (StringUtils.isBlank(json)) { throw new InvalidJsonException(); }
        // Because all parameters are optional, a check to see if at least one
        // parameter
        // has been specified is necessary in order to avoid JSON requests with
        // no parameters
        if (!json.matches("(?s)\\A\\{.*?(\\\".*?\\\"\\s*?:\\s*?)+.*?\\}\\z")) { throw new PlatformDataIntegrityException(
                "error.msg.invalid.request.body.no.parameters", "Provided JSON request body does not have any parameters."); }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, this.supportedParametersForUpdate);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("datatable");

        final JsonElement element = this.fromApiJsonHelper.parse(json);
        final String apptableName = this.fromApiJsonHelper.extractStringNamed("apptableName", element);
        baseDataValidator.reset().parameter("apptableName").value(apptableName).ignoreIfNull().notBlank()
                .isOneOfTheseValues(this.supportedApptableNames);
        final String fkColumnName = (apptableName != null) ? apptableName.substring(2) + "_id" : "";

        final JsonArray changeColumns = this.fromApiJsonHelper.extractJsonArrayNamed("changeColumns", element);
        baseDataValidator.reset().parameter("changeColumns").value(changeColumns).ignoreIfNull().jsonArrayNotEmpty();

        final Boolean metaData = this.fromApiJsonHelper.extractBooleanNamed("metaData", element);
        baseDataValidator.reset().parameter("metaData").value(metaData).ignoreIfNull().notBlank().isOneOfTheseValues(true, false);

        final List<String> affectedColumns = new ArrayList<>();

        if (changeColumns != null) {
            for (final JsonElement column : changeColumns) {
                this.fromApiJsonHelper.checkForUnsupportedParameters(column.getAsJsonObject(), this.supportedParametersForChangeColumns);

                final String name = this.fromApiJsonHelper.extractStringNamed("name", column);
                baseDataValidator.reset().parameter("name").value(name).notBlank().isNotOneOfTheseValues("id", fkColumnName)
                        .matchesRegularExpression(DATATABLE_COLUMN_NAME_REGEX_PATTERN);

                final String newName = this.fromApiJsonHelper.extractStringNamed("newName", column);
                baseDataValidator.reset().parameter("newName").value(newName).ignoreIfNull().notBlank().notExceedingLengthOf(50)
                        .isNotOneOfTheseValues("id", fkColumnName).matchesRegularExpression(DATATABLE_NAME_REGEX_PATTERN);

                if (this.fromApiJsonHelper.parameterExists("length", column)) {
                    final String lengthStr = this.fromApiJsonHelper.extractStringNamed("length", column);
                    if (StringUtils.isWhitespace(lengthStr) || !StringUtils.isNumeric(lengthStr) || StringUtils.isBlank(lengthStr)) {
                        baseDataValidator.reset().parameter("length").failWithCode("not.greater.than.zero");
                    } else {
                        final Integer length = Integer.parseInt(lengthStr);
                        baseDataValidator.reset().parameter("length").value(length).ignoreIfNull().notBlank().positiveAmount();
                    }
                }

                final String code = this.fromApiJsonHelper.extractStringNamed("code", column);
                baseDataValidator.reset().parameter("code").value(code).ignoreIfNull().notBlank().notExceedingLengthOf(100)
                        .matchesRegularExpression(DATATABLE_COLUMN_NAME_REGEX_PATTERN);

                final String newCode = this.fromApiJsonHelper.extractStringNamed("newCode", column);
                baseDataValidator.reset().parameter("newCode").value(newCode).ignoreIfNull().notBlank().notExceedingLengthOf(100)
                        .matchesRegularExpression(DATATABLE_COLUMN_NAME_REGEX_PATTERN);

                if (StringUtils.isBlank(code) && StringUtils.isNotBlank(newCode)) {
                    baseDataValidator.reset().parameter("code").value(code).cantBeBlankWhenParameterProvidedIs("newCode", newCode);
                }

                final Boolean mandatory = this.fromApiJsonHelper.extractBooleanNamed("mandatory", column);
                baseDataValidator.reset().parameter("mandatory").value(mandatory).ignoreIfNull().notBlank().isOneOfTheseValues(true, false);

                final Boolean after = this.fromApiJsonHelper.extractBooleanNamed("after", column);
                baseDataValidator.reset().parameter("after").value(after).ignoreIfNull().notBlank().isOneOfTheseValues(true, false);

                final String displayCondition = this.fromApiJsonHelper.extractStringNamed("displayCondition", column);
                baseDataValidator.reset().parameter("displayCondition").value(displayCondition).ignoreIfNull().notBlank();

                final String formulaExpression = this.fromApiJsonHelper.extractStringNamed("formulaExpression", column);
                baseDataValidator.reset().parameter("formulaExpression").value(formulaExpression).ignoreIfNull().notBlank();

                // Add to list of affected columns so we can ignore them when fetching the original data:
                affectedColumns.add(name);

                final String type = allColumns.get(name).getColumnType();
                this.expressionContext.setVariable(newName, sampleExpressionValues.get(type.toLowerCase()));
            }
        }

        final JsonArray addColumns = this.fromApiJsonHelper.extractJsonArrayNamed("addColumns", element);
        baseDataValidator.reset().parameter("addColumns").value(addColumns).ignoreIfNull().jsonArrayNotEmpty();

        if (addColumns != null) {
            for (final JsonElement column : addColumns) {
                this.fromApiJsonHelper.checkForUnsupportedParameters(column.getAsJsonObject(), this.supportedParametersForAddColumns);

                final String name = this.fromApiJsonHelper.extractStringNamed("name", column);
                baseDataValidator.reset().parameter("name").value(name).notBlank().isNotOneOfTheseValues("id", fkColumnName)
                        .matchesRegularExpression(DATATABLE_COLUMN_NAME_REGEX_PATTERN);

                validateType(baseDataValidator, column);

                final Boolean mandatory = this.fromApiJsonHelper.extractBooleanNamed("mandatory", column);
                baseDataValidator.reset().parameter("mandatory").value(mandatory).ignoreIfNull().notBlank().isOneOfTheseValues(true, false);

                final Boolean after = this.fromApiJsonHelper.extractBooleanNamed("after", column);
                baseDataValidator.reset().parameter("after").value(after).ignoreIfNull().notBlank().isOneOfTheseValues(true, false);
                if(metaData !=null && metaData){
                    final String labelName = this.fromApiJsonHelper.extractStringNamed("labelName",column);
                    baseDataValidator.reset().parameter("labelName").value(labelName).notBlank()
                            .notExceedingLengthOf(300);
                    final Integer order = this.fromApiJsonHelper.extractIntegerSansLocaleNamed("order",column);
                    baseDataValidator.reset().parameter("order").value(order).notNull().integerGreaterThanZero();
                }

                // Add to parser with sample values::
                final String type = this.fromApiJsonHelper.extractStringNamed("type", column);
                this.expressionContext.setVariable(name, sampleExpressionValues.get(type.toLowerCase()));
            }
        }

        final JsonArray dropColumns = this.fromApiJsonHelper.extractJsonArrayNamed("dropColumns", element);
        baseDataValidator.reset().parameter("dropColumns").value(dropColumns).ignoreIfNull().jsonArrayNotEmpty();

        if (dropColumns != null) {
            for (final JsonElement column : dropColumns) {
                this.fromApiJsonHelper.checkForUnsupportedParameters(column.getAsJsonObject(), this.supportedParametersForDropColumns);

                final String name = this.fromApiJsonHelper.extractStringNamed("name", column);
                baseDataValidator.reset().parameter("name").value(name).notBlank().isNotOneOfTheseValues("id", fkColumnName)
                        .matchesRegularExpression(DATATABLE_COLUMN_NAME_REGEX_PATTERN);

                affectedColumns.add(name);
            }
        }

        // Loop through the existing columns:
        for(ResultsetColumnHeaderData column : allColumns.values())
        {
            if(!affectedColumns.contains(column.getColumnName()))
            {
                this.expressionContext.setVariable(column.getColumnName(), sampleExpressionValues.get(column.getColumnType().toLowerCase()));
            }
        }



        // IF this column is affected either ignore (for deleted), or grab the correct values (for new or edit):
        if (changeColumns != null) {
            for (JsonElement changedColumn : changeColumns) {
                final String displayCondition = this.fromApiJsonHelper.extractStringNamed("displayCondition", changedColumn);
                if (displayCondition != null) {
                    baseDataValidator.reset().parameter("displayCondition").value(displayCondition).validateBooleanExpression(this.expressionContext);
                }
            }
        }

        if (addColumns != null) {
            for (JsonElement addedColumn : addColumns) {
                final String displayCondition = this.fromApiJsonHelper.extractStringNamed("displayCondition", addedColumn);
                if (displayCondition != null) {
                    baseDataValidator.reset().parameter("displayCondition").value(displayCondition).validateBooleanExpression(this.expressionContext);
                }
            }
        }

        // Loop through the columns again doing a find and replace stuff:
        for(ResultsetColumnHeaderData column : allColumns.values())
        {

            if(!affectedColumns.contains(column.getColumnName()))
            {
                // Find the displayCondition:
                final String displayCondition = column.getColumnDisplayExpression();
                if (displayCondition != null && !StringUtils.isWhitespace(displayCondition) && StringUtils.isNotBlank(displayCondition)) {
                    // Try the condition:
                    baseDataValidator.reset().parameter("displayCondition").value(displayCondition).validateBooleanExpression(this.expressionContext);
                }

                // Find the formulaExpressions:
                final String formulaExpression = column.getColumnFormulaExpression();
                if (formulaExpression != null && !StringUtils.isWhitespace(formulaExpression) && StringUtils.isNotBlank(formulaExpression)) {
                    // Try the condition:
                    baseDataValidator.reset().parameter("formulaExpression").value(formulaExpression).validateObjectExpression(this.expressionContext);
                }
            }
        }


        final String categoryIdParameterName = "category";
        if (this.fromApiJsonHelper.parameterExists(categoryIdParameterName, element)) {
            final Long categoryId = this.fromApiJsonHelper.extractLongNamed(categoryIdParameterName, element);
            baseDataValidator.reset().parameter(categoryIdParameterName).value(categoryId).ignoreIfNull().integerGreaterThanZero();
        }

        final String displayName = "displayName";
        if (this.fromApiJsonHelper.parameterExists(displayName, element)) {
            final String displayNameVal = this.fromApiJsonHelper.extractStringNamed(displayName, element);
            baseDataValidator.reset().parameter(displayName).value(displayNameVal).notNull().notBlank();
        }

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    private void throwExceptionIfValidationWarningsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) { throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist",
                "Validation errors exist.", dataValidationErrors); }
    }
}
