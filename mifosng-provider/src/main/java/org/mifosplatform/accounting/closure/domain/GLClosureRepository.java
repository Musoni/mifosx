/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.accounting.closure.domain;

import org.hibernate.dialect.pagination.LimitHandler;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GLClosureRepository extends JpaRepository<GLClosure, Long>, JpaSpecificationExecutor<GLClosure> {

//    @Query("from GLClosure closure where closure.closingDate = (select max(closure1.closingDate) from GLClosure closure1 where closure1.office.id=:officeId and closure1.deleted is false)  and closure.office.id= :officeId and closure.deleted is false")


    public static final String FINDLASTESTGLCLOSUREBYBRANCH = "select * from acc_gl_closure t where t.closing_date =(select max(m.closing_date) from acc_gl_closure m where m.office_id =:officeId and m.is_deleted = 0) and t.office_id =:officeId and t.is_deleted = 0 order by t.id desc limit 1";

    @Query(value=FINDLASTESTGLCLOSUREBYBRANCH,nativeQuery = true)
    GLClosure getLatestGLClosureByBranch(@Param("officeId") Long officeId);
}
