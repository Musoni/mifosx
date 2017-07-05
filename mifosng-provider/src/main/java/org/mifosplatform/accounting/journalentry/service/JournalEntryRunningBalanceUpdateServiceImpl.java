/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.accounting.journalentry.service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import org.joda.time.LocalDate;
import org.mifosplatform.accounting.common.AccountingEnumerations;
import org.mifosplatform.accounting.glaccount.domain.GLAccountType;
import org.mifosplatform.accounting.journalentry.api.JournalEntryJsonInputParams;
import org.mifosplatform.accounting.journalentry.data.JournalEntryData;
import org.mifosplatform.accounting.journalentry.data.JournalEntryDataValidator;
import org.mifosplatform.accounting.journalentry.domain.JournalEntryType;
import org.mifosplatform.infrastructure.core.api.JsonCommand;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResult;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResultBuilder;
import org.mifosplatform.infrastructure.core.data.EnumOptionData;
import org.mifosplatform.infrastructure.core.domain.JdbcSupport;
import org.mifosplatform.infrastructure.core.serialization.FromJsonHelper;
import org.mifosplatform.infrastructure.core.service.RoutingDataSource;
import org.mifosplatform.infrastructure.jobs.annotation.CronTarget;
import org.mifosplatform.infrastructure.jobs.service.JobName;
import org.mifosplatform.organisation.office.domain.Office;
import org.mifosplatform.organisation.office.domain.OfficeRepository;
import org.mifosplatform.organisation.office.exception.OfficeNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JournalEntryRunningBalanceUpdateServiceImpl implements JournalEntryRunningBalanceUpdateService {

    private final static Logger logger = LoggerFactory.getLogger(JournalEntryRunningBalanceUpdateServiceImpl.class);

    private final JdbcTemplate jdbcTemplate;

    private final OfficeRepository officeRepository;

    private final JournalEntryDataValidator dataValidator;

    private final FromJsonHelper fromApiJsonHelper;

    private final GLJournalEntryMapper entryMapper = new GLJournalEntryMapper();

    private final String officeRunningBalanceSql = "select je.office_running_balance as runningBalance,je.account_id as accountId from acc_gl_journal_entry je "
            + "inner join (select max(id) as id from acc_gl_journal_entry where office_id=?  and entry_date < ? group by account_id,entry_date) je2 "
            + "inner join (select max(entry_date) as date from acc_gl_journal_entry where office_id=? and entry_date < ? group by account_id) je3 "
            + "where je2.id = je.id and je.entry_date = je3.date group by je.id order by je.entry_date DESC " ;

    private final String organizationRunningBalanceSql = "select je.organization_running_balance as runningBalance,je.account_id as accountId from acc_gl_journal_entry je "
            + "inner join (select max(id) as id from acc_gl_journal_entry where entry_date < ? group by account_id,entry_date) je2 "
            + "inner join (select max(entry_date) as date from acc_gl_journal_entry where entry_date < ? group by account_id) je3 "
            + "where je2.id = je.id and je.entry_date = je3.date group by je.id order by je.entry_date DESC, je.id DESC " ;

    private final String officesRunningBalanceSql = "select je.office_running_balance as officeRunningBalance," +
            "je.organization_running_balance as organizationRunningBalance, je.id as transactionId, " +
            "je.account_id as accountId,je.office_id as officeId, je.entry_date as entryDate "
            + "from acc_gl_journal_entry je "
            + "inner join (select max(id) as id from acc_gl_journal_entry where entry_date < ? group by office_id,account_id,entry_date) je2 "
            + "inner join (select max(entry_date) as date from acc_gl_journal_entry where entry_date < ? group by office_id,account_id) je3 "
            + "where je2.id = je.id and je.entry_date = je3.date group by je.id order by je.entry_date DESC, je.id DESC " ;

    private final int limit = 500000;

    @Autowired
    public JournalEntryRunningBalanceUpdateServiceImpl(final RoutingDataSource dataSource, final OfficeRepository officeRepository,
            final JournalEntryDataValidator dataValidator, final FromJsonHelper fromApiJsonHelper) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.officeRepository = officeRepository;
        this.dataValidator = dataValidator;
        this.fromApiJsonHelper = fromApiJsonHelper;
    }

    @Override
    @CronTarget(jobName = JobName.ACCOUNTING_RUNNING_BALANCE_UPDATE)
    public void updateRunningBalance() {

        String dateFinder = "select MIN(je.entry_date) as entityDate from acc_gl_journal_entry  je "
                + "where je.is_running_balance_calculated=0 ";

        try {
            Date entityDate = this.jdbcTemplate.queryForObject(dateFinder, Date.class);
            if(entityDate != null) {
                updateOrganizationRunningBalance(entityDate);
                updateDerivedOrganizationRunningBalance();
            }
            else
            {
                logger.debug("No new journal entries found for updating.");
            }

        } catch (EmptyResultDataAccessException e) {
            logger.debug("No new journal entries found for updating.");
        }
    }

    @Override
    public CommandProcessingResult updateOfficeRunningBalance(JsonCommand command) {
        this.dataValidator.validateForUpdateRunningbalance(command);
        final Long officeId = this.fromApiJsonHelper.extractLongNamed(JournalEntryJsonInputParams.OFFICE_ID.getValue(),
                command.parsedJson());
        CommandProcessingResultBuilder commandProcessingResultBuilder = new CommandProcessingResultBuilder().withCommandId(command
                .commandId());
        if (officeId == null) {
            updateRunningBalance();
        } else {
            final Office office = this.officeRepository.findOne(officeId);
            if (office == null) { throw new OfficeNotFoundException(officeId); }

            String dateFinder = "select MIN(je.entry_date) as entityDate " + "from acc_gl_journal_entry  je "
                    + "where je.is_running_balance_calculated=0  and je.office_id=?";
            try {

                Date entityDate = this.jdbcTemplate.queryForObject(dateFinder, Date.class, officeId);
                updateRunningBalance(officeId, entityDate);
            } catch (EmptyResultDataAccessException e) {
                logger.debug("No results found for updation of office running balance with office id:" + officeId);
            }
            commandProcessingResultBuilder.withOfficeId(officeId);
        }
        return commandProcessingResultBuilder.build();
    }

    private void updateDerivedOrganizationRunningBalance() {
        // Update all Organization balances after using the same (mapped) amount:
        String updateSql = "update acc_gl_account as ac join\n" +
                "(\n" +
                "select je.account_id, \n" +
                "SUM(IF(type_enum = 1, IF(accounts.classification_enum IN (1,5), amount *-1, amount), IF(accounts.classification_enum IN (1,5), amount, amount *-1))) as movement,\n" +
                "max(je.id) as new_last_entry_id_derived\n" +
                "from acc_gl_journal_entry as je\n" +
                "left join acc_gl_account as accounts ON accounts.id = je.account_id \n" +
                "WHERE je.id > IFNULL(accounts.last_entry_id_derived,0) \n" +
                "group by je.account_id) as upd on ac.id = upd.account_id\n" +
                "set `organization_running_balance_derived` = IFNULL(organization_running_balance_derived,0) + IFNULL(upd.movement,0),\n" +
                "\tac.`last_entry_id_derived` = IFNULL(upd.new_last_entry_id_derived,0)\n" +
                ";";

        this.jdbcTemplate.update(updateSql);
    }

    private void updateOrganizationRunningBalance(Date entityDate) {
        Map<Long, BigDecimal> runningBalanceMap = new HashMap<>(5);
        Map<Long, Object> runningBalanceObject = new HashMap<>(5);
        Map<Long, Map<Long, BigDecimal>> officesRunningBalance = new HashMap<>();

        List<Map<String, Object>> officesRunningBalanceList = jdbcTemplate.queryForList(officesRunningBalanceSql, entityDate, entityDate);
        for (Map<String, Object> entries : officesRunningBalanceList) {
            Long accountId = (Long) entries.get("accountId");
            Long officeId = (Long) entries.get("officeId");
            Long transactionId = (Long) entries.get("transactionId");
            Date entryDate = (Date) entries.get("entryDate");

            // Map out office running balances:
            Map<Long, BigDecimal> runningBalance = null;
            if (officesRunningBalance.containsKey(officeId)) {
                runningBalance = officesRunningBalance.get(officeId);
            } else {
                runningBalance = new HashMap<>();
                officesRunningBalance.put(officeId, runningBalance);
            }
            if (!runningBalance.containsKey(accountId)) {
                runningBalance.put(accountId, (BigDecimal) entries.get("officeRunningBalance"));
            }

            // Deal with Oganizaiton running balances, if there is no entry present for this accountID, add it:
            if (!runningBalanceMap.containsKey(accountId)) {
                runningBalanceMap.put(accountId, (BigDecimal) entries.get("organizationRunningBalance"));
                runningBalanceObject.put(accountId, entries);
            }
            else
            {
                // This block of code goes to compare the most recent entryDate and transactionId
                Map existingEntry = (Map) runningBalanceObject.get(accountId);
                Date existingEntryDate = (Date) existingEntry.get("entryDate");
                Long existingTransactionId = (Long) existingEntry.get("transactionId");

                // If the existing Entry date is before the date of this entry then we need to update.
                // If the date is the same then only update if the transactionId of the new entry is larger than the existing.
                if(existingEntryDate.compareTo(entryDate) == -1 || (existingEntryDate.compareTo(entryDate) == 0 && existingTransactionId.compareTo(transactionId) == -1))
                {
                    runningBalanceMap.put(accountId, (BigDecimal) entries.get("organizationRunningBalance"));
                    runningBalanceObject.put(accountId, entries);
                }
            }
        }

        int startFrom = 0;
        int maxIterations = 10;

        // Get the first set of data:
        List<JournalEntryData> entryDatas = jdbcTemplate.query( entryMapper.organizationRunningBalanceSchema(), entryMapper,
                new Object[] { entityDate, limit, startFrom });

        while (startFrom < (maxIterations * limit)) {

            if (entryDatas.size() > 0) {
                // run a batch update of many SQL statements at a time
                final Integer batchUpdateSize = 1000;
                final Integer batchUpdateSizeMinusOne = batchUpdateSize - 1;
                String[] updateSql = new String[batchUpdateSize];
                int i = 0;
                for (JournalEntryData entryData : entryDatas) {
                    Map<Long, BigDecimal> officeRunningBalanceMap = null;
                    if (officesRunningBalance.containsKey(entryData.getOfficeId())) {
                        officeRunningBalanceMap = officesRunningBalance.get(entryData.getOfficeId());
                    } else {
                        officeRunningBalanceMap = new HashMap<>();
                        officesRunningBalance.put(entryData.getOfficeId(), officeRunningBalanceMap);
                    }
                    BigDecimal officeRunningBalance = calculateRunningBalance(entryData, officeRunningBalanceMap);
                    BigDecimal runningBalance = calculateRunningBalance(entryData, runningBalanceMap);
                    String sql = "UPDATE acc_gl_journal_entry je SET je.is_running_balance_calculated=1, je.organization_running_balance="
                            + runningBalance + ",je.office_running_balance=" + officeRunningBalance + " WHERE  je.id=" + entryData.getId();
                    updateSql[i++] = sql;

                    if (i == batchUpdateSizeMinusOne) {
                        // run a batch update of the update SQL statements
                        this.jdbcTemplate.batchUpdate(updateSql);

                        // reset counter and string array
                        i = 0;
                        updateSql = new String[batchUpdateSize];
                    }
                }

                this.jdbcTemplate.batchUpdate(updateSql);

            }

            // Update startFrom & Fetch next set of data::
            startFrom = startFrom + limit;
            entryDatas = jdbcTemplate.query(entryMapper.organizationRunningBalanceSchema(), entryMapper,
                    new Object[]{entityDate, limit, startFrom});

            if(entryDatas.size() == 0 )
            {
                logger.debug("Got an empty set of results, moving on.");

                break;
            }
        }



    }


    private void updateRunningBalance(Long officeId, Date entityDate) {
        Map<Long, BigDecimal> runningBalanceMap = new HashMap<>(5);

        List<Map<String, Object>> list = jdbcTemplate.queryForList(officeRunningBalanceSql, officeId, entityDate, officeId, entityDate);
        for (Map<String, Object> entries : list) {
            Long accountId = (Long) entries.get("accountId");
            if (!runningBalanceMap.containsKey(accountId)) {
                runningBalanceMap.put(accountId, (BigDecimal) entries.get("runningBalance"));
            }
        }

        int startFrom = 0;
        int maxIterations = 500 * limit;

        // Get the first set of data:
        List<JournalEntryData> entryDatas = jdbcTemplate.query(entryMapper.officeRunningBalanceSchema(), entryMapper, new Object[] {
                officeId, entityDate, limit, startFrom });

        while (startFrom <= (maxIterations * limit))
        {
            if (entryDatas.size() > 0) {
                String[] updateSql = new String[entryDatas.size()];
                int i = 0;
                for (JournalEntryData entryData : entryDatas) {
                    BigDecimal runningBalance = calculateRunningBalance(entryData, runningBalanceMap);
                    String sql = "UPDATE acc_gl_journal_entry je SET je.office_running_balance=" + runningBalance + " WHERE  je.id="
                            + entryData.getId();
                    updateSql[i++] = sql;
                }

                this.jdbcTemplate.batchUpdate(updateSql);

            }
            else
            {
                break;
            }

            // Update startFrom & Fetch next set of data::
            startFrom = startFrom + limit;
            entryDatas = jdbcTemplate.query(entryMapper.officeRunningBalanceSchema(), entryMapper, new Object[] {
                    officeId, entityDate, limit, startFrom });
        }


    }

    private BigDecimal calculateRunningBalance(JournalEntryData entry, Map<Long, BigDecimal> runningBalanceMap) {
        BigDecimal runningBalance = BigDecimal.ZERO;
        if (runningBalanceMap.containsKey(entry.getGlAccountId())) {
            runningBalance = runningBalanceMap.get(entry.getGlAccountId());
        }
        GLAccountType accounttype = GLAccountType.fromInt(entry.getGlAccountType().getId().intValue());
        JournalEntryType entryType = JournalEntryType.fromInt(entry.getEntryType().getId().intValue());
        boolean isIncrease = false;
        switch (accounttype) {
            case ASSET:
                if (entryType.isDebitType()) {
                    isIncrease = true;
                }
            break;
            case EQUITY:
                if (entryType.isCreditType()) {
                    isIncrease = true;
                }
            break;
            case EXPENSE:
                if (entryType.isDebitType()) {
                    isIncrease = true;
                }
            break;
            case INCOME:
                if (entryType.isCreditType()) {
                    isIncrease = true;
                }
            break;
            case LIABILITY:
                if (entryType.isCreditType()) {
                    isIncrease = true;
                }
            break;
        }
        if (isIncrease) {
            runningBalance = runningBalance.add(entry.getAmount());
        } else {
            runningBalance = runningBalance.subtract(entry.getAmount());
        }
        runningBalanceMap.put(entry.getGlAccountId(), runningBalance);
        return runningBalance;
    }

    private BigDecimal calculateMovement(JournalEntryData entry) {
        BigDecimal amount = BigDecimal.ZERO;

        GLAccountType accounttype = GLAccountType.fromInt(entry.getGlAccountType().getId().intValue());
        JournalEntryType entryType = JournalEntryType.fromInt(entry.getEntryType().getId().intValue());
        boolean isIncrease = false;
        switch (accounttype) {
            case ASSET:
                if (entryType.isDebitType()) {
                    isIncrease = true;
                }
                break;
            case EQUITY:
                if (entryType.isCreditType()) {
                    isIncrease = true;
                }
                break;
            case EXPENSE:
                if (entryType.isDebitType()) {
                    isIncrease = true;
                }
                break;
            case INCOME:
                if (entryType.isCreditType()) {
                    isIncrease = true;
                }
                break;
            case LIABILITY:
                if (entryType.isCreditType()) {
                    isIncrease = true;
                }
                break;
        }
        if (isIncrease) {
            amount = amount.add(entry.getAmount());
        } else {
            amount = amount.subtract(entry.getAmount());
        }
        return amount;
    }

    private static final class GLJournalEntryMapper implements RowMapper<JournalEntryData> {

        public String officeRunningBalanceSchema() {
            return "select je.id as id,je.account_id as glAccountId,je.type_enum as entryType,je.amount as amount, "
                    + "glAccount.classification_enum as classification,je.office_id as officeId, je.entry_date as entryDate, je.is_running_balance_calculated as isCalculated  "
                    + "from acc_gl_journal_entry je , acc_gl_account glAccount " + "where je.account_id = glAccount.id "
                    + "and je.office_id=? and je.entry_date >= ?  order by je.entry_date,je.id LIMIT ? OFFSET ?";
        }

        public String organizationRunningBalanceSchema() {
            return "select je.id as id,je.account_id as glAccountId," + "je.type_enum as entryType,je.amount as amount, "
                    + "glAccount.classification_enum as classification,je.office_id as officeId, je.entry_date as entryDate, je.is_running_balance_calculated as isCalculated   "
                    + "from acc_gl_journal_entry je , acc_gl_account glAccount " + "where je.account_id = glAccount.id "
                    + "and je.entry_date >= ? order by je.entry_date,je.id LIMIT ? OFFSET ?";
        }

        @Override
        public JournalEntryData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

            final Long id = rs.getLong("id");
            final Long glAccountId = rs.getLong("glAccountId");
            final Long officeId = rs.getLong("officeId");
            final int accountTypeId = JdbcSupport.getInteger(rs, "classification");
            final EnumOptionData accountType = AccountingEnumerations.gLAccountType(accountTypeId);
            final BigDecimal amount = rs.getBigDecimal("amount");
            final int entryTypeId = JdbcSupport.getInteger(rs, "entryType");
            final EnumOptionData entryType = AccountingEnumerations.journalEntryType(entryTypeId);
            final LocalDate entryDate = JdbcSupport.getLocalDate(rs, "entryDate");
            final Boolean isCalculated = rs.getBoolean("isCalculated");

            return new JournalEntryData(id, officeId, null, null, glAccountId, null, accountType, entryDate, entryType, amount, null, null,
                    null, null, null, null, null, null, null, null, null, null, isCalculated, null, null,null);
        }
    }

}
