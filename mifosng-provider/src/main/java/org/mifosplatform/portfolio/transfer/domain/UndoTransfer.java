/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.portfolio.transfer.domain;

import org.joda.time.LocalDate;
import org.mifosplatform.organisation.office.domain.Office;
import org.mifosplatform.organisation.staff.domain.Staff;
import org.mifosplatform.portfolio.client.domain.Client;
import org.mifosplatform.portfolio.group.domain.Group;
import org.mifosplatform.useradministration.domain.AppUser;
import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

@Entity
@Table(name="m_undo_transfer")
public class UndoTransfer extends AbstractPersistable<Long> {

    @ManyToOne(optional = true)
    @JoinColumn(name = "client_id", nullable = true)
    private Client client;

    @ManyToOne(optional = true)
    @JoinColumn(name = "group_id", nullable = true)
    private Group group;

    @ManyToOne(optional = true)
    @JoinColumn(name = "staff_id", nullable = true)
    private Staff staff;

    @Column(name = "transfer_from_office_id", nullable = true)
    private Long transferFromOfficeId;

    @Column(name = "transfer_from_group_id", nullable = true)
    private Long transferFromGroupId;

    @Column(name = "transfer_from_staff_id", nullable = true)
    private Long transferFromStaffId;

    @Column(name = "submittedon_date", nullable = true)
    @Temporal(TemporalType.DATE)
    private Date submittedOnDate;

    @ManyToOne(optional = true)
    @JoinColumn(name = "submittedon_userid", nullable = true)
    private AppUser submittedBy;

    @Column(name = "approvedon_date", nullable = true)
    @Temporal(TemporalType.DATE)
    private Date approvedOnDate;

    @ManyToOne(optional = true)
    @JoinColumn(name = "approvedon_userid", nullable = true)
    private AppUser approvedBy;

    @Column(name = "office_joining_date", nullable = true)
    @Temporal(TemporalType.DATE)
    private Date officeJoiningDate;

    @Column(name = "is_group_transfer", nullable = false)
    private boolean groupTransfer;

    @Column(name = "is_transfer_undone", nullable = false)
    private boolean transferUndone;

    public UndoTransfer() {
    }

    private UndoTransfer(Client client, Group group, Staff staff, Long transferFromOfficeId, Long transferFromGroupId,
                        Long transferFromStaffId, Date submittedOnDate, AppUser submittedBy, Date approvedOnDate,
                        AppUser approvedBy,Date officeJoiningDate) {
        this.client = client;
        this.group = group;
        this.staff = staff;
        this.transferFromOfficeId = transferFromOfficeId;
        this.transferFromGroupId = transferFromGroupId;
        this.transferFromStaffId = transferFromStaffId;
        this.submittedOnDate = submittedOnDate;
        this.submittedBy = submittedBy;
        this.approvedOnDate = approvedOnDate;
        this.approvedBy = approvedBy;
        this.officeJoiningDate = officeJoiningDate;

    }

    public static UndoTransfer instance(final Client client, final Group group,final Staff staff,final Long transferFromOfficeId,
                                        final Long transferFromGroupId,final Long transferFromStaffId,final Date submittedOnDate,
                                        final AppUser submittedBy, final Date approvedOnDate,final AppUser approvedBy,final Date officeJoiningDate){
        return new UndoTransfer(client,group,staff,transferFromOfficeId,transferFromGroupId,transferFromStaffId,submittedOnDate,submittedBy,
                approvedOnDate,approvedBy,officeJoiningDate);
    }

    public Client getClient() {
        return this.client;
    }

    public Long getTransferFromStaffId() {
        return this.transferFromStaffId;
    }

    public Long getTransferFromGroupId() {
        return this.transferFromGroupId;
    }

    public Staff getStaff() {
        return this.staff;
    }

    public Group getGroup() {
        return this.group;
    }

    public Long getTransferFromOfficeId() {
        return this.transferFromOfficeId;
    }

    public Date getOfficeJoiningDate() {
        return this.officeJoiningDate;
    }

    public void setGroupTransfer(boolean groupTransfer) {
        this.groupTransfer = groupTransfer;
    }

    public boolean isGroupTransfer() {
        return this.groupTransfer;
    }

    public boolean isTransferUndone() {
        return this.transferUndone;
    }

    public void updateTransferUndone(boolean transferUndone) {
        this.transferUndone = transferUndone;
    }

    public LocalDate getSubmittedOnDate() {
        return new LocalDate(this.submittedOnDate);
    }
}
