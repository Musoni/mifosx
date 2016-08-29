/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataqueries.domain;
/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import org.mifosplatform.infrastructure.core.api.JsonCommand;
import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.*;

@Entity
@Table(name = "m_entity_datatable_check")

public class EntityDatatableChecks extends AbstractPersistable<Long> {

    @Column(name="application_table_name", nullable = false)
    private String entity;

    @OneToOne(fetch= FetchType.LAZY)
    @JoinColumn(name="x_registered_table_id",insertable=false, updatable=false)
    private RegisteredTable datatable;

    @Column(name="x_registered_table_id", nullable = false)
    private Long datatableId;

    @Column(name = "status_enum",nullable = false)
    private Long status;

    @Column(name="system_defined")
    private boolean systemDefined;


    @Column(name="product_loan_id", nullable = false)
    private Long productLoanId;



    public EntityDatatableChecks() {
    }

    public EntityDatatableChecks(final String entity, final Long datatableId, final Long status, final boolean systemDefined, final Long productLoanId) {

        this.entity = entity;
        this.status = status;
        this.datatableId = datatableId;
        this.systemDefined = systemDefined;
        this.productLoanId = productLoanId;
    }

    public static EntityDatatableChecks fromJson(final JsonCommand command){

        final String entity = command.stringValueOfParameterNamed("entity");
        final Long status = command.longValueOfParameterNamed("status");
        final Long datatableId=command.longValueOfParameterNamed("datatableId");

        boolean systemDefined = false;

        if(command.parameterExists("systemDefined")){
             systemDefined = command.booleanObjectValueOfParameterNamed("systemDefined");
        }else{
            systemDefined = false;
        }

        Long productLoanId =null;

        if(command.parameterExists("productId")){
           productLoanId = command.longValueOfParameterNamed("productId");
        }

        return new EntityDatatableChecks(entity,datatableId,status,systemDefined,productLoanId);

    }

    public String getEntity() {
        return this.entity;
    }

    public Long getStatus() {
        return this.status;
    }

    public Long getDatatableId() {
        return this.datatableId;
    }

    public boolean isSystemDefined() {return this.systemDefined;}

    public RegisteredTable getDatatable(){return this.datatable;}

    public Long getProductLoanId() {
        return productLoanId;
    }
}
