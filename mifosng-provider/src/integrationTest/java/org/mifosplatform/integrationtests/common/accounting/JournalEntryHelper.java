/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.integrationtests.common.accounting;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;

import com.google.gson.Gson;
import org.mifosplatform.integrationtests.common.CommonConstants;
import org.mifosplatform.integrationtests.common.Utils;

import com.jayway.restassured.specification.RequestSpecification;
import com.jayway.restassured.specification.ResponseSpecification;

@SuppressWarnings("rawtypes")
public class JournalEntryHelper {

    private final RequestSpecification requestSpec;
    private final ResponseSpecification responseSpec;

    public JournalEntryHelper(final RequestSpecification requestSpec, final ResponseSpecification responseSpec) {
        this.requestSpec = requestSpec;
        this.responseSpec = responseSpec;
    }

    public void checkJournalEntryForExpenseAccount(final Account expenseAccount, final String date, final JournalEntry... accountEntries) {
        checkJournalEntry(null, expenseAccount, date, accountEntries);
    }

    public void checkJournalEntryForAssetAccount(final Account assetAccount, final String date, final JournalEntry... accountEntries) {
        checkJournalEntry(null, assetAccount, date, accountEntries);
    }

    public void checkJournalEntryForIncomeAccount(final Account incomeAccount, final String date, final JournalEntry... accountEntries) {
        checkJournalEntry(null, incomeAccount, date, accountEntries);
    }

    public void checkJournalEntryForLiabilityAccount(final Account liabilityAccount, final String date,
            final JournalEntry... accountEntries) {
        checkJournalEntry(null, liabilityAccount, date, accountEntries);
    }

    public void checkJournalEntryForLiabilityAccount(final Integer officeId, final Account liabilityAccount, final String date,
            final JournalEntry... accountEntries) {
        checkJournalEntry(officeId, liabilityAccount, date, accountEntries);
    }

    public void checkInterBranchJournalEntry(final String transactionId, final JournalEntry... accountEntries){
        final ArrayList<HashMap> response = getJournalEntriesByTransactionId(transactionId);
        assertNotEquals("No new interbranch control journal entries created", accountEntries.length, response.size());
        Float totalCreditAmount = 0f;
        Float totalDebitAmount = 0f;
        HashMap<Integer,Float> creditAmountPerOffice = new HashMap<>();
        HashMap<Integer,Float> debitAmountPerOffice = new HashMap<>();

        for (int i = 0 ; i < response.size() ; i++){
            final Integer officeId = getOfficeIdFromJournalEntry(response, i);
            final Float entryAmount = getTransactionAmountFromJournalEntry(response, i);

            if(getEntryValueFromJournalEntry(response, i).equals(JournalEntry.TransactionType.CREDIT.toString())){
                Float officeAmount = creditAmountPerOffice.containsKey(officeId) ? creditAmountPerOffice.get(officeId) : 0f;
                officeAmount += entryAmount;
                totalCreditAmount += entryAmount;
                creditAmountPerOffice.put(officeId,officeAmount);
            }

            if(getEntryValueFromJournalEntry(response, i).equals(JournalEntry.TransactionType.DEBIT.toString())){
                Float officeAmount = debitAmountPerOffice.containsKey(officeId) ? debitAmountPerOffice.get(officeId) : 0f;
                officeAmount += entryAmount;
                totalDebitAmount += entryAmount;
                debitAmountPerOffice.put(officeId,officeAmount);
            }
        }
        assertEquals("Credits and Debits aren't equal", totalCreditAmount, totalDebitAmount);
        assertEquals("Every branch must have equal debits and credits", creditAmountPerOffice.size(), debitAmountPerOffice.size());
        for (Integer officeId : creditAmountPerOffice.keySet()) {
            assertEquals("Every branch must have equal debits and credits", creditAmountPerOffice.get(officeId), debitAmountPerOffice.get(officeId));
        }
    }

    public void ensureNoAccountingTransactionsWithTransactionId(final String transactionId) {
        ArrayList<HashMap> transactions = getJournalEntriesByTransactionId(transactionId);
        assertTrue("Tranasactions are is not empty", transactions.isEmpty());

    }

    private String getEntryValueFromJournalEntry(final ArrayList<HashMap> entryResponse, final int entryNumber) {
        final HashMap map = (HashMap) entryResponse.get(entryNumber).get("entryType");
        return (String) map.get("value");
    }

    private Float getTransactionAmountFromJournalEntry(final ArrayList<HashMap> entryResponse, final int entryNumber) {
        return (Float) entryResponse.get(entryNumber).get("amount");
    }

    private Integer getOfficeIdFromJournalEntry(final ArrayList<HashMap> entryResponse, final int entryNumber){
        return (Integer) entryResponse.get(entryNumber).get("officeId");
    }

    public Object createJournalEntries(final String response, final ResponseSpecification responseSpec, final JournalEntry... accountEntries){
        return Utils.performServerPost(this.requestSpec, responseSpec,
                "/mifosng-provider/api/v1/journalentries?" + Utils.TENANT_IDENTIFIER, getAsJSON(accountEntries),
                response);
    }

    private void checkJournalEntry(final Integer officeId, final Account account, final String date, final JournalEntry... accountEntries) {
        final String url = createURLForGettingAccountEntries(account, date, officeId);
        final ArrayList<HashMap> response = Utils.performServerGet(this.requestSpec, this.responseSpec, url, "pageItems");
        for (int i = 0; i < accountEntries.length; i++) {
            assertThat(getEntryValueFromJournalEntry(response, i), equalTo(accountEntries[i].getTransactionType()));
            assertThat(getTransactionAmountFromJournalEntry(response, i), equalTo(accountEntries[i].getTransactionAmount()));
        }
    }

    private String createURLForGettingAccountEntries(final Account account, final String date, final Integer officeId) {
        String url = new String("/mifosng-provider/api/v1/journalentries?glAccountId=" + account.getAccountID() + "&type="
                + account.getAccountType() + "&fromDate=" + date + "&toDate=" + date + "&tenantIdentifier=default"
                + "&orderBy=id&sortOrder=desc&locale=en&dateFormat=dd MMMM yyyy");
        if (officeId != null) {
            url = url + "&officeId=" + officeId;
        }
        return url;
    }

    private ArrayList<HashMap> getJournalEntriesByTransactionId(final String transactionId) {
        final String url = createURLForGettingAccountEntriesByTransactionId(transactionId);
        final ArrayList<HashMap> response = Utils.performServerGet(this.requestSpec, this.responseSpec, url, "pageItems");
        return response;
    }

    private String createURLForGettingAccountEntriesByTransactionId(final String transactionId) {
        return new String("/mifosng-provider/api/v1/journalentries?transactionId=" + transactionId + "&tenantIdentifier=default"
                + "&orderBy=id&sortOrder=desc&locale=en&dateFormat=dd MMMM yyyy");
    }

    private static String getAsJSON(final JournalEntry... accountEntries) {
        final HashMap<String, Object> map = new HashMap<>();
        final ArrayList<HashMap<String, String>> credits = new ArrayList<>();
        final ArrayList<HashMap<String, String>> debits = new ArrayList<>();

        for (int i = 0; i < accountEntries.length; i++) {
            HashMap<String, String> creditOrDebitMap = new HashMap<>();

            creditOrDebitMap.put("glAccountId",accountEntries[i].getAccountId().toString());
            creditOrDebitMap.put("officeId",accountEntries[i].getOfficeId().toString());
            creditOrDebitMap.put("amount",accountEntries[i].getTransactionAmount().toString());

            if(accountEntries[i].getTransactionType().equals(JournalEntry.TransactionType.CREDIT.toString())){
                credits.add(creditOrDebitMap);
            }
            if(accountEntries[i].getTransactionType().equals(JournalEntry.TransactionType.DEBIT.toString())){
                debits.add(creditOrDebitMap);
            }
        }

        map.put("transactionDate", "01-01-2011");
        map.put("comments", "");
        map.put("referenceNumber", "");
        map.put("useAccountingRule", "false");
        map.put("locale", "en_GB");
        map.put("currencyCode", "USD");
        map.put("dateFormat", "dd-MM-yyyy");
        map.put("credits", credits);
        map.put("debits", debits);
        System.out.println("map : " + map);
        return new Gson().toJson(map);
    }

}
