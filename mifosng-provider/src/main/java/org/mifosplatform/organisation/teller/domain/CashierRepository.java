/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.organisation.teller.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.persistence.NamedNativeQuery;
import java.util.List;

/**
 * Provides the domain repository for accessing, adding, modifying or deleting cashiers.
 *
 * @author Markus Geiss
 * @see org.mifosplatform.organisation.teller.domain.Cashier
 * @since 2.0.0
 */
public interface CashierRepository extends JpaRepository<Cashier, Long>, JpaSpecificationExecutor<Cashier> {
    // no added behavior

    public static final String FIND_ACTIVE_TELLER_CASHIER = "from Cashier c where c.teller.id= :tellerId and c.isActive = 1 ";

    public static final String FIND_ACTIVE_CASHIERS = "select * from m_cashiers c where c.staff_id = :staffId and c.teller_id !=:tellerId and c.is_active = 1 ";

    @Query(FIND_ACTIVE_TELLER_CASHIER)
    List<Cashier> getActiveTellerCashier(@Param("tellerId")  Long tellerId);


    @Query(value=FIND_ACTIVE_CASHIERS,nativeQuery = true)
    List<Cashier> getActiveCashier(@Param("staffId")  Long staffId, @Param("tellerId")  Long tellerId);
}
