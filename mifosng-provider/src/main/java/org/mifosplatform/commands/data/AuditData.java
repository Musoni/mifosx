/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.commands.data;

import org.joda.time.DateTime;

/**
 * Immutable data object representing client data.
 */
public final class AuditData {

    @SuppressWarnings("unused")
    private final Long id;
    @SuppressWarnings("unused")
    private final String actionName;
    private final String entityName;
    @SuppressWarnings("unused")
    private final Long resourceId;
    @SuppressWarnings("unused")
    private final Long subresourceId;
    @SuppressWarnings("unused")
    private final String maker;
    @SuppressWarnings("unused")
    private final DateTime madeOnDate;
    @SuppressWarnings("unused")
    private final String checker;
    @SuppressWarnings("unused")
    private final DateTime checkedOnDate;
    @SuppressWarnings("unused")
    private final String processingResult;
    private String commandAsJson;
    @SuppressWarnings("unused")
    private final String officeName;
    @SuppressWarnings("unused")
    private final String groupLevelName;
    @SuppressWarnings("unused")
    private final String groupName;
    @SuppressWarnings("unused")
    private final String clientName;
    @SuppressWarnings("unused")
    private final String loanAccountNo;
    @SuppressWarnings("unused")
    private final String savingsAccountNo;
    @SuppressWarnings("unused")
    private final Long clientId;
    @SuppressWarnings("unused")
    private final Long loanId;
    @SuppressWarnings("unused")
    private final String url;
    @SuppressWarnings("unused")
    private Double loanDisbursementAmount;
    @SuppressWarnings("unused")
    private DateTime loanDisbursementDate;
    @SuppressWarnings("unused")
    private Double loanOutStanding;
    @SuppressWarnings("unused")
    private String loanStatus;
    @SuppressWarnings("unused")
    private Double savingOutStanding;
    @SuppressWarnings("unused")
    private String savingStatus;
    @SuppressWarnings("unused")
    private Long savingsId;
    @SuppressWarnings("unused")
    private String clientGroupName;

    public AuditData(final Long id, final String actionName, final String entityName, final Long resourceId, final Long subresourceId,
            final String maker, final DateTime madeOnDate, final String checker, final DateTime checkedOnDate,
            final String processingResult, final String commandAsJson, final String officeName, final String groupLevelName,
            final String groupName, final String clientName, final String loanAccountNo, final String savingsAccountNo,
            final Long clientId, final Long loanId, final String url,final Long savingsId) {

        this.id = id;
        this.actionName = actionName;
        this.entityName = entityName;
        this.resourceId = resourceId;
        this.subresourceId = subresourceId;
        this.maker = maker;
        this.madeOnDate = madeOnDate;
        this.checker = checker;
        this.checkedOnDate = checkedOnDate;
        this.commandAsJson = commandAsJson;
        this.processingResult = processingResult;
        this.officeName = officeName;
        this.groupLevelName = groupLevelName;
        this.groupName = groupName;
        this.clientName = clientName;
        this.loanAccountNo = loanAccountNo;
        this.savingsAccountNo = savingsAccountNo;
        this.clientId = clientId;
        this.loanId = loanId;
        this.url = url;
        this.loanDisbursementAmount = null;
        this.loanDisbursementDate = null;
        this.loanOutStanding = null;
        this.loanStatus = null;
        this.savingOutStanding = null;
        this.savingStatus = null;
        this.savingsId = savingsId;
        this.clientGroupName = null;
    }
    
	public AuditData(final Long id, final String actionName, final String entityName, final Long resourceId, final Long subresourceId,
            final String maker, final DateTime madeOnDate, final String checker, final DateTime checkedOnDate,
            final String processingResult, final String commandAsJson, final String officeName, final String groupLevelName,
            final String groupName, final String clientName, final String loanAccountNo, final String savingsAccountNo,
            final Long clientId, final Long loanId, final String url,final Double disbursementAmount, final DateTime disbursementDate,
            final Double loanOutStanding, final String loanStatus,final Double savingOutStanding, final String savingStatus,
            final Long savingsId,final String clientGroupName) {

       this.id = id;
       this.actionName = actionName;
       this.entityName = entityName;
       this.resourceId = resourceId;
       this.subresourceId = subresourceId;
       this.maker = maker;
       this.madeOnDate = madeOnDate;
       this.checker = checker;
       this.checkedOnDate = checkedOnDate;
       this.commandAsJson = commandAsJson;
       this.processingResult = processingResult;
       this.officeName = officeName;
       this.groupLevelName = groupLevelName;
       this.groupName = groupName;
       this.clientName = clientName;
       this.loanAccountNo = loanAccountNo;
       this.savingsAccountNo = savingsAccountNo;
       this.clientId = clientId;
       this.loanId = loanId;
       this.url = url;
       this.loanDisbursementAmount = disbursementAmount;
       this.loanDisbursementDate =disbursementDate;
       this.loanOutStanding =loanOutStanding;
       this.loanStatus = loanStatus;
       this.savingStatus = savingStatus;
       this.savingOutStanding = savingOutStanding;
        this.savingsId = savingsId;
        this.clientGroupName = clientGroupName;
   }

    public void setCommandAsJson(final String commandAsJson) {
        this.commandAsJson = commandAsJson;
    }

    public String getCommandAsJson() {
        return this.commandAsJson;
    }

    public String getEntityName() {
        return this.entityName;
    }
}