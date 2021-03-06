/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.jobs.service;

public enum JobName {

    UPDATE_LOAN_SUMMARY("Update loan Summary"), //
    UPDATE_LOAN_ARREARS_AGEING("Update Loan Arrears Ageing"), //
    UPDATE_LOAN_PAID_IN_ADVANCE("Update Loan Paid In Advance"), //
    APPLY_ANNUAL_FEE_FOR_SAVINGS("Apply Annual Fee For Savings"), //
    APPLY_HOLIDAYS_TO_LOANS("Apply Holidays To Loans"), //
    POST_INTEREST_FOR_SAVINGS("Post Interest For Savings"), //
    TRANSFER_FEE_CHARGE_FOR_LOANS("Transfer Fee For Loans From Savings"), //
    ACCOUNTING_RUNNING_BALANCE_UPDATE("Update Accounting Running Balances"), //
    PAY_DUE_SAVINGS_CHARGES("Pay Due Savings Charges"), //
    APPLY_CHARGE_TO_OVERDUE_LOAN_INSTALLMENT("Apply penalty to overdue loans"),
    EXECUTE_STANDING_INSTRUCTIONS("Execute Standing Instruction"),
    ADD_ACCRUAL_ENTRIES("Add Accrual Transactions"),
    UPDATE_NPA("Update Non Performing Assets"),
    UPDATE_DEPOSITS_ACCOUNT_MATURITY_DETAILS("Update Deposit Accounts Maturity details"),
    TRANSFER_INTEREST_TO_SAVINGS("Transfer Interest To Savings"),
    ADD_PERIODIC_ACCRUAL_ENTRIES("Add Periodic Accrual Transactions"),
    RECALCULATE_INTEREST_FOR_LOAN("Recalculate Interest For Loans"),
    GENERATE_RD_SCEHDULE("Generate Mandatory Savings Schedule"),
    GENERATE_LOANLOSS_PROVISIONING("Generate Loan Loss Provisioning"),
    SEND_MESSAGES_TO_SMS_GATEWAY("Send messages to SMS gateway"), 
    GET_DELIVERY_REPORTS_FROM_SMS_GATEWAY("Get delivery reports from SMS gateway"),
    UPDATE_SMS_OUTBOUND_WITH_CAMPAIGN_MESSAGE("Update Sms Outbound with campaign message"),
    ALLOCATE_OVERPAYMENTS_TO_SAVINGS("Allocate overpayments to savings"),

    SEND_MESSAGES_TO_EMAIL_GATEWAY("Send messages to Email gateway"),
    UPDATE_EMAIL_OUTBOUND_WITH_CAMPAIGN_MESSAGE("Update Email Outbound with campaign message"),
    EXECUTE_REPORT_MAILING_JOBS("Execute Report Mailing Jobs"),
    EXECUTE_EMAIL("Execute Email"),
    APPLY_CHARGE_TO_OVERDUE_ON_MATURITY_LOANS("Apply penalty to overdue on maturity loans"),
    APPLY_PRODUCT_CHARGE_TO_EXISTING_SAVINGS_ACCOUNT("Apply product charge to existing savings account"),
    CALCULATE_DASHBOARD_METRICS("Calculate dashboard metrics");


    private final String name;

    private JobName(final String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}