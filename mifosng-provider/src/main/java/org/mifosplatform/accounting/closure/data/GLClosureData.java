/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.accounting.closure.data;

import org.joda.time.LocalDate;
import org.mifosplatform.organisation.office.data.OfficeData;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Immutable object representing a General Ledger Account
 * 
 * Note: no getter/setters required as google-gson will produce json from fields
 * of object.
 */
public class GLClosureData {

    private final Long id;
    private final Long officeId;
    @SuppressWarnings("unused")
    private final String officeName;
    private final LocalDate closingDate;
    private final boolean deleted;
    @SuppressWarnings("unused")
    private final LocalDate createdDate;
    @SuppressWarnings("unused")
    private final LocalDate lastUpdatedDate;
    @SuppressWarnings("unused")
    private final Long createdByUserId;
    @SuppressWarnings("unused")
    private final String createdByUsername;
    @SuppressWarnings("unused")
    private final Long lastUpdatedByUserId;
    @SuppressWarnings("unused")
    private final String lastUpdatedByUsername;
    @SuppressWarnings("unused")
    private final String comments;

    private Collection<OfficeData> allowedOffices = new ArrayList<>();

    private final String incomeAndExpenseBookingTransactionId;

    public GLClosureData(final Long id, final Long officeId, final String officeName, final LocalDate closingDate, final boolean deleted,
            final LocalDate createdDate, final LocalDate lastUpdatedDate, final Long createdByUserId, final String createdByUsername,
            final Long lastUpdatedByUserId, final String lastUpdatedByUsername, final String comments,
            final String incomeAndExpenseBookingTransactionId) {
        this.id = id;
        this.officeId = officeId;
        this.officeName = officeName;
        this.closingDate = closingDate;
        this.deleted = deleted;
        this.createdDate = createdDate;
        this.lastUpdatedDate = lastUpdatedDate;
        this.createdByUserId = createdByUserId;
        this.createdByUsername = createdByUsername;
        this.lastUpdatedByUserId = lastUpdatedByUserId;
        this.lastUpdatedByUsername = lastUpdatedByUsername;
        this.comments = comments;
        this.allowedOffices = null;
        this.incomeAndExpenseBookingTransactionId = incomeAndExpenseBookingTransactionId;
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    public final Collection<OfficeData> getAllowedOffices() {
        return this.allowedOffices;
    }

    public void setAllowedOffices(final Collection<OfficeData> allowedOffices) {
        this.allowedOffices = allowedOffices;
    }

    /**
     * @return the officeId
     */
    public Long getOfficeId() {
        return officeId;
    }

    /**
     * @return the closingDate
     */
    public LocalDate getClosingDate() {
        return closingDate;
    }

    /**
     * @return deleted
     */
    public boolean isDeleted() {
        return this.deleted;
    }
}