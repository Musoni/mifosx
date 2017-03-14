/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataqueries.data;

import java.util.List;


public class ResultSetCodeValueData {
    private final int id;
    private final String name;
    private final String label;
    private final String defaultValue;
    private final List<ResultsetColumnValueData> columnValues;

    public ResultSetCodeValueData(int id, String name, String label, String defaultValue, List<ResultsetColumnValueData> columnValues) {
        this.id = id;
        this.name = name;
        this.label = label;
        this.defaultValue = defaultValue;
        this.columnValues = columnValues;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public List<ResultsetColumnValueData> getColumnValues() {
        return columnValues;
    }
}