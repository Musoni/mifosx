/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataqueries.data;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.mifosplatform.infrastructure.core.exception.PlatformDataIntegrityException;

/**
 * Immutable data object representing a resultset column.
 */
public final class ResultsetColumnHeaderData {

    private final String columnName;
    private final String columnType;
    private final Long columnLength;
    private final String columnDisplayType;
    private final String columnDisplayExpression;
    private final String columnFormulaExpression;
    private final boolean isColumnNullable;
    @SuppressWarnings("unused")
    private final boolean isColumnPrimaryKey;

    private final List<ResultsetColumnValueData> columnValues;
    private final String columnCode;

    private final String labelName;
    private final Integer order;

    public static ResultsetColumnHeaderData basic(final String columnName, final String columnType) {

        final Long columnLength = null;
        final boolean columnNullable = false;
        final boolean columnIsPrimaryKey = false;
        final List<ResultsetColumnValueData> columnValues = new ArrayList<>();
        final String columnCode = null;
        final String labelName = null;
        final Integer order  = null;
        final String columnDisplayExpression = null;
        final String columnFormulaExpression = null;
        return new ResultsetColumnHeaderData(columnName, columnType, columnLength, columnNullable, columnIsPrimaryKey, columnValues,
                columnCode,labelName,order, columnDisplayExpression, columnFormulaExpression);
    }

    public static ResultsetColumnHeaderData detailed(final String columnName, final String columnType, final Long columnLength,
            final boolean columnNullable, final boolean columnIsPrimaryKey, final List<ResultsetColumnValueData> columnValues,
            final String columnCode, final String columnDisplayExpression, final String columnFormulaExpression  ) {
        return new ResultsetColumnHeaderData(columnName, columnType, columnLength, columnNullable, columnIsPrimaryKey, columnValues,
                columnCode,null,null, columnDisplayExpression, columnFormulaExpression);
    }

    private ResultsetColumnHeaderData(final String columnName, final String columnType, final Long columnLength,
            final boolean columnNullable, final boolean columnIsPrimaryKey, final List<ResultsetColumnValueData> columnValues,
            final String columnCode, final String labelName,final Integer order, final String columnDisplayExpression, final String columnFormulaExpression) {
        this.columnName = columnName;
        this.columnType = columnType;
        this.columnLength = columnLength;
        this.isColumnNullable = columnNullable;
        this.isColumnPrimaryKey = columnIsPrimaryKey;
        this.columnValues = columnValues;
        this.columnCode = columnCode;
        this.labelName = labelName;
        this.order   = order;
        this.columnDisplayExpression = columnDisplayExpression;
        this.columnFormulaExpression = columnFormulaExpression;

        String displayType = null;
        if (this.columnCode == null) {
            if (isString()) {
                displayType = "STRING";
            } else if (isAnyInteger() && ( columnName.startsWith("signature_") || columnName.startsWith("image_") )) {
                displayType = "IMAGE";
            } else if (isAnyInteger()) {
                displayType = "INTEGER";
            }
            else if (isDate()) {
                displayType = "DATE";
            } else if (isDateTime()) {
                displayType = "DATETIME";
            } else if (isDecimal()) {
                displayType = "DECIMAL";
            } else if (isAnyText()) {
                displayType = "TEXT";
            } else if(isBit()) {
                displayType = "BOOLEAN";
            } else {
                throw new PlatformDataIntegrityException("error.msg.invalid.lookup.type", "Invalid Lookup Type:" + this.columnType
                        + " - Column Name: " + this.columnName);
            }

        } else {
            if (isInt()) {
                displayType = "CODELOOKUP";
            } else if(isVarchar() && isCheckboxColumnType()){
                displayType = "CHECKBOX";
            }
            else if (isVarchar()) {
                displayType = "CODEVALUE";
            } else {
                throw new PlatformDataIntegrityException("error.msg.invalid.lookup.type", "Invalid Lookup Type:" + this.columnType
                        + " - Column Name: " + this.columnName);
            }
        }

        this.columnDisplayType = displayType;
    }

    public boolean isNamed(final String columnName) {
        return this.columnName.equalsIgnoreCase(columnName);
    }

    private boolean isAnyText() {
        return isText() || isTinyText() || isMediumText() || isLongText();
    }

    private boolean isText() {
        return "text".equalsIgnoreCase(this.columnType);
    }

    private boolean isTinyText() {
        return "tinytext".equalsIgnoreCase(this.columnType);
    }

    private boolean isMediumText() {
        return "mediumtext".equalsIgnoreCase(this.columnType);
    }

    private boolean isLongText() {
        return "longtext".equalsIgnoreCase(this.columnType);
    }

    private boolean isDecimal() {
        return "decimal".equalsIgnoreCase(this.columnType);
    }

    private boolean isDate() {
        return "date".equalsIgnoreCase(this.columnType);
    }

    private boolean isDateTime() {
        return "datetime".equalsIgnoreCase(this.columnType);
    }

    public boolean isString() {
        return isVarchar() || isChar();
    }

    private boolean isChar() {
        return "char".equalsIgnoreCase(this.columnType);
    }

    private boolean isVarchar() {
        return "varchar".equalsIgnoreCase(this.columnType);
    }

    private boolean isAnyInteger() {
        return isInt() || isSmallInt() || isTinyInt() || isMediumInt() || isBigInt();
    }

    private boolean isInt() {
        return "int".equalsIgnoreCase(this.columnType);
    }

    private boolean isSmallInt() {
        return "smallint".equalsIgnoreCase(this.columnType);
    }

    private boolean isTinyInt() {
        return "tinyint".equalsIgnoreCase(this.columnType);
    }

    private boolean isMediumInt() {
        return "mediumint".equalsIgnoreCase(this.columnType);
    }

    public boolean isBigInt() {
        return "bigint".equalsIgnoreCase(this.columnType);
    }

    private boolean isBit() {
        return "bit".equalsIgnoreCase(this.columnType);
    }

    public String getColumnName() {
        return this.columnName;
    }

    public String getColumnType() {
        return this.columnType;
    }

    public Long getColumnLength() {
        return this.columnLength;
    }

    public String getColumnDisplayType() {
        return this.columnDisplayType;
    }

    public boolean isDateDisplayType() {
        return "DATE".equalsIgnoreCase(this.columnDisplayType);
    }

    public boolean isDateTimeDisplayType() {
        return "DATETIME".equalsIgnoreCase(this.columnDisplayType);
    }

    public boolean isIntegerDisplayType() {
        return "INTEGER".equalsIgnoreCase(this.columnDisplayType);
    }

    public boolean isDecimalDisplayType() {
        return "DECIMAL".equalsIgnoreCase(this.columnDisplayType);
    }

    public boolean isBooleanDisplayType() {
        return "BOOLEAN".equalsIgnoreCase(this.columnDisplayType);
    }

    public boolean isCodeValueDisplayType() {
        return "CODEVALUE".equalsIgnoreCase(this.columnDisplayType);
    }

    public boolean isCheckboxColumnType(){

       if(this.columnName.indexOf("_cb")>0) return true;

        return false;
    }

    public boolean isImage(){
        return "IMAGE".equalsIgnoreCase(this.columnDisplayType);
    }

    public boolean isCodeLookupDisplayType() {
        return "CODELOOKUP".equalsIgnoreCase(this.columnDisplayType);
    }

    public boolean isMandatory() {
        return !isOptional();
    }

    public boolean isOptional() {
        return this.isColumnNullable;
    }

    public boolean hasColumnValues() {
        return !this.columnValues.isEmpty();
    }

    public boolean isColumnValueAllowed(final String match) {
        boolean allowed = false;
        final String unEscapeMatch = StringEscapeUtils.unescapeHtml(match);
        for (final ResultsetColumnValueData allowedValue : this.columnValues) {
            if (allowedValue.matches(unEscapeMatch)) {
                allowed = true;
            }
        }
        return allowed;
    }

    public boolean isColumnValueNotAllowed(final String match) {
        return !isColumnValueAllowed(match);
    }

    public boolean isColumnCodeNotAllowed(final Integer match) {
        return !isColumnCodeAllowed(match);
    }

    public boolean isColumnCodeAllowed(final Integer match) {
        boolean allowed = false;
        for (final ResultsetColumnValueData allowedValue : this.columnValues) {
            if (allowedValue.codeMatches(match)) {
                allowed = true;
            }
        }
        return allowed;
    }

    public String getColumnCode() {
        return this.columnCode;
    }

    public String getLabelName() {
        return this.labelName;
    }

    public Integer getOrder() {
        return this.order;
    }


    public String getColumnDisplayExpression() {
        return columnDisplayExpression;
    }

    public String getColumnFormulaExpression() {
        return columnFormulaExpression;
    }
}