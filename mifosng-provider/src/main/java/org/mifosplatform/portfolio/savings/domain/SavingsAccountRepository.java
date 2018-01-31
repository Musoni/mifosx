/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.portfolio.savings.domain;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SavingsAccountRepository extends JpaRepository<SavingsAccount, Long>, JpaSpecificationExecutor<SavingsAccount> {


    @Query("from SavingsAccount s_acc where s_acc.client.id = :clientId")
    List<SavingsAccount> findSavingAccountByClientId(@Param("clientId") Long clientId);

    @Query("from SavingsAccount s_acc where s_acc.status = :status")
    List<SavingsAccount> findSavingAccountByStatus(@Param("status") Integer status);

    @Query("from SavingsAccount s_acc where s_acc.status = :status and (s_acc.nominalAnnualInterestRate !=0 or s_acc.nominalAnnualInterestRateOverdraft !=0)")
    List<SavingsAccount> findSavingAccountByStatusAndInterest(@Param("status") Integer status);

    @Query("from SavingsAccount sa where sa.client.id = :clientId and sa.group.id = :groupId")
    List<SavingsAccount> findByClientIdAndGroupId(@Param("clientId") Long clientId, @Param("groupId") Long groupId);

    @Query("select case when (count (saving) > 0) then true else false end from SavingsAccount saving where saving.client.id = :clientId and saving.status in (100,200,300,303,304)")
    boolean doNonClosedSavingAccountsExistForClient(@Param("clientId") Long clientId);

    @Query("from SavingsAccount sa where sa.client.id is null and sa.group.id = :groupId")
    List<SavingsAccount> findByGroupId(@Param("groupId") Long groupId);

    @Query("from SavingsAccount sa where sa.id = :accountId and sa.depositType = :depositAccountTypeId")
    SavingsAccount findByIdAndDepositAccountType(@Param("accountId") Long accountId,
            @Param("depositAccountTypeId") Integer depositAccountTypeId);

    @Query("select case when (count (saving) > 0) then true else false end from SavingsAccount saving where saving.group.id = :groupId and saving.status in (100,200,300,303,304)")
    boolean doNonClosedSavingAccountsExistForGroup(@Param("groupId") Long groupId);

    @Query(value="select * from m_savings_account sa where not exists (select * from m_savings_account_charge mg where mg.charge_id = :chargeId and mg.savings_account_id = sa.id ) and sa.product_id = :productId and sa.deposit_type_enum=100 and sa.status_enum =300 group by sa.id", nativeQuery = true)
    List<SavingsAccount> savingsAccountWithoutCharge(@Param("productId") Long productId,@Param("chargeId") Long chargeId);

    @Query("select s_acc.id from SavingsAccount s_acc")
    List<Long> retrieveIdOfAllSavingsAccounts();
}