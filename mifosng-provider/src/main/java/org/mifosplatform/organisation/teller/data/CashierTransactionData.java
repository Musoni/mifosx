/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.organisation.teller.data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;

import org.mifosplatform.organisation.monetary.data.CurrencyData;
import org.mifosplatform.organisation.teller.domain.CashierTxnType;

public final class CashierTransactionData implements Serializable {

    private final Long id;
    private final Long cashierId;
    private final CashierTxnType txnType;
    private final BigDecimal txnAmount;
    private final BigDecimal runningBalance;
    private final Date txnDate;
    private final Long entityId;
    private final String entityType;
    private final String txnNote;
    private final Date createdDate;
    private final String currencyCode;
    
    // Template fields
    private final Long officeId;
    private final String officeName;
    private final Long tellerId;
    private final String tellerName;
    private final String cashierName;
        
    private final CashierData cashierData;
    private final Date startDate;
    private final Date endDate;

    private final Long clientId;
    private final String clientName;
    private final String accountNo;
    private final String reference;


    
    private final Collection<CurrencyData> currencyOptions;

    /*
     * Creates a new cashier.
     */
    private CashierTransactionData(final Long id, final Long cashierId, CashierTxnType txnType, 
    		final BigDecimal txnAmount, final Date txnDate, String txnNote, 
    		String entityType, Long entityId, Date createdDate, 
    		Long officeId, String officeName, Long tellerId, String tellerName, String cashierName,
    		CashierData cashierData, Date startDate, Date endDate, final Collection<CurrencyData> currencyOptions,final String currencyCode,
                                   final BigDecimal runningBalance,final Long clientId,final String clientName,final String accountNo, final String reference) {
        this.id = id;
        this.cashierId = cashierId;
        this.txnType = txnType;
        this.txnAmount = txnAmount;
        this.txnDate = txnDate;
        this.txnNote = txnNote;
        this.entityType = entityType;
        this.entityId = entityId;
        this.createdDate = createdDate;
        
        this.officeId = officeId;
        this.officeName = officeName;
        this.tellerId = tellerId;
        this.tellerName = tellerName;
        this.cashierName = cashierName;
        this.cashierData = cashierData;
        
        this.startDate = startDate;
        this.endDate = endDate;
        
        this.currencyOptions = currencyOptions;

        this.currencyCode = currencyCode;
        this.runningBalance = runningBalance;
        this.clientId = clientId;
        this.clientName = clientName;
        this.accountNo = accountNo;
        this.reference = reference;
    }

    public static CashierTransactionData instance(final Long id, final Long cashierId, CashierTxnType txnType,
    		final BigDecimal txnAmount, final Date txnDate, final String txnNote,
    		final String entityType, final Long entityId, final Date createdDate,
    		final Long officeId, final String officeName, final Long tellerId,
    		final String tellerName, final String cashierName, final CashierData cashierData,
    		Date startDate, Date endDate,final String currencyCode, final BigDecimal runningBalance) {
        return new CashierTransactionData(id, cashierId, txnType, txnAmount, txnDate, txnNote, entityType, 
        		entityId, createdDate, officeId, officeName, tellerId,
        		tellerName, cashierName, cashierData, startDate, endDate, null,currencyCode,runningBalance,
                null,null,null,null);
    }

    public static CashierTransactionData instance(final Long id, final Long cashierId, CashierTxnType txnType,
                                                  final BigDecimal txnAmount, final Date txnDate, final String txnNote,
                                                  final String entityType, final Long entityId, final Date createdDate,
                                                  final Long officeId, final String officeName, final Long tellerId,
                                                  final String tellerName, final String cashierName, final CashierData cashierData,
                                                  Date startDate, Date endDate,final String currencyCode, final BigDecimal runningBalance,
                                                  final Long clientId, final String clientName, final String accountNo, final String reference) {
        return new CashierTransactionData(id, cashierId, txnType, txnAmount, txnDate, txnNote, entityType,
                entityId, createdDate, officeId, officeName, tellerId,
                tellerName, cashierName, cashierData, startDate, endDate, null,currencyCode,runningBalance,
                clientId,clientName,accountNo,reference);
    }
    
    public static CashierTransactionData template (final Long cashierId,  
    		final Long tellerId, final String tellerName,
    		final Long officeId, final String officeName, final String cashierName,
    		final CashierData cashierData, Date startDate, Date endDate, final Collection<CurrencyData> currencyOptions) {
        return new CashierTransactionData(null, cashierId, null, null, null, null, null, 
        		null, null, officeId, officeName, tellerId, tellerName, cashierName, cashierData,

                startDate, endDate, currencyOptions,null,null,
                null,null,null,null);
    }

    public Long getId() {
        return id;
    }

    public Long getCashierId() {
        return cashierId;
    }
    
    public CashierTxnType getTxnType() {
    	return txnType;
    }
    
    public BigDecimal getTxnAmount() {
    	return txnAmount;
    }
    
    public Date getTxnDate() {
    	return txnDate;
    }
    
    public String getTxnNote() {
    	return txnNote;
    }
    
    public String getEntityType() {
    	return entityType;
    }
    
    public Long getEntityId() {
    	return entityId;
    }

    public Date getCreatedDate() {
    	return createdDate;
    }

    public Long getOfficeId() {
        return officeId;
    }
    
    public String getOfficeName() {
    	return officeName;
    }

    public Long getTellerId() {
        return tellerId;
    }
    
    public String getTellerName() {
    	return tellerName;
    }

    public String getCashierName() {
    	return cashierName;
    }
    
    public Date getStartDate() {
    	return startDate;
    }
    
    public Date getEndDate() {
    	return endDate;
    }

    public CashierData getCashierData() {
    	return cashierData;
    }

    public String currencyCode(){return currencyCode;}

    public BigDecimal getRunningBalance() {
        return runningBalance;
    }
}
