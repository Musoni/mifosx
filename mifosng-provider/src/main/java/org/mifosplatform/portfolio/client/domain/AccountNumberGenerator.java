/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.portfolio.client.domain;

import org.apache.commons.lang3.StringUtils;
import org.mifosplatform.infrastructure.accountnumberformat.domain.AccountNumberFormat;
import org.mifosplatform.infrastructure.accountnumberformat.domain.AccountNumberFormatEnumerations.AccountNumberPrefixType;
import org.mifosplatform.infrastructure.accountnumberformat.domain.CustomAccountType;
import org.mifosplatform.infrastructure.codes.domain.CodeValue;
import org.mifosplatform.portfolio.group.domain.Group;
import org.mifosplatform.portfolio.loanaccount.domain.Loan;
import org.mifosplatform.portfolio.savings.domain.SavingsAccount;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Example {@link AccountNumberGenerator} for clients that takes an entities
 * auto generated database id and zero fills it ensuring the identifier is
 * always of a given <code>maxLength</code>.
 */
@Component
public class AccountNumberGenerator {

    private final static int maxLength = 9;

    private final static String ID = "id";
    private final static String CLIENT_TYPE = "clientType";
    private final static String OFFICE_NAME = "officeName";
    private final static String OFFICE_EXTERNAL_ID = "officeExternalId";
    private final static String LOAN_PRODUCT_SHORT_NAME = "loanProductShortName";
    private final static String SAVINGS_PRODUCT_SHORT_NAME = "savingsProductShortName";

    public String generate(Client client, AccountNumberFormat accountNumberFormat) {
        Map<String, String> propertyMap = new HashMap<>();
        propertyMap.put(ID, client.getId().toString());
        propertyMap.put(OFFICE_NAME, client.getOffice().getName());
        CodeValue clientType = client.clientType();
        if (clientType != null) {
            propertyMap.put(CLIENT_TYPE, clientType.label());
        }
        String externalId = client.getOffice().getExternalId();
        if(externalId !=null){
            propertyMap.put(OFFICE_EXTERNAL_ID,externalId);
        }
        return generateAccountNumber(propertyMap, accountNumberFormat);
    }

    public String generateCustom(Client client,AccountNumberFormat accountNumberFormat){
        Map<Object,String> customMap = new HashMap<>();
        customMap.put(ID, client.getId().toString());
        customMap.put(CustomAccountType.OFFICE_EXTERNAL_ID.getValue(),client.getOffice().getExternalId());
        customMap.put(CustomAccountType.OFFICE_ID.getValue(),client.getOffice().getId().toString());
        customMap.put(CustomAccountType.CLIENT_TYPE.getValue(),"I");
        customMap.put(CustomAccountType.STAFF_ID.getValue(), client.getStaff().getId().toString());
        final String delimiter = this.delimiter(accountNumberFormat.getCustomPattern());
        return generateCustomNumber(customMap, accountNumberFormat, delimiter);
    }

    public String generate(Loan loan, AccountNumberFormat accountNumberFormat) {
        Map<String, String> propertyMap = new HashMap<>();
        propertyMap.put(ID, loan.getId().toString());
        propertyMap.put(OFFICE_NAME, loan.getOffice().getName());
        propertyMap.put(LOAN_PRODUCT_SHORT_NAME, loan.loanProduct().getShortName());
        return generateAccountNumber(propertyMap, accountNumberFormat);
    }

    public String generateCustom(Loan loan,AccountNumberFormat accountNumberFormat){
        Map<Object,String> customMap = new HashMap<>();
        customMap.put(ID,loan.getId().toString());
        customMap.put(CustomAccountType.OFFICE_ID.getValue(),loan.getClient().getOffice().getId().toString());
        customMap.put(CustomAccountType.OFFICE_EXTERNAL_ID.getValue(),loan.getClient().getOffice().getExternalId());
        customMap.put(CustomAccountType.LOAN_PRODUCT.getValue(),loan.getLoanProduct().getId().toString());
        customMap.put(CustomAccountType.LOAN_PRODUCT_SHORT_NAME.getValue(),loan.getLoanProduct().getShortName());
        customMap.put(CustomAccountType.STAFF_ID.getValue(),loan.getLoanOfficer().getId().toString());
        customMap.put(CustomAccountType.CLIENT_ID.getValue(),loan.getClient().getId().toString());
        final String delimiter = this.delimiter(accountNumberFormat.getCustomPattern());
        return generateCustomNumber(customMap,accountNumberFormat,delimiter);
    }

    public String generateCustom(Group group,AccountNumberFormat accountNumberFormat){
        Map<Object,String> customMap = new HashMap<>();
        customMap.put(ID, group.getId().toString());
        customMap.put(CustomAccountType.OFFICE_ID.getValue(),group.getOffice().getId().toString());
        customMap.put(CustomAccountType.STAFF_ID.getValue(),group.getStaff().getId().toString());
        customMap.put(CustomAccountType.OFFICE_EXTERNAL_ID.getValue(),group.getOffice().getExternalId());
        customMap.put(CustomAccountType.GROUP_TYPE.getValue(),"GRP");
        final String delimiter = this.delimiter(accountNumberFormat.getCustomPattern());
        return generateCustomNumber(customMap,accountNumberFormat,delimiter);
    }

    public String generateCustom(SavingsAccount savingsAccount,AccountNumberFormat accountNumberFormat){
        Map<Object,String> customMap = new HashMap<>();
        customMap.put(ID, savingsAccount.getId().toString());
        customMap.put(CustomAccountType.OFFICE_ID.getValue(),savingsAccount.getClient().getOffice().getId().toString());
        customMap.put(CustomAccountType.OFFICE_EXTERNAL_ID.getValue(), savingsAccount.getClient().getOffice().getExternalId());
        customMap.put(CustomAccountType.SAVING_PRODUCT_SHORT_NAME.getValue(),savingsAccount.savingsProduct().getShortName());
        customMap.put(CustomAccountType.SAVINGS_PRODUCT.getValue(),savingsAccount.savingsProduct().getId().toString());
        customMap.put(CustomAccountType.STAFF_ID.getValue(), savingsAccount.getSavingsOfficer().getId().toString());
        customMap.put(CustomAccountType.CLIENT_ID.getValue(), savingsAccount.getClient().getId().toString());
        final String delimiter = this.delimiter(accountNumberFormat.getCustomPattern());
        return generateCustomNumber(customMap,accountNumberFormat,delimiter);
    }


    private String delimiter(String customPattern) {
        String delimiter = "";
        if(customPattern.contains("-")){
            delimiter = "-";
        }else if(customPattern.contains(".")){
            delimiter = ".";
        }else if(customPattern.contains("/")){
            delimiter = "/";
        }
        return delimiter;
    }

    public String generate(SavingsAccount savingsAccount, AccountNumberFormat accountNumberFormat) {
        Map<String, String> propertyMap = new HashMap<>();
        propertyMap.put(ID, savingsAccount.getId().toString());
        propertyMap.put(OFFICE_NAME, savingsAccount.office().getName());
        propertyMap.put(SAVINGS_PRODUCT_SHORT_NAME, savingsAccount.savingsProduct().getShortName());
        return generateAccountNumber(propertyMap, accountNumberFormat);
    }

    private String generateAccountNumber(Map<String, String> propertyMap, AccountNumberFormat accountNumberFormat) {
        int maxLength = AccountNumberGenerator.maxLength;
        if(accountNumberFormat !=null && accountNumberFormat.getZeroPadding() !=null){
            maxLength =  accountNumberFormat.getZeroPadding();
        }
        String accountNumber = StringUtils.leftPad(propertyMap.get(ID), maxLength, '0');
        if (accountNumberFormat != null && accountNumberFormat.getPrefixEnum() != null) {
            AccountNumberPrefixType accountNumberPrefixType = AccountNumberPrefixType.fromInt(accountNumberFormat.getPrefixEnum());
            String prefix = null;
            switch (accountNumberPrefixType) {
                case CLIENT_TYPE:
                    prefix = propertyMap.get(CLIENT_TYPE);
                break;

                case OFFICE_NAME:
                    prefix = propertyMap.get(OFFICE_NAME);
                break;

                case LOAN_PRODUCT_SHORT_NAME:
                    prefix = propertyMap.get(LOAN_PRODUCT_SHORT_NAME);
                break;

                case SAVINGS_PRODUCT_SHORT_NAME:
                    prefix = propertyMap.get(SAVINGS_PRODUCT_SHORT_NAME);
                break;

                case OFFICE_EXTERNAL_ID:
                    prefix = propertyMap.get(OFFICE_EXTERNAL_ID);
                break;

                default:
                break;

            }
            accountNumber = StringUtils.overlay(accountNumber, prefix, 0, 0);
        }
        return accountNumber;
    }
    
    public String generateCenterAccountNumber(Group group, AccountNumberFormat accountNumberFormat) {
    	Map<String, String> propertyMap = new HashMap<>();
        propertyMap.put(ID, group.getId().toString());
        propertyMap.put(OFFICE_NAME, group.getOffice().getName());        
        return generateAccountNumber(propertyMap, accountNumberFormat);
    }


    private String generateCustomNumber(Map<Object, String> propertyMap,AccountNumberFormat accountNumberFormat,String delimiter){
        int zeroPadding = (accountNumberFormat.getZeroPadding() != null) ? accountNumberFormat.getZeroPadding() : 4;
        String accountNumber = StringUtils.leftPad(propertyMap.get(ID), zeroPadding, '0');
        if(accountNumberFormat !=null && accountNumberFormat.getCustomPattern() !=null){
            String split[] = accountNumberFormat.getCustomPattern().split(delimiter); // maximum size is 3 since its defined by musoni
            if(split.length == 3){
                accountNumber =  this.leftPadFormat(propertyMap, Integer.parseInt(split[0]), zeroPadding) + delimiter +
                        this.leftPadFormat(propertyMap, Integer.parseInt(split[1]), zeroPadding) + delimiter +
                        this.leftPadFormat(propertyMap, Integer.parseInt(split[2]), zeroPadding) + delimiter + accountNumber;
            }else if(split.length == 2){
                accountNumber =  this.leftPadFormat(propertyMap, Integer.parseInt(split[0]), zeroPadding) + delimiter +
                        this.leftPadFormat(propertyMap, Integer.parseInt(split[1]), zeroPadding) + delimiter + accountNumber;
            }else if(split.length == 1){
                accountNumber = this.leftPadFormat(propertyMap,Integer.parseInt(split[0]),zeroPadding) + delimiter + accountNumber;
            }
        }
        return accountNumber;
    }

    public String generateGroupAccountNumber(Group group, AccountNumberFormat accountNumberFormat) {
        Map<String, String> propertyMap = new HashMap<>();
        propertyMap.put(ID, group.getId().toString());
        propertyMap.put(OFFICE_NAME, group.getOffice().getName());
        return generateAccountNumber(propertyMap, accountNumberFormat);
    }


    private String leftPadFormat(Map<Object, String> propertyMap, Integer mapObject,int zeroPadding){
        String leftPadFormat ="";
        if(mapObject.equals(5) || mapObject.equals(1) || mapObject.equals(7) || mapObject.equals(6)|| mapObject.equals(10)){
            leftPadFormat = propertyMap.get(mapObject);
        }else{ leftPadFormat  = StringUtils.leftPad(propertyMap.get(mapObject),zeroPadding,'0');}
        return leftPadFormat;
    }
}