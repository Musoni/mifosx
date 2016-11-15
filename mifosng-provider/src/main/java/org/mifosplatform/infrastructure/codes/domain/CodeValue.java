/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.codes.domain;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.apache.commons.lang.StringUtils;
import org.mifosplatform.infrastructure.codes.CodeConstants.CODEVALUE_JSON_INPUT_PARAMS;
import org.mifosplatform.infrastructure.codes.data.CodeValueData;
import org.mifosplatform.infrastructure.core.api.JsonCommand;
import org.springframework.data.jpa.domain.AbstractPersistable;

@Entity
@Table(name = "m_code_value", uniqueConstraints = { @UniqueConstraint(columnNames = { "code_id", "code_value" }, name = "unique_code_value") })
public class CodeValue extends AbstractPersistable<Long> {

    @Column(name = "code_value", length = 100)
    private String label;

    @Column(name = "order_position")
    private int position;

    @Column(name = "code_description")
    private String description;

    @ManyToOne
    @JoinColumn(name = "code_id", nullable = false)
    private Code code;
    
    @Column(name = "is_active")
    private boolean isActive;
    
    @Column(name = "is_mandatory")
    private boolean isMandatory;

    public static CodeValue createNew(final Code code, final String label, final int position, final String description,
            final boolean isActive, final boolean isMandatory) {
        return new CodeValue(code, label, position, description, isActive, isMandatory);
    }

    protected CodeValue() {
        //
    }

    private CodeValue(final Code code, final String label, final int position, final String description, final boolean isActive, 
            final boolean isMandatory) {
        this.code = code;
        this.label = StringUtils.defaultIfEmpty(label, null);
        this.position = position;
        this.description = description;
        this.isActive = isActive;
        this.isMandatory = isMandatory;
    }

    public String label() {
        return this.label;
    }

    public int position() {
        return this.position;
    }

    public static CodeValue fromJson(final Code code, final JsonCommand command) {

        final String label = command.stringValueOfParameterNamed(CODEVALUE_JSON_INPUT_PARAMS.NAME.getValue());
        Integer position = command.integerValueSansLocaleOfParameterNamed(CODEVALUE_JSON_INPUT_PARAMS.POSITION.getValue());
        String description = command.stringValueOfParameterNamed(CODEVALUE_JSON_INPUT_PARAMS.DESCRIPTION.getValue());
        Boolean isActiveObj = command.booleanObjectValueOfParameterNamed(CODEVALUE_JSON_INPUT_PARAMS.IS_ACTIVE.getValue());
        boolean isActive = true;
        if (isActiveObj != null) {
            isActive = isActiveObj;
        }
        if (position == null) {
            position = new Integer(0);
        }
        
        final boolean isMandatory = command.booleanPrimitiveValueOfParameterNamed(CODEVALUE_JSON_INPUT_PARAMS.IS_MANDATORY.getValue());

        return new CodeValue(code, label, position.intValue(), description, isActive, isMandatory);
    }

    public Map<String, Object> update(final JsonCommand command) {

        final Map<String, Object> actualChanges = new LinkedHashMap<>(2);

        final String labelParamName = CODEVALUE_JSON_INPUT_PARAMS.NAME.getValue();
        if (command.isChangeInStringParameterNamed(labelParamName, this.label)) {
            final String newValue = command.stringValueOfParameterNamed(labelParamName);
            actualChanges.put(labelParamName, newValue);
            this.label = StringUtils.defaultIfEmpty(newValue, null);
        }

        final String decriptionParamName = CODEVALUE_JSON_INPUT_PARAMS.DESCRIPTION.getValue();
        if (command.isChangeInStringParameterNamed(decriptionParamName, this.description)) {
            final String newValue = command.stringValueOfParameterNamed(decriptionParamName);
            actualChanges.put(decriptionParamName, newValue);
            this.description = StringUtils.defaultIfEmpty(newValue, null);
        }

        final String positionParamName = CODEVALUE_JSON_INPUT_PARAMS.POSITION.getValue();
        if (command.isChangeInIntegerSansLocaleParameterNamed(positionParamName, this.position)) {
            final Integer newValue = command.integerValueSansLocaleOfParameterNamed(positionParamName);
            actualChanges.put(positionParamName, newValue);
            this.position = newValue.intValue();
        }
        
        final String isMandatoryParamName = CODEVALUE_JSON_INPUT_PARAMS.IS_MANDATORY.getValue();
        if (command.isChangeInBooleanParameterNamed(isMandatoryParamName, this.isMandatory)) {
            final boolean isMandatory = command.booleanPrimitiveValueOfParameterNamed(isMandatoryParamName);
            actualChanges.put(isMandatoryParamName, isMandatory);
            
            this.isMandatory = isMandatory;
        }

        final String isActiveParamName = CODEVALUE_JSON_INPUT_PARAMS.IS_ACTIVE.getValue();
        if (command.isChangeInBooleanParameterNamed(isActiveParamName, this.isActive)) {
            final Boolean newValue = command.booleanPrimitiveValueOfParameterNamed(isActiveParamName);
            actualChanges.put(isActiveParamName, newValue);
            this.isActive = newValue.booleanValue();
        }

        return actualChanges;
    }

    public CodeValueData toData() {
        return CodeValueData.instance(getId(), this.label, this.position, this.description, 
                this.isActive, this.isMandatory);
    }
    
    /** 
     * set the isActive property to false 
     * 
     * @return None
     **/
    public void delete() {
        this.isActive = false;
        
        // update the name of the code value
        this.label = this.label + "_deleted_" + this.getId();
    }
    
    /** 
     * @return the "deleted" property (true/false) 
     **/
    public boolean isDeleted() {
        return !this.isActive;
    }
    
    /**
     * @return true if the "isActive" property is true, else false
     */
    public boolean isActive() {
        return this.isActive;
    }

    /**
     * @return true if the isMandatory property is true
     */
    public boolean isMandatory() { return this.isMandatory; }
}

