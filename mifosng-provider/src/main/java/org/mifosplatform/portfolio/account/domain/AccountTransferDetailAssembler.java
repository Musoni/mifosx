/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.portfolio.account.domain;

import java.util.Locale;

import org.mifosplatform.infrastructure.core.api.JsonCommand;
import org.mifosplatform.infrastructure.core.serialization.FromJsonHelper;
import org.mifosplatform.organisation.office.domain.Office;
import org.mifosplatform.organisation.office.domain.OfficeRepository;
import org.mifosplatform.portfolio.client.domain.Client;
import org.mifosplatform.portfolio.client.domain.ClientRepositoryWrapper;
import org.mifosplatform.portfolio.group.domain.Group;
import org.mifosplatform.portfolio.group.domain.GroupRepository;
import org.mifosplatform.portfolio.group.domain.GroupRepositoryWrapper;
import org.mifosplatform.portfolio.loanaccount.domain.Loan;
import org.mifosplatform.portfolio.loanaccount.service.LoanAssembler;
import org.mifosplatform.portfolio.savings.domain.SavingsAccount;
import org.mifosplatform.portfolio.savings.domain.SavingsAccountAssembler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.JsonElement;

import static org.mifosplatform.portfolio.account.AccountDetailConstants.*;

@Service
public class AccountTransferDetailAssembler {

    private final ClientRepositoryWrapper clientRepository;
    private final GroupRepositoryWrapper groupRepository;
    private final OfficeRepository officeRepository;
    private final SavingsAccountAssembler savingsAccountAssembler;
    private final FromJsonHelper fromApiJsonHelper;
    private final LoanAssembler loanAccountAssembler;

    @Autowired
    public AccountTransferDetailAssembler(final ClientRepositoryWrapper clientRepository, final OfficeRepository officeRepository,
            final SavingsAccountAssembler savingsAccountAssembler, final FromJsonHelper fromApiJsonHelper,
            final LoanAssembler loanAccountAssembler,final GroupRepositoryWrapper groupRepository) {
        this.clientRepository = clientRepository;
        this.officeRepository = officeRepository;
        this.savingsAccountAssembler = savingsAccountAssembler;
        this.fromApiJsonHelper = fromApiJsonHelper;
        this.loanAccountAssembler = loanAccountAssembler;
        this.groupRepository = groupRepository;
    }

    public AccountTransferDetails assembleSavingsToSavingsTransfer(final JsonCommand command) {

        final Long fromSavingsId = command.longValueOfParameterNamed(fromAccountIdParamName);
        final SavingsAccount fromSavingsAccount = this.savingsAccountAssembler.assembleFrom(fromSavingsId);

        final Long toSavingsId = command.longValueOfParameterNamed(toAccountIdParamName);
        final SavingsAccount toSavingsAccount = this.savingsAccountAssembler.assembleFrom(toSavingsId);

        return assembleSavingsToSavingsTransfer(command, fromSavingsAccount, toSavingsAccount);

    }

    public AccountTransferDetails assembleSavingsToLoanTransfer(final JsonCommand command) {

        final Long fromSavingsAccountId = command.longValueOfParameterNamed(fromAccountIdParamName);
        final SavingsAccount fromSavingsAccount = this.savingsAccountAssembler.assembleFrom(fromSavingsAccountId);

        final Long toLoanAccountId = command.longValueOfParameterNamed(toAccountIdParamName);
        final Loan toLoanAccount = this.loanAccountAssembler.assembleFrom(toLoanAccountId);

        return assembleSavingsToLoanTransfer(command, fromSavingsAccount, toLoanAccount);

    }

    public AccountTransferDetails assembleLoanToSavingsTransfer(final JsonCommand command) {

        final Long fromLoanAccountId = command.longValueOfParameterNamed(fromAccountIdParamName);
        final Loan fromLoanAccount = this.loanAccountAssembler.assembleFrom(fromLoanAccountId);

        final Long toSavingsAccountId = command.longValueOfParameterNamed(toAccountIdParamName);
        final SavingsAccount toSavingsAccount = this.savingsAccountAssembler.assembleFrom(toSavingsAccountId);

        return assembleLoanToSavingsTransfer(command, fromLoanAccount, toSavingsAccount);
    }

    public AccountTransferDetails assembleSavingsToSavingsTransfer(final JsonCommand command, final SavingsAccount fromSavingsAccount,
            final SavingsAccount toSavingsAccount) {

        final JsonElement element = command.parsedJson();

        final Long fromOfficeId = this.fromApiJsonHelper.extractLongNamed(fromOfficeIdParamName, element);
        final Office fromOffice = this.officeRepository.findOne(fromOfficeId);

        Client fromClient = null;
        Group fromGroup = null;

        if(this.fromApiJsonHelper.parameterExists(fromClientIdParamName,element)) {
            final Long fromClientId = this.fromApiJsonHelper.extractLongNamed(fromClientIdParamName, element);
              fromClient = this.clientRepository.findOneWithNotFoundDetection(fromClientId);

        }else if(this.fromApiJsonHelper.parameterExists(fromGroupIdParamName,element)){

            final Long fromGroupId = this.fromApiJsonHelper.extractLongNamed(fromGroupIdParamName,element);
            fromGroup = this.groupRepository.findOneWithNotFoundDetection(fromGroupId);
        }



        final Long toOfficeId = this.fromApiJsonHelper.extractLongNamed(toOfficeIdParamName, element);
        final Office toOffice = this.officeRepository.findOne(toOfficeId);

        Group toGroup = null;
        Client toClient = null;

        if(this.fromApiJsonHelper.parameterExists(toClientIdParamName,element)) {
             final Long toClientId = this.fromApiJsonHelper.extractLongNamed(toClientIdParamName, element);
            toClient = this.clientRepository.findOneWithNotFoundDetection(toClientId);

        }else if(this.fromApiJsonHelper.parameterExists(toGroupIdParamName,element)){

             Long toGroupId = this.fromApiJsonHelper.extractLongNamed(toGroupIdParamName, element);
             toGroup = this.groupRepository.findOneWithNotFoundDetection(toGroupId);
        }



        final Integer transfertype = this.fromApiJsonHelper.extractIntegerNamed(transferTypeParamName, element, Locale.getDefault());

        return AccountTransferDetails.savingsToSavingsTransfer(fromOffice, fromClient, fromSavingsAccount, toOffice, toClient,
                toSavingsAccount, transfertype,fromGroup,toGroup);

    }

    public AccountTransferDetails assembleSavingsToLoanTransfer(final JsonCommand command, final SavingsAccount fromSavingsAccount,
            final Loan toLoanAccount) {

        final JsonElement element = command.parsedJson();

        final Long fromOfficeId = this.fromApiJsonHelper.extractLongNamed(fromOfficeIdParamName, element);
        final Office fromOffice = this.officeRepository.findOne(fromOfficeId);

        Client fromClient = null;
        Group fromGroup = null;

        if(this.fromApiJsonHelper.parameterExists(fromClientIdParamName,element)) {
            final Long fromClientId = this.fromApiJsonHelper.extractLongNamed(fromClientIdParamName, element);
            fromClient = this.clientRepository.findOneWithNotFoundDetection(fromClientId);

        }else if(this.fromApiJsonHelper.parameterExists(fromGroupIdParamName,element)){

            final Long fromGroupId = this.fromApiJsonHelper.extractLongNamed(fromGroupIdParamName,element);
            fromGroup = this.groupRepository.findOneWithNotFoundDetection(fromGroupId);
        }

        final Long toOfficeId = this.fromApiJsonHelper.extractLongNamed(toOfficeIdParamName, element);
        final Office toOffice = this.officeRepository.findOne(toOfficeId);

        Group toGroup = null;
        Client toClient = null;

        if(this.fromApiJsonHelper.parameterExists(toClientIdParamName,element)) {
            final Long toClientId = this.fromApiJsonHelper.extractLongNamed(toClientIdParamName, element);
            toClient = this.clientRepository.findOneWithNotFoundDetection(toClientId);

        }else if(this.fromApiJsonHelper.parameterExists(toGroupIdParamName,element)){

            Long toGroupId = this.fromApiJsonHelper.extractLongNamed(toGroupIdParamName,element);
            toGroup = this.groupRepository.findOneWithNotFoundDetection(toGroupId);
        }

        final Integer transfertype = this.fromApiJsonHelper.extractIntegerNamed(transferTypeParamName, element, Locale.getDefault());

        return AccountTransferDetails.savingsToLoanTransfer(fromOffice, fromClient, fromSavingsAccount, toOffice, toClient, toLoanAccount,
                transfertype,toGroup,fromGroup);

    }

    public AccountTransferDetails assembleLoanToSavingsTransfer(final JsonCommand command, final Loan fromLoanAccount,
            final SavingsAccount toSavingsAccount) {

        final JsonElement element = command.parsedJson();

        final Long fromOfficeId = this.fromApiJsonHelper.extractLongNamed(fromOfficeIdParamName, element);
        final Office fromOffice = this.officeRepository.findOne(fromOfficeId);

        Client fromClient = null;
        Group fromGroup = null;

        if(this.fromApiJsonHelper.parameterExists(fromClientIdParamName,element)) {
            final Long fromClientId = this.fromApiJsonHelper.extractLongNamed(fromClientIdParamName, element);
            fromClient = this.clientRepository.findOneWithNotFoundDetection(fromClientId);

        }else if(this.fromApiJsonHelper.parameterExists(fromGroupIdParamName,element)){

            final Long fromGroupId = this.fromApiJsonHelper.extractLongNamed(toGroupIdParamName,element);
            fromGroup = this.groupRepository.findOneWithNotFoundDetection(fromGroupId);
        }

        final Long toOfficeId = this.fromApiJsonHelper.extractLongNamed(toOfficeIdParamName, element);
        final Office toOffice = this.officeRepository.findOne(toOfficeId);

        Group toGroup = null;
        Client toClient = null;

        if(this.fromApiJsonHelper.parameterExists(toClientIdParamName,element)) {
            final Long toClientId = this.fromApiJsonHelper.extractLongNamed(toClientIdParamName, element);
            toClient = this.clientRepository.findOneWithNotFoundDetection(toClientId);

        }else if(this.fromApiJsonHelper.parameterExists(toGroupIdParamName,element)){

            Long toGroupId = this.fromApiJsonHelper.extractLongNamed(toGroupIdParamName, element);
            toGroup = this.groupRepository.findOneWithNotFoundDetection(toGroupId);
        }

        final Integer transfertype = this.fromApiJsonHelper.extractIntegerNamed(transferTypeParamName, element, Locale.getDefault());

        return AccountTransferDetails.LoanTosavingsTransfer(fromOffice, fromClient, fromLoanAccount, toOffice, toClient, toSavingsAccount,
                transfertype,fromGroup,toGroup);
    }

    public AccountTransferDetails assembleLoanToLoanTransfer(final JsonCommand command, final Loan fromLoanAccount,
                                                             final Loan toLoanAccount) {

        final JsonElement element = command.parsedJson();

        final Long fromOfficeId = this.fromApiJsonHelper.extractLongNamed(fromOfficeIdParamName, element);
        final Office fromOffice = this.officeRepository.findOne(fromOfficeId);

        Client fromClient = null;
        Group fromGroup = null;

        if(this.fromApiJsonHelper.parameterExists(fromClientIdParamName,element)) {
            final Long fromClientId = this.fromApiJsonHelper.extractLongNamed(fromClientIdParamName, element);
            fromClient = this.clientRepository.findOneWithNotFoundDetection(fromClientId);

        }else if(this.fromApiJsonHelper.parameterExists(fromGroupIdParamName,element)){

            final Long fromGroupId = this.fromApiJsonHelper.extractLongNamed(fromGroupIdParamName,element);
            fromGroup = this.groupRepository.findOneWithNotFoundDetection(fromGroupId);
        }

        final Long toOfficeId = this.fromApiJsonHelper.extractLongNamed(toOfficeIdParamName, element);
        final Office toOffice = this.officeRepository.findOne(toOfficeId);

        Group toGroup = null;
        Client toClient = null;

        if(this.fromApiJsonHelper.parameterExists(toClientIdParamName,element)) {
            final Long toClientId = this.fromApiJsonHelper.extractLongNamed(toClientIdParamName, element);
            toClient = this.clientRepository.findOneWithNotFoundDetection(toClientId);

        }else if(this.fromApiJsonHelper.parameterExists(toGroupIdParamName,element)){

            Long toGroupId = this.fromApiJsonHelper.extractLongNamed(toGroupIdParamName, element);
            toGroup = this.groupRepository.findOneWithNotFoundDetection(toGroupId);
        }

        final Integer transfertype = this.fromApiJsonHelper.extractIntegerNamed(transferTypeParamName, element, Locale.getDefault());

        return AccountTransferDetails.loanToLoanTransfer(fromOffice, fromClient, fromLoanAccount, toOffice, toClient, toLoanAccount,
                transfertype,fromGroup,toGroup);
    }

    public AccountTransferDetails assembleSavingsToLoanTransfer(final SavingsAccount fromSavingsAccount, final Loan toLoanAccount,
            Integer transferType) {
        final Office fromOffice = fromSavingsAccount.office();
        final Client fromClient = fromSavingsAccount.getClient();
        final Office toOffice = toLoanAccount.getOffice();
        final Client toClient = toLoanAccount.client();

        return AccountTransferDetails.savingsToLoanTransfer(fromOffice, fromClient, fromSavingsAccount, toOffice, toClient, toLoanAccount,
                transferType);

    }

    public AccountTransferDetails assembleSavingsToSavingsTransfer(final SavingsAccount fromSavingsAccount,
            final SavingsAccount toSavingsAccount, Integer transferType) {
        final Office fromOffice = fromSavingsAccount.office();
        final Client fromClient = fromSavingsAccount.getClient();
        final Office toOffice = toSavingsAccount.office();
        final Client toClient = toSavingsAccount.getClient();

        return AccountTransferDetails.savingsToSavingsTransfer(fromOffice, fromClient, fromSavingsAccount, toOffice, toClient,
                toSavingsAccount, transferType);
    }

    public AccountTransferDetails assembleLoanToSavingsTransfer(final Loan fromLoanAccount, final SavingsAccount toSavingsAccount,
            Integer transferType) {
        final Office fromOffice = fromLoanAccount.getOffice();
        final Client fromClient = fromLoanAccount.client();
        final Office toOffice = toSavingsAccount.office();
        final Client toClient = toSavingsAccount.getClient();

        return AccountTransferDetails.LoanTosavingsTransfer(fromOffice, fromClient, fromLoanAccount, toOffice, toClient, toSavingsAccount,
                transferType);
    }

    public AccountTransferDetails assembleLoanToLoanTransfer(final Loan fromLoanAccount, final Loan toLoanAccount,
                                                             Integer transferType){
        final Office fromOffice = fromLoanAccount.getOffice();
        final Client fromClient = fromLoanAccount.client();
        final Office toOffice = toLoanAccount.getOffice();
        final Client toClient = toLoanAccount.client();

        return AccountTransferDetails.loanToLoanTransfer(fromOffice, fromClient, fromLoanAccount, toOffice, toClient, toLoanAccount,
                transferType);
    }
}