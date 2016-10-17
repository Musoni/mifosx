/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.portfolio.transfer.service;

import com.google.common.collect.Iterables;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.joda.time.LocalDate;
import org.mifosplatform.accounting.journalentry.service.JournalEntryWritePlatformService;
import org.mifosplatform.infrastructure.core.api.JsonCommand;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResult;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResultBuilder;
import org.mifosplatform.infrastructure.core.service.DateUtils;
import org.mifosplatform.infrastructure.security.service.PlatformSecurityContext;
import org.mifosplatform.organisation.monetary.domain.ApplicationCurrency;
import org.mifosplatform.organisation.monetary.domain.ApplicationCurrencyRepositoryWrapper;
import org.mifosplatform.organisation.monetary.domain.MonetaryCurrency;
import org.mifosplatform.organisation.office.domain.Office;
import org.mifosplatform.organisation.office.domain.OfficeRepositoryWrapper;
import org.mifosplatform.organisation.staff.domain.Staff;
import org.mifosplatform.organisation.staff.domain.StaffRepositoryWrapper;
import org.mifosplatform.portfolio.calendar.domain.Calendar;
import org.mifosplatform.portfolio.calendar.domain.CalendarEntityType;
import org.mifosplatform.portfolio.calendar.domain.CalendarInstance;
import org.mifosplatform.portfolio.calendar.domain.CalendarInstanceRepository;
import org.mifosplatform.portfolio.calendar.domain.CalendarType;
import org.mifosplatform.portfolio.calendar.service.CalendarUtils;
import org.mifosplatform.portfolio.client.domain.Client;
import org.mifosplatform.portfolio.client.domain.ClientRepositoryWrapper;
import org.mifosplatform.portfolio.client.domain.ClientStatus;
import org.mifosplatform.portfolio.client.exception.ClientHasBeenClosedException;
import org.mifosplatform.portfolio.group.domain.Group;
import org.mifosplatform.portfolio.group.domain.GroupRepositoryWrapper;
import org.mifosplatform.portfolio.group.exception.ClientNotInGroupException;
import org.mifosplatform.portfolio.group.exception.GroupNotActiveException;
import org.mifosplatform.portfolio.loanaccount.domain.Loan;
import org.mifosplatform.portfolio.loanaccount.domain.LoanRepository;
import org.mifosplatform.portfolio.loanaccount.domain.LoanTransaction;
import org.mifosplatform.portfolio.loanaccount.domain.LoanTransactionRepository;
import org.mifosplatform.portfolio.loanaccount.domain.LoanTransactionType;
import org.mifosplatform.portfolio.loanaccount.service.LoanWritePlatformService;
import org.mifosplatform.portfolio.note.service.NoteWritePlatformService;
import org.mifosplatform.portfolio.savings.domain.SavingsAccount;
import org.mifosplatform.portfolio.savings.domain.SavingsAccountRepository;
import org.mifosplatform.portfolio.savings.domain.SavingsAccountTransaction;
import org.mifosplatform.portfolio.savings.domain.SavingsAccountTransactionRepository;
import org.mifosplatform.portfolio.savings.service.SavingsAccountWritePlatformService;
import org.mifosplatform.portfolio.transfer.api.TransferApiConstants;
import org.mifosplatform.portfolio.transfer.data.TransfersDataValidator;
import org.mifosplatform.portfolio.transfer.domain.UndoTransfer;
import org.mifosplatform.portfolio.transfer.domain.UndoTransferRepository;
import org.mifosplatform.portfolio.transfer.domain.UndoTransferRepositoryWrapper;
import org.mifosplatform.portfolio.transfer.exception.ClientNotAwaitingTransferApprovalException;
import org.mifosplatform.portfolio.transfer.exception.ClientNotAwaitingTransferApprovalOrOnHoldException;
import org.mifosplatform.portfolio.transfer.exception.TransferNotSupportedException;
import org.mifosplatform.portfolio.transfer.exception.TransferNotSupportedException.TRANSFER_NOT_SUPPORTED_REASON;
import org.mifosplatform.portfolio.transfer.exception.UndoTransferNotFoundException;
import org.mifosplatform.portfolio.transfer.exception.UndoTransferWrongType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TransferWritePlatformServiceJpaRepositoryImpl implements TransferWritePlatformService {

    private final ClientRepositoryWrapper clientRepository;
    private final OfficeRepositoryWrapper officeRepository;
    private final CalendarInstanceRepository calendarInstanceRepository;
    private final GroupRepositoryWrapper groupRepository;
    private final LoanWritePlatformService loanWritePlatformService;
    private final SavingsAccountWritePlatformService savingsAccountWritePlatformService;
    private final LoanRepository loanRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final TransfersDataValidator transfersDataValidator;
    private final NoteWritePlatformService noteWritePlatformService;
    private final StaffRepositoryWrapper staffRepositoryWrapper;
    private final LoanTransactionRepository loanTransactionRepository;
    private final UndoTransferRepositoryWrapper undoTransferRepositoryWrapper;
    private final PlatformSecurityContext context;
    private final UndoTransferRepository undoTransferRepository;
    private final JournalEntryWritePlatformService journalEntryWritePlatformService;
    private final ApplicationCurrencyRepositoryWrapper applicationCurrencyRepositoryWrapper;
    private final SavingsAccountTransactionRepository savingsAccountTransactionRepository ;

    @Autowired
    public TransferWritePlatformServiceJpaRepositoryImpl(final ClientRepositoryWrapper clientRepository,
            final OfficeRepositoryWrapper officeRepository, final CalendarInstanceRepository calendarInstanceRepository,
            final LoanWritePlatformService loanWritePlatformService, final GroupRepositoryWrapper groupRepository,
            final LoanRepository loanRepository, final TransfersDataValidator transfersDataValidator,
            final NoteWritePlatformService noteWritePlatformService, final StaffRepositoryWrapper staffRepositoryWrapper,
            final SavingsAccountRepository savingsAccountRepository,
            final SavingsAccountWritePlatformService savingsAccountWritePlatformService,final LoanTransactionRepository loanTransactionRepository,
            final UndoTransferRepositoryWrapper undoTransferRepositoryWrapper,final PlatformSecurityContext context,
            final UndoTransferRepository undoTransferRepository,final JournalEntryWritePlatformService journalEntryWritePlatformService,
            final ApplicationCurrencyRepositoryWrapper applicationCurrencyRepositoryWrapper,
            final SavingsAccountTransactionRepository savingsAccountTransactionRepository) {
        this.clientRepository = clientRepository;
        this.officeRepository = officeRepository;
        this.calendarInstanceRepository = calendarInstanceRepository;
        this.loanWritePlatformService = loanWritePlatformService;
        this.groupRepository = groupRepository;
        this.loanRepository = loanRepository;
        this.transfersDataValidator = transfersDataValidator;
        this.noteWritePlatformService = noteWritePlatformService;
        this.staffRepositoryWrapper = staffRepositoryWrapper;
        this.savingsAccountRepository = savingsAccountRepository;
        this.savingsAccountWritePlatformService = savingsAccountWritePlatformService;
        this.loanTransactionRepository = loanTransactionRepository;
        this.undoTransferRepositoryWrapper = undoTransferRepositoryWrapper;
        this.context = context;
        this.undoTransferRepository = undoTransferRepository;
        this.journalEntryWritePlatformService = journalEntryWritePlatformService;
        this.applicationCurrencyRepositoryWrapper = applicationCurrencyRepositoryWrapper;
        this.savingsAccountTransactionRepository = savingsAccountTransactionRepository;

    }

    @Override
    @Transactional
    public CommandProcessingResult transferClientsBetweenGroups(final Long sourceGroupId, final JsonCommand jsonCommand) {
        this.transfersDataValidator.validateForClientsTransferBetweenGroups(jsonCommand.json());

        final Group sourceGroup = this.groupRepository.findOneWithNotFoundDetection(sourceGroupId);
        final Long destinationGroupId = jsonCommand.longValueOfParameterNamed(TransferApiConstants.destinationGroupIdParamName);
        final Group destinationGroup = this.groupRepository.findOneWithNotFoundDetection(destinationGroupId);
        final Long staffId = jsonCommand.longValueOfParameterNamed(TransferApiConstants.newStaffIdParamName);
        final Boolean inheritDestinationGroupLoanOfficer = jsonCommand
                .booleanObjectValueOfParameterNamed(TransferApiConstants.inheritDestinationGroupLoanOfficer);
        Staff staff = null;
        final Office sourceOffice = sourceGroup.getOffice();
        if (staffId != null) {
            staff = this.staffRepositoryWrapper.findByOfficeHierarchyWithNotFoundDetection(staffId, sourceOffice.getHierarchy());
        }

        final List<Client> clients = assembleListOfClients(jsonCommand);

        if (sourceGroupId == destinationGroupId) { throw new TransferNotSupportedException(
                TRANSFER_NOT_SUPPORTED_REASON.SOURCE_AND_DESTINATION_GROUP_CANNOT_BE_SAME, sourceGroupId, destinationGroupId); }

        /*** Do not allow bulk client transfers across branches ***/
        if (!(sourceOffice.getId() == destinationGroup.getOffice().getId())) { throw new TransferNotSupportedException(
                TRANSFER_NOT_SUPPORTED_REASON.BULK_CLIENT_TRANSFER_ACROSS_BRANCHES, sourceGroupId, destinationGroupId); }

        for (final Client client : clients) {
            transferClientBetweenGroups(sourceGroup, client, destinationGroup, inheritDestinationGroupLoanOfficer, staff);
        }

        return new CommandProcessingResultBuilder() //
                .withEntityId(sourceGroupId) //
                .build();
    }

    /****
     * Variables that would make sense <br/>
     * <ul>
     * <li>inheritDestinationGroupLoanOfficer: Default true</li>
     * <li>newStaffId: Optional field with Id of new Loan Officer to be linked
     * to this client and all his JLG loans for this group</li>
     * ***/
    @Transactional
    public void transferClientBetweenGroups(final Group sourceGroup, final Client client, final Group destinationGroup,
            final Boolean inheritDestinationGroupLoanOfficer, final Staff newLoanOfficer) {

        // next I shall validate that the client is present in this group
        if (!sourceGroup.hasClientAsMember(client)) { throw new ClientNotInGroupException(client.getId(), sourceGroup.getId()); }
        // Is client active?
        if (client.isNotActive()) { throw new ClientHasBeenClosedException(client.getId()); }

        /**
         * TODO: for now we need to ensure that only one collection sheet
         * calendar can be linked with a center or group entity <br/>
         **/
        final CalendarInstance sourceGroupCalendarInstance = this.calendarInstanceRepository
                .findByEntityIdAndEntityTypeIdAndCalendarTypeId(sourceGroup.getId(), CalendarEntityType.GROUPS.getValue(),
                        CalendarType.COLLECTION.getValue());
        // get all customer loans synced with this group calendar Instance
        final List<CalendarInstance> activeLoanCalendarInstances = this.calendarInstanceRepository
                .findCalendarInstancesForActiveLoansByGroupIdAndClientId(sourceGroup.getId(), client.getId());

        /**
         * if a calendar is present in the source group along with loans synced
         * with it, we should ensure that the destination group also has a
         * collection calendar
         **/
        if (sourceGroupCalendarInstance != null && !activeLoanCalendarInstances.isEmpty()) {
            // get the destination calendar
            final CalendarInstance destinationGroupCalendarInstance = this.calendarInstanceRepository
                    .findByEntityIdAndEntityTypeIdAndCalendarTypeId(destinationGroup.getId(), CalendarEntityType.GROUPS.getValue(),
                            CalendarType.COLLECTION.getValue());

            if (destinationGroupCalendarInstance == null) { throw new TransferNotSupportedException(
                    TRANSFER_NOT_SUPPORTED_REASON.DESTINATION_GROUP_HAS_NO_MEETING, destinationGroup.getId());

            }
            final Calendar sourceGroupCalendar = sourceGroupCalendarInstance.getCalendar();
            final Calendar destinationGroupCalendar = destinationGroupCalendarInstance.getCalendar();

            /***
             * Ensure that the recurrence pattern are same for collection
             * meeting in both the source and the destination calendar
             ***/
            if (!(CalendarUtils.isFrequencySame(sourceGroupCalendar.getRecurrence(), destinationGroupCalendar.getRecurrence()) && CalendarUtils
                    .isIntervalSame(sourceGroupCalendar.getRecurrence(), destinationGroupCalendar.getRecurrence()))) { throw new TransferNotSupportedException(
                    TRANSFER_NOT_SUPPORTED_REASON.DESTINATION_GROUP_MEETING_FREQUENCY_MISMATCH, sourceGroup.getId(),
                    destinationGroup.getId()); }

            /** map all JLG loans for this client to the destinationGroup **/
            for (final CalendarInstance calendarInstance : activeLoanCalendarInstances) {
                calendarInstance.updateCalendar(destinationGroupCalendar);
                this.calendarInstanceRepository.save(calendarInstance);
            }
            // reschedule all JLG Loans to follow new Calendar
            this.loanWritePlatformService.applyMeetingDateChanges(destinationGroupCalendar, activeLoanCalendarInstances);
        }

        /**
         * Now Change the loan officer for this client and all his active JLG
         * loans
         **/
        final Staff destinationGroupLoanOfficer = destinationGroup.getStaff();

        /** In case of a loan officer transfer, set the new loan officer value **/
        if (sourceGroup.getId().equals(destinationGroup.getId()) && newLoanOfficer != null) {
            client.updateStaff(newLoanOfficer);
        }/*** Else default to destination group Officer (If present) ***/
        else if (destinationGroupLoanOfficer != null) {
            client.updateStaff(destinationGroupLoanOfficer);
        }

        client.getGroups().add(destinationGroup);
        this.clientRepository.saveAndFlush(client);

        /**
         * Active JLG loans are now linked to the new Group and Loan officer
         **/
        final List<Loan> allClientJLGLoans = this.loanRepository.findByClientIdAndGroupId(client.getId(), sourceGroup.getId());
        for (final Loan loan : allClientJLGLoans) {
            if (loan.status().isActiveOrAwaitingApprovalOrDisbursal()) {
                loan.updateGroup(destinationGroup);
                if (inheritDestinationGroupLoanOfficer != null && inheritDestinationGroupLoanOfficer == true
                        && destinationGroupLoanOfficer != null) {
                    loan.reassignLoanOfficer(destinationGroupLoanOfficer, DateUtils.getLocalDateOfTenant());
                } else if (newLoanOfficer != null) {
                    loan.reassignLoanOfficer(newLoanOfficer, DateUtils.getLocalDateOfTenant());
                }
                this.loanRepository.saveAndFlush(loan);
            }
        }

        /**
         * change client group membership (only if source group and destination
         * group are not the same, i.e only Loan officer Transfer)
         **/
        if (!sourceGroup.getId().equals(destinationGroup.getId())) {
            client.getGroups().remove(sourceGroup);
        }

    }

    /**
     * This API is meant for transferring clients between branches mainly by
     * Organizations following an Individual lending Model <br/>
     * 
     * @param clientId
     * @param jsonCommand
     * @return
     **/
    @Transactional
    @Override
    public CommandProcessingResult proposeAndAcceptClientTransfer(final Long clientId, final JsonCommand jsonCommand) {
        // validation
        this.transfersDataValidator.validateForProposeAndAcceptClientTransfer(jsonCommand.json());

        final Long destinationOfficeId = jsonCommand.longValueOfParameterNamed(TransferApiConstants.destinationOfficeIdParamName);
        final Office office = this.officeRepository.findOneWithNotFoundDetection(destinationOfficeId);
        final Client client = this.clientRepository.findOneWithNotFoundDetection(clientId);
        handleClientTransferLifecycleEvent(client, office, TransferEventType.PROPOSAL, jsonCommand);
        this.clientRepository.saveAndFlush(client);
        handleClientTransferLifecycleEvent(client, client.getTransferToOffice(), TransferEventType.ACCEPTANCE, jsonCommand);
        this.clientRepository.save(client);

        return new CommandProcessingResultBuilder() //
                .withOfficeId(client.officeId())
                .withClientId(clientId) //
                .withEntityId(clientId) //
                .build();
    }

    /**
     * This API is meant for transferring clients between branches mainly by
     * Organizations following an Individual lending Model <br/>
     * If the Client is linked to any Groups, we can optionally choose to have
     * all the linkages broken and all JLG Loans are converted into Individual
     * Loans
     * 
     * @param clientId
     * @param jsonCommand
     * @return
     **/
    @Transactional
    @Override
    public CommandProcessingResult proposeClientTransfer(final Long clientId, final JsonCommand jsonCommand) {
        // validation
        this.transfersDataValidator.validateForProposeClientTransfer(jsonCommand.json());

        final Long destinationOfficeId = jsonCommand.longValueOfParameterNamed(TransferApiConstants.destinationOfficeIdParamName);
        final Office office = this.officeRepository.findOneWithNotFoundDetection(destinationOfficeId);
        final Client client = this.clientRepository.findOneWithNotFoundDetection(clientId);
        handleClientTransferLifecycleEvent(client, office, TransferEventType.PROPOSAL, jsonCommand);
        this.clientRepository.save(client);

        return new CommandProcessingResultBuilder() //
                .withOfficeId(client.officeId())
                .withClientId(clientId) //
                .withEntityId(clientId) //
                .build();
    }

    /**
     * This API is meant for transferring clients between branches mainly by
     * Organizations following an Individual lending Model <br/>
     * If the Client is linked to any Groups, we can optionally choose to have
     * all the linkages broken and all JLG Loans are converted into Individual
     * Loans
     * 
     * @param clientId
     * @param jsonCommand
     * @return
     **/
    @Transactional
    @Override
    public CommandProcessingResult acceptClientTransfer(final Long clientId, final JsonCommand jsonCommand) {
        // validation
        this.transfersDataValidator.validateForAcceptClientTransfer(jsonCommand.json());
        final Client client = this.clientRepository.findOneWithNotFoundDetection(clientId);
        validateClientAwaitingTransferAcceptance(client);

        handleClientTransferLifecycleEvent(client, client.getTransferToOffice(), TransferEventType.ACCEPTANCE, jsonCommand);
        this.clientRepository.save(client);

        return new CommandProcessingResultBuilder() //
                .withOfficeId(client.officeId())
                .withClientId(clientId) //
                .withEntityId(clientId) //
                .build();
    }

    @Override
    @Transactional
    public CommandProcessingResult withdrawClientTransfer(final Long clientId, final JsonCommand jsonCommand) {
        // validation
        this.transfersDataValidator.validateForWithdrawClientTransfer(jsonCommand.json());
        final Client client = this.clientRepository.findOneWithNotFoundDetection(clientId);
        validateClientAwaitingTransferAcceptanceOnHold(client);

        handleClientTransferLifecycleEvent(client, client.getOffice(), TransferEventType.WITHDRAWAL, jsonCommand);
        this.clientRepository.save(client);

        return new CommandProcessingResultBuilder() //
                .withOfficeId(client.officeId())
                .withClientId(clientId) //
                .withEntityId(clientId) //
                .build();
    }

    @Override
    @Transactional
    public CommandProcessingResult rejectClientTransfer(final Long clientId, final JsonCommand jsonCommand) {
        // validation
        this.transfersDataValidator.validateForRejectClientTransfer(jsonCommand.json());
        final Client client = this.clientRepository.findOneWithNotFoundDetection(clientId);
        handleClientTransferLifecycleEvent(client, client.getOffice(), TransferEventType.REJECTION, jsonCommand);
        this.clientRepository.save(client);

        return new CommandProcessingResultBuilder() //
                .withOfficeId(client.officeId())
                .withClientId(clientId) //
                .withEntityId(clientId) //
                .build();
    }

    private void handleClientTransferLifecycleEvent(final Client client, final Office destinationOffice,
            final TransferEventType transferEventType, final JsonCommand jsonCommand) {
        final Date todaysDate = DateUtils.getDateOfTenant();
        /** Get destination loan officer if exists **/
        Staff staff = null;
        Group destinationGroup = null;
        final Long staffId = jsonCommand.longValueOfParameterNamed(TransferApiConstants.newStaffIdParamName);
        final Long destinationGroupId = jsonCommand.longValueOfParameterNamed(TransferApiConstants.destinationGroupIdParamName);
        if (staffId != null) {
            staff = this.staffRepositoryWrapper.findByOfficeHierarchyWithNotFoundDetection(staffId, destinationOffice.getHierarchy());
        }
        if (transferEventType.isAcceptance() && destinationGroupId != null) {
            destinationGroup = this.groupRepository.findByOfficeWithNotFoundDetection(destinationGroupId, destinationOffice);
        }

        /** Find out if this is a transfer between offices **/
        Boolean isOfficeTransfer = true;
        if(destinationOffice.equals(client.getOffice()))
        {
            isOfficeTransfer = false;
        }

        /*** Handle Active Loans ***/
        if (this.loanRepository.doNonClosedLoanAccountsExistForClient(client.getId())) {
            // get each individual loan for the client
            for (final Loan loan : this.loanRepository.findLoanByClientId(client.getId())) {
                /**
                 * We need to create transactions etc only for loans which are
                 * disbursed and not yet closed
                 **/
                if (loan.isDisbursed() && !loan.isClosed()) {
                    switch (transferEventType) {
                        case ACCEPTANCE:
                            this.loanWritePlatformService.acceptLoanTransfer(loan.getId(), DateUtils.getLocalDateOfTenant(),
                                    destinationOffice, staff, isOfficeTransfer);
                        break;
                        case PROPOSAL:
                            if(isOfficeTransfer) {
                                this.loanWritePlatformService.initiateLoanTransfer(loan.getId(), DateUtils.getLocalDateOfTenant());
                            }
                        break;
                        case REJECTION:
                            this.loanWritePlatformService.rejectLoanTransfer(loan.getId());
                        break;
                        case WITHDRAWAL:
                            if(isOfficeTransfer) {
                                this.loanWritePlatformService.withdrawLoanTransfer(loan.getId(), DateUtils.getLocalDateOfTenant());
                            }
                    }
                }
            }
        }

        /*** Handle Active Savings (Currently throw and exception) ***/
        if (this.savingsAccountRepository.doNonClosedSavingAccountsExistForClient(client.getId())) {
            // get each individual saving account for the client
            for (final SavingsAccount savingsAccount : this.savingsAccountRepository.findSavingAccountByClientId(client.getId())) {
                if (savingsAccount.isActivated() && !savingsAccount.isClosed()) {
                    switch (transferEventType) {
                        case ACCEPTANCE:
                            this.savingsAccountWritePlatformService.acceptSavingsTransfer(savingsAccount.getId(),
                                    DateUtils.getLocalDateOfTenant(), destinationOffice, staff, isOfficeTransfer);
                        break;
                        case PROPOSAL:
                            if(isOfficeTransfer) {
                                this.savingsAccountWritePlatformService.initiateSavingsTransfer(savingsAccount.getId(),
                                        DateUtils.getLocalDateOfTenant());
                            }
                        break;
                        case REJECTION:
                            this.savingsAccountWritePlatformService.rejectSavingsTransfer(savingsAccount.getId());
                        break;
                        case WITHDRAWAL:
                            if(isOfficeTransfer) {
                                this.savingsAccountWritePlatformService.withdrawSavingsTransfer(savingsAccount.getId(),
                                        DateUtils.getLocalDateOfTenant());
                            }
                    }
                }
            }
        }

        switch (transferEventType) {
            case ACCEPTANCE:
                Integer oldStatus = client.getStatus();

                // If client has rejected status, the office joining date is not available:
                Date officeJoiningDate = null;
                if(client.getStatus().equals(ClientStatus.REJECTED.getValue()) || client.getStatus().equals(ClientStatus.PENDING.getValue()))
                {
                    officeJoiningDate =  todaysDate;
                }

                // Set clientstatus to active, as some of the actions below require it to be active before transfer.
                client.setStatus(ClientStatus.ACTIVE.getValue());

                Long sourceGroupId = null;
                if(client.getGroups().size() == 1){
                    Group getSourceGroup = Iterables.get(client.getGroups(),0);
                    sourceGroupId = getSourceGroup.getId();
                }

                final UndoTransfer undoTransfer = UndoTransfer.instance(client,null,null,client.getOffice().getId(),sourceGroupId,client.getStaff().getId(),todaysDate,this.context.authenticatedUser(),
                        todaysDate,this.context.authenticatedUser(),officeJoiningDate);
                this.undoTransferRepositoryWrapper.saveAndFlush(undoTransfer);
                client.updateTransferToOffice(null);
                client.updateOffice(destinationOffice);
                client.updateOfficeJoiningDate(todaysDate);
                if (client.getGroups().size() == 1) {
                    if (destinationGroup == null) {
                        throw new TransferNotSupportedException(TRANSFER_NOT_SUPPORTED_REASON.CLIENT_DESTINATION_GROUP_NOT_SPECIFIED,
                                client.getId());
                    } else if (!destinationGroup.isActive()) { throw new GroupNotActiveException(destinationGroup.getId()); }
                    transferClientBetweenGroups(Iterables.get(client.getGroups(), 0), client, destinationGroup, true, staff);
                } else if (client.getGroups().size() == 0 && destinationGroup != null) {
                    client.getGroups().add(destinationGroup);
                    client.updateStaff(destinationGroup.getStaff());
                    if (staff != null) {
                        client.updateStaff(staff);
                    }
                    /* for individual clients update clients table with staff id */
                }else if(destinationGroup == null) {
                    if(staff !=null){ client.updateStaff(staff);}
                }

                client.setStatus(oldStatus);
            break;
            case PROPOSAL:
                if(client.getStatus().equals(ClientStatus.ACTIVE)) {
                    client.setStatus(ClientStatus.TRANSFER_IN_PROGRESS.getValue());
                }
                client.updateTransferToOffice(destinationOffice);
            break;
            case REJECTION:
                client.setStatus(ClientStatus.TRANSFER_ON_HOLD.getValue());
                client.updateTransferToOffice(null);
                break;
            case WITHDRAWAL:
                client.setStatus(ClientStatus.ACTIVE.getValue());
                client.updateTransferToOffice(null);
        }

        this.noteWritePlatformService.createAndPersistClientNote(client, jsonCommand);
    }

    private List<Client> assembleListOfClients(final JsonCommand command) {

        final List<Client> clients = new ArrayList<>();

        if (command.parameterExists(TransferApiConstants.clients)) {
            final JsonArray clientsArray = command.arrayOfParameterNamed(TransferApiConstants.clients);
            if (clientsArray != null) {
                for (int i = 0; i < clientsArray.size(); i++) {

                    final JsonObject jsonObject = clientsArray.get(i).getAsJsonObject();
                    if (jsonObject.has(TransferApiConstants.idParamName)) {
                        final Long id = jsonObject.get(TransferApiConstants.idParamName).getAsLong();
                        final Client client = this.clientRepository.findOneWithNotFoundDetection(id);
                        clients.add(client);
                    }
                }
            }
        }
        return clients;
    }

    private void validateClientAwaitingTransferAcceptance(final Client client) {
        if (!client.isTransferInProgress()) { throw new ClientNotAwaitingTransferApprovalException(client.getId()); }
    }

    /**
     * private void validateGroupAwaitingTransferAcceptance(final Group group) {
     * if (!group.isTransferInProgress()) { throw new
     * ClientNotAwaitingTransferApprovalException(group.getId()); } }
     **/

    private void validateClientAwaitingTransferAcceptanceOnHold(final Client client) {
        if (!client.isTransferInProgressOrOnHold()) { throw new ClientNotAwaitingTransferApprovalOrOnHoldException(client.getId()); }
    }

    /**
     * private void validateGroupAwaitingTransferAcceptanceOnHold(final Group
     * group) { if (!group.isTransferInProgressOrOnHold()) { throw new
     * ClientNotAwaitingTransferApprovalException(group.getId()); } }
     **/

    @Override
    @Transactional
    public CommandProcessingResult transferGroupBetweenBranches(Long sourceGroupId, JsonCommand jsonCommand) {
        // validation
        this.transfersDataValidator.validateForProposeAndAcceptClientTransfer(jsonCommand.json());
        final Date todaysDate = DateUtils.getDateOfTenant();

        final Long destinationOfficeId = jsonCommand.longValueOfParameterNamed(TransferApiConstants.destinationOfficeIdParamName);
        final Office office = this.officeRepository.findOneWithNotFoundDetection(destinationOfficeId);
        final Group sourceGroup = this.groupRepository.findOneWithNotFoundDetection(sourceGroupId);
        final Long destinationGroupId = jsonCommand.longValueOfParameterNamed(TransferApiConstants.destinationGroupIdParamName);
        final Long staffId = jsonCommand.longValueOfParameterNamed(TransferApiConstants.newStaffIdParamName);
        Staff staff = null;
        if (staffId != null) {
            staff = this.staffRepositoryWrapper.findByOfficeHierarchyWithNotFoundDetection(staffId, office.getHierarchy());
        }

        //validate sourceOffice  and destinationOffice should not be the same
        if(sourceGroup.getOffice().getId().equals(office.getId())){
             throw new TransferNotSupportedException(
                    TRANSFER_NOT_SUPPORTED_REASON.SOURCE_AND_DESTINATION_OFFICE_CANNOT_BE_THE_SAME, sourceGroup.getOffice().getId(), destinationOfficeId);
        }
        //this allows you to move the group to another branch while keeping the group name



        if((sourceGroup.getId().longValue() == destinationGroupId) && (sourceGroup.getOffice().getId().longValue() != office.getId())){

            final Long transferFromOfficeId = sourceGroup.getOffice().getId();
            final Long transferFromStaffId = sourceGroup.getStaff().getId();
            //move source group to different office
            sourceGroup.updateOffice(office);
            //change loanOfficer of group
            sourceGroup.updateStaff(staff);

            final UndoTransfer undoTransfer = UndoTransfer.instance(null, sourceGroup, null,transferFromOfficeId, sourceGroupId, transferFromStaffId, todaysDate, this.context.authenticatedUser(),
                    todaysDate, this.context.authenticatedUser(), sourceGroup.getActivationLocalDate().toDate());
            this.undoTransferRepositoryWrapper.saveAndFlush(undoTransfer);


            /** update loan and savings officer for group loans */

            final LocalDate loanOfficerReassignmentDate = DateUtils.getLocalDateOfTenant();

            if(this.loanRepository.doNonClosedLoanAccountsExistForGroup(sourceGroupId)){
                for(final Loan loan : this.loanRepository.findByGroupId(sourceGroupId)){
                    if(!loan.isClosed()){
                        loan.reassignLoanOfficer(staff,loanOfficerReassignmentDate);
                    }
                }
            }
            if(this.savingsAccountRepository.doNonClosedSavingAccountsExistForGroup(sourceGroupId)){
                for(final SavingsAccount savingsAccount : this.savingsAccountRepository.findByGroupId(sourceGroupId)){
                    if(!savingsAccount.isClosed()){
                        savingsAccount.reassignSavingsOfficer(staff,loanOfficerReassignmentDate);
                    }
                }
            }

            final Set<Client> clients = sourceGroup.getClientMembers();

            for(Client client : clients){
                handleClientTransferLifecycleEvent(client, office, TransferEventType.PROPOSAL, jsonCommand);
                this.clientRepository.saveAndFlush(client);
                handleClientTransferLifecycleEvent(client, client.getTransferToOffice(), TransferEventType.ACCEPTANCE, jsonCommand);
                this.clientRepository.save(client);
                //update client in undoTransfer with boolean part Of group
                UndoTransfer undoTransfers = this.undoTransferRepository.findClientUndoTransfer(client.getId());
                undoTransfers.setGroupTransfer(true);
                this.undoTransferRepository.save(undoTransfers);
            }


        }




        return new CommandProcessingResultBuilder() //
                .withGroupId(sourceGroupId)
                .withOfficeId(sourceGroup.officeId())
                .withEntityId(sourceGroupId) //
                .build();
    }

    @Override
    public CommandProcessingResult transferLoanOfficerToGroup(Long sourceGroupId, JsonCommand jsonCommand) {
        this.transfersDataValidator.validateTransferLoanOfficerToGroup(jsonCommand.json());

        final Group sourceGroup = this.groupRepository.findOneWithNotFoundDetection(sourceGroupId);
        final Office office = this.officeRepository.findOneWithNotFoundDetection(sourceGroup.getOffice().getId());

        final Long staffId = jsonCommand.longValueOfParameterNamed(TransferApiConstants.newStaffIdParamName);
        Staff staff = null;
        if (staffId != null) {
            staff = this.staffRepositoryWrapper.findByOfficeHierarchyWithNotFoundDetection(staffId, office.getHierarchy());
        }

        //validate  staff id of the group cannot be the same as the previous
        if(staff !=null) {
            if(sourceGroup.getStaff().getId().equals(staff.getId())){
                throw new TransferNotSupportedException(
                        TRANSFER_NOT_SUPPORTED_REASON.SOURCE_AND_DESTINATION_STAFF_CANNOT_BE_THE_SAME, sourceGroup.getStaff().getId());
            }
        }

        /*
         loan officer reassignment date default to today to avoid validation problems
         */
        LocalDate loanOfficerReassignmentDate = DateUtils.getLocalDateOfTenant();

        //update loan officer of the group
        sourceGroup.updateStaff(staff);
        /*
        update loan officer for client and  update officer for clients loans and savings
         */
        Set<Client> clients = sourceGroup.getClientMembers();
        for(Client client : clients){
            client.updateStaff(staff);
            if(this.loanRepository.doNonClosedLoanAccountsExistForClient(client.getId())){
                   for(final Loan loan :this.loanRepository.findLoanByClientId(client.getId())){
                       if (!loan.isClosed()){
                           loan.reassignLoanOfficer(staff,loanOfficerReassignmentDate);
                       }
                }
            }
            if(this.savingsAccountRepository.doNonClosedSavingAccountsExistForClient(client.getId())){
                for (final SavingsAccount savingsAccount : this.savingsAccountRepository.findSavingAccountByClientId(client.getId())){
                    if (!savingsAccount.isClosed()){
                        savingsAccount.reassignSavingsOfficer(staff, loanOfficerReassignmentDate);
                    }
                }
            }
        }


        /*
          update group loans and savings
         */

        if(this.loanRepository.doNonClosedLoanAccountsExistForGroup(sourceGroupId)){
            for(final Loan loan : this.loanRepository.findByGroupId(sourceGroupId)){
                if(!loan.isClosed()){
                    loan.reassignLoanOfficer(staff,loanOfficerReassignmentDate);
                }
            }
        }

        if(this.savingsAccountRepository.doNonClosedSavingAccountsExistForGroup(sourceGroupId)){
            for(final SavingsAccount savingsAccount : this.savingsAccountRepository.findByGroupId(sourceGroupId)){
                if(!savingsAccount.isClosed()){
                    savingsAccount.reassignSavingsOfficer(staff,loanOfficerReassignmentDate);
                }
            }
        }

        return new CommandProcessingResultBuilder() //
                .withOfficeId(office.getId())
                .withGroupId(sourceGroupId)
                .withEntityId(sourceGroupId) //
                .build();
    }



    public CommandProcessingResult undoClientTransfer(final Long clientId, final JsonCommand jsonCommand) {
        final Client client = this.clientRepository.findOneWithNotFoundDetection(clientId);
        if(client.isNotActive() && this.undoTransferRepositoryWrapper.doesClientExistInUndoTransfer(clientId)){
            throw new TransferNotSupportedException(TRANSFER_NOT_SUPPORTED_REASON.UNDO_TRANSFER_NOT_SUPPORTED);
        }

        final UndoTransfer clientUndoTransfer = this.undoTransferRepository.findClientUndoTransfer(clientId);
        if(clientUndoTransfer.isGroupTransfer()){
           throw new UndoTransferWrongType();
        }

        if(clientUndoTransfer.getTransferFromOfficeId() != client.getOffice().getId()) {
            if (this.paymentTransactionAfterTransfer(clientId)) {
                throw new TransferNotSupportedException(TRANSFER_NOT_SUPPORTED_REASON.TRANSACTIONS_AFTER_TRANSFER);
            }
        }

        /*
          reverse transaction made on initiate transfer and accept transfer
          Ideally this reversal would be inside the previous If, but we can't put it there because there are still transfers that did do the bookings, that can be reversed.
         */
        this.reverseTransferTransaction(clientId,clientUndoTransfer.getSubmittedOnDate());




        this.undoClientTransfer(client,clientId);

        return new CommandProcessingResultBuilder() //
                .withOfficeId(client.officeId())
                .withClientId(clientId) //
                .withEntityId(clientId) //
                .build();
    }


    private void undoClientTransfer(final Client client,final Long clientId){

        final UndoTransfer undoTransfer = this.undoTransferRepository.findClientUndoTransfer(clientId);

        if(undoTransfer == null ){
            throw new TransferNotSupportedException(TRANSFER_NOT_SUPPORTED_REASON.UNDO_TRANSFER_NOT_SUPPORTED);
        }

        final Staff staff = this.staffRepositoryWrapper.findOneWithNotFoundDetection(undoTransfer.getTransferFromStaffId());
        final Office office = this.officeRepository.findOneWithNotFoundDetection(undoTransfer.getTransferFromOfficeId());
        Group group  = null;
        if(undoTransfer.getTransferFromGroupId() != null){
            group = this.groupRepository.findOneWithNotFoundDetection(undoTransfer.getTransferFromGroupId());
        }

        client.updateStaff(staff);
        client.updateOffice(office);
        client.updateOfficeJoiningDate(undoTransfer.getOfficeJoiningDate());
        if(group !=null){
            if(client.getGroups().size() == 1){
                Group transferToGroup = Iterables.get(client.getGroups(),0);
                client.getGroups().remove(transferToGroup);
                client.getGroups().add(group);
            }
        }

        if(this.loanRepository.doNonClosedLoanAccountsExistForClient(client.getId())){
            for(final Loan loan :this.loanRepository.findLoanByClientId(client.getId())){
                if (loan.isDisbursed() && !loan.isClosed()){
                    loan.reassignLoanOfficer(staff,DateUtils.getLocalDateOfTenant());
                }
            }
        }
        if(this.savingsAccountRepository.doNonClosedSavingAccountsExistForClient(client.getId())){
            for (final SavingsAccount savingsAccount : this.savingsAccountRepository.findSavingAccountByClientId(client.getId())){
                if (!savingsAccount.isClosed()){
                    savingsAccount.reassignSavingsOfficer(staff,DateUtils.getLocalDateOfTenant());
                }
            }
        }
        this.clientRepository.save(client);
        undoTransfer.updateTransferUndone(true);
        this.undoTransferRepository.save(undoTransfer);


    }

    private void reverseTransferTransaction(final Long clientId,final LocalDate clientTransferDate){
        if(this.loanRepository.doNonClosedLoanAccountsExistForClient(clientId)){
            for(final Loan loan : this.loanRepository.findLoanByClientId(clientId)){
                if(loan.isDisbursed() && !loan.isClosed()){
                    List<Long> existingTransactionIds = new ArrayList<Long>(loan.findExistingTransactionIds());
                    List<Long> existingReversedTransactionIds = new ArrayList<Long>(loan.findExistingReversedTransactionIds());
                    for(final LoanTransaction loanTransaction :this.loanTransactionRepository.currentTransferTransaction(loan.getId())){
                        if(loanTransaction.getTransactionDate().equals(clientTransferDate)){
                            if(loanTransaction.getTypeOf().equals(LoanTransactionType.INITIATE_TRANSFER) && loanTransaction.isNotReversed()){
                                loanTransaction.reverse();
                            }
                            if(loanTransaction.getTypeOf().equals(LoanTransactionType.APPROVE_TRANSFER) && loanTransaction.isNotReversed()){
                                loanTransaction.reverse();
                            }
                        }

                    }
                    this.postJournalEntriesLoan(loan,existingTransactionIds,existingReversedTransactionIds);
                }

            }
        }
        if(this.savingsAccountRepository.doNonClosedSavingAccountsExistForClient(clientId)) {
            for(final SavingsAccount savingsAccount: this.savingsAccountRepository.findSavingAccountByClientId(clientId)){
                if(!savingsAccount.isClosed()){
                    Set<Long> existingTransactionIds = new HashSet<Long>(savingsAccount.findExistingTransactionIds());
                    Set<Long> existingReversedTransactionIds = new HashSet<Long>(savingsAccount.findExistingReversedTransactionIds());
                    for(SavingsAccountTransaction savingsAccountTransaction: this.savingsAccountTransactionRepository.currentTransferTransaction(savingsAccount.getId())){
                        if(savingsAccountTransaction.transactionLocalDate().equals(clientTransferDate)){
                            if(savingsAccountTransaction.isTransferInitiation() && savingsAccountTransaction.isNotReversed()){
                                savingsAccountTransaction.reverse();
                            }
                            if(savingsAccountTransaction.isTransferApproval() && savingsAccountTransaction.isNotReversed()){
                                savingsAccountTransaction.reverse();
                            }
                        }

                    }

                    this.postJournalEntries(savingsAccount, existingTransactionIds, existingReversedTransactionIds);
                }
            }
        }

    }

    private void postJournalEntries(final SavingsAccount savingsAccount, final Set<Long> existingTransactionIds,
                                    final Set<Long> existingReversedTransactionIds) {

        final MonetaryCurrency currency = savingsAccount.getCurrency();
        final ApplicationCurrency applicationCurrency = this.applicationCurrencyRepositoryWrapper.findOneWithNotFoundDetection(currency);
        boolean isAccountTransfer = false;
        final Map<String, Object> accountingBridgeData = savingsAccount.deriveAccountingBridgeData(applicationCurrency.toData(),
                existingTransactionIds, existingReversedTransactionIds, isAccountTransfer);
        this.journalEntryWritePlatformService.createJournalEntriesForSavings(accountingBridgeData);
    }

    private void postJournalEntriesLoan(final Loan loan, final List<Long> existingTransactionIds,
                                    final List<Long> existingReversedTransactionIds) {

        final MonetaryCurrency currency = loan.getCurrency();
        final ApplicationCurrency applicationCurrency = this.applicationCurrencyRepositoryWrapper.findOneWithNotFoundDetection(currency);
        boolean isAccountTransfer = false;
        final Map<String, Object> accountingBridgeData = loan.deriveAccountingBridgeData(applicationCurrency.toData(),
                existingTransactionIds, existingReversedTransactionIds, isAccountTransfer);
        this.journalEntryWritePlatformService.createJournalEntriesForLoan(accountingBridgeData);
    }

    private boolean paymentTransactionAfterTransfer(final Long clientId){
        boolean hasPaymentTransactionHappenedAfterTransfer = false;
        /*
            find clients loan and check if a loan transaction has
            been made after transfer
         */
        if(this.loanRepository.doNonClosedLoanAccountsExistForClient(clientId)){
            for(final Loan loan : this.loanRepository.findLoanByClientId(clientId)){
                if(loan.isDisbursed() && !loan.isClosed()){
                    Integer transactionEnumType= 13;
                    for(LoanTransaction loanTransaction: this.loanTransactionRepository.transactionsAfterClientTransfer(loan.getId(),transactionEnumType)){
                        final LoanTransaction lastApprovedClientTransfer = this.loanTransactionRepository.lastApprovedTransfer(loan.getId());
                        if(!loanTransaction.isReversed() && (loanTransaction.getTransactionDate().isAfter(lastApprovedClientTransfer.getTransactionDate())
                                || (loanTransaction.getTransactionDate().isEqual(lastApprovedClientTransfer.getTransactionDate()) && loanTransaction.getId() > lastApprovedClientTransfer.getId())))
                        {   //means active transactions exist after transfer
                            hasPaymentTransactionHappenedAfterTransfer = true;
                            break;
                        }
                    }
                }
            }
        }
        if(this.savingsAccountRepository.doNonClosedSavingAccountsExistForClient(clientId)) {
            for(final SavingsAccount savingsAccount: this.savingsAccountRepository.findSavingAccountByClientId(clientId)){
                if(!savingsAccount.isClosed()){
                    for(SavingsAccountTransaction savingsAccountTransaction: this.savingsAccountTransactionRepository.transactionsAfterClientTransfer(savingsAccount.getId(),13)){
                        final SavingsAccountTransaction lastApprovedClientTransfer = this.savingsAccountTransactionRepository.lastApprovedTransfer(savingsAccount.getId());
                        if(!savingsAccountTransaction.isReversed() && (savingsAccountTransaction.transactionLocalDate().isAfter(lastApprovedClientTransfer.transactionLocalDate())
                        || (savingsAccountTransaction.transactionLocalDate().isEqual(lastApprovedClientTransfer.transactionLocalDate()) && savingsAccountTransaction.getId() > lastApprovedClientTransfer.getId()))){
                            hasPaymentTransactionHappenedAfterTransfer = true;
                            break;
                        }
                    }
                }
            }
        }
        return hasPaymentTransactionHappenedAfterTransfer;
    }

    @Override
    public CommandProcessingResult undoGroupTransfer(Long groupId, JsonCommand jsonCommand) {

        final Group group = this.groupRepository.findOneWithNotFoundDetection(groupId);

        if(group.isNotActive() && this.undoTransferRepositoryWrapper.doesGroupExistInUndoTransfer(groupId)){
            throw new TransferNotSupportedException(TRANSFER_NOT_SUPPORTED_REASON.UNDO_GROUP_TRANSFER_NOT_SUPPORTED);
        }
        final UndoTransfer groupUndoTransfer = this.undoTransferRepository.findGroupUndoTransfer(groupId);

        if(groupUndoTransfer == null){
            throw new TransferNotSupportedException(TRANSFER_NOT_SUPPORTED_REASON.UNDO_GROUP_TRANSFER_NOT_SUPPORTED);
        }

        final Set<Client> clients = group.getClientMembers();
        for(Client client : clients){
            //if new client is added to the group before the transfer refuse transfer
            if(!this.undoTransferRepositoryWrapper.doesClientExistInUndoTransfer(client.getId())){
                throw new UndoTransferNotFoundException(client.getId());
            }
            if(this.paymentTransactionAfterTransfer(client.getId())){
                throw new TransferNotSupportedException(TRANSFER_NOT_SUPPORTED_REASON.TRANSACTIONS_AFTER_TRANSFER);
            }
        }

        final Staff oldStaffBeforeTransfer = this.staffRepositoryWrapper.findOneWithNotFoundDetection(groupUndoTransfer.getTransferFromStaffId());

        final Office oldOfficeBeforeTransfer = this.officeRepository.findOneWithNotFoundDetection(groupUndoTransfer.getTransferFromOfficeId());

        group.updateStaff(oldStaffBeforeTransfer);
        group.updateOffice(oldOfficeBeforeTransfer);

        /* update group loans and savings back to old loan officer */
        final LocalDate loanOfficerReassignmentDate = DateUtils.getLocalDateOfTenant();
        if(this.loanRepository.doNonClosedLoanAccountsExistForGroup(groupId)){
            for(final Loan loan : this.loanRepository.findByGroupId(groupId)){
                if(!loan.isClosed()){
                    loan.reassignLoanOfficer(oldStaffBeforeTransfer,loanOfficerReassignmentDate);
                }
            }
        }

        if(this.savingsAccountRepository.doNonClosedSavingAccountsExistForGroup(groupId)){
            for(final SavingsAccount savingsAccount : this.savingsAccountRepository.findByGroupId(groupId)){
                if(!savingsAccount.isClosed()){
                    savingsAccount.reassignSavingsOfficer(oldStaffBeforeTransfer,loanOfficerReassignmentDate);
                }
            }
        }



        /*
          transfer client back to group and
         */
        for(Client client : clients){
             this.reverseTransferTransaction(client.getId(),groupUndoTransfer.getSubmittedOnDate());
             this.undoClientTransfer(client,client.getId());
        }

        groupUndoTransfer.updateTransferUndone(true);   // transfer undo to true
        this.undoTransferRepository.save(groupUndoTransfer);

        return new CommandProcessingResultBuilder() //
                .withOfficeId(group.officeId())
                .withGroupId(groupId) //
                .withEntityId(groupId) //
                .build();
    }
}