/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataqueries.domain;

import com.google.gson.JsonElement;
import org.apache.commons.lang.StringUtils;
import org.mifosplatform.infrastructure.codes.exception.SystemDefinedCodeCannotBeChangedException;
import org.mifosplatform.infrastructure.core.api.JsonCommand;
import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "x_registered_table_metadata")
public class RegisteredTableMetaData  extends AbstractPersistable<Long> {

    @ManyToOne
    @JoinColumn(name = "registered_table_id")
    private RegisteredTable registeredTable;

    @Column(name = "table_name")
    private String tableName;

    @Column(name = "field_name")
    private String fieldName;

    @Column(name = "label_name")
    private String labelName;

    @Column(name = "ordering")
    private Integer order;

    @Column(name="system_defined")
    private boolean systemDefined;

    @Column(name="display_condition")
    private String displayCondition;

    @Column(name="formula_expression")
    private String formulaExpression;

    @Column(name="type")
    private String type;

    public RegisteredTableMetaData() {
    }

    public static RegisteredTableMetaData createNewRegisterTableMetaData(final RegisteredTable registeredTable,final String tableName,final Map<String,Object> mapObject){
        final String fieldName = mapObject.get("fieldName").toString();
        final String labelName =  mapObject.get("labelName").toString();
        final Integer order    = (Integer) mapObject.get("order");

        String displayCondition = null;
        if(mapObject.get("displayCondition") != null && !mapObject.get("displayCondition").toString().isEmpty()) {
            displayCondition = mapObject.get("displayCondition").toString();
        }

        String formulaExpression = null;
        if(mapObject.get("formulaExpression") != null && !mapObject.get("formulaExpression").toString().isEmpty()) {
            formulaExpression = mapObject.get("formulaExpression").toString();
        }

        String type = null;
        if(mapObject.get("type") != null && !mapObject.get("type").toString().isEmpty()) {
            type = mapObject.get("type").toString();
        }

        return new RegisteredTableMetaData(registeredTable,tableName,fieldName,labelName,order, displayCondition, formulaExpression,type);
    }

    private RegisteredTableMetaData(final RegisteredTable registeredTable,final String tableName, final String fieldName, final String labelName, final Integer order, final String displayCondition, final String formulaExpression,final String type) {
        this.registeredTable = registeredTable;
        this.tableName = tableName;
        this.fieldName = fieldName;
        this.labelName = labelName;
        this.order = order;
        this.displayCondition = displayCondition;
        this.formulaExpression = formulaExpression;
        this.type = type;
    }

    public Map<String, Object> update(final JsonCommand command) {

        final Map<String, Object> actualChanges = new LinkedHashMap<String, Object>(3);

        final String fieldName = "name";
        if (command.isChangeInStringParameterNamed(fieldName, this.fieldName)) {
            final String newValue = command.stringValueOfParameterNamed(fieldName);
            actualChanges.put(fieldName, newValue);
            this.fieldName = StringUtils.defaultIfEmpty(newValue, null);
        }
        final String labelName = " labelName";
        if (command.isChangeInStringParameterNamed(labelName , this.labelName)) {
            final String newValue = command.stringValueOfParameterNamed(labelName);
            actualChanges.put(labelName, newValue);
            this.labelName = StringUtils.defaultIfEmpty(newValue, null);
        }

        if(command.parameterExists("order"))  {
            if (command.isChangeInIntegerParameterNamed("order", this.order)) {
                final Integer newValue = command.integerValueOfParameterNamed("order");
                actualChanges.put("order", newValue);
            }
        }

        if(command.parameterExists("displayCondition"))  {
            if (command.isChangeInStringParameterNamed("displayCondition", this.displayCondition)) {
                final String newValue = command.stringValueOfParameterNamed("displayCondition");
                actualChanges.put("displayCondition", newValue);
            }
        }

        if(command.parameterExists("formulaExpression"))  {
            if (command.isChangeInStringParameterNamed("formulaExpression", this.formulaExpression)) {
                final String newValue = command.stringValueOfParameterNamed("formulaExpression");
                actualChanges.put("formulaExpression", newValue);
            }
        }
        return actualChanges;
    }

    public String getTableName() {
        return this.tableName;
    }

    public String getFieldName() {
        return this.fieldName;
    }

    public String getLabelName() {
        return this.labelName;
    }

    public Integer getOrder() {
        return this.order;
    }

    public RegisteredTable getRegisteredTable() {
        return this.registeredTable;
    }

    public void updateOrder(final Integer order){
        this.order = order;
    }
    public void updateDisplayCondition(final String displayCondition){
        this.displayCondition = displayCondition;
    }

    public void updateFormulaExpression(final String formulaExpression){
        this.formulaExpression = formulaExpression;
    }

    public void updateLabelName(final String labelName){
          this.labelName = labelName;
    }

    public boolean isSystemDefined() { return this.systemDefined;}

    public void updateIsSystemDefined(final boolean systemDefined) { this.systemDefined = systemDefined;}

    public String getDisplayCondition() { return this.displayCondition; }

    public String getFormulaExpression() { return this.formulaExpression; }

    public String getType() { return type;}
}
