/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.organisation.staff.service;

import java.util.ArrayList;
import java.util.Collection;

import org.mifosplatform.organisation.staff.data.StaffData;

public interface StaffReadPlatformService {

    StaffData retrieveStaff(Long staffId);

    Collection<StaffData> retrieveAllStaffForDropdown(Long officeId);

    Collection<StaffData> retrieveAllLoanOfficersInOfficeById(final Long officeId);

    /**
     * returns all staff in offices that are above the provided
     * <code>officeId</code>.
     */
    Collection<StaffData> retrieveAllStaffInOfficeAndItsParentOfficeHierarchy(Long officeId, boolean loanOfficersOnly);

    Collection<StaffData> retrieveAllStaffForDropdownLinkedToUser(Long officeId);

    Collection<StaffData> retrieveAllStaff(String sqlSearch, Long officeId, boolean loanOfficersOnly, String status);
    
    Object[] hasAssociatedItems(final Long staffId);
    
    ArrayList<String> hasAssociatedClients(final Long staffId);
    
    ArrayList<String> hasAssociatedGroups(final Long staffId);
    
    ArrayList<String> hasAssociatedLoans(final Long staffId);
    
    ArrayList<String> hasAssociatedSavingsAccounts(final Long staffId);
}