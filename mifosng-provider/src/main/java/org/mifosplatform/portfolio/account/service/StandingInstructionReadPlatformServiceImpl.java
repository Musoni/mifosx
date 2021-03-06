/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.portfolio.account.service;

import static org.mifosplatform.portfolio.account.service.AccountTransferEnumerations.accountType;
import static org.mifosplatform.portfolio.account.service.AccountTransferEnumerations.recurrenceType;
import static org.mifosplatform.portfolio.account.service.AccountTransferEnumerations.standingInstructionPriority;
import static org.mifosplatform.portfolio.account.service.AccountTransferEnumerations.standingInstructionStatus;
import static org.mifosplatform.portfolio.account.service.AccountTransferEnumerations.standingInstructionType;
import static org.mifosplatform.portfolio.account.service.AccountTransferEnumerations.transferType;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.joda.time.LocalDate;
import org.joda.time.MonthDay;
import org.mifosplatform.infrastructure.core.data.EnumOptionData;
import org.mifosplatform.infrastructure.core.domain.JdbcSupport;
import org.mifosplatform.infrastructure.core.service.DateUtils;
import org.mifosplatform.infrastructure.core.service.Page;
import org.mifosplatform.infrastructure.core.service.PaginationHelper;
import org.mifosplatform.infrastructure.core.service.RoutingDataSource;
import org.mifosplatform.organisation.office.data.OfficeData;
import org.mifosplatform.organisation.office.service.OfficeReadPlatformService;
import org.mifosplatform.portfolio.account.PortfolioAccountType;
import org.mifosplatform.portfolio.account.data.PortfolioAccountDTO;
import org.mifosplatform.portfolio.account.data.PortfolioAccountData;
import org.mifosplatform.portfolio.account.data.StandingInstructionDTO;
import org.mifosplatform.portfolio.account.data.StandingInstructionData;
import org.mifosplatform.portfolio.account.data.StandingInstructionDuesData;
import org.mifosplatform.portfolio.account.domain.AccountTransferRecurrenceType;
import org.mifosplatform.portfolio.account.domain.AccountTransferType;
import org.mifosplatform.portfolio.account.domain.StandingInstructionPriority;
import org.mifosplatform.portfolio.account.domain.StandingInstructionStatus;
import org.mifosplatform.portfolio.account.domain.StandingInstructionType;
import org.mifosplatform.portfolio.account.exception.AccountTransferNotFoundException;
import org.mifosplatform.portfolio.client.data.ClientData;
import org.mifosplatform.portfolio.client.service.ClientReadPlatformService;
import org.mifosplatform.portfolio.common.service.CommonEnumerations;
import org.mifosplatform.portfolio.common.service.DropdownReadPlatformService;
import org.mifosplatform.infrastructure.core.service.SearchParameters;
import org.mifosplatform.portfolio.group.data.GroupGeneralData;
import org.mifosplatform.portfolio.group.service.GroupReadPlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class StandingInstructionReadPlatformServiceImpl implements StandingInstructionReadPlatformService {

    private final JdbcTemplate jdbcTemplate;
    private final ClientReadPlatformService clientReadPlatformService;
    private final GroupReadPlatformService groupReadPlatformService;
    private final OfficeReadPlatformService officeReadPlatformService;
    private final PortfolioAccountReadPlatformService portfolioAccountReadPlatformService;
    private final DropdownReadPlatformService dropdownReadPlatformService;

    // mapper
    private final StandingInstructionMapper standingInstructionMapper;

    // pagination
    private final PaginationHelper<StandingInstructionData> paginationHelper = new PaginationHelper<>();

    @Autowired
    public StandingInstructionReadPlatformServiceImpl(final RoutingDataSource dataSource,
            final ClientReadPlatformService clientReadPlatformService, final OfficeReadPlatformService officeReadPlatformService,
            final PortfolioAccountReadPlatformService portfolioAccountReadPlatformService,
            final DropdownReadPlatformService dropdownReadPlatformService, final GroupReadPlatformService groupReadPlatformService) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.clientReadPlatformService = clientReadPlatformService;
        this.groupReadPlatformService = groupReadPlatformService;
        this.officeReadPlatformService = officeReadPlatformService;
        this.portfolioAccountReadPlatformService = portfolioAccountReadPlatformService;
        this.dropdownReadPlatformService = dropdownReadPlatformService;
        this.standingInstructionMapper = new StandingInstructionMapper();
    }

    @Override
    public StandingInstructionData retrieveTemplate(final Long fromOfficeId, final Long fromClientId, final Long fromGroupId,
            final Long fromAccountId, final Integer fromAccountType, final Long toOfficeId, final Long toClientId,
            final Long toGroupId, final Long toAccountId, final Integer toAccountType, Integer transferType) {

        AccountTransferType accountTransferType = AccountTransferType.INVALID;
        if (transferType != null) {
            accountTransferType = AccountTransferType.fromInt(transferType);
        }

        final EnumOptionData loanAccountType = accountType(PortfolioAccountType.LOAN);
        final EnumOptionData savingsAccountType = accountType(PortfolioAccountType.SAVINGS);

        final Integer mostRelevantFromAccountType = fromAccountType;
        Collection<EnumOptionData> fromAccountTypeOptions = null;
        Collection<EnumOptionData> toAccountTypeOptions = null;

        if (accountTransferType.isAccountTransfer()) {
            fromAccountTypeOptions = Arrays.asList(savingsAccountType);
            toAccountTypeOptions = Arrays.asList(savingsAccountType);
        } else if (accountTransferType.isLoanRepayment()) {
            fromAccountTypeOptions = Arrays.asList(savingsAccountType);
            toAccountTypeOptions = Arrays.asList(loanAccountType);
        } else {
            fromAccountTypeOptions = Arrays.asList(savingsAccountType, loanAccountType);
            toAccountTypeOptions = Arrays.asList(loanAccountType, savingsAccountType);
        }
        final Integer mostRelevantToAccountType = toAccountType;

        final EnumOptionData fromAccountTypeData = accountType(mostRelevantFromAccountType);
        final EnumOptionData toAccountTypeData = accountType(mostRelevantToAccountType);

        // from settings
        OfficeData fromOffice = null;
        ClientData fromClient = null;
        GroupGeneralData fromGroup = null;
        PortfolioAccountData fromAccount = null;

        OfficeData toOffice = null;
        ClientData toClient = null;
        GroupGeneralData toGroup = null;
        PortfolioAccountData toAccount = null;

        // template
        Collection<PortfolioAccountData> fromAccountOptions = null;
        Collection<PortfolioAccountData> toAccountOptions = null;

        Long mostRelevantFromOfficeId = fromOfficeId;
        Long mostRelevantFromClientId = fromClientId;
        Long mostRelevantFromGroupId = fromGroupId;

        Long mostRelevantToOfficeId = toOfficeId;
        Long mostRelevantToClientId = toClientId;
        Long mostRelevantToGroupId = toGroupId;

        if (fromAccountId != null) {
            Integer accountType;
            if (mostRelevantFromAccountType == 1) {
                accountType = PortfolioAccountType.LOAN.getValue();
            } else {
                accountType = PortfolioAccountType.SAVINGS.getValue();
            }
            fromAccount = this.portfolioAccountReadPlatformService.retrieveOne(fromAccountId, accountType);

            // override provided fromClient with client of account
            mostRelevantFromClientId = fromAccount.clientId();
            mostRelevantFromGroupId = fromAccount.groupId();
        }

        Collection<OfficeData> fromOfficeOptions = this.officeReadPlatformService.retrieveAllOfficesForDropdown();
        Collection<ClientData> fromClientOptions = null;
        Collection<GroupGeneralData> fromGroupOptions = null;

        if (mostRelevantFromClientId != null) {
            fromClient = this.clientReadPlatformService.retrieveOne(mostRelevantFromClientId);
            mostRelevantFromOfficeId = fromClient.officeId();
            long[] loanStatus = null;
            if (mostRelevantFromAccountType == 1) {
                loanStatus = new long[] { 300, 700 };
            }
            PortfolioAccountDTO portfolioAccountDTO = new PortfolioAccountDTO(mostRelevantFromAccountType, mostRelevantFromClientId,
                    loanStatus);
            fromAccountOptions = this.portfolioAccountReadPlatformService.retrieveAllForLookup(portfolioAccountDTO);

            if (mostRelevantFromOfficeId != null) {
                fromClientOptions = this.clientReadPlatformService.retrieveAllForLookupByOfficeId(mostRelevantFromOfficeId);
            }
        }

        if (mostRelevantFromGroupId != null) {
            fromGroup = this.groupReadPlatformService.retrieveOne(mostRelevantFromGroupId);
            mostRelevantFromOfficeId = fromGroup.officeId();
            long[] loanStatus = null;
            String currencyCode = fromAccount != null ? fromAccount.currencyCode() : null;
            if (mostRelevantFromAccountType == 1) {
                loanStatus = new long[] { 300, 700 };
            }
            PortfolioAccountDTO portfolioAccountDTO = new PortfolioAccountDTO(mostRelevantFromAccountType, currencyCode,
                    loanStatus, null, mostRelevantFromGroupId);
            fromAccountOptions = this.portfolioAccountReadPlatformService.retrieveAllForLookup(portfolioAccountDTO);

            if (mostRelevantFromOfficeId != null) {
                fromGroupOptions = this.groupReadPlatformService.retrieveGroupsForLookup(mostRelevantFromOfficeId);
            }
        }

        if (mostRelevantFromOfficeId != null) {
            fromOffice = this.officeReadPlatformService.retrieveOffice(mostRelevantFromOfficeId);
        }

        // defaults
        final LocalDate transferDate = DateUtils.getLocalDateOfTenant();
        Collection<OfficeData> toOfficeOptions = fromOfficeOptions;
        Collection<ClientData> toClientOptions = null;
        Collection<GroupGeneralData> toGroupOptions = null;

        if (toAccountId != null && fromAccount != null) {
            toAccount = this.portfolioAccountReadPlatformService.retrieveOne(toAccountId, mostRelevantToAccountType,
                    fromAccount.currencyCode());
            mostRelevantToClientId = toAccount.clientId();
            mostRelevantToGroupId = toAccount.groupId();
        }

        if (mostRelevantToClientId != null) {
            toClient = this.clientReadPlatformService.retrieveOne(mostRelevantToClientId);
            mostRelevantToOfficeId = toClient.officeId();

            toClientOptions = this.clientReadPlatformService.retrieveAllForLookupByOfficeId(mostRelevantToOfficeId);

            toAccountOptions = retrieveAccountOptions(fromAccount, mostRelevantToAccountType, mostRelevantToClientId, null);
        }

        if (mostRelevantToGroupId != null) {
            toGroup = this.groupReadPlatformService.retrieveOne(mostRelevantToGroupId);
            mostRelevantToOfficeId = toGroup.officeId();

            toGroupOptions = this.groupReadPlatformService.retrieveGroupsForLookup(mostRelevantToOfficeId);

            toAccountOptions = retrieveAccountOptions(fromAccount, mostRelevantToAccountType, null, mostRelevantToGroupId);
        }

        if (mostRelevantToOfficeId != null) {
            toOffice = this.officeReadPlatformService.retrieveOffice(mostRelevantToOfficeId);
            toOfficeOptions = this.officeReadPlatformService.retrieveAllOfficesForDropdown();

            toClientOptions = toClientOptions != null ? toClientOptions : this.clientReadPlatformService.retrieveAllForLookupByOfficeId(mostRelevantToOfficeId);
            toGroupOptions = toGroupOptions != null ? toGroupOptions : this.groupReadPlatformService.retrieveGroupsForLookup(mostRelevantToOfficeId);
            if (toClientOptions != null && toClientOptions.size() == 1) {
                toClient = new ArrayList<>(toClientOptions).get(0);

                toAccountOptions = retrieveAccountOptions(fromAccount, mostRelevantToAccountType, mostRelevantToClientId, null);
            }
            if (toGroupOptions != null && toGroupOptions.size() == 1) {
                toGroup = new ArrayList<>(toGroupOptions).get(0);

                toAccountOptions = retrieveAccountOptions(fromAccount, mostRelevantToAccountType, null, mostRelevantToGroupId);
            }
        }

        final Collection<EnumOptionData> transferTypeOptions = Arrays.asList(transferType(AccountTransferType.ACCOUNT_TRANSFER),
                transferType(AccountTransferType.LOAN_REPAYMENT)/*
                                                                 * ,
                                                                 * transferType(
                                                                 * AccountTransferType
                                                                 * .
                                                                 * CHARGE_PAYMENT
                                                                 * )
                                                                 */);
        final Collection<EnumOptionData> statusOptions = Arrays.asList(standingInstructionStatus(StandingInstructionStatus.ACTIVE),
                standingInstructionStatus(StandingInstructionStatus.DISABLED));
        final Collection<EnumOptionData> instructionTypeOptions = Arrays.asList(standingInstructionType(StandingInstructionType.FIXED),
                standingInstructionType(StandingInstructionType.DUES));
        final Collection<EnumOptionData> priorityOptions = Arrays.asList(standingInstructionPriority(StandingInstructionPriority.URGENT),
                standingInstructionPriority(StandingInstructionPriority.HIGH),
                standingInstructionPriority(StandingInstructionPriority.MEDIUM),
                standingInstructionPriority(StandingInstructionPriority.LOW));
        final Collection<EnumOptionData> recurrenceTypeOptions = Arrays.asList(recurrenceType(AccountTransferRecurrenceType.PERIODIC),
                recurrenceType(AccountTransferRecurrenceType.AS_PER_DUES));
        final Collection<EnumOptionData> recurrenceFrequencyOptions = this.dropdownReadPlatformService.retrievePeriodFrequencyTypeOptions();

        return StandingInstructionData.template(fromOffice, fromClient, fromGroup, fromAccountTypeData, fromAccount, transferDate,
                toOffice, toClient, toGroup, toAccountTypeData, toAccount, fromOfficeOptions, fromClientOptions, fromGroupOptions,
                fromAccountTypeOptions, fromAccountOptions, toOfficeOptions, toClientOptions, toGroupOptions, toAccountTypeOptions,
                toAccountOptions, transferTypeOptions, statusOptions, instructionTypeOptions, priorityOptions, recurrenceTypeOptions,
                recurrenceFrequencyOptions);
    }

    private Collection<PortfolioAccountData> retrieveAccountOptions(final PortfolioAccountData excludeThisAccountFromOptions,
            final Integer toAccountType, final Long toClientId, final Long toGroupId) {

        final String currencyCode = excludeThisAccountFromOptions != null ? excludeThisAccountFromOptions.currencyCode() : null;

        PortfolioAccountDTO portfolioAccountDTO = null;
        if(toClientId != null) {
            portfolioAccountDTO = new PortfolioAccountDTO(toAccountType, toClientId, currencyCode, null, null);
        }else if(toGroupId != null){
            portfolioAccountDTO = new PortfolioAccountDTO(toAccountType, currencyCode, null, null, toGroupId);
        }
        Collection<PortfolioAccountData> accountOptions = this.portfolioAccountReadPlatformService
                .retrieveAllForLookup(portfolioAccountDTO);
        if (!CollectionUtils.isEmpty(accountOptions)) {
            accountOptions.remove(excludeThisAccountFromOptions);
        } else {
            accountOptions = null;
        }

        return accountOptions;
    }

    @Override
    public Page<StandingInstructionData> retrieveAll(final StandingInstructionDTO standingInstructionDTO) {

        final StringBuilder sqlBuilder = new StringBuilder(200);
        sqlBuilder.append("select SQL_CALC_FOUND_ROWS ");
        sqlBuilder.append(this.standingInstructionMapper.schema());
        if (standingInstructionDTO.transferType() != null || standingInstructionDTO.clientId() != null
                || standingInstructionDTO.clientName() != null || standingInstructionDTO.groupId() != null
                || standingInstructionDTO.groupName() != null) {
            sqlBuilder.append(" where ");
        }
        boolean addAndCaluse = false;
        List<Object> paramObj = new ArrayList<>();
        if (standingInstructionDTO.transferType() != null) {
            if (addAndCaluse) {
                sqlBuilder.append(" and ");
            }
            sqlBuilder.append(" atd.transfer_type=? ");
            paramObj.add(standingInstructionDTO.transferType());
            addAndCaluse = true;
        }
        if (standingInstructionDTO.clientId() != null) {
            if (addAndCaluse) {
                sqlBuilder.append(" and ");
            }
            sqlBuilder.append(" fromclient.id=? ");
            paramObj.add(standingInstructionDTO.clientId());
            addAndCaluse = true;
        } else if (standingInstructionDTO.clientName() != null) {
            if (addAndCaluse) {
                sqlBuilder.append(" and ");
            }
            sqlBuilder.append(" fromclient.display_name=? ");
            paramObj.add(standingInstructionDTO.clientName());
            addAndCaluse = true;
        }

        if (standingInstructionDTO.groupId() != null) {
            if (addAndCaluse) {
                sqlBuilder.append(" and ");
            }
            sqlBuilder.append(" fromgroup.id=? ");
            paramObj.add(standingInstructionDTO.groupId());
            addAndCaluse = true;
        } else if (standingInstructionDTO.groupName() != null) {
            if (addAndCaluse) {
                sqlBuilder.append(" and ");
            }
            sqlBuilder.append(" fromgroup.display_name=? ");
            paramObj.add(standingInstructionDTO.groupName());
            addAndCaluse = true;
        }

        if (standingInstructionDTO.fromAccountType() != null && standingInstructionDTO.fromAccount() != null) {
            PortfolioAccountType accountType = PortfolioAccountType.fromInt(standingInstructionDTO.fromAccountType());
            if (addAndCaluse) {
                sqlBuilder.append(" and ");
            }
            if (accountType.isSavingsAccount()) {
                sqlBuilder.append(" fromsavacc.id=? ");
                paramObj.add(standingInstructionDTO.fromAccount());
            } else if (accountType.isLoanAccount()) {
                sqlBuilder.append(" fromloanacc.id=? ");
                paramObj.add(standingInstructionDTO.fromAccount());
            }
            addAndCaluse = true;
        }

        final SearchParameters searchParameters = standingInstructionDTO.searchParameters();
        if (searchParameters.isOrderByRequested()) {
            sqlBuilder.append(" order by ").append(searchParameters.getOrderBy());

            if (searchParameters.isSortOrderProvided()) {
                sqlBuilder.append(' ').append(searchParameters.getSortOrder());
            }
        }

        if (searchParameters.isLimited()) {
            sqlBuilder.append(" limit ").append(searchParameters.getLimit());
            if (searchParameters.isOffset()) {
                sqlBuilder.append(" offset ").append(searchParameters.getOffset());
            }
        }

        final Object[] finalObjectArray = paramObj.toArray();
        final String sqlCountRows = "SELECT FOUND_ROWS()";
        return this.paginationHelper.fetchPage(this.jdbcTemplate, sqlCountRows, sqlBuilder.toString(), finalObjectArray,
                this.standingInstructionMapper);
    }

    @Override
    public Collection<StandingInstructionData> retrieveAll(final Integer status, final LocalDate dueDate) {
        final StringBuilder sqlBuilder = new StringBuilder(200);
        final String currentDate = (dueDate != null) ? dueDate.toString() : "CURRENT_DATE()";
        sqlBuilder.append("select ");
        sqlBuilder.append(this.standingInstructionMapper.schema());
        sqlBuilder
                .append(" where atsi.status=? and ? >= atsi.valid_from and (atsi.valid_till IS NULL or ? < atsi.valid_till) ")
                .append(" and (atsi.last_run_date <> ? or atsi.last_run_date IS NULL)")
                .append(" ORDER BY atsi.priority DESC");
        return this.jdbcTemplate.query(sqlBuilder.toString(), this.standingInstructionMapper, status, currentDate, currentDate, currentDate);
    }

    @Override
    public StandingInstructionData retrieveOne(final Long instructionId) {

        try {
            final String sql = "select " + this.standingInstructionMapper.schema() + " where atsi.id = ?";

            return this.jdbcTemplate.queryForObject(sql, this.standingInstructionMapper, new Object[] { instructionId });
        } catch (final EmptyResultDataAccessException e) {
            throw new AccountTransferNotFoundException(instructionId);
        }
    }

    @Override
    public StandingInstructionDuesData retriveLoanDuesData(final Long loanId, final LocalDate dueDate) {
        final String currentDate = (dueDate != null) ? dueDate.toString() : "CURRENT_DATE()";
        final StandingInstructionLoanDuesMapper rm = new StandingInstructionLoanDuesMapper();
        final String sql = "select " + rm.schema() + " where ml.id= ? and ls.duedate <= ? and ls.completed_derived <> 1";
        return this.jdbcTemplate.queryForObject(sql, rm, new Object[] { loanId, currentDate });
    }

    private static final class StandingInstructionMapper implements RowMapper<StandingInstructionData> {

        private final String schemaSql;

        public StandingInstructionMapper() {
            final StringBuilder sqlBuilder = new StringBuilder(400);
            sqlBuilder.append("atsi.id as id,atsi.name as name, atsi.priority as priority,");
            sqlBuilder.append("atsi.status as status, atsi.instruction_type as instructionType,");
            sqlBuilder.append("atsi.amount as amount,");
            sqlBuilder.append("atsi.valid_from as validFrom, atsi.valid_till as validTill,");
            sqlBuilder.append("atsi.recurrence_type as recurrenceType, atsi.recurrence_frequency as recurrenceFrequency,");
            sqlBuilder.append("atsi.recurrence_interval as recurrenceInterval, atsi.recurrence_on_day as recurrenceOnDay,");
            sqlBuilder.append("atsi.recurrence_on_month as recurrenceOnMonth, atsi. maximum_iterations as maximumIteration,");
            sqlBuilder.append("atd.id as accountDetailId,atd.transfer_type as transferType,");
            sqlBuilder.append("fromoff.id as fromOfficeId, fromoff.name as fromOfficeName,");
            sqlBuilder.append("tooff.id as toOfficeId, tooff.name as toOfficeName,");
            sqlBuilder.append("fromclient.id as fromClientId, fromclient.display_name as fromClientName,");
            sqlBuilder.append("fromgroup.id as fromGroupId, fromgroup.display_name as fromGroupName,");
            sqlBuilder.append("toclient.id as toClientId, toclient.display_name as toClientName,");
            sqlBuilder.append("togroup.id as toGroupId, togroup.display_name as toGroupName,");
            sqlBuilder.append("fromsavacc.id as fromSavingsAccountId, fromsavacc.account_no as fromSavingsAccountNo,");
            sqlBuilder.append("fromsp.id as fromProductId, fromsp.name as fromProductName, ");
            sqlBuilder.append("fromloanacc.id as fromLoanAccountId, fromloanacc.account_no as fromLoanAccountNo,");
            sqlBuilder.append("fromlp.id as fromLoanProductId, fromlp.name as fromLoanProductName,");
            sqlBuilder.append("tosavacc.id as toSavingsAccountId, tosavacc.account_no as toSavingsAccountNo,");
            sqlBuilder.append("tosp.id as toProductId, tosp.name as toProductName, ");
            sqlBuilder.append("toloanacc.id as toLoanAccountId, toloanacc.account_no as toLoanAccountNo, ");
            sqlBuilder.append("tolp.id as toLoanProductId, tolp.name as toLoanProductName ");
            sqlBuilder.append(" FROM m_account_transfer_standing_instructions atsi ");
            sqlBuilder.append("join m_account_transfer_details atd on atd.id = atsi.account_transfer_details_id ");
            sqlBuilder.append("join m_office fromoff on fromoff.id = atd.from_office_id ");
            sqlBuilder.append("join m_office tooff on tooff.id = atd.to_office_id ");
            sqlBuilder.append("left join m_client fromclient on fromclient.id = atd.from_client_id ");
            sqlBuilder.append("left join m_client toclient on toclient.id = atd.to_client_id ");
            sqlBuilder.append("left join m_group fromgroup on fromgroup.id = atd.from_group_id ");
            sqlBuilder.append("left join m_group togroup on togroup.id = atd.to_group_id ");
            sqlBuilder.append("left join m_savings_account fromsavacc on fromsavacc.id = atd.from_savings_account_id ");
            sqlBuilder.append("left join m_savings_product fromsp ON fromsavacc.product_id = fromsp.id ");
            sqlBuilder.append("left join m_loan fromloanacc on fromloanacc.id = atd.from_loan_account_id ");
            sqlBuilder.append("left join m_product_loan fromlp ON fromloanacc.product_id = fromlp.id ");
            sqlBuilder.append("left join m_savings_account tosavacc on tosavacc.id = atd.to_savings_account_id ");
            sqlBuilder.append("left join m_savings_product tosp ON tosavacc.product_id = tosp.id ");
            sqlBuilder.append("left join m_loan toloanacc on toloanacc.id = atd.to_loan_account_id ");
            sqlBuilder.append("left join m_product_loan tolp ON toloanacc.product_id = tolp.id ");

            this.schemaSql = sqlBuilder.toString();
        }

        public String schema() {
            return this.schemaSql;
        }

        @Override
        public StandingInstructionData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

            final Long id = rs.getLong("id");
            final Long accountDetailId = rs.getLong("accountDetailId");
            final String name = rs.getString("name");
            final Integer priority = JdbcSupport.getInteger(rs, "priority");
            EnumOptionData priorityEnum = AccountTransferEnumerations.standingInstructionPriority(priority);

            final Integer status = JdbcSupport.getInteger(rs, "status");
            EnumOptionData statusEnum = AccountTransferEnumerations.standingInstructionStatus(status);
            final Integer instructionType = JdbcSupport.getInteger(rs, "instructionType");
            EnumOptionData instructionTypeEnum = AccountTransferEnumerations.standingInstructionType(instructionType);
            final LocalDate validFrom = JdbcSupport.getLocalDate(rs, "validFrom");
            final LocalDate validTill = JdbcSupport.getLocalDate(rs, "validTill");
            final BigDecimal transferAmount = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "amount");
            final Integer recurrenceType = JdbcSupport.getInteger(rs, "recurrenceType");
            EnumOptionData recurrenceTypeEnum = AccountTransferEnumerations.recurrenceType(recurrenceType);
            final Integer recurrenceFrequency = JdbcSupport.getInteger(rs, "recurrenceFrequency");

            EnumOptionData recurrenceFrequencyEnum = null;
            if (recurrenceFrequency != null) {
                recurrenceFrequencyEnum = CommonEnumerations.termFrequencyType(recurrenceFrequency, "recurrence");
            }
            final Integer recurrenceInterval = JdbcSupport.getInteger(rs, "recurrenceInterval");

            MonthDay recurrenceOnMonthDay = null;
            final Integer recurrenceOnDay = JdbcSupport.getInteger(rs, "recurrenceOnDay");
            final Integer recurrenceOnMonth = JdbcSupport.getInteger(rs, "recurrenceOnMonth");
            if (recurrenceOnDay != null) {
                recurrenceOnMonthDay = new MonthDay(recurrenceOnMonth, recurrenceOnDay);
            }

            final Integer transferType = rs.getInt("transferType");
            EnumOptionData transferTypeEnum = AccountTransferEnumerations.transferType(transferType);

            /*
             * final String currencyCode = rs.getString("currencyCode"); final
             * String currencyName = rs.getString("currencyName"); final String
             * currencyNameCode = rs.getString("currencyNameCode"); final String
             * currencyDisplaySymbol = rs.getString("currencyDisplaySymbol");
             * final Integer currencyDigits = JdbcSupport.getInteger(rs,
             * "currencyDigits"); final Integer inMultiplesOf =
             * JdbcSupport.getInteger(rs, "inMultiplesOf"); final CurrencyData
             * currency = new CurrencyData(currencyCode, currencyName,
             * currencyDigits, inMultiplesOf, currencyDisplaySymbol,
             * currencyNameCode);
             */
            final Long fromOfficeId = JdbcSupport.getLong(rs, "fromOfficeId");
            final String fromOfficeName = rs.getString("fromOfficeName");
            final OfficeData fromOffice = OfficeData.dropdown(fromOfficeId, fromOfficeName, null);

            final Long toOfficeId = JdbcSupport.getLong(rs, "toOfficeId");
            final String toOfficeName = rs.getString("toOfficeName");
            final OfficeData toOffice = OfficeData.dropdown(toOfficeId, toOfficeName, null);

            final Long fromClientId = JdbcSupport.getLong(rs, "fromClientId");
            final String fromClientName = rs.getString("fromClientName");
            final ClientData fromClient = ClientData.lookup(fromClientId, fromClientName, fromOfficeId, fromOfficeName);

            final Long toClientId = JdbcSupport.getLong(rs, "toClientId");
            final String toClientName = rs.getString("toClientName");
            final ClientData toClient = ClientData.lookup(toClientId, toClientName, toOfficeId, toOfficeName);

            final Long fromGroupId = JdbcSupport.getLong(rs, "fromGroupId");
            final String fromGroupName = rs.getString("fromGroupName");
            final GroupGeneralData fromGroup = GroupGeneralData.lookup(fromGroupId, fromGroupName, fromOfficeId, fromOfficeName);

            final Long toGroupId = JdbcSupport.getLong(rs, "toGroupId");
            final String toGroupName = rs.getString("toGroupName");
            final GroupGeneralData toGroup = GroupGeneralData.lookup(toGroupId, toGroupName, toOfficeId, toOfficeName);

            final Long fromSavingsAccountId = JdbcSupport.getLong(rs, "fromSavingsAccountId");
            final String fromSavingsAccountNo = rs.getString("fromSavingsAccountNo");
            final Long fromProductId = JdbcSupport.getLong(rs, "fromProductId");
            final String fromProductName = rs.getString("fromProductName");
            final Long fromLoanAccountId = JdbcSupport.getLong(rs, "fromLoanAccountId");
            final String fromLoanAccountNo = rs.getString("fromLoanAccountNo");
            final Long fromLoanProductId = JdbcSupport.getLong(rs, "fromLoanProductId");
            final String fromLoanProductName = rs.getString("fromLoanProductName");
            PortfolioAccountData fromAccount = null;
            EnumOptionData fromAccountType = null;
            if (fromSavingsAccountId != null) {
                fromAccount = new PortfolioAccountData(fromSavingsAccountId, fromSavingsAccountNo, null, null, null, null, null,
                        fromProductId, fromProductName, null, null, null);
                fromAccountType = accountType(PortfolioAccountType.SAVINGS);
            } else if (fromLoanAccountId != null) {
                fromAccount = new PortfolioAccountData(fromLoanAccountId, fromLoanAccountNo, null, null, null, null, null,
                        fromLoanProductId, fromLoanProductName, null, null, null);
                fromAccountType = accountType(PortfolioAccountType.LOAN);
            }

            PortfolioAccountData toAccount = null;
            EnumOptionData toAccountType = null;
            final Long toSavingsAccountId = JdbcSupport.getLong(rs, "toSavingsAccountId");
            final String toSavingsAccountNo = rs.getString("toSavingsAccountNo");
            final Long toProductId = JdbcSupport.getLong(rs, "toProductId");
            final String toProductName = rs.getString("toProductName");
            final Long toLoanAccountId = JdbcSupport.getLong(rs, "toLoanAccountId");
            final String toLoanAccountNo = rs.getString("toLoanAccountNo");
            final Long toLoanProductId = JdbcSupport.getLong(rs, "toLoanProductId");
            final String toLoanProductName = rs.getString("toLoanProductName");

            if (toSavingsAccountId != null) {
                toAccount = new PortfolioAccountData(toSavingsAccountId, toSavingsAccountNo, null, null, null, null, null, toProductId,
                        toProductName, null, null, null);
                toAccountType = accountType(PortfolioAccountType.SAVINGS);
            } else if (toLoanAccountId != null) {
                toAccount = new PortfolioAccountData(toLoanAccountId, toLoanAccountNo, null, null, null, null, null, toLoanProductId,
                        toLoanProductName, null, null, null);
                toAccountType = accountType(PortfolioAccountType.LOAN);
            }
            
            final Integer maximumIteration = JdbcSupport.getInteger(rs, "maximumIteration");

            return StandingInstructionData.instance(id, accountDetailId, name, fromOffice, toOffice, fromClient, toClient, fromGroup, toGroup, fromAccountType,
                    fromAccount, toAccountType, toAccount, transferTypeEnum, priorityEnum, instructionTypeEnum, statusEnum, transferAmount,
                    validFrom, validTill, recurrenceTypeEnum, recurrenceFrequencyEnum, recurrenceInterval, recurrenceOnMonthDay, maximumIteration);
        }
    }

    private static final class StandingInstructionLoanDuesMapper implements RowMapper<StandingInstructionDuesData> {

        private final String schemaSql;

        public StandingInstructionLoanDuesMapper() {
            final StringBuilder sqlBuilder = new StringBuilder(400);

            sqlBuilder.append("max(ls.duedate) as dueDate,sum(ls.principal_amount) as principalAmount,");
            sqlBuilder.append("sum(ls.principal_completed_derived) as principalCompleted,");
            sqlBuilder.append("sum(ls.principal_writtenoff_derived) as principalWrittenOff,");
            sqlBuilder.append("sum(ls.interest_amount) as interestAmount,");
            sqlBuilder.append("sum(ls.interest_completed_derived) as interestCompleted,");
            sqlBuilder.append("sum(ls.interest_writtenoff_derived) as interestWrittenOff,");
            sqlBuilder.append("sum(ls.interest_waived_derived) as interestWaived,");
            sqlBuilder.append("sum(ls.penalty_charges_amount) as penalityAmount,");
            sqlBuilder.append("sum(ls.penalty_charges_completed_derived) as penalityCompleted,");
            sqlBuilder.append("sum(ls.penalty_charges_writtenoff_derived)as penaltyWrittenOff,");
            sqlBuilder.append("sum(ls.penalty_charges_waived_derived) as penaltyWaived,");
            sqlBuilder.append("sum(ls.fee_charges_amount) as feeAmount,");
            sqlBuilder.append("sum(ls.fee_charges_completed_derived) as feecompleted,");
            sqlBuilder.append("sum(ls.fee_charges_writtenoff_derived) as feeWrittenOff,");
            sqlBuilder.append("sum(ls.fee_charges_waived_derived) as feeWaived ");
            sqlBuilder.append("from m_loan_repayment_schedule ls ");
            sqlBuilder.append(" join m_loan ml on ml.id = ls.loan_id ");

            this.schemaSql = sqlBuilder.toString();
        }

        public String schema() {
            return this.schemaSql;
        }

        @Override
        public StandingInstructionDuesData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

            final LocalDate dueDate = JdbcSupport.getLocalDate(rs, "dueDate");
            final BigDecimal principalDue = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "principalAmount");
            final BigDecimal principalPaid = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "principalCompleted");
            final BigDecimal principalWrittenOff = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "principalWrittenOff");
            final BigDecimal principalOutstanding = principalDue.subtract(principalPaid).subtract(principalWrittenOff);

            final BigDecimal interestExpectedDue = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "interestAmount");
            final BigDecimal interestPaid = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "interestCompleted");
            final BigDecimal interestWrittenOff = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "interestWrittenOff");
            final BigDecimal interestWaived = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "interestWaived");
            final BigDecimal interestActualDue = interestExpectedDue.subtract(interestWaived).subtract(interestWrittenOff);
            final BigDecimal interestOutstanding = interestActualDue.subtract(interestPaid);

            final BigDecimal penaltyChargesExpectedDue = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "penalityAmount");
            final BigDecimal penaltyChargesPaid = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "penalityCompleted");
            final BigDecimal penaltyChargesWrittenOff = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "penaltyWrittenOff");
            final BigDecimal penaltyChargesWaived = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "penaltyWaived");
            final BigDecimal penaltyChargesActualDue = penaltyChargesExpectedDue.subtract(penaltyChargesWaived).subtract(
                    penaltyChargesWrittenOff);
            final BigDecimal penaltyChargesOutstanding = penaltyChargesActualDue.subtract(penaltyChargesPaid);

            final BigDecimal feeChargesExpectedDue = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "feeAmount");
            final BigDecimal feeChargesPaid = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "feecompleted");
            final BigDecimal feeChargesWrittenOff = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "feeWrittenOff");
            final BigDecimal feeChargesWaived = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "feeWaived");
            final BigDecimal feeChargesActualDue = feeChargesExpectedDue.subtract(feeChargesWaived).subtract(feeChargesWrittenOff);
            final BigDecimal feeChargesOutstanding = feeChargesActualDue.subtract(feeChargesPaid);

            final BigDecimal totalOutstanding = principalOutstanding.add(interestOutstanding).add(feeChargesOutstanding)
                    .add(penaltyChargesOutstanding);

            return new StandingInstructionDuesData(dueDate, totalOutstanding);
        }
    }

}