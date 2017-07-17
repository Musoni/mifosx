/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.portfolio.savings.domain;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.joda.time.LocalDate;
import org.mifosplatform.accounting.common.AccountingRuleType;
import org.mifosplatform.infrastructure.codes.domain.CodeValue;
import org.mifosplatform.infrastructure.codes.domain.CodeValueRepositoryWrapper;
import org.mifosplatform.infrastructure.core.api.JsonCommand;
import org.mifosplatform.infrastructure.core.serialization.FromJsonHelper;
import org.mifosplatform.organisation.monetary.domain.MonetaryCurrency;
import org.mifosplatform.portfolio.charge.domain.Charge;
import org.mifosplatform.portfolio.charge.domain.ChargeRepositoryWrapper;
import org.mifosplatform.portfolio.charge.exception.ChargeCannotBeAppliedToException;
import org.mifosplatform.portfolio.loanproduct.exception.InvalidCurrencyException;
import org.mifosplatform.portfolio.savings.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static org.mifosplatform.portfolio.interestratechart.InterestRateChartSlabApiConstants.annualInterestRateParamName;
import static org.mifosplatform.portfolio.savings.SavingsApiConstants.*;

@Component
public class SavingsProductAssembler {

    private final ChargeRepositoryWrapper chargeRepository;
    private final CodeValueRepositoryWrapper codeValueRepository;
    private final FromJsonHelper fromApiJsonHelper;
    private final SavingsProductInterestRateChartRepository savingsProductInterestRateChartRepository;
    private final ApplyChargesToExistingSavingsAccountRepository applyChargesToExistingSavingsAccountRepository;

    @Autowired
    public SavingsProductAssembler(final ChargeRepositoryWrapper chargeRepository,
                                   final CodeValueRepositoryWrapper codeValueRepository, final FromJsonHelper fromApiJsonHelper,
                                   final SavingsProductInterestRateChartRepository savingsProductInterestRateChartRepository,
                                   final ApplyChargesToExistingSavingsAccountRepository applyChargesToExistingSavingsAccountRepository) {
        this.chargeRepository = chargeRepository;
        this.codeValueRepository = codeValueRepository;
        this.fromApiJsonHelper = fromApiJsonHelper;
        this.savingsProductInterestRateChartRepository = savingsProductInterestRateChartRepository;
        this.applyChargesToExistingSavingsAccountRepository = applyChargesToExistingSavingsAccountRepository;
    }

    public SavingsProduct assemble(final JsonCommand command) {

        final String name = command.stringValueOfParameterNamed(nameParamName);
        final String shortName = command.stringValueOfParameterNamed(shortNameParamName);
        final String description = command.stringValueOfParameterNamed(descriptionParamName);

        final String currencyCode = command.stringValueOfParameterNamed(currencyCodeParamName);
        final Integer digitsAfterDecimal = command.integerValueOfParameterNamed(digitsAfterDecimalParamName);
        final Integer inMultiplesOf = command.integerValueOfParameterNamed(inMultiplesOfParamName);
        final MonetaryCurrency currency = new MonetaryCurrency(currencyCode, digitsAfterDecimal, inMultiplesOf);

        final BigDecimal interestRate = command.bigDecimalValueOfParameterNamed(nominalAnnualInterestRateParamName);

        SavingsCompoundingInterestPeriodType interestCompoundingPeriodType = null;
        final Integer interestPeriodTypeValue = command.integerValueOfParameterNamed(interestCompoundingPeriodTypeParamName);
        if (interestPeriodTypeValue != null) {
            interestCompoundingPeriodType = SavingsCompoundingInterestPeriodType.fromInt(interestPeriodTypeValue);
        }

        SavingsPostingInterestPeriodType interestPostingPeriodType = null;
        final Integer interestPostingPeriodTypeValue = command.integerValueOfParameterNamed(interestPostingPeriodTypeParamName);
        if (interestPostingPeriodTypeValue != null) {
            interestPostingPeriodType = SavingsPostingInterestPeriodType.fromInt(interestPostingPeriodTypeValue);
        }

        SavingsInterestCalculationType interestCalculationType = null;
        final Integer interestCalculationTypeValue = command.integerValueOfParameterNamed(interestCalculationTypeParamName);
        if (interestCalculationTypeValue != null) {
            interestCalculationType = SavingsInterestCalculationType.fromInt(interestCalculationTypeValue);
        }

        SavingsInterestCalculationDaysInYearType interestCalculationDaysInYearType = null;
        final Integer interestCalculationDaysInYearTypeValue = command
                .integerValueOfParameterNamed(interestCalculationDaysInYearTypeParamName);
        if (interestCalculationDaysInYearTypeValue != null) {
            interestCalculationDaysInYearType = SavingsInterestCalculationDaysInYearType.fromInt(interestCalculationDaysInYearTypeValue);
        }

        final BigDecimal minRequiredOpeningBalance = command
                .bigDecimalValueOfParameterNamedDefaultToNullIfZero(minRequiredOpeningBalanceParamName);

        final Integer lockinPeriodFrequency = command.integerValueOfParameterNamedDefaultToNullIfZero(lockinPeriodFrequencyParamName);
        SavingsPeriodFrequencyType lockinPeriodFrequencyType = null;
        final Integer lockinPeriodFrequencyTypeValue = command.integerValueOfParameterNamed(lockinPeriodFrequencyTypeParamName);
        if (lockinPeriodFrequencyTypeValue != null) {
            lockinPeriodFrequencyType = SavingsPeriodFrequencyType.fromInt(lockinPeriodFrequencyTypeValue);
        }

        boolean iswithdrawalFeeApplicableForTransfer = false;
        if (command.parameterExists(withdrawalFeeForTransfersParamName)) {
            iswithdrawalFeeApplicableForTransfer = command.booleanPrimitiveValueOfParameterNamed(withdrawalFeeForTransfersParamName);
        }

        final AccountingRuleType accountingRuleType = AccountingRuleType.fromInt(command.integerValueOfParameterNamed("accountingRule"));

        CodeValue productGroup = null;
        if(command.parameterExists(productGroupIdParamName)){
            Long productGroupId = command.longValueOfParameterNamed(productGroupIdParamName);
            productGroup = this.codeValueRepository.findOneWithNotFoundDetection(productGroupId);
        }

        // Savings product charges
        final Set<Charge> charges = assembleListOfSavingsProductCharges(command, currencyCode);

        final Set<ApplyChargesToExistingSavingsAccount> applyChargesToExistingSavingsAccounts = assembleApplyChargesToExistingSavingsAccount(command);

        boolean allowOverdraft = false;
        if (command.parameterExists(allowOverdraftParamName)) {
            allowOverdraft = command.booleanPrimitiveValueOfParameterNamed(allowOverdraftParamName);
        }

        BigDecimal overdraftLimit = BigDecimal.ZERO;
        if(command.parameterExists(overdraftLimitParamName)){
            overdraftLimit = command.bigDecimalValueOfParameterNamed(overdraftLimitParamName);
        }

        BigDecimal nominalAnnualInterestRateOverdraft = BigDecimal.ZERO;
        if(command.parameterExists(nominalAnnualInterestRateOverdraftParamName)){
        	nominalAnnualInterestRateOverdraft = command.bigDecimalValueOfParameterNamed(nominalAnnualInterestRateOverdraftParamName);
        }
        
        BigDecimal minOverdraftForInterestCalculation = BigDecimal.ZERO;
        if(command.parameterExists(minOverdraftForInterestCalculationParamName)){
        	minOverdraftForInterestCalculation = command.bigDecimalValueOfParameterNamed(minOverdraftForInterestCalculationParamName);
        }

        boolean enforceMinRequiredBalance = false;
        if (command.parameterExists(enforceMinRequiredBalanceParamName)) {
            enforceMinRequiredBalance = command.booleanPrimitiveValueOfParameterNamed(enforceMinRequiredBalanceParamName);
        }

        BigDecimal minRequiredBalance = BigDecimal.ZERO;
        if(command.parameterExists(minRequiredBalanceParamName)){
            minRequiredBalance = command.bigDecimalValueOfParameterNamed(minRequiredBalanceParamName);
        }
        final BigDecimal minBalanceForInterestCalculation = command
                .bigDecimalValueOfParameterNamedDefaultToNullIfZero(minBalanceForInterestCalculationParamName);

        Set<SavingsProductInterestRateChart> savingsProductInterestRateChart = null;

        if(command.parameterExists(interestRateCharts)){
            savingsProductInterestRateChart= this.assembleSetOfInterestRateCharts(command);
        }

        return SavingsProduct.createNew(name, shortName, description, currency, interestRate, productGroup, interestCompoundingPeriodType,
                interestPostingPeriodType, interestCalculationType, interestCalculationDaysInYearType, minRequiredOpeningBalance,
                lockinPeriodFrequency, lockinPeriodFrequencyType, iswithdrawalFeeApplicableForTransfer, accountingRuleType, charges,
                allowOverdraft, overdraftLimit, enforceMinRequiredBalance, minRequiredBalance, minBalanceForInterestCalculation,
                nominalAnnualInterestRateOverdraft, minOverdraftForInterestCalculation,savingsProductInterestRateChart,applyChargesToExistingSavingsAccounts);
    }

    public Set<ApplyChargesToExistingSavingsAccount> assembleApplyChargesToExistingSavingsAccount(final JsonCommand command){

        final Set<ApplyChargesToExistingSavingsAccount> applyChargesToExistingSavingsAccounts = new HashSet<>();

        if (command.parameterExists(chargesParamName)) {
            final JsonArray chargesArray = command.arrayOfParameterNamed(chargesParamName);
            if (chargesArray != null) {
                for (int i = 0; i < chargesArray.size(); i++) {

                    final JsonObject jsonObject = chargesArray.get(i).getAsJsonObject();
                    if (jsonObject.has(idParamName)) {
                        final Long id = jsonObject.get(idParamName).getAsLong();
                        if(jsonObject.has(addProductChargeToExistingAccountsParamName)){
                            final boolean addChargeToExistingSavingsAccount = jsonObject.get(addProductChargeToExistingAccountsParamName).getAsBoolean();
                            final Charge charge = this.chargeRepository.findOneWithNotFoundDetection(id);
                            if(addChargeToExistingSavingsAccount){
                                if(charge.isAnnualFee() || charge.isMonthlyFee() || charge.isWithdrawalFee()){
                                    ApplyChargesToExistingSavingsAccount applyChargesToExistingSavingsAccount = new ApplyChargesToExistingSavingsAccount(null,charge,addChargeToExistingSavingsAccount);
                                    applyChargesToExistingSavingsAccounts.add(applyChargesToExistingSavingsAccount);
                                }
                            }
                        }
                    }
                }
            }
        }
        return applyChargesToExistingSavingsAccounts;

    }

    public Set<Charge> assembleListOfSavingsProductCharges(final JsonCommand command, final String savingsProductCurrencyCode) {

        final Set<Charge> charges = new HashSet<>();

        if (command.parameterExists(chargesParamName)) {
            final JsonArray chargesArray = command.arrayOfParameterNamed(chargesParamName);
            if (chargesArray != null) {
                for (int i = 0; i < chargesArray.size(); i++) {

                    final JsonObject jsonObject = chargesArray.get(i).getAsJsonObject();
                    if (jsonObject.has(idParamName)) {
                        final Long id = jsonObject.get(idParamName).getAsLong();

                        final Charge charge = this.chargeRepository.findOneWithNotFoundDetection(id);

                        if (!charge.isSavingsCharge()) {
                            final String errorMessage = "Charge with identifier " + charge.getId()
                                    + " cannot be applied to Savings product.";
                            throw new ChargeCannotBeAppliedToException("savings.product", errorMessage, charge.getId());
                        }

                        if (!savingsProductCurrencyCode.equals(charge.getCurrencyCode())) {
                            final String errorMessage = "Charge and Savings Product must have the same currency.";
                            throw new InvalidCurrencyException("charge", "attach.to.savings.product", errorMessage);
                        }
                        charges.add(charge);
                    }
                }
            }
        }

        return charges;
    }

    public Set<SavingsProductInterestRateChart> assembleSetOfInterestRateCharts(final JsonCommand command){

        final Set<SavingsProductInterestRateChart> savingsProductInterestRateChart = new HashSet<>();

        if(command.parameterExists(interestRateCharts)){
            final JsonArray interestRateChartArray = command.arrayOfParameterNamed(interestRateCharts);
            if(interestRateChartArray != null){
                for (int i = 0; i <interestRateChartArray.size(); i++) {
                    final JsonObject interestRateChartElement = interestRateChartArray.get(i).getAsJsonObject();
                    final Locale locale = this.fromApiJsonHelper.extractLocaleParameter(interestRateChartElement);
                    SavingsProductInterestRateChart interestRateChart = this.assembleFrom(interestRateChartElement,locale);
                    savingsProductInterestRateChart.add(interestRateChart);
                }
            }
        }

        return savingsProductInterestRateChart;

    }

    public SavingsProductInterestRateChart assembleFrom(final JsonElement element, Locale locale) {

        final String name = this.fromApiJsonHelper.extractStringNamed(nameParamName, element);

        String description = "";

        if(this.fromApiJsonHelper.parameterExists(descriptionParamName, element)){
            description = this.fromApiJsonHelper.extractStringNamed(descriptionParamName, element);
        }
        final LocalDate fromDate = this.fromApiJsonHelper.extractLocalDateNamed(fromDateParamName, element);
        final LocalDate endDate = this.fromApiJsonHelper.extractLocalDateNamed(endDateParamName, element);

        final BigDecimal annualInterestRate = this.fromApiJsonHelper.extractBigDecimalNamed(annualInterestRateParamName,element,locale);

        boolean applyToExistingSavings = false;

        if(this.fromApiJsonHelper.parameterExists(applyToExistingSavingsAccountParamName,element)){
            applyToExistingSavings = this.fromApiJsonHelper.extractBooleanNamed(applyToExistingSavingsAccountParamName,element);
        }

        final SavingsProductInterestRateChart savingsProductInterestRateChart =  SavingsProductInterestRateChart.createNew(null,name,description,fromDate,endDate,annualInterestRate,applyToExistingSavings);

        return savingsProductInterestRateChart;
    }
}