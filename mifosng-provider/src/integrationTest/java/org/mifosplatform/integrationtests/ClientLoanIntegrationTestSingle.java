/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.integrationtests;

import com.google.gson.JsonObject;
import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.builder.ResponseSpecBuilder;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.specification.RequestSpecification;
import com.jayway.restassured.specification.ResponseSpecification;
import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mifosplatform.integrationtests.common.ClientHelper;
import org.mifosplatform.integrationtests.common.SchedulerJobHelper;
import org.mifosplatform.integrationtests.common.Utils;
import org.mifosplatform.integrationtests.common.accounting.*;
import org.mifosplatform.integrationtests.common.charges.ChargesHelper;
import org.mifosplatform.integrationtests.common.loans.LoanApplicationTestBuilder;
import org.mifosplatform.integrationtests.common.loans.LoanProductTestBuilder;
import org.mifosplatform.integrationtests.common.loans.LoanStatusChecker;
import org.mifosplatform.integrationtests.common.loans.LoanTransactionHelper;
import org.mifosplatform.integrationtests.common.savings.AccountTransferHelper;
import org.mifosplatform.integrationtests.common.savings.SavingsAccountHelper;
import org.mifosplatform.integrationtests.common.savings.SavingsProductHelper;
import org.mifosplatform.integrationtests.common.savings.SavingsStatusChecker;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * Client Loan Integration Test for checking Loan Application Repayments
 * Schedule, loan charges, penalties, loan repayments and verifying accounting
 * transactions
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class ClientLoanIntegrationTestSingle {

    public static final String MINIMUM_OPENING_BALANCE = "1000.0";
    public static final String ACCOUNT_TYPE_INDIVIDUAL = "INDIVIDUAL";

    private static final String NONE = "1";
    private static final String CASH_BASED = "2";
    private static final String ACCRUAL_PERIODIC = "3";
    private static final String ACCRUAL_UPFRONT = "4";

    private ResponseSpecification responseSpec;
    private RequestSpecification requestSpec;
    private LoanTransactionHelper loanTransactionHelper;
    private JournalEntryHelper journalEntryHelper;
    private AccountHelper accountHelper;
    private SchedulerJobHelper schedulerJobHelper;
    private PeriodicAccrualAccountingHelper periodicAccrualAccountingHelper;
    private SavingsAccountHelper savingsAccountHelper;
    private AccountTransferHelper accountTransferHelper;

    @Before
    public void setup() {
        Utils.initializeRESTAssured();
        this.requestSpec = new RequestSpecBuilder().setContentType(ContentType.JSON).build();
        this.requestSpec.header("Authorization", "Basic " + Utils.loginIntoServerAndGetBase64EncodedAuthenticationKey());
        this.responseSpec = new ResponseSpecBuilder().expectStatusCode(200).build();
        this.loanTransactionHelper = new LoanTransactionHelper(this.requestSpec, this.responseSpec);
        this.accountHelper = new AccountHelper(this.requestSpec, this.responseSpec);
    }

    private void validateCharge(Integer amountPercentage, final List<HashMap> loanCharges, final String amount, final String outstanding,
            String amountPaid, String amountWaived) {
        HashMap chargeDetail = getloanCharge(amountPercentage, loanCharges);
        Assert.assertTrue(new Float(amount).compareTo(new Float(String.valueOf(chargeDetail.get("amountOrPercentage")))) == 0);
        Assert.assertTrue(new Float(outstanding).compareTo(new Float(String.valueOf(chargeDetail.get("amountOutstanding")))) == 0);
        Assert.assertTrue(new Float(amountPaid).compareTo(new Float(String.valueOf(chargeDetail.get("amountPaid")))) == 0);
        Assert.assertTrue(new Float(amountWaived).compareTo(new Float(String.valueOf(chargeDetail.get("amountWaived")))) == 0);
    }

    private void validateChargeExcludePrecission(Integer amountPercentage, final List<HashMap> loanCharges, final String amount,
            final String outstanding, String amountPaid, String amountWaived) {
        DecimalFormat twoDForm = new DecimalFormat("#");
        HashMap chargeDetail = getloanCharge(amountPercentage, loanCharges);
        Assert.assertTrue(new Float(twoDForm.format(new Float(amount))).compareTo(new Float(twoDForm.format(new Float(String
                .valueOf(chargeDetail.get("amountOrPercentage")))))) == 0);
        Assert.assertTrue(new Float(twoDForm.format(new Float(outstanding))).compareTo(new Float(twoDForm.format(new Float(String
                .valueOf(chargeDetail.get("amountOutstanding")))))) == 0);
        Assert.assertTrue(new Float(twoDForm.format(new Float(amountPaid))).compareTo(new Float(twoDForm.format(new Float(String
                .valueOf(chargeDetail.get("amountPaid")))))) == 0);
        Assert.assertTrue(new Float(twoDForm.format(new Float(amountWaived))).compareTo(new Float(twoDForm.format(new Float(String
                .valueOf(chargeDetail.get("amountWaived")))))) == 0);
    }

    public void validateNumberForEqual(String val, String val2) {
        Assert.assertTrue(new Float(val).compareTo(new Float(val2)) == 0);
    }

    public void validateNumberForEqualWithMsg(String msg, String val, String val2) {
        Assert.assertTrue(msg + "expected " + val + " but was " + val2, new Float(val).compareTo(new Float(val2)) == 0);
    }

    public void validateNumberForEqualExcludePrecission(String val, String val2) {
        DecimalFormat twoDForm = new DecimalFormat("#");
        Assert.assertTrue(new Float(twoDForm.format(new Float(val))).compareTo(new Float(twoDForm.format(new Float(val2)))) == 0);
    }

    private Integer createLoanProduct(final boolean multiDisburseLoan, final String accountingRule, final Account... accounts) {
        System.out.println("------------------------------CREATING NEW LOAN PRODUCT ---------------------------------------");
        LoanProductTestBuilder builder = new LoanProductTestBuilder() //
                .withPrincipal("12,000.00") //
                .withNumberOfRepayments("4") //
                .withRepaymentAfterEvery("1") //
                .withRepaymentTypeAsMonth() //
                .withinterestRatePerPeriod("1") //
                .withInterestRateFrequencyTypeAsMonths() //
                .withAmortizationTypeAsEqualInstallments() //
                .withInterestTypeAsDecliningBalance() //
                .withTranches(multiDisburseLoan) //
                .withAccounting(accountingRule, accounts);
        if (multiDisburseLoan) {
            builder = builder.withInterestCalculationPeriodTypeAsRepaymentPeriod(true);
        }
        final String loanProductJSON = builder.build(null);
        return this.loanTransactionHelper.getLoanProductId(loanProductJSON);
    }

    private Integer createLoanProduct(final String inMultiplesOf, final String digitsAfterDecimal, final String repaymentStrategy) {
        System.out.println("------------------------------CREATING NEW LOAN PRODUCT ---------------------------------------");
        final String loanProductJSON = new LoanProductTestBuilder() //
                .withPrincipal("10000000.00") //
                .withNumberOfRepayments("24") //
                .withRepaymentAfterEvery("1") //
                .withRepaymentTypeAsMonth() //
                .withinterestRatePerPeriod("2") //
                .withInterestRateFrequencyTypeAsMonths() //
                .withRepaymentStrategy(repaymentStrategy) //
                .withAmortizationTypeAsEqualPrincipalPayment() //
                .withInterestTypeAsDecliningBalance() //
                .currencyDetails(digitsAfterDecimal, inMultiplesOf).build(null);
        return this.loanTransactionHelper.getLoanProductId(loanProductJSON);
    }

    private Integer createLoanProduct(final String inMultiplesOf, final String digitsAfterDecimal, final String repaymentStrategy,
            final String accountingRule, final Account... accounts) {
        System.out.println("------------------------------CREATING NEW LOAN PRODUCT ---------------------------------------");
        final String loanProductJSON = new LoanProductTestBuilder() //
                .withPrincipal("10000000.00") //
                .withNumberOfRepayments("24") //
                .withRepaymentAfterEvery("1") //
                .withRepaymentTypeAsMonth() //
                .withinterestRatePerPeriod("2") //
                .withInterestRateFrequencyTypeAsMonths() //
                .withRepaymentStrategy(repaymentStrategy) //
                .withAmortizationTypeAsEqualPrincipalPayment() //
                .withInterestTypeAsDecliningBalance() //
                .currencyDetails(digitsAfterDecimal, inMultiplesOf).withAccounting(accountingRule, accounts).build(null);
        return this.loanTransactionHelper.getLoanProductId(loanProductJSON);
    }

    private Integer applyForLoanApplication(final Integer clientID, final Integer loanProductID, String graceOnPrincipalPayment) {
        System.out.println("--------------------------------APPLYING FOR LOAN APPLICATION--------------------------------");
        final String loanApplicationJSON = new LoanApplicationTestBuilder() //
                .withPrincipal("10000000.00") //
                .withLoanTermFrequency("24") //
                .withLoanTermFrequencyAsMonths() //
                .withNumberOfRepayments("24") //
                .withRepaymentEveryAfter("1") //
                .withRepaymentFrequencyTypeAsMonths() //
                .withInterestRatePerPeriod("2") //
                .withAmortizationTypeAsEqualPrincipalPayments() //
                .withInterestTypeAsDecliningBalance() //
                .withInterestCalculationPeriodTypeSameAsRepaymentPeriod() //
                .withPrincipalGrace(graceOnPrincipalPayment).withExpectedDisbursementDate("2 June 2014") //
                .withSubmittedOnDate("2 June 2014") //
                .build(clientID.toString(), loanProductID.toString(), null);
        return this.loanTransactionHelper.getLoanId(loanApplicationJSON);
    }

    private Integer applyForLoanApplication(final Integer clientID, final Integer loanProductID, List<HashMap> charges,
            final String savingsId, String principal) {
        System.out.println("--------------------------------APPLYING FOR LOAN APPLICATION--------------------------------");
        final String loanApplicationJSON = new LoanApplicationTestBuilder() //
                .withPrincipal(principal) //
                .withLoanTermFrequency("4") //
                .withLoanTermFrequencyAsMonths() //
                .withNumberOfRepayments("4") //
                .withRepaymentEveryAfter("1") //
                .withRepaymentFrequencyTypeAsMonths() //
                .withInterestRatePerPeriod("2") //
                .withAmortizationTypeAsEqualInstallments() //
                .withInterestTypeAsDecliningBalance() //
                .withInterestCalculationPeriodTypeSameAsRepaymentPeriod() //
                .withExpectedDisbursementDate("20 September 2011") //
                .withSubmittedOnDate("20 September 2011") //
                .withCharges(charges).build(clientID.toString(), loanProductID.toString(), savingsId);
        return this.loanTransactionHelper.getLoanId(loanApplicationJSON);
    }

    private Integer applyForLoanApplicationWithTranches(final Integer clientID, final Integer loanProductID, List<HashMap> charges,
            final String savingsId, String principal, List<HashMap> tranches) {
        System.out.println("--------------------------------APPLYING FOR LOAN APPLICATION--------------------------------");
        final String loanApplicationJSON = new LoanApplicationTestBuilder() //
                .withPrincipal(principal) //
                .withLoanTermFrequency("4") //
                .withLoanTermFrequencyAsMonths() //
                .withNumberOfRepayments("4") //
                .withRepaymentEveryAfter("1") //
                .withRepaymentFrequencyTypeAsMonths() //
                .withInterestRatePerPeriod("2") //
                .withAmortizationTypeAsEqualInstallments() //
                .withInterestTypeAsDecliningBalance() //
                .withInterestCalculationPeriodTypeSameAsRepaymentPeriod() //
                .withExpectedDisbursementDate("1 March 2014") //
                .withTranches(tranches) //
                .withSubmittedOnDate("1 March 2014") //

                .withCharges(charges).build(clientID.toString(), loanProductID.toString(), savingsId);
        return this.loanTransactionHelper.getLoanId(loanApplicationJSON);
    }

    private String updateLoanJson(final Integer clientID, final Integer loanProductID, List<HashMap> charges, String savingsId) {
        System.out.println("--------------------------------APPLYING FOR LOAN APPLICATION--------------------------------");
        final String loanApplicationJSON = new LoanApplicationTestBuilder() //
                .withPrincipal("10,000.00") //
                .withLoanTermFrequency("4") //
                .withLoanTermFrequencyAsMonths() //
                .withNumberOfRepayments("4") //
                .withRepaymentEveryAfter("1") //
                .withRepaymentFrequencyTypeAsMonths() //
                .withInterestRatePerPeriod("2") //
                .withAmortizationTypeAsEqualInstallments() //
                .withInterestTypeAsDecliningBalance() //
                .withInterestCalculationPeriodTypeSameAsRepaymentPeriod() //
                .withExpectedDisbursementDate("20 September 2011") //
                .withSubmittedOnDate("20 September 2011") //
                .withCharges(charges).build(clientID.toString(), loanProductID.toString(), savingsId);
        return loanApplicationJSON;
    }

    private Integer applyForLoanApplicationWithPaymentStrategy(final Integer clientID, final Integer loanProductID, List<HashMap> charges,
            final String savingsId, String principal, final String repaymentStrategy) {
        System.out.println("--------------------------------APPLYING FOR LOAN APPLICATION--------------------------------");
        final String loanApplicationJSON = new LoanApplicationTestBuilder() //
                .withPrincipal(principal) //
                .withLoanTermFrequency("4") //
                .withLoanTermFrequencyAsMonths() //
                .withNumberOfRepayments("4") //
                .withRepaymentEveryAfter("1") //
                .withRepaymentFrequencyTypeAsMonths() //
                .withInterestRatePerPeriod("2") //
                .withAmortizationTypeAsEqualInstallments() //
                .withInterestTypeAsDecliningBalance() //
                .withInterestCalculationPeriodTypeSameAsRepaymentPeriod() //
                .withExpectedDisbursementDate("20 September 2011") //
                .withSubmittedOnDate("20 September 2011") //
                .withwithRepaymentStrategy(repaymentStrategy) //
                .withCharges(charges).build(clientID.toString(), loanProductID.toString(), savingsId);
        return this.loanTransactionHelper.getLoanId(loanApplicationJSON);
    }

    private Integer applyForLoanApplicationWithPaymentStrategyAndPastMonth(final Integer clientID, final Integer loanProductID,
            List<HashMap> charges, final String savingsId, String principal, final String repaymentStrategy, final int month) {
        System.out.println("--------------------------------APPLYING FOR LOAN APPLICATION--------------------------------");

        Calendar fourMonthsfromNowCalendar = Calendar.getInstance(Utils.getTimeZoneOfTenant());
        fourMonthsfromNowCalendar.add(Calendar.MONTH, month);
        DateFormat dateFormat = new SimpleDateFormat("dd MMMMMM yyyy");
        dateFormat.setTimeZone(Utils.getTimeZoneOfTenant());
        String fourMonthsfromNow = dateFormat.format(fourMonthsfromNowCalendar.getTime());
        final String loanApplicationJSON = new LoanApplicationTestBuilder() //
                .withPrincipal(principal) //
                .withLoanTermFrequency("6") //
                .withLoanTermFrequencyAsMonths() //
                .withNumberOfRepayments("6") //
                .withRepaymentEveryAfter("1") //
                .withRepaymentFrequencyTypeAsMonths() //
                .withInterestRatePerPeriod("2") //
                .withAmortizationTypeAsEqualInstallments() //
                .withInterestTypeAsFlatBalance() //
                .withInterestCalculationPeriodTypeSameAsRepaymentPeriod() //
                .withExpectedDisbursementDate(fourMonthsfromNow) //
                .withSubmittedOnDate(fourMonthsfromNow) //
                .withwithRepaymentStrategy(repaymentStrategy) //
                .withCharges(charges).build(clientID.toString(), loanProductID.toString(), savingsId);
        return this.loanTransactionHelper.getLoanId(loanApplicationJSON);
    }

    private void verifyLoanRepaymentSchedule(final ArrayList<HashMap> loanSchedule) {
        System.out.println("--------------------VERIFYING THE PRINCIPAL DUES,INTEREST DUE AND DUE DATE--------------------------");

        assertEquals("Checking for Due Date for 1st Month", new ArrayList<>(Arrays.asList(2011, 10, 20)), loanSchedule.get(1)
                .get("dueDate"));
        assertEquals("Checking for Principal Due for 1st Month", new Float("2911.49"), loanSchedule.get(1).get("principalOriginalDue"));
        assertEquals("Checking for Interest Due for 1st Month", new Float("240.00"), loanSchedule.get(1).get("interestOriginalDue"));

        assertEquals("Checking for Due Date for 2nd Month", new ArrayList<>(Arrays.asList(2011, 11, 20)), loanSchedule.get(2)
                .get("dueDate"));
        assertEquals("Checking for Principal Due for 2nd Month", new Float("2969.72"), loanSchedule.get(2).get("principalDue"));
        assertEquals("Checking for Interest Due for 2nd Month", new Float("181.77"), loanSchedule.get(2).get("interestOriginalDue"));

        assertEquals("Checking for Due Date for 3rd Month", new ArrayList<>(Arrays.asList(2011, 12, 20)), loanSchedule.get(3)
                .get("dueDate"));
        assertEquals("Checking for Principal Due for 3rd Month", new Float("3029.11"), loanSchedule.get(3).get("principalDue"));
        assertEquals("Checking for Interest Due for 3rd Month", new Float("122.38"), loanSchedule.get(3).get("interestOriginalDue"));

        assertEquals("Checking for Due Date for 4th Month", new ArrayList<>(Arrays.asList(2012, 1, 20)), loanSchedule.get(4).get("dueDate"));
        assertEquals("Checking for Principal Due for 4th Month", new Float("3089.68"), loanSchedule.get(4).get("principalDue"));
        assertEquals("Checking for Interest Due for 4th Month", new Float("61.79"), loanSchedule.get(4).get("interestOriginalDue"));
    }

    private void verifyLoanRepaymentScheduleForEqualPrincipal(final ArrayList<HashMap> loanSchedule) {
        System.out.println("--------------------VERIFYING THE PRINCIPAL DUES,INTEREST DUE AND DUE DATE--------------------------");

        assertEquals("Checking for Due Date for 1st Month", new ArrayList<>(Arrays.asList(2014, 7, 2)), loanSchedule.get(1).get("dueDate"));
        assertEquals("Checking for Principal Due for 1st Month", new Float("416700"), loanSchedule.get(1).get("principalOriginalDue"));
        assertEquals("Checking for Interest Due for 1st Month", new Float("200000"), loanSchedule.get(1).get("interestOriginalDue"));

        assertEquals("Checking for Due Date for 2nd Month", new ArrayList<>(Arrays.asList(2014, 8, 2)), loanSchedule.get(2).get("dueDate"));
        assertEquals("Checking for Principal Due for 2nd Month", new Float("416700"), loanSchedule.get(2).get("principalDue"));
        assertEquals("Checking for Interest Due for 2nd Month", new Float("191700"), loanSchedule.get(2).get("interestOriginalDue"));

        assertEquals("Checking for Due Date for 3rd Month", new ArrayList<>(Arrays.asList(2014, 9, 2)), loanSchedule.get(3).get("dueDate"));
        assertEquals("Checking for Principal Due for 3rd Month", new Float("416700"), loanSchedule.get(3).get("principalDue"));
        assertEquals("Checking for Interest Due for 3rd Month", new Float("183300"), loanSchedule.get(3).get("interestOriginalDue"));

        assertEquals("Checking for Due Date for 4th Month", new ArrayList<>(Arrays.asList(2014, 10, 2)), loanSchedule.get(4).get("dueDate"));
        assertEquals("Checking for Principal Due for 4th Month", new Float("416700"), loanSchedule.get(4).get("principalDue"));
        assertEquals("Checking for Interest Due for 4th Month", new Float("175000"), loanSchedule.get(4).get("interestOriginalDue"));

        assertEquals("Checking for Due Date for 5th Month", new ArrayList<>(Arrays.asList(2014, 11, 2)), loanSchedule.get(5).get("dueDate"));
        assertEquals("Checking for Principal Due for 5th Month", new Float("416700"), loanSchedule.get(5).get("principalDue"));
        assertEquals("Checking for Interest Due for 5th Month", new Float("166700"), loanSchedule.get(5).get("interestOriginalDue"));

        assertEquals("Checking for Due Date for 6th Month", new ArrayList<>(Arrays.asList(2014, 12, 2)), loanSchedule.get(6).get("dueDate"));
        assertEquals("Checking for Principal Due for 6th Month", new Float("416700"), loanSchedule.get(6).get("principalDue"));
        assertEquals("Checking for Interest Due for 6th Month", new Float("158300"), loanSchedule.get(6).get("interestOriginalDue"));

        assertEquals("Checking for Due Date for 10th Month", new ArrayList<>(Arrays.asList(2015, 4, 2)), loanSchedule.get(10)
                .get("dueDate"));
        assertEquals("Checking for Principal Due for 10th Month", new Float("416700"), loanSchedule.get(10).get("principalDue"));
        assertEquals("Checking for Interest Due for 10th Month", new Float("125000"), loanSchedule.get(10).get("interestOriginalDue"));

        assertEquals("Checking for Due Date for 20th Month", new ArrayList<>(Arrays.asList(2016, 2, 2)), loanSchedule.get(20)
                .get("dueDate"));
        assertEquals("Checking for Principal Due for 20th Month", new Float("416700"), loanSchedule.get(20).get("principalDue"));
        assertEquals("Checking for Interest Due for 20th Month", new Float("41700"), loanSchedule.get(20).get("interestOriginalDue"));

        assertEquals("Checking for Due Date for 24th Month", new ArrayList<>(Arrays.asList(2016, 6, 2)), loanSchedule.get(24)
                .get("dueDate"));
        assertEquals("Checking for Principal Due for 24th Month", new Float("415900"), loanSchedule.get(24).get("principalDue"));
        assertEquals("Checking for Interest Due for 24th Month", new Float("8300"), loanSchedule.get(24).get("interestOriginalDue"));

    }

    private void verifyLoanRepaymentScheduleForEqualPrincipalWithGrace(final ArrayList<HashMap> loanSchedule) {
        System.out.println("--------------------VERIFYING THE PRINCIPAL DUES,INTEREST DUE AND DUE DATE--------------------------");

        assertEquals("Checking for Due Date for 1st Month", new ArrayList<>(Arrays.asList(2014, 7, 2)), loanSchedule.get(1).get("dueDate"));
        validateNumberForEqualWithMsg("Checking for Principal Due for 1st Month", "0.0",
                String.valueOf(loanSchedule.get(1).get("principalOriginalDue")));
        assertEquals("Checking for Interest Due for 1st Month", new Float("200000"), loanSchedule.get(1).get("interestOriginalDue"));

        assertEquals("Checking for Due Date for 2nd Month", new ArrayList<>(Arrays.asList(2014, 8, 2)), loanSchedule.get(2).get("dueDate"));
        validateNumberForEqualWithMsg("Checking for Principal Due for 2nd Month", "0.0",
                String.valueOf(loanSchedule.get(2).get("principalOriginalDue")));
        assertEquals("Checking for Interest Due for 2nd Month", new Float("200000"), loanSchedule.get(2).get("interestOriginalDue"));

        assertEquals("Checking for Due Date for 3rd Month", new ArrayList<>(Arrays.asList(2014, 9, 2)), loanSchedule.get(3).get("dueDate"));
        validateNumberForEqualWithMsg("Checking for Principal Due for 3rd Month", "0.0",
                String.valueOf(loanSchedule.get(3).get("principalDue")));
        assertEquals("Checking for Interest Due for 3rd Month", new Float("200000"), loanSchedule.get(3).get("interestOriginalDue"));

        assertEquals("Checking for Due Date for 4th Month", new ArrayList<>(Arrays.asList(2014, 10, 2)), loanSchedule.get(4).get("dueDate"));
        validateNumberForEqualWithMsg("Checking for Principal Due for 4th Month", "0",
                String.valueOf(loanSchedule.get(4).get("principalDue")));
        assertEquals("Checking for Interest Due for 4th Month", new Float("200000"), loanSchedule.get(4).get("interestOriginalDue"));

        assertEquals("Checking for Due Date for 5th Month", new ArrayList<>(Arrays.asList(2014, 11, 2)), loanSchedule.get(5).get("dueDate"));
        validateNumberForEqualWithMsg("Checking for Principal Due for 5th Month", "0",
                String.valueOf(loanSchedule.get(5).get("principalDue")));
        assertEquals("Checking for Interest Due for 5th Month", new Float("200000"), loanSchedule.get(5).get("interestOriginalDue"));

        assertEquals("Checking for Due Date for 6th Month", new ArrayList<>(Arrays.asList(2014, 12, 2)), loanSchedule.get(6).get("dueDate"));
        assertEquals("Checking for Principal Due for 6th Month", new Float("526300"), loanSchedule.get(6).get("principalDue"));
        assertEquals("Checking for Interest Due for 6th Month", new Float("200000"), loanSchedule.get(6).get("interestOriginalDue"));

        assertEquals("Checking for Due Date for 7th Month", new ArrayList<>(Arrays.asList(2015, 1, 2)), loanSchedule.get(7).get("dueDate"));
        assertEquals("Checking for Principal Due for 7th Month", new Float("526300"), loanSchedule.get(7).get("principalDue"));
        assertEquals("Checking for Interest Due for 7th Month", new Float("189500"), loanSchedule.get(7).get("interestOriginalDue"));

        assertEquals("Checking for Due Date for 10th Month", new ArrayList<>(Arrays.asList(2015, 4, 2)), loanSchedule.get(10)
                .get("dueDate"));
        assertEquals("Checking for Principal Due for 10th Month", new Float("526300"), loanSchedule.get(10).get("principalDue"));
        assertEquals("Checking for Interest Due for 10th Month", new Float("157900"), loanSchedule.get(10).get("interestOriginalDue"));

        assertEquals("Checking for Due Date for 20th Month", new ArrayList<>(Arrays.asList(2016, 2, 2)), loanSchedule.get(20)
                .get("dueDate"));
        assertEquals("Checking for Principal Due for 20th Month", new Float("526300"), loanSchedule.get(20).get("principalDue"));
        assertEquals("Checking for Interest Due for 20th Month", new Float("52600"), loanSchedule.get(20).get("interestOriginalDue"));

        assertEquals("Checking for Due Date for 24th Month", new ArrayList<>(Arrays.asList(2016, 6, 2)), loanSchedule.get(24)
                .get("dueDate"));
        assertEquals("Checking for Principal Due for 24th Month", new Float("526600"), loanSchedule.get(24).get("principalDue"));
        assertEquals("Checking for Interest Due for 24th Month", new Float("10500"), loanSchedule.get(24).get("interestOriginalDue"));

    }

    private void addCharges(List<HashMap> charges, Integer chargeId, String amount, String duedate) {
        charges.add(charges(chargeId, amount, duedate));
    }

    private HashMap charges(Integer chargeId, String amount, String duedate) {
        HashMap charge = new HashMap(2);
        charge.put("chargeId", chargeId.toString());
        charge.put("amount", amount);
        if (duedate != null) {
            charge.put("dueDate", duedate);
        }
        return charge;
    }

    private HashMap getloanCharge(Integer chargeId, List<HashMap> charges) {
        HashMap charge = null;
        for (HashMap loancharge : charges) {
            if (loancharge.get("chargeId").equals(chargeId)) {
                charge = loancharge;
            }
        }
        return charge;
    }

    private List<HashMap> copyChargesForUpdate(List<HashMap> charges, Integer deleteWithChargeId, String amount) {
        List<HashMap> loanCharges = new ArrayList<>();
        for (HashMap charge : charges) {
            if (!charge.get("chargeId").equals(deleteWithChargeId)) {
                loanCharges.add(copyForUpdate(charge, amount));
            }
        }
        return loanCharges;
    }

    private HashMap copyForUpdate(HashMap charge, String amount) {
        HashMap map = new HashMap();
        map.put("id", charge.get("id"));
        if (amount == null) {
            map.put("amount", charge.get("amountOrPercentage"));
        } else {
            map.put("amount", amount);
        }
        if (charge.get("dueDate") != null) {
            map.put("dueDate", charge.get("dueDate"));
        }
        map.put("chargeId", charge.get("chargeId"));
        return map;
    }

    private HashMap createTrancheDetail(final String date, final String amount) {
        HashMap detail = new HashMap();
        detail.put("expectedDisbursementDate", date);
        detail.put("principal", amount);

        return detail;
    }

    @Test
    public void testLoanScheduleWithInterestRecalculation_WITH_PERIODIC_ACCOUNTING() {
        this.loanTransactionHelper = new LoanTransactionHelper(this.requestSpec, this.responseSpec);
        this.periodicAccrualAccountingHelper = new PeriodicAccrualAccountingHelper(this.requestSpec, this.responseSpec);
        this.journalEntryHelper = new JournalEntryHelper(this.requestSpec, this.responseSpec);

        DateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy");
        dateFormat.setTimeZone(Utils.getTimeZoneOfTenant());

        final Account assetAccount = this.accountHelper.createAssetAccount();
        final Account incomeAccount = this.accountHelper.createIncomeAccount();
        final Account expenseAccount = this.accountHelper.createExpenseAccount();
        final Account overpaymentAccount = this.accountHelper.createLiabilityAccount();

        Calendar todaysDate = Calendar.getInstance(Utils.getTimeZoneOfTenant());
        System.out.println("Disbursal Date Calendar " + todaysDate.getTime());
        todaysDate.add(Calendar.DAY_OF_MONTH, -14);
        final String LOAN_DISBURSEMENT_DATE = dateFormat.format(todaysDate.getTime());

        final Integer clientID = ClientHelper.createClient(this.requestSpec, this.responseSpec);
        ClientHelper.verifyClientCreatedOnServer(this.requestSpec, this.responseSpec, clientID);
        Account[] accounts = { assetAccount, incomeAccount, expenseAccount, overpaymentAccount };
        final Integer loanProductID = createLoanProductWithInterestRecalculation(LoanProductTestBuilder.MIFOS_STANDARD_STRATEGY,
                LoanProductTestBuilder.RECALCULATION_COMPOUNDING_METHOD_NONE,
                LoanProductTestBuilder.RECALCULATION_STRATEGY_REDUCE_EMI_AMOUN,
                LoanProductTestBuilder.RECALCULATION_FREQUENCY_TYPE_SAME_AS_REPAYMENT_PERIOD, "0", null,
                LoanProductTestBuilder.INTEREST_APPLICABLE_STRATEGY_ON_PRE_CLOSE_DATE, accounts);

        final Integer loanID = applyForLoanApplicationForInterestRecalculation(clientID, loanProductID, LOAN_DISBURSEMENT_DATE, null,
                LoanApplicationTestBuilder.MIFOS_STANDARD_STRATEGY, new ArrayList<HashMap>(0));

        Assert.assertNotNull(loanID);
        HashMap loanStatusHashMap = LoanStatusChecker.getStatusOfLoan(this.requestSpec, this.responseSpec, loanID);
        LoanStatusChecker.verifyLoanIsPending(loanStatusHashMap);

        ArrayList<HashMap> loanSchedule = this.loanTransactionHelper.getLoanRepaymentSchedule(this.requestSpec, this.responseSpec, loanID);
        List<Map<String, Object>> expectedvalues = new ArrayList<>();
        todaysDate = Calendar.getInstance(Utils.getTimeZoneOfTenant());
        System.out.println("Date during repayment schedule" + todaysDate.getTime());
        addRepaymentValues(expectedvalues, todaysDate, -1, false, "2482.76", "46.15", "0.0", "0.0");
        addRepaymentValues(expectedvalues, todaysDate, 1, false, "2494.22", "34.69", "0.0", "0.0");
        addRepaymentValues(expectedvalues, todaysDate, 1, false, "2505.73", "23.18", "0.0", "0.0");
        addRepaymentValues(expectedvalues, todaysDate, 1, false, "2517.29", "11.62", "0.0", "0.0");
        verifyLoanRepaymentSchedule(loanSchedule, expectedvalues);

        System.out.println("-----------------------------------APPROVE LOAN-----------------------------------------");
        loanStatusHashMap = this.loanTransactionHelper.approveLoan(LOAN_DISBURSEMENT_DATE, loanID);
        LoanStatusChecker.verifyLoanIsApproved(loanStatusHashMap);
        LoanStatusChecker.verifyLoanIsWaitingForDisbursal(loanStatusHashMap);

        System.out.println("-------------------------------DISBURSE LOAN-------------------------------------------");
        loanStatusHashMap = this.loanTransactionHelper.disburseLoan(LOAN_DISBURSEMENT_DATE, loanID);
        LoanStatusChecker.verifyLoanIsActive(loanStatusHashMap);

        loanSchedule = this.loanTransactionHelper.getLoanRepaymentSchedule(this.requestSpec, this.responseSpec, loanID);
        expectedvalues = new ArrayList<>();
        todaysDate = Calendar.getInstance(Utils.getTimeZoneOfTenant());
        addRepaymentValues(expectedvalues, todaysDate, -1, false, "2482.76", "46.15", "0.0", "0.0");
        addRepaymentValues(expectedvalues, todaysDate, 1, false, "2482.76", "46.15", "0.0", "0.0");
        addRepaymentValues(expectedvalues, todaysDate, 1, false, "2505.67", "23.24", "0.0", "0.0");
        addRepaymentValues(expectedvalues, todaysDate, 1, false, "2528.81", "11.67", "0.0", "0.0");

        verifyLoanRepaymentSchedule(loanSchedule, expectedvalues);

        final JournalEntry[] assetAccountInitialEntry = { new JournalEntry(10000.0f, JournalEntry.TransactionType.CREDIT),
                new JournalEntry(10000.0f, JournalEntry.TransactionType.DEBIT), };
        this.journalEntryHelper.checkJournalEntryForAssetAccount(assetAccount, LOAN_DISBURSEMENT_DATE, assetAccountInitialEntry);
        todaysDate = Calendar.getInstance(Utils.getTimeZoneOfTenant());
        String runOndate = dateFormat.format(todaysDate.getTime());
        this.periodicAccrualAccountingHelper.runPeriodicAccrualAccounting(runOndate);
        this.loanTransactionHelper.checkAccrualTransactionForRepayment(Utils.getLocalDateOfTenant().minusDays(7), 46.15f, 0f, 0f, loanID);
        this.loanTransactionHelper.checkAccrualTransactionForRepayment(Utils.getLocalDateOfTenant(), 46.15f, 0f, 0f, loanID);

        todaysDate = Calendar.getInstance(Utils.getTimeZoneOfTenant());
        todaysDate.add(Calendar.DAY_OF_MONTH, -7);
        final String LOAN_FIRST_REPAYMENT_DATE = dateFormat.format(todaysDate.getTime());
        Float totalDueForCurrentPeriod = (Float) loanSchedule.get(1).get("totalDueForPeriod");
        this.loanTransactionHelper.makeRepayment(LOAN_FIRST_REPAYMENT_DATE, totalDueForCurrentPeriod, loanID);

        loanSchedule = this.loanTransactionHelper.getLoanRepaymentSchedule(this.requestSpec, this.responseSpec, loanID);
        expectedvalues = new ArrayList<>();
        todaysDate = Calendar.getInstance(Utils.getTimeZoneOfTenant());
        addRepaymentValues(expectedvalues, todaysDate, -1, false, "2482.76", "46.15", "0.0", "0.0");
        addRepaymentValues(expectedvalues, todaysDate, 1, false, "2494.22", "34.69", "0.0", "0.0");
        addRepaymentValues(expectedvalues, todaysDate, 1, false, "2505.73", "23.18", "0.0", "0.0");
        addRepaymentValues(expectedvalues, todaysDate, 1, false, "2517.29", "11.62", "0.0", "0.0");
        verifyLoanRepaymentSchedule(loanSchedule, expectedvalues);

        this.loanTransactionHelper.checkAccrualTransactionForRepayment(Utils.getLocalDateOfTenant().minusDays(7), 46.15f, 0f, 0f, loanID);
        this.loanTransactionHelper.checkAccrualTransactionForRepayment(Utils.getLocalDateOfTenant(), 34.69f, 0f, 0f, loanID);

        HashMap prepayDetail = this.loanTransactionHelper.getPrepayAmount(this.requestSpec, this.responseSpec, loanID);
        String prepayAmount = String.valueOf(prepayDetail.get("amount"));
        todaysDate = Calendar.getInstance(Utils.getTimeZoneOfTenant());
        String LOAN_REPAYMENT_DATE = dateFormat.format(todaysDate.getTime());
        this.loanTransactionHelper.makeRepayment(LOAN_REPAYMENT_DATE, new Float(prepayAmount), loanID);
        loanStatusHashMap = LoanStatusChecker.getStatusOfLoan(this.requestSpec, this.responseSpec, loanID);
        LoanStatusChecker.verifyLoanAccountIsClosed(loanStatusHashMap);

        this.loanTransactionHelper.checkAccrualTransactionForRepayment(Utils.getLocalDateOfTenant().minusDays(7), 46.15f, 0f, 0f, loanID);
        this.loanTransactionHelper.checkAccrualTransactionForRepayment(Utils.getLocalDateOfTenant(), 34.69f, 0f, 0f, loanID);

    }

    private void checkAccrualTransactions(final ArrayList<HashMap> loanSchedule, final Integer loanID) {

        for (int i = 1; i < loanSchedule.size(); i++) {

            final HashMap repayment = loanSchedule.get(i);

            final ArrayList<Integer> dueDateAsArray = (ArrayList<Integer>) repayment.get("dueDate");
            final LocalDate transactionDate = new LocalDate(dueDateAsArray.get(0), dueDateAsArray.get(1), dueDateAsArray.get(2));

            final Float interestPortion = BigDecimal.valueOf(Double.valueOf(repayment.get("interestDue").toString()))
                    .subtract(BigDecimal.valueOf(Double.valueOf(repayment.get("interestWaived").toString())))
                    .subtract(BigDecimal.valueOf(Double.valueOf(repayment.get("interestWrittenOff").toString()))).floatValue();

            final Float feePortion = BigDecimal.valueOf(Double.valueOf(repayment.get("feeChargesDue").toString()))
                    .subtract(BigDecimal.valueOf(Double.valueOf(repayment.get("feeChargesWaived").toString())))
                    .subtract(BigDecimal.valueOf(Double.valueOf(repayment.get("feeChargesWrittenOff").toString()))).floatValue();

            final Float penaltyPortion = BigDecimal.valueOf(Double.valueOf(repayment.get("penaltyChargesDue").toString()))
                    .subtract(BigDecimal.valueOf(Double.valueOf(repayment.get("penaltyChargesWaived").toString())))
                    .subtract(BigDecimal.valueOf(Double.valueOf(repayment.get("penaltyChargesWrittenOff").toString()))).floatValue();

            this.loanTransactionHelper.checkAccrualTransactionForRepayment(transactionDate, interestPortion, feePortion, penaltyPortion,
                    loanID);
        }
    }


    private void testLoanScheduleWithInterestRecalculation_WITH_REST_SAME_AS_REPAYMENT_INTEREST_COMPOUND_NONE_STRATEGY_REDUCE_EMI_PRE_CLOSE_INTEREST(
            String preCloseInterestStrategy, String preCloseAmount) {
        this.loanTransactionHelper = new LoanTransactionHelper(this.requestSpec, this.responseSpec);
        DateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy");
        dateFormat.setTimeZone(Utils.getTimeZoneOfTenant());

        Calendar todaysDate = Calendar.getInstance(Utils.getTimeZoneOfTenant());
        todaysDate.add(Calendar.DAY_OF_MONTH, -16);
        final String LOAN_DISBURSEMENT_DATE = dateFormat.format(todaysDate.getTime());

        final Integer clientID = ClientHelper.createClient(this.requestSpec, this.responseSpec);
        ClientHelper.verifyClientCreatedOnServer(this.requestSpec, this.responseSpec, clientID);
        final Integer loanProductID = createLoanProductWithInterestRecalculation(LoanProductTestBuilder.MIFOS_STANDARD_STRATEGY,
                LoanProductTestBuilder.RECALCULATION_COMPOUNDING_METHOD_NONE,
                LoanProductTestBuilder.RECALCULATION_STRATEGY_REDUCE_EMI_AMOUN,
                LoanProductTestBuilder.RECALCULATION_FREQUENCY_TYPE_SAME_AS_REPAYMENT_PERIOD, "0", null, preCloseInterestStrategy, null);

        final Integer loanID = applyForLoanApplicationForInterestRecalculation(clientID, loanProductID, LOAN_DISBURSEMENT_DATE, null,
                LoanApplicationTestBuilder.MIFOS_STANDARD_STRATEGY, new ArrayList<HashMap>(0));

        Assert.assertNotNull(loanID);
        HashMap loanStatusHashMap = LoanStatusChecker.getStatusOfLoan(this.requestSpec, this.responseSpec, loanID);
        LoanStatusChecker.verifyLoanIsPending(loanStatusHashMap);

        ArrayList<HashMap> loanSchedule = this.loanTransactionHelper.getLoanRepaymentSchedule(this.requestSpec, this.responseSpec, loanID);
        List<Map<String, Object>> expectedvalues = new ArrayList<>();
        todaysDate = Calendar.getInstance(Utils.getTimeZoneOfTenant());
        addRepaymentValues(expectedvalues, todaysDate, -9, true, "2482.76", "46.15", "0.0", "0.0");
        addRepaymentValues(expectedvalues, todaysDate, 1, false, "2494.22", "34.69", "0.0", "0.0");
        addRepaymentValues(expectedvalues, todaysDate, 1, false, "2505.73", "23.18", "0.0", "0.0");
        addRepaymentValues(expectedvalues, todaysDate, 1, false, "2517.29", "11.62", "0.0", "0.0");
        verifyLoanRepaymentSchedule(loanSchedule, expectedvalues);

        System.out.println("-----------------------------------APPROVE LOAN-----------------------------------------");
        loanStatusHashMap = this.loanTransactionHelper.approveLoan(LOAN_DISBURSEMENT_DATE, loanID);
        LoanStatusChecker.verifyLoanIsApproved(loanStatusHashMap);
        LoanStatusChecker.verifyLoanIsWaitingForDisbursal(loanStatusHashMap);

        System.out.println("-------------------------------DISBURSE LOAN-------------------------------------------");
        loanStatusHashMap = this.loanTransactionHelper.disburseLoan(LOAN_DISBURSEMENT_DATE, loanID);
        LoanStatusChecker.verifyLoanIsActive(loanStatusHashMap);

        loanSchedule = this.loanTransactionHelper.getLoanRepaymentSchedule(this.requestSpec, this.responseSpec, loanID);
        expectedvalues = new ArrayList<>();
        todaysDate = Calendar.getInstance(Utils.getTimeZoneOfTenant());
        addRepaymentValues(expectedvalues, todaysDate, -9, true, "2482.76", "46.15", "0.0", "0.0");
        addRepaymentValues(expectedvalues, todaysDate, 1, false, "2482.76", "46.15", "0.0", "0.0");
        addRepaymentValues(expectedvalues, todaysDate, 1, false, "2482.76", "46.15", "0.0", "0.0");
        addRepaymentValues(expectedvalues, todaysDate, 1, false, "2551.72", "11.78", "0.0", "0.0");

        verifyLoanRepaymentSchedule(loanSchedule, expectedvalues);

        todaysDate = Calendar.getInstance(Utils.getTimeZoneOfTenant());
        todaysDate.add(Calendar.DAY_OF_MONTH, -9);
        final String LOAN_FIRST_REPAYMENT_DATE = dateFormat.format(todaysDate.getTime());
        Float totalDueForCurrentPeriod = (Float) loanSchedule.get(1).get("totalDueForPeriod");
        this.loanTransactionHelper.makeRepayment(LOAN_FIRST_REPAYMENT_DATE, totalDueForCurrentPeriod, loanID);

        loanSchedule = this.loanTransactionHelper.getLoanRepaymentSchedule(this.requestSpec, this.responseSpec, loanID);
        expectedvalues = new ArrayList<>();
        todaysDate = Calendar.getInstance(Utils.getTimeZoneOfTenant());
        addRepaymentValues(expectedvalues, todaysDate, -9, true, "2482.76", "46.15", "0.0", "0.0");
        addRepaymentValues(expectedvalues, todaysDate, 1, false, "2494.22", "34.69", "0.0", "0.0");
        addRepaymentValues(expectedvalues, todaysDate, 1, false, "2494.22", "34.69", "0.0", "0.0");
        addRepaymentValues(expectedvalues, todaysDate, 1, false, "2528.8", "11.67", "0.0", "0.0");
        verifyLoanRepaymentSchedule(loanSchedule, expectedvalues);

        HashMap prepayDetail = this.loanTransactionHelper.getPrepayAmount(this.requestSpec, this.responseSpec, loanID);
        String prepayAmount = String.valueOf(prepayDetail.get("amount"));
        validateNumberForEqualWithMsg("verify pre-close amount", preCloseAmount, prepayAmount);
        todaysDate = Calendar.getInstance(Utils.getTimeZoneOfTenant());
        String LOAN_REPAYMENT_DATE = dateFormat.format(todaysDate.getTime());
        this.loanTransactionHelper.makeRepayment(LOAN_REPAYMENT_DATE, new Float(prepayAmount), loanID);
        loanStatusHashMap = LoanStatusChecker.getStatusOfLoan(this.requestSpec, this.responseSpec, loanID);
        LoanStatusChecker.verifyLoanAccountIsClosed(loanStatusHashMap);
    }


    private void testLoanScheduleWithInterestRecalculation_WITH_REST_WEEKLY_INTEREST_COMPOUND_INTEREST_FEE_STRATEGY_REDUCE_NEXT_INSTALLMENTS_PRE_CLOSE_INTEREST(
            String preCloseInterestStrategy, String preCloseAmount) {
        this.loanTransactionHelper = new LoanTransactionHelper(this.requestSpec, this.responseSpec);

        DateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy");
        dateFormat.setTimeZone(Utils.getTimeZoneOfTenant());

        Calendar todaysDate = Calendar.getInstance(Utils.getTimeZoneOfTenant());
        todaysDate.add(Calendar.DAY_OF_MONTH, -16);
        final String LOAN_DISBURSEMENT_DATE = dateFormat.format(todaysDate.getTime());
        todaysDate.add(Calendar.DAY_OF_MONTH, -4);
        final String REST_START_DATE = dateFormat.format(todaysDate.getTime());

        todaysDate = Calendar.getInstance(Utils.getTimeZoneOfTenant());
        todaysDate.add(Calendar.DAY_OF_MONTH, -16);
        todaysDate.add(Calendar.DAY_OF_MONTH, 2);
        final String LOAN_FLAT_CHARGE_DATE = dateFormat.format(todaysDate.getTime());
        todaysDate.add(Calendar.DAY_OF_MONTH, 14);
        final String LOAN_INTEREST_CHARGE_DATE = dateFormat.format(todaysDate.getTime());
        List<HashMap> charges = new ArrayList<>(2);
        Integer flat = ChargesHelper.createCharges(requestSpec, responseSpec,
                ChargesHelper.getLoanSpecifiedDueDateJSON(ChargesHelper.CHARGE_CALCULATION_TYPE_FLAT, "100", false));
        Integer principalPercentage = ChargesHelper.createCharges(requestSpec, responseSpec,
                ChargesHelper.getLoanSpecifiedDueDateJSON(ChargesHelper.CHARGE_CALCULATION_TYPE_PERCENTAGE_AMOUNT, "2", false));

        addCharges(charges, flat, "100", LOAN_FLAT_CHARGE_DATE);
        addCharges(charges, principalPercentage, "2", LOAN_INTEREST_CHARGE_DATE);

        final Integer clientID = ClientHelper.createClient(this.requestSpec, this.responseSpec);
        ClientHelper.verifyClientCreatedOnServer(this.requestSpec, this.responseSpec, clientID);
        final Integer loanProductID = createLoanProductWithInterestRecalculationAndCompoundingDetails(
                LoanProductTestBuilder.MIFOS_STANDARD_STRATEGY, LoanProductTestBuilder.RECALCULATION_COMPOUNDING_METHOD_INTEREST_AND_FEE,
                LoanProductTestBuilder.RECALCULATION_STRATEGY_RESCHEDULE_NEXT_REPAYMENTS,
                LoanProductTestBuilder.RECALCULATION_FREQUENCY_TYPE_WEEKLY, "1", REST_START_DATE,
                LoanProductTestBuilder.RECALCULATION_FREQUENCY_TYPE_SAME_AS_REPAYMENT_PERIOD, null, null, preCloseInterestStrategy, null);

        final Integer loanID = applyForLoanApplicationForInterestRecalculation(clientID, loanProductID, LOAN_DISBURSEMENT_DATE,
                REST_START_DATE, LoanApplicationTestBuilder.MIFOS_STANDARD_STRATEGY, charges);

        Assert.assertNotNull(loanID);
        HashMap loanStatusHashMap = LoanStatusChecker.getStatusOfLoan(this.requestSpec, this.responseSpec, loanID);
        LoanStatusChecker.verifyLoanIsPending(loanStatusHashMap);

        ArrayList<HashMap> loanSchedule = this.loanTransactionHelper.getLoanRepaymentSchedule(this.requestSpec, this.responseSpec, loanID);
        List<Map<String, Object>> expectedvalues = new ArrayList<>();
        todaysDate = Calendar.getInstance(Utils.getTimeZoneOfTenant());
        addRepaymentValues(expectedvalues, todaysDate, -9, true, "2482.76", "46.15", "100.0", "0.0");
        addRepaymentValues(expectedvalues, todaysDate, 1, false, "2489.3", "39.61", "0.0", "0.0");
        addRepaymentValues(expectedvalues, todaysDate, 1, false, "2500.78", "28.13", "200", "0.0");
        addRepaymentValues(expectedvalues, todaysDate, 1, false, "2527.16", "16.62", "0.0", "0.0");
        verifyLoanRepaymentSchedule(loanSchedule, expectedvalues);

        System.out.println("-----------------------------------APPROVE LOAN-----------------------------------------");
        loanStatusHashMap = this.loanTransactionHelper.approveLoan(LOAN_DISBURSEMENT_DATE, loanID);
        LoanStatusChecker.verifyLoanIsApproved(loanStatusHashMap);
        LoanStatusChecker.verifyLoanIsWaitingForDisbursal(loanStatusHashMap);

        System.out.println("-------------------------------DISBURSE LOAN-------------------------------------------");
        loanStatusHashMap = this.loanTransactionHelper.disburseLoan(LOAN_DISBURSEMENT_DATE, loanID);
        LoanStatusChecker.verifyLoanIsActive(loanStatusHashMap);

        loanSchedule = this.loanTransactionHelper.getLoanRepaymentSchedule(this.requestSpec, this.responseSpec, loanID);
        expectedvalues = new ArrayList<>();
        todaysDate = Calendar.getInstance(Utils.getTimeZoneOfTenant());
        addRepaymentValues(expectedvalues, todaysDate, -9, true, "2482.76", "46.15", "100.0", "0.0");
        addRepaymentValues(expectedvalues, todaysDate, 1, false, "2482.08", "46.83", "0.0", "0.0");
        addRepaymentValues(expectedvalues, todaysDate, 1, false, "2495.47", "33.44", "200", "0.0");
        addRepaymentValues(expectedvalues, todaysDate, 1, false, "2539.69", "16.66", "0.0", "0.0");

        verifyLoanRepaymentSchedule(loanSchedule, expectedvalues);

        Calendar repaymentDate = Calendar.getInstance(Utils.getTimeZoneOfTenant());
        repaymentDate.add(Calendar.DAY_OF_MONTH, -9);
        final String LOAN_FIRST_REPAYMENT_DATE = dateFormat.format(repaymentDate.getTime());
        Float totalDueForCurrentPeriod = (Float) loanSchedule.get(1).get("totalDueForPeriod");
        this.loanTransactionHelper.makeRepayment(LOAN_FIRST_REPAYMENT_DATE, totalDueForCurrentPeriod, loanID);

        loanSchedule = this.loanTransactionHelper.getLoanRepaymentSchedule(this.requestSpec, this.responseSpec, loanID);
        expectedvalues = new ArrayList<>();
        todaysDate = Calendar.getInstance(Utils.getTimeZoneOfTenant());
        addRepaymentValues(expectedvalues, todaysDate, -9, true, "2482.76", "46.15", "100.0", "0.0");
        addRepaymentValues(expectedvalues, todaysDate, 1, false, "2489.3", "39.61", "0.0", "0.0");
        addRepaymentValues(expectedvalues, todaysDate, 1, false, "2500.7", "28.21", "200", "0.0");
        addRepaymentValues(expectedvalues, todaysDate, 1, false, "2527.24", "16.62", "0.0", "0.0");
        verifyLoanRepaymentSchedule(loanSchedule, expectedvalues);

        HashMap prepayDetail = this.loanTransactionHelper.getPrepayAmount(this.requestSpec, this.responseSpec, loanID);
        String prepayAmount = String.valueOf(prepayDetail.get("amount"));
        validateNumberForEqualWithMsg("verify pre-close amount", preCloseAmount, prepayAmount);
        todaysDate = Calendar.getInstance(Utils.getTimeZoneOfTenant());
        String LOAN_REPAYMENT_DATE = dateFormat.format(todaysDate.getTime());
        this.loanTransactionHelper.makeRepayment(LOAN_REPAYMENT_DATE, new Float(prepayAmount), loanID);
        loanStatusHashMap = LoanStatusChecker.getStatusOfLoan(this.requestSpec, this.responseSpec, loanID);
        LoanStatusChecker.verifyLoanAccountIsClosed(loanStatusHashMap);
    }


    private void testLoanScheduleWithInterestRecalculation_FOR_PRE_CLOSE_WITH_MORATORIUM(final String preCloseStrategy,
            final String preCloseAmount) {
        this.loanTransactionHelper = new LoanTransactionHelper(this.requestSpec, this.responseSpec);

        DateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy");
        dateFormat.setTimeZone(Utils.getTimeZoneOfTenant());

        Calendar todaysDate = Calendar.getInstance(Utils.getTimeZoneOfTenant());
        todaysDate.add(Calendar.DAY_OF_MONTH, -1);
        final String LOAN_DISBURSEMENT_DATE = dateFormat.format(todaysDate.getTime());

        final Integer clientID = ClientHelper.createClient(this.requestSpec, this.responseSpec);
        ClientHelper.verifyClientCreatedOnServer(this.requestSpec, this.responseSpec, clientID);
        final Integer loanProductID = createLoanProductWithInterestRecalculation(LoanProductTestBuilder.MIFOS_STANDARD_STRATEGY,
                LoanProductTestBuilder.RECALCULATION_COMPOUNDING_METHOD_NONE,
                LoanProductTestBuilder.RECALCULATION_STRATEGY_REDUCE_EMI_AMOUN,
                LoanProductTestBuilder.RECALCULATION_FREQUENCY_TYPE_SAME_AS_REPAYMENT_PERIOD, "0", null, preCloseStrategy, null);

        final Integer loanID = applyForLoanApplicationForInterestRecalculationWithMoratorium(clientID, loanProductID,
                LOAN_DISBURSEMENT_DATE, null, LoanApplicationTestBuilder.MIFOS_STANDARD_STRATEGY, new ArrayList<HashMap>(0), "1", null);

        Assert.assertNotNull(loanID);
        HashMap loanStatusHashMap = LoanStatusChecker.getStatusOfLoan(this.requestSpec, this.responseSpec, loanID);
        LoanStatusChecker.verifyLoanIsPending(loanStatusHashMap);

        ArrayList<HashMap> loanSchedule = this.loanTransactionHelper.getLoanRepaymentSchedule(this.requestSpec, this.responseSpec, loanID);
        List<Map<String, Object>> expectedvalues = new ArrayList<>();
        todaysDate = Calendar.getInstance(Utils.getTimeZoneOfTenant());
        todaysDate.add(Calendar.DAY_OF_MONTH, -1);
        addRepaymentValues(expectedvalues, todaysDate, 1, false, "2482.76", "0.0", "0.0", "0.0");
        addRepaymentValues(expectedvalues, todaysDate, 1, false, "2494.22", "80.84", "0.0", "0.0");
        addRepaymentValues(expectedvalues, todaysDate, 1, false, "2505.73", "23.18", "0.0", "0.0");
        addRepaymentValues(expectedvalues, todaysDate, 1, false, "2517.29", "11.62", "0.0", "0.0");
        verifyLoanRepaymentSchedule(loanSchedule, expectedvalues);

        System.out.println("-----------------------------------APPROVE LOAN-----------------------------------------");
        loanStatusHashMap = this.loanTransactionHelper.approveLoan(LOAN_DISBURSEMENT_DATE, loanID);
        LoanStatusChecker.verifyLoanIsApproved(loanStatusHashMap);
        LoanStatusChecker.verifyLoanIsWaitingForDisbursal(loanStatusHashMap);

        System.out.println("-------------------------------DISBURSE LOAN-------------------------------------------");
        loanStatusHashMap = this.loanTransactionHelper.disburseLoan(LOAN_DISBURSEMENT_DATE, loanID);
        LoanStatusChecker.verifyLoanIsActive(loanStatusHashMap);

        loanSchedule = this.loanTransactionHelper.getLoanRepaymentSchedule(this.requestSpec, this.responseSpec, loanID);
        expectedvalues = new ArrayList<>();
        todaysDate = Calendar.getInstance(Utils.getTimeZoneOfTenant());
        todaysDate.add(Calendar.DAY_OF_MONTH, -1);
        addRepaymentValues(expectedvalues, todaysDate, 1, false, "2482.76", "0.0", "0.0", "0.0");
        addRepaymentValues(expectedvalues, todaysDate, 1, false, "2494.22", "80.84", "0.0", "0.0");
        addRepaymentValues(expectedvalues, todaysDate, 1, false, "2505.73", "23.18", "0.0", "0.0");
        addRepaymentValues(expectedvalues, todaysDate, 1, false, "2517.29", "11.62", "0.0", "0.0");

        verifyLoanRepaymentSchedule(loanSchedule, expectedvalues);

        HashMap prepayDetail = this.loanTransactionHelper.getPrepayAmount(this.requestSpec, this.responseSpec, loanID);
        String prepayAmount = String.valueOf(prepayDetail.get("amount"));
        validateNumberForEqualWithMsg("verify pre-close amount", preCloseAmount, prepayAmount);
        todaysDate = Calendar.getInstance(Utils.getTimeZoneOfTenant());
        String LOAN_REPAYMENT_DATE = dateFormat.format(todaysDate.getTime());
        this.loanTransactionHelper.makeRepayment(LOAN_REPAYMENT_DATE, new Float(prepayAmount), loanID);
        loanStatusHashMap = LoanStatusChecker.getStatusOfLoan(this.requestSpec, this.responseSpec, loanID);
        LoanStatusChecker.verifyLoanAccountIsClosed(loanStatusHashMap);
    }

    private void addRepaymentValues(List<Map<String, Object>> expectedvalues, Calendar todaysDate, int addPeriod, boolean isAddDays,
            String principalDue, String interestDue, String feeChargesDue, String penaltyChargesDue) {
        Map<String, Object> values = new HashMap<>(3);
        if (isAddDays) {
            values.put("dueDate", getDateAsArray(todaysDate, addPeriod));
        } else {
            values.put("dueDate", getDateAsArray(todaysDate, addPeriod * 7));
        }
        System.out.println("Updated date " + values.get("dueDate"));
        values.put("principalDue", principalDue);
        values.put("interestDue", interestDue);
        values.put("feeChargesDue", feeChargesDue);
        values.put("penaltyChargesDue", penaltyChargesDue);
        expectedvalues.add(values);
    }

    private List getDateAsArray(Calendar todaysDate, int addPeriod) {
        return getDateAsArray(todaysDate, addPeriod, Calendar.DAY_OF_MONTH);
    }

    private List getDateAsArray(Calendar todaysDate, int addvalue, int type) {
        todaysDate.add(type, addvalue);
        return new ArrayList<>(Arrays.asList(todaysDate.get(Calendar.YEAR), todaysDate.get(Calendar.MONTH) + 1,
                todaysDate.get(Calendar.DAY_OF_MONTH)));
    }

    private Integer createLoanProductWithInterestRecalculation(final String repaymentStrategy,
            final String interestRecalculationCompoundingMethod, final String rescheduleStrategyMethod,
            final String recalculationRestFrequencyType, final String recalculationRestFrequencyInterval,
            final String recalculationRestFrequencyDate, final String preCloseInterestCalculationStrategy, final Account[] accounts) {
        final String recalculationCompoundingFrequencyType = null;
        final String recalculationCompoundingFrequencyInterval = null;
        final String recalculationCompoundingFrequencyDate = null;
        return createLoanProductWithInterestRecalculation(repaymentStrategy, interestRecalculationCompoundingMethod,
                rescheduleStrategyMethod, recalculationRestFrequencyType, recalculationRestFrequencyInterval,
                recalculationRestFrequencyDate, recalculationCompoundingFrequencyType, recalculationCompoundingFrequencyInterval,
                recalculationCompoundingFrequencyDate, preCloseInterestCalculationStrategy, accounts, null, false);
    }

    private Integer createLoanProductWithInterestRecalculationAndCompoundingDetails(final String repaymentStrategy,
            final String interestRecalculationCompoundingMethod, final String rescheduleStrategyMethod,
            final String recalculationRestFrequencyType, final String recalculationRestFrequencyInterval,
            final String recalculationRestFrequencyDate, final String recalculationCompoundingFrequencyType,
            final String recalculationCompoundingFrequencyInterval, final String recalculationCompoundingFrequencyDate,
            final String preCloseInterestCalculationStrategy, final Account[] accounts) {
        return createLoanProductWithInterestRecalculation(repaymentStrategy, interestRecalculationCompoundingMethod,
                rescheduleStrategyMethod, recalculationRestFrequencyType, recalculationRestFrequencyInterval,
                recalculationRestFrequencyDate, recalculationCompoundingFrequencyType, recalculationCompoundingFrequencyInterval,
                recalculationCompoundingFrequencyDate, preCloseInterestCalculationStrategy, accounts, null, false);
    }

    private Integer createLoanProductWithInterestRecalculation(final String repaymentStrategy,
            final String interestRecalculationCompoundingMethod, final String rescheduleStrategyMethod,
            final String recalculationRestFrequencyType, final String recalculationRestFrequencyInterval,
            final String recalculationRestFrequencyDate, final String recalculationCompoundingFrequencyType,
            final String recalculationCompoundingFrequencyInterval, final String recalculationCompoundingFrequencyDate,
            final String preCloseInterestCalculationStrategy, final Account[] accounts, final String chargeId,
            boolean isArrearsBasedOnOriginalSchedule) {
        System.out.println("------------------------------CREATING NEW LOAN PRODUCT ---------------------------------------");
        LoanProductTestBuilder builder = new LoanProductTestBuilder()
                .withPrincipal("10000000.00")
                .withNumberOfRepayments("24")
                .withRepaymentAfterEvery("1")
                .withRepaymentTypeAsWeek()
                .withinterestRatePerPeriod("2")
                .withInterestRateFrequencyTypeAsMonths()
                .withRepaymentStrategy(repaymentStrategy)
                .withAmortizationTypeAsEqualPrincipalPayment()
                .withInterestCalculationPeriodTypeAsRepaymentPeriod(true)
                .withInterestTypeAsDecliningBalance()
                .withInterestRecalculationDetails(interestRecalculationCompoundingMethod, rescheduleStrategyMethod,
                        preCloseInterestCalculationStrategy)
                .withInterestRecalculationRestFrequencyDetails(recalculationRestFrequencyType, recalculationRestFrequencyInterval,
                        recalculationRestFrequencyDate)
                .withInterestRecalculationCompoundingFrequencyDetails(recalculationCompoundingFrequencyType,
                        recalculationCompoundingFrequencyInterval, recalculationCompoundingFrequencyDate);
        if (accounts != null) {
            builder = builder.withAccountingRulePeriodicAccrual(accounts);
        }

        if (isArrearsBasedOnOriginalSchedule) builder = builder.withArrearsConfiguration();

        final String loanProductJSON = builder.build(chargeId);
        return this.loanTransactionHelper.getLoanProductId(loanProductJSON);
    }

    private Integer applyForLoanApplicationForInterestRecalculation(final Integer clientID, final Integer loanProductID,
            final String disbursementDate, final String restStartDate, final String repaymentStrategy, final List<HashMap> charges) {
        final String compoundingStartDate = null;
        return applyForLoanApplicationForInterestRecalculation(clientID, loanProductID, disbursementDate, restStartDate,
                compoundingStartDate, repaymentStrategy, charges);
    }

    private Integer applyForLoanApplicationForInterestRecalculationWithMoratorium(final Integer clientID, final Integer loanProductID,
            final String disbursementDate, final String restStartDate, final String repaymentStrategy, final List<HashMap> charges,
            final String graceOnInterestPayment, final String graceOnPrincipalPayment) {
        final String compoundingStartDate = null;
        return applyForLoanApplicationForInterestRecalculation(clientID, loanProductID, disbursementDate, restStartDate,
                compoundingStartDate, repaymentStrategy, charges, graceOnInterestPayment, graceOnPrincipalPayment);
    }

    private Integer applyForLoanApplicationForInterestRecalculation(final Integer clientID, final Integer loanProductID,
            final String disbursementDate, final String restStartDate, final String compoundingStartDate, final String repaymentStrategy,
            final List<HashMap> charges) {
        return applyForLoanApplicationForInterestRecalculation(clientID, loanProductID, disbursementDate, restStartDate,
                compoundingStartDate, repaymentStrategy, charges, null, null);
    }

    private Integer applyForLoanApplicationForInterestRecalculation(final Integer clientID, final Integer loanProductID,
            final String disbursementDate, final String restStartDate, final String compoundingStartDate, final String repaymentStrategy,
            final List<HashMap> charges, final String graceOnInterestPayment, final String graceOnPrincipalPayment) {
        System.out.println("--------------------------------APPLYING FOR LOAN APPLICATION--------------------------------");
        final String loanApplicationJSON = new LoanApplicationTestBuilder() //
                .withPrincipal("10000.00") //
                .withLoanTermFrequency("4") //
                .withLoanTermFrequencyAsWeeks() //
                .withNumberOfRepayments("4") //
                .withRepaymentEveryAfter("1") //
                .withRepaymentFrequencyTypeAsWeeks() //
                .withInterestRatePerPeriod("2") //
                .withAmortizationTypeAsEqualInstallments() //
                .withInterestTypeAsDecliningBalance() //
                .withInterestCalculationPeriodTypeSameAsRepaymentPeriod() //
                .withExpectedDisbursementDate(disbursementDate) //
                .withSubmittedOnDate(disbursementDate) //
                .withRestFrequencyDate(restStartDate)//
                .withCompoundingFrequencyDate(compoundingStartDate)//
                .withwithRepaymentStrategy(repaymentStrategy) //
                .withPrincipalGrace(graceOnPrincipalPayment) //
                .withInterestGrace(graceOnInterestPayment)//
                .withCharges(charges)//
                .build(clientID.toString(), loanProductID.toString(), null);
        return this.loanTransactionHelper.getLoanId(loanApplicationJSON);
    }

    private void verifyLoanRepaymentSchedule(final ArrayList<HashMap> loanSchedule, List<Map<String, Object>> expectedvalues) {
        int index = 1;
        verifyLoanRepaymentSchedule(loanSchedule, expectedvalues, index);

    }

    private void verifyLoanRepaymentSchedule(final ArrayList<HashMap> loanSchedule, List<Map<String, Object>> expectedvalues, int index) {
        System.out.println("--------------------VERIFYING THE PRINCIPAL DUES,INTEREST DUE AND DUE DATE--------------------------");
        for (Map<String, Object> values : expectedvalues) {
            assertEquals("Checking for Due Date for  installment " + index, values.get("dueDate"), loanSchedule.get(index).get("dueDate"));
            validateNumberForEqualWithMsg("Checking for Principal Due for installment " + index,
                    String.valueOf(values.get("principalDue")), String.valueOf(loanSchedule.get(index).get("principalDue")));
            validateNumberForEqualWithMsg("Checking for Interest Due for installment " + index, String.valueOf(values.get("interestDue")),
                    String.valueOf(loanSchedule.get(index).get("interestDue")));
            validateNumberForEqualWithMsg("Checking for Fee charge Due for installment " + index,
                    String.valueOf(values.get("feeChargesDue")), String.valueOf(loanSchedule.get(index).get("feeChargesDue")));
            validateNumberForEqualWithMsg("Checking for Penalty charge Due for installment " + index,
                    String.valueOf(values.get("penaltyChargesDue")), String.valueOf(loanSchedule.get(index).get("penaltyChargesDue")));
            index++;
        }
    }


    private Integer createSavingsProduct(final RequestSpecification requestSpec, final ResponseSpecification responseSpec,
            final String minOpenningBalance) {
        System.out.println("------------------------------CREATING NEW SAVINGS PRODUCT ---------------------------------------");
        SavingsProductHelper savingsProductHelper = new SavingsProductHelper();

        final String savingsProductJSON = savingsProductHelper
        //
                .withInterestCompoundingPeriodTypeAsDaily()
                //
                .withInterestPostingPeriodTypeAsMonthly()
                //
                .withInterestCalculationPeriodTypeAsDailyBalance()

                .withMinimumOpenningBalance(minOpenningBalance).build();
        return SavingsProductHelper.createSavingsProduct(savingsProductJSON, requestSpec, responseSpec);
    }



    private void validateIfValuesAreNotOverridden(Integer loanID, Integer loanProductID) {
        String loanProductDetails = this.loanTransactionHelper.getLoanProductDetails(this.requestSpec, this.responseSpec, loanProductID);
        String loanDetails = this.loanTransactionHelper.getLoanDetails(this.requestSpec, this.responseSpec, loanID);
        List<String> comparisonAttributes = Arrays.asList("amortizationType", "interestType", "transactionProcessingStrategyId",
                "interestCalculationPeriodType", "repaymentFrequencyType", "graceOnPrincipalPayment", "graceOnInterestPayment",
                "inArrearsTolerance", "graceOnArrearsAgeing");

        for (String comparisonAttribute : comparisonAttributes) {
            assertEquals(JsonPath.from(loanProductDetails).get(comparisonAttribute), JsonPath.from(loanDetails).get(comparisonAttribute));
        }
    }

    private JsonObject createLoanProductConfigurationDetail(JsonObject loanProductConfiguration, Boolean bool) {
        loanProductConfiguration.addProperty("amortizationType", bool);
        loanProductConfiguration.addProperty("interestType", bool);
        loanProductConfiguration.addProperty("transactionProcessingStrategyId", bool);
        loanProductConfiguration.addProperty("interestCalculationPeriodType", bool);
        loanProductConfiguration.addProperty("inArrearsTolerance", bool);
        loanProductConfiguration.addProperty("repaymentEvery", bool);
        loanProductConfiguration.addProperty("graceOnPrincipalAndInterestPayment", bool);
        loanProductConfiguration.addProperty("graceOnArrearsAgeing", bool);
        return loanProductConfiguration;
    }

    private Integer applyForLoanApplicationWithProductConfigurationAsTrue(final Integer clientID, final Integer loanProductID,
            String principal) {
        System.out.println("--------------------------------APPLYING FOR LOAN APPLICATION--------------------------------");
        final String loanApplicationJSON = new LoanApplicationTestBuilder() //
                .withPrincipal(principal) //
                .withRepaymentEveryAfter("1") //
                .withLoanTermFrequency("4") //
                .withLoanTermFrequencyAsMonths() //
                .withNumberOfRepayments("4") //
                .withRepaymentFrequencyTypeAsMonths() //
                .withInterestRatePerPeriod("2") //
                .withInterestCalculationPeriodTypeSameAsRepaymentPeriod() //
                .withExpectedDisbursementDate("1 March 2014") //
                .withSubmittedOnDate("1 March 2014") //
                .build(clientID.toString(), loanProductID.toString(), null);
        return this.loanTransactionHelper.getLoanId(loanApplicationJSON);
    }

    private Integer applyForLoanApplicationWithProductConfigurationAsFalse(final Integer clientID, final Integer loanProductID,
            String principal) {
        System.out.println("--------------------------------APPLYING FOR LOAN APPLICATION--------------------------------");
        final String loanApplicationJSON = new LoanApplicationTestBuilder()
                //
                .withPrincipal(principal)
                //
                .withRepaymentEveryAfter("2")
                //
                .withAmortizationTypeAsEqualPrincipalPayments().withRepaymentFrequencyTypeAsWeeks()
                .withwithRepaymentStrategy(LoanProductTestBuilder.RBI_INDIA_STRATEGY).withInterestTypeAsFlatBalance()
                .withInterestCalculationPeriodTypeSameAsRepaymentPeriod().withPrincipalGrace("1").withInterestGrace("1")
                .withLoanTermFrequency("4") //
                .withLoanTermFrequencyAsMonths() //
                .withNumberOfRepayments("4") //
                .withRepaymentFrequencyTypeAsMonths() //
                .withInterestRatePerPeriod("2") //
                .withInterestCalculationPeriodTypeSameAsRepaymentPeriod() //
                .withExpectedDisbursementDate("1 March 2014") //
                .withSubmittedOnDate("1 March 2014") //
                .build(clientID.toString(), loanProductID.toString(), null);

        return this.loanTransactionHelper.getLoanId(loanApplicationJSON);
    }
}