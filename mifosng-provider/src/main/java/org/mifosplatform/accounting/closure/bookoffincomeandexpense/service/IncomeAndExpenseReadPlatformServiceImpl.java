/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.accounting.closure.bookoffincomeandexpense.service;


import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.mifosplatform.accounting.closure.bookoffincomeandexpense.data.IncomeAndExpenseJournalEntryData;
import org.mifosplatform.accounting.glaccount.domain.GLAccountType;
import org.mifosplatform.infrastructure.core.domain.JdbcSupport;
import org.mifosplatform.infrastructure.core.service.RoutingDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Service
public class IncomeAndExpenseReadPlatformServiceImpl implements IncomeAndExpenseReadPlatformService {

    private final JdbcTemplate jdbcTemplate;
    private final DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd");

    @Autowired
    public IncomeAndExpenseReadPlatformServiceImpl(final RoutingDataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }


    private static final class FinancialEndOfYearMapper implements RowMapper<IncomeAndExpenseJournalEntryData> {

        public String schema(){
            return  " je.id,je.account_id,je.reversed,je.office_id,je.is_running_balance_calculated,je.entry_date,je.type_enum,je.amount," +
                    " je.organization_running_balance,je.office_running_balance,je.type_enum,je.description,t.classification_enum as income_or_expense_account," +
                    " t.name as glAccountName" +
                    " from acc_gl_journal_entry as je"+
                    " JOIN (select max(je.id) as id ,ac.classification_enum,ac.name" +
                    " from acc_gl_journal_entry as je right join acc_gl_account as ac on je.account_id = ac.id"+
                    " and ac.classification_enum IN (?,?) where je.entry_date <= ? and je.`reversed` = 0"+
                    " and je.office_id = ?" +
                    " group by je.account_id, je.office_id) as t on je.id = t.id"+
                    " order by je.entry_date,je.id";
        }
        @Override
        public IncomeAndExpenseJournalEntryData mapRow(ResultSet rs, int rowNum) throws SQLException {
            final Long id = rs.getLong("id");
            final Long accountId = rs.getLong("account_id");
            final Long officeId = rs.getLong("office_id");
            final LocalDate entryDate = JdbcSupport.getLocalDate(rs, "entry_date");
            final Boolean reversed = rs.getBoolean("reversed");
            final Boolean isRunningBalanceCalculated = rs.getBoolean("is_running_balance_calculated");
            final String comments = rs.getString("description");
            final BigDecimal officeRunningBalance = rs.getBigDecimal("office_running_balance");
            final BigDecimal organizationRunningBalance = rs.getBigDecimal("organization_running_balance");
            final int accountTypeId = JdbcSupport.getInteger(rs, "income_or_expense_account");
            final int entryTypeId = JdbcSupport.getInteger(rs, "type_enum");
            final String glAccountName = rs.getString("glAccountName");

            return new IncomeAndExpenseJournalEntryData(id,accountId,officeId,entryDate,reversed,isRunningBalanceCalculated,comments,
                    officeRunningBalance,organizationRunningBalance,accountTypeId,entryTypeId,glAccountName,null);
        }
    }

    @Override
    public List<IncomeAndExpenseJournalEntryData> retrieveAllIncomeAndExpenseJournalEntryData(Long officeId, LocalDate date) {
        final FinancialEndOfYearMapper rm = new FinancialEndOfYearMapper();

        final String sql = "select " + rm.schema() ;
        return this.jdbcTemplate.query(sql,rm, new Object[] {GLAccountType.INCOME.getValue(),GLAccountType.EXPENSE.getValue(), formatter.print(date),officeId});
    }
}

