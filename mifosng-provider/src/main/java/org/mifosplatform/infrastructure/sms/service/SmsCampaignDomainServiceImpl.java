/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.sms.service;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.mifosplatform.infrastructure.codes.domain.CodeValueRepository;
import org.mifosplatform.infrastructure.dataqueries.data.GenericResultsetData;
import org.mifosplatform.infrastructure.dataqueries.exception.ReportParameterNotFoundException;
import org.mifosplatform.infrastructure.dataqueries.service.GenericDataService;
import org.mifosplatform.infrastructure.dataqueries.service.ReadReportingService;
import org.mifosplatform.infrastructure.sms.domain.*;
import org.mifosplatform.portfolio.account.service.AccountTransfersReadPlatformService;
import org.mifosplatform.portfolio.account.service.AccountTransfersWritePlatformService;
import org.mifosplatform.portfolio.client.domain.Client;
import org.mifosplatform.portfolio.client.domain.ClientRepository;
import org.mifosplatform.portfolio.common.BusinessEventNotificationConstants;
import org.mifosplatform.portfolio.common.service.BusinessEventListner;
import org.mifosplatform.portfolio.common.service.BusinessEventNotifierService;
import org.mifosplatform.portfolio.common.BusinessEventNotificationConstants.BUSINESS_EVENTS;
import org.mifosplatform.portfolio.group.domain.Group;
import org.mifosplatform.portfolio.group.domain.GroupRepository;
import org.mifosplatform.portfolio.loanaccount.domain.Loan;
import org.mifosplatform.portfolio.loanaccount.domain.LoanTransaction;
import org.mifosplatform.portfolio.loanaccount.domain.LoanTransactionRepository;
import org.mifosplatform.portfolio.loanaccount.exception.InvalidLoanTypeException;
import org.mifosplatform.portfolio.paymentdetail.domain.PaymentDetailRepository;
import org.mifosplatform.portfolio.savings.domain.DepositAccountOnHoldTransactionRepository;
import org.mifosplatform.portfolio.savings.domain.SavingsAccountDomainService;
import org.mifosplatform.portfolio.savings.domain.SavingsAccountTransactionSummaryWrapper;
import org.mifosplatform.portfolio.savings.domain.SavingsHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.AbstractPersistable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.domain.Specifications;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.IOException;
import java.math.RoundingMode;
import java.security.InvalidParameterException;
import java.sql.Time;
import java.util.*;

/**
 * Created by Stefan on 4-7-2016.
 */

@Service
public class SmsCampaignDomainServiceImpl implements SmsCampaignDomainService {

    private final SmsCampaignRepository smsCampaignRepository;
    private final SmsMessageRepository smsMessageRepository;
    private final ClientRepository clientRepository;
    private final AccountTransfersWritePlatformService accountTransfersWritePlatformService;
    private final BusinessEventNotifierService businessEventNotifierService;
    private final DepositAccountOnHoldTransactionRepository depositAccountOnHoldTransactionRepository;
    private final Map<Long, Long> releaseLoanIds = new HashMap<>(2);
    private final RoundingMode roundingMode = RoundingMode.HALF_EVEN;
    private final SavingsAccountDomainService savingsAccountDomainService;
    private final SavingsAccountTransactionSummaryWrapper savingsAccountTransactionSummaryWrapper;
    private final SavingsHelper savingsHelper;
    private final AccountTransfersReadPlatformService accountTransfersReadPlatformService;
    private final SmsCampaignWritePlatformService smsCampaignWritePlatformCommandHandler;
    private final GroupRepository groupRepository;
    private final CodeValueRepository codeValueRepository;
    private final PaymentDetailRepository paymentDetailRepository;
    private final ReadReportingService readReportingService;
    private final GenericDataService genericDataService;

    @Autowired
    public SmsCampaignDomainServiceImpl(final SmsCampaignRepository smsCampaignRepository, final SmsMessageRepository smsMessageRepository,
                                        final AccountTransfersWritePlatformService accountTransfersWritePlatformService,
                                        final BusinessEventNotifierService businessEventNotifierService,
                                        final DepositAccountOnHoldTransactionRepository depositAccountOnHoldTransactionRepository,
                                        final SavingsAccountDomainService savingsAccountDomainService, final GenericDataService genericDataService,
                                        final SavingsAccountTransactionSummaryWrapper savingsAccountTransactionSummaryWrapper,
                                        final AccountTransfersReadPlatformService accountTransfersReadPlatformService,
                                        final CodeValueRepository codeValueRepository, final ClientRepository clientRepository,
                                        final PaymentDetailRepository paymentDetailRepository, final SmsCampaignWritePlatformService smsCampaignWritePlatformCommandHandler,
                                        final GroupRepository groupRepository, final ReadReportingService readReportingService){
        this.smsCampaignRepository = smsCampaignRepository;
        this.smsMessageRepository = smsMessageRepository;
        this.accountTransfersWritePlatformService = accountTransfersWritePlatformService;
        this.businessEventNotifierService = businessEventNotifierService;
        this.depositAccountOnHoldTransactionRepository = depositAccountOnHoldTransactionRepository;
        this.savingsAccountDomainService = savingsAccountDomainService;
        this.savingsAccountTransactionSummaryWrapper = savingsAccountTransactionSummaryWrapper;
        this.accountTransfersReadPlatformService = accountTransfersReadPlatformService;
        this.savingsHelper = new SavingsHelper(this.accountTransfersReadPlatformService);
        this.codeValueRepository = codeValueRepository;
        this.paymentDetailRepository = paymentDetailRepository;
        this.clientRepository = clientRepository;
        this.smsCampaignWritePlatformCommandHandler = smsCampaignWritePlatformCommandHandler;
        this.groupRepository = groupRepository;
        this.readReportingService = readReportingService;
        this.genericDataService = genericDataService;
    }

    @PostConstruct
    public void addListners() {
        this.businessEventNotifierService.addBusinessEventPostListners(BUSINESS_EVENTS.LOAN_APPROVED, new SendSmsOnLoanApproved());
        this.businessEventNotifierService.addBusinessEventPostListners(BUSINESS_EVENTS.LOAN_REJECTED, new SendSmsOnLoanRejected());
        this.businessEventNotifierService.addBusinessEventPostListners(BUSINESS_EVENTS.LOAN_MAKE_REPAYMENT, new SendSmsOnLoanRepayment());
    }

    private void notifyRejectedLoanOwner(Loan loan) {
        ArrayList<SmsCampaign> smsCampaigns = retrieveSmsCampaigns("loan rejected");
        if(smsCampaigns.size()>0){
            for (SmsCampaign campaign:smsCampaigns){
                if(campaign.isActive()) {
                    this.smsCampaignWritePlatformCommandHandler.insertDirectCampaignIntoSmsOutboundTable(loan, campaign);
                }
            }
        }
    }

    private void notifyAcceptedLoanOwner(Loan loan) {
        ArrayList<SmsCampaign> smsCampaigns = retrieveSmsCampaigns("loan approved");

        if(smsCampaigns.size()>0){
            for (SmsCampaign campaign:smsCampaigns){
                if(campaign.isActive()) {
                    this.smsCampaignWritePlatformCommandHandler.insertDirectCampaignIntoSmsOutboundTable(loan, campaign);
                }
            }
        }
    }

    private void sendSmsForLoanRepayment(LoanTransaction loanTransaction) {
        ArrayList<SmsCampaign> smsCampaigns = retrieveSmsCampaigns("loan repayment");

        if(smsCampaigns.size()>0){
            for (SmsCampaign smsCampaign:smsCampaigns){
                if(smsCampaign.isActive()) {
                    try {
                        Loan loan = loanTransaction.getLoan();
                        final Set<Client> groupClients = new HashSet<>();
                        if(loan.hasInvalidLoanType()){
                            throw new InvalidLoanTypeException("Loan Type cannot be 0 for the Triggered Sms Campaign");
                        }
                        if(loan.isGroupLoan()){
                            Group group = this.groupRepository.findOne(loan.getGroupId());
                            groupClients.addAll(group.getClientMembers());
                        }else{
                            groupClients.add(loan.client());
                        }
                        HashMap<String, String> campaignParams = new ObjectMapper().readValue(smsCampaign.getParamValue(), new TypeReference<HashMap<String, String>>() {});

                        if(groupClients.size()>0) {
                            for(Client client : groupClients) {
                                HashMap<String, Object> smsParams = processRepaymentDataForSms(loanTransaction, client);
                                for (String key : campaignParams.keySet()) {
                                    String value = campaignParams.get(key);
                                    String spvalue = null;
                                    boolean spkeycheck = smsParams.containsKey(key);
                                    if (spkeycheck) {
                                        spvalue = smsParams.get(key).toString();
                                    }
                                    if (spkeycheck && !(value.equals("-1") || spvalue.equals(value))) {
                                        throw new RuntimeException();
                                    }
                                }
                                String message = this.smsCampaignWritePlatformCommandHandler.compileSmsTemplate(smsCampaign.getMessage(), smsCampaign.getCampaignName(), smsParams);
                                Object mobileNo = smsParams.get("mobileNo");

                                if (mobileNo != null) {
                                    SmsMessage smsMessage = SmsMessage.pendingSms(null, null, client, null, message, null, mobileNo.toString(), smsCampaign.getCampaignName());
                                    this.smsMessageRepository.save(smsMessage);
                                }
                            }
                        }
                    } catch (final IOException e) {
                        System.out.println("smsParams does not contain the following key: " + e.getMessage());
                    } catch (final RuntimeException e) {
                        System.out.println("RuntimeException: " + e.getMessage());
                    }
                }
            }
        }
    }

    private ArrayList<SmsCampaign> retrieveSmsCampaigns(String paramValue){
        List<SmsCampaign> initialSmsCampaignList = smsCampaignRepository.findByCampaignType(SmsCampaignType.TRIGGERED.getValue());
        ArrayList<SmsCampaign> smsCampaigns = new ArrayList();

        for(SmsCampaign campaign : initialSmsCampaignList){
            if(campaign.getParamValue().toLowerCase().contains(paramValue)){
                smsCampaigns.add(campaign);
            }
        }
        return smsCampaigns;
    }

    private HashMap<String, Object> processRepaymentDataForSms(final LoanTransaction loanTransaction, Client groupClient){

        HashMap<String, Object> smsParams = new HashMap<String, Object>();
        Loan loan = loanTransaction.getLoan();
        final Client client;
        if(loan.isGroupLoan() && groupClient != null){
            client = groupClient;
        }else if(loan.isIndividualLoan()){
            client = loan.getClient();
        }else{
            throw new InvalidParameterException("");
        }

        DateTimeFormatter timeFormatter = DateTimeFormat.forPattern("HH:mm");
        DateTimeFormatter dateFormatter = DateTimeFormat.forPattern("MMM:d:yyyy");

        smsParams.put("id",loanTransaction.getLoan().getClientId());
        smsParams.put("firstname",client.getFirstname());
        smsParams.put("middlename",client.getMiddlename());
        smsParams.put("lastname",client.getLastname());
        smsParams.put("FullName",client.getDisplayName());
        smsParams.put("mobileNo",client.mobileNo());
        smsParams.put("LoanAmount",loan.getPrincpal());
        smsParams.put("LoanOutstanding",loanTransaction.getOutstandingLoanBalance());
        smsParams.put("loanId",loan.getId());
        smsParams.put("LoanAccountId", loan.getAccountNumber());
        smsParams.put("${officeId}", client.getOffice().getId());
        smsParams.put("${staffId}", client.getStaff().getId());
        smsParams.put("repaymentAmount", loanTransaction.getAmount(loan.getCurrency()));
        smsParams.put("RepaymentDate", loanTransaction.getCreatedDateTime().toLocalDate().toString(dateFormatter));
        smsParams.put("RepaymentTime", loanTransaction.getCreatedDateTime().toLocalTime().toString(timeFormatter));
        smsParams.put("receiptNumber", loanTransaction.getPaymentDetail().getReceiptNumber());

        return smsParams;
    }

    private class SendSmsOnLoanApproved implements BusinessEventListner{

        @Override
        public void businessEventToBeExecuted(@SuppressWarnings("unused") Map<BusinessEventNotificationConstants.BUSINESS_ENTITY, Object> businessEventEntity) {

        }

        @Override
        public void businessEventWasExecuted(Map<BusinessEventNotificationConstants.BUSINESS_ENTITY, Object> businessEventEntity) {
            Object entity = businessEventEntity.get(BusinessEventNotificationConstants.BUSINESS_ENTITY.LOAN);
            if (entity instanceof Loan) {
                Loan loan = (Loan) entity;
                notifyAcceptedLoanOwner(loan);
            }
        }
    }

    private class SendSmsOnLoanRejected implements BusinessEventListner{

        @Override
        public void businessEventToBeExecuted(Map<BusinessEventNotificationConstants.BUSINESS_ENTITY, Object> businessEventEntity) {

        }

        @Override
        public void businessEventWasExecuted(Map<BusinessEventNotificationConstants.BUSINESS_ENTITY, Object> businessEventEntity) {
            Object entity = businessEventEntity.get(BusinessEventNotificationConstants.BUSINESS_ENTITY.LOAN);
            if (entity instanceof Loan) {
                Loan loan = (Loan) entity;
                notifyRejectedLoanOwner(loan);
            }
        }
    }

    private class SendSmsOnLoanRepayment implements BusinessEventListner{

        @Override
        public void businessEventToBeExecuted(@SuppressWarnings("unused") Map<BusinessEventNotificationConstants.BUSINESS_ENTITY, Object> businessEventEntity) {

        }

        @Override
        public void businessEventWasExecuted(Map<BusinessEventNotificationConstants.BUSINESS_ENTITY, Object> businessEventEntity) {
            Object entity = businessEventEntity.get(BusinessEventNotificationConstants.BUSINESS_ENTITY.LOAN_TRANSACTION);
            if (entity instanceof LoanTransaction) {
                LoanTransaction loanTransaction = (LoanTransaction) entity;
                sendSmsForLoanRepayment(loanTransaction);
            }
        }
    }
}
