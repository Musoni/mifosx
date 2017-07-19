/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.portfolio.savings.domain;

import java.math.BigDecimal;
import java.util.*;

import javax.persistence.*;

import com.google.gson.JsonObject;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.mifosplatform.accounting.common.AccountingRuleType;
import org.mifosplatform.infrastructure.codes.domain.CodeValue;
import org.mifosplatform.infrastructure.core.api.JsonCommand;
import org.mifosplatform.infrastructure.core.data.ApiParameterError;
import org.mifosplatform.infrastructure.core.data.DataValidatorBuilder;
import org.mifosplatform.infrastructure.core.domain.LocalDateInterval;
import org.mifosplatform.infrastructure.core.exception.PlatformApiDataValidationException;
import org.mifosplatform.organisation.monetary.domain.MonetaryCurrency;
import org.mifosplatform.organisation.monetary.domain.Money;
import org.mifosplatform.portfolio.charge.domain.Charge;
import org.mifosplatform.portfolio.interestratechart.domain.InterestRateChart;
import org.mifosplatform.portfolio.savings.*;
import org.springframework.data.jpa.domain.AbstractPersistable;

import com.google.gson.JsonArray;

import static org.mifosplatform.portfolio.interestratechart.InterestRateChartApiConstants.idParamName;
import static org.mifosplatform.portfolio.savings.SavingsApiConstants.*;

@Entity
@Table(name = "m_savings_product", uniqueConstraints = { @UniqueConstraint(columnNames = { "name" }, name = "sp_unq_name"),
        @UniqueConstraint(columnNames = { "short_name" }, name = "sp_unq_short_name") })
@Inheritance
@DiscriminatorColumn(name = "deposit_type_enum", discriminatorType = DiscriminatorType.INTEGER)
@DiscriminatorValue("100")
public class SavingsProduct extends AbstractPersistable<Long> {

    @Column(name = "name", nullable = false, unique = true)
    protected String name;

    @Column(name = "short_name", nullable = false, unique = true)
    protected String shortName;

    @Column(name = "description", length = 500, nullable = false)
    protected String description;

    @Embedded
    protected MonetaryCurrency currency;

    @Column(name = "nominal_annual_interest_rate", scale = 6, precision = 19, nullable = false)
    protected BigDecimal nominalAnnualInterestRate;

    /**
     * The interest period is the span of time at the end of which savings in a
     * client's account earn interest.
     * 
     * A value from the {@link SavingsCompoundingInterestPeriodType}
     * enumeration.
     */
    @Column(name = "interest_compounding_period_enum", nullable = false)
    protected Integer interestCompoundingPeriodType;

    /**
     * A value from the {@link SavingsPostingInterestPeriodType} enumeration.
     */
    @Column(name = "interest_posting_period_enum", nullable = false)
    protected Integer interestPostingPeriodType;

    /**
     * A value from the {@link SavingsInterestCalculationType} enumeration.
     */
    @Column(name = "interest_calculation_type_enum", nullable = false)
    protected Integer interestCalculationType;

    /**
     * A value from the {@link SavingsInterestCalculationDaysInYearType}
     * enumeration.
     */
    @Column(name = "interest_calculation_days_in_year_type_enum", nullable = false)
    protected Integer interestCalculationDaysInYearType;

    @Column(name = "min_required_opening_balance", scale = 6, precision = 19, nullable = true)
    protected BigDecimal minRequiredOpeningBalance;

    @Column(name = "lockin_period_frequency", nullable = true)
    protected Integer lockinPeriodFrequency;

    @Column(name = "lockin_period_frequency_enum", nullable = true)
    protected Integer lockinPeriodFrequencyType;

    /**
     * A value from the {@link AccountingRuleType} enumeration.
     */
    @Column(name = "accounting_type", nullable = false)
    protected Integer accountingRule;

    @Column(name = "withdrawal_fee_for_transfer")
    protected boolean withdrawalFeeApplicableForTransfer;

    @ManyToMany
    @JoinTable(name = "m_savings_product_charge", joinColumns = @JoinColumn(name = "savings_product_id") , inverseJoinColumns = @JoinColumn(name = "charge_id") )
    protected Set<Charge> charges;

    @Column(name = "allow_overdraft")
    private boolean allowOverdraft;

    @Column(name = "overdraft_limit", scale = 6, precision = 19, nullable = true)
    private BigDecimal overdraftLimit;

	@Column(name = "nominal_annual_interest_rate_overdraft", scale = 6, precision = 19, nullable = true)
    private BigDecimal nominalAnnualInterestRateOverdraft;

    @Column(name = "min_overdraft_for_interest_calculation", scale = 6, precision = 19, nullable = true)
    private BigDecimal minOverdraftForInterestCalculation;
    
    @Column(name = "enforce_min_required_balance")
    private boolean enforceMinRequiredBalance;

    @Column(name = "min_required_balance", scale = 6, precision = 19, nullable = true)
    private BigDecimal minRequiredBalance;

    @Column(name = "min_balance_for_interest_calculation", scale = 6, precision = 19, nullable = true)
    private BigDecimal minBalanceForInterestCalculation;

    @Column(name = "start_date", nullable = true)
    @Temporal(TemporalType.DATE)
    private Date startDate;

    @Column(name = "close_date", nullable = true)
    @Temporal(TemporalType.DATE)
    private Date closeDate;

    @ManyToOne
    @JoinColumn(name = "product_group")
    private CodeValue productGroup;

    @LazyCollection(LazyCollectionOption.FALSE)
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "savingsProduct", orphanRemoval = true)
    private Set<SavingsProductInterestRateChart> savingsProductInterestRateCharts = new HashSet<>();

    @LazyCollection(LazyCollectionOption.FALSE)
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "savingsProduct", orphanRemoval = true)
    private Set<ApplyChargesToExistingSavingsAccount> applyChargesToExistingSavingsAccounts = new HashSet<>();

    public static SavingsProduct createNew(final String name, final String shortName, final String description,
            final MonetaryCurrency currency, final BigDecimal interestRate, final CodeValue productGroup,
            final SavingsCompoundingInterestPeriodType interestCompoundingPeriodType,
            final SavingsPostingInterestPeriodType interestPostingPeriodType, final SavingsInterestCalculationType interestCalculationType,
            final SavingsInterestCalculationDaysInYearType interestCalculationDaysInYearType, final BigDecimal minRequiredOpeningBalance,
            final Integer lockinPeriodFrequency, final SavingsPeriodFrequencyType lockinPeriodFrequencyType,
            final boolean withdrawalFeeApplicableForTransfer, final AccountingRuleType accountingRuleType, final Set<Charge> charges,
            final boolean allowOverdraft, final BigDecimal overdraftLimit, final boolean enforceMinRequiredBalance,
            final BigDecimal minRequiredBalance, final BigDecimal minBalanceForInterestCalculation,
            final BigDecimal nominalAnnualInterestRateOverdraft, final BigDecimal minOverdraftForInterestCalculation,final Set<SavingsProductInterestRateChart> savingsProductInterestRateCharts,
            final Set<ApplyChargesToExistingSavingsAccount> applyChargesToExistingSavingsAccounts) {

        return new SavingsProduct(name, shortName, description, currency, interestRate, interestCompoundingPeriodType,
                interestPostingPeriodType, interestCalculationType, interestCalculationDaysInYearType, minRequiredOpeningBalance,
                lockinPeriodFrequency, lockinPeriodFrequencyType, withdrawalFeeApplicableForTransfer, accountingRuleType, charges,
                allowOverdraft, overdraftLimit, enforceMinRequiredBalance, minRequiredBalance, minBalanceForInterestCalculation, 
                nominalAnnualInterestRateOverdraft, minOverdraftForInterestCalculation, null, null, productGroup,savingsProductInterestRateCharts,applyChargesToExistingSavingsAccounts);
    }

    protected SavingsProduct() {
        this.name = null;
        this.description = null;
    }

    protected SavingsProduct(final String name, final String shortName, final String description, final MonetaryCurrency currency,
            final BigDecimal interestRate, final SavingsCompoundingInterestPeriodType interestCompoundingPeriodType,
            final SavingsPostingInterestPeriodType interestPostingPeriodType, final SavingsInterestCalculationType interestCalculationType,
            final SavingsInterestCalculationDaysInYearType interestCalculationDaysInYearType, final BigDecimal minRequiredOpeningBalance,
            final Integer lockinPeriodFrequency, final SavingsPeriodFrequencyType lockinPeriodFrequencyType,
            final boolean withdrawalFeeApplicableForTransfer, final AccountingRuleType accountingRuleType, final Set<Charge> charges,
            final boolean allowOverdraft, final BigDecimal overdraftLimit, BigDecimal minBalanceForInterestCalculation,
            final CodeValue productGroup,final Set<SavingsProductInterestRateChart> savingsProductInterestRateCharts,
            final Set<ApplyChargesToExistingSavingsAccount> applyChargesToExistingSavingsAccounts) {
        this(name, shortName, description, currency, interestRate, interestCompoundingPeriodType, interestPostingPeriodType,
                interestCalculationType, interestCalculationDaysInYearType, minRequiredOpeningBalance, lockinPeriodFrequency,
                lockinPeriodFrequencyType, withdrawalFeeApplicableForTransfer, accountingRuleType, charges, allowOverdraft, overdraftLimit,
                false, null, minBalanceForInterestCalculation, null, null, null, null, productGroup, savingsProductInterestRateCharts,applyChargesToExistingSavingsAccounts);
    }

    protected SavingsProduct(final String name, final String shortName, final String description, final MonetaryCurrency currency,
            final BigDecimal interestRate, final SavingsCompoundingInterestPeriodType interestCompoundingPeriodType,
            final SavingsPostingInterestPeriodType interestPostingPeriodType, final SavingsInterestCalculationType interestCalculationType,
            final SavingsInterestCalculationDaysInYearType interestCalculationDaysInYearType, final BigDecimal minRequiredOpeningBalance,
            final Integer lockinPeriodFrequency, final SavingsPeriodFrequencyType lockinPeriodFrequencyType,
            final boolean withdrawalFeeApplicableForTransfer, final AccountingRuleType accountingRuleType, final Set<Charge> charges,
            final boolean allowOverdraft, final BigDecimal overdraftLimit, final boolean enforceMinRequiredBalance,
            final BigDecimal minRequiredBalance, BigDecimal minBalanceForInterestCalculation,
            final BigDecimal nominalAnnualInterestRateOverdraft, final BigDecimal minOverdraftForInterestCalculation,
            final LocalDate startDate, final LocalDate closeDate, final CodeValue productGroup,final Set<SavingsProductInterestRateChart> savingsProductInterestRateCharts,
            final Set<ApplyChargesToExistingSavingsAccount> applyChargesToExistingSavingsAccounts) {

        this.name = name;
        this.shortName = shortName;
        this.description = description;

        this.currency = currency;
        this.nominalAnnualInterestRate = interestRate;
        this.interestCompoundingPeriodType = interestCompoundingPeriodType.getValue();
        this.interestPostingPeriodType = interestPostingPeriodType.getValue();
        this.interestCalculationType = interestCalculationType.getValue();
        this.interestCalculationDaysInYearType = interestCalculationDaysInYearType.getValue();

        if (minRequiredOpeningBalance != null) {
            this.minRequiredOpeningBalance = Money.of(currency, minRequiredOpeningBalance).getAmount();
        }

        this.lockinPeriodFrequency = lockinPeriodFrequency;
        if (lockinPeriodFrequency != null && lockinPeriodFrequencyType != null) {
            this.lockinPeriodFrequencyType = lockinPeriodFrequencyType.getValue();
        }

        this.withdrawalFeeApplicableForTransfer = withdrawalFeeApplicableForTransfer;

        if (accountingRuleType != null) {
            this.accountingRule = accountingRuleType.getValue();
        }

        if (charges != null) {
            this.charges = charges;
        }

        validateLockinDetails();
        this.allowOverdraft = allowOverdraft;
        this.overdraftLimit = overdraftLimit;
        this.nominalAnnualInterestRateOverdraft = nominalAnnualInterestRateOverdraft;
        this.minOverdraftForInterestCalculation = minOverdraftForInterestCalculation;

        esnureOverdraftLimitsSetForOverdraftAccounts();

        this.enforceMinRequiredBalance = enforceMinRequiredBalance;
        this.minRequiredBalance = minRequiredBalance;
        this.minBalanceForInterestCalculation = minBalanceForInterestCalculation;
        this.startDate = startDate != null ? startDate.toDateTimeAtStartOfDay().toDate() : null;
        this.closeDate = closeDate != null ? closeDate.toDateTimeAtCurrentTime().toDate() : null;
        this.productGroup = productGroup;
        updateSavingsProductInterestRateCharts(savingsProductInterestRateCharts);

        if(applyChargesToExistingSavingsAccounts != null){
            this.applyChargesToExistingSavingsAccounts = associateApplyChargeToExistingSavingsAccountWith(applyChargesToExistingSavingsAccounts);
        }
    }

    /**
     * If overdrafts are allowed and the overdraft limit is not set, set the
     * same to Zero
     **/
    private void esnureOverdraftLimitsSetForOverdraftAccounts() {

        if (this.allowOverdraft) {
            this.overdraftLimit = this.overdraftLimit == null? BigDecimal.ZERO : this.overdraftLimit;
            this.nominalAnnualInterestRateOverdraft = this.nominalAnnualInterestRateOverdraft == null? BigDecimal.ZERO : this.nominalAnnualInterestRateOverdraft;
            this.minOverdraftForInterestCalculation = this.minOverdraftForInterestCalculation == null? BigDecimal.ZERO : this.minOverdraftForInterestCalculation;
        }
    }

    public MonetaryCurrency currency() {
        return this.currency.copy();
    }

    public BigDecimal nominalAnnualInterestRate() {
        return this.nominalAnnualInterestRate;
    }

    public SavingsCompoundingInterestPeriodType interestCompoundingPeriodType() {
        return SavingsCompoundingInterestPeriodType.fromInt(this.interestCompoundingPeriodType);
    }

    public SavingsPostingInterestPeriodType interestPostingPeriodType() {
        return SavingsPostingInterestPeriodType.fromInt(this.interestPostingPeriodType);
    }

    public SavingsInterestCalculationType interestCalculationType() {
        return SavingsInterestCalculationType.fromInt(this.interestCalculationType);
    }

    public SavingsInterestCalculationDaysInYearType interestCalculationDaysInYearType() {
        return SavingsInterestCalculationDaysInYearType.fromInt(this.interestCalculationDaysInYearType);
    }

    public BigDecimal minRequiredOpeningBalance() {
        return this.minRequiredOpeningBalance;
    }

    public Integer lockinPeriodFrequency() {
        return this.lockinPeriodFrequency;
    }

    public SavingsPeriodFrequencyType lockinPeriodFrequencyType() {
        SavingsPeriodFrequencyType type = null;
        if (this.lockinPeriodFrequencyType != null) {
            type = SavingsPeriodFrequencyType.fromInt(this.lockinPeriodFrequencyType);
        }
        return type;
    }

    public Map<String, Object> update(final JsonCommand command) {

        final Map<String, Object> actualChanges = new LinkedHashMap<>(10);

        final String localeAsInput = command.locale();

        if (command.isChangeInStringParameterNamed(nameParamName, this.name)) {
            final String newValue = command.stringValueOfParameterNamed(nameParamName);
            actualChanges.put(nameParamName, newValue);
            this.name = newValue;
        }

        if (command.isChangeInStringParameterNamed(shortNameParamName, this.name)) {
            final String newValue = command.stringValueOfParameterNamed(shortNameParamName);
            actualChanges.put(shortNameParamName, newValue);
            this.shortName = newValue;
        }

        if (command.isChangeInStringParameterNamed(descriptionParamName, this.description)) {
            final String newValue = command.stringValueOfParameterNamed(descriptionParamName);
            actualChanges.put(descriptionParamName, newValue);
            this.description = newValue;
        }

        Integer digitsAfterDecimal = this.currency.getDigitsAfterDecimal();
        if (command.isChangeInIntegerParameterNamed(digitsAfterDecimalParamName, digitsAfterDecimal)) {
            final Integer newValue = command.integerValueOfParameterNamed(digitsAfterDecimalParamName);
            actualChanges.put(digitsAfterDecimalParamName, newValue);
            actualChanges.put(localeParamName, localeAsInput);
            digitsAfterDecimal = newValue;
            this.currency = new MonetaryCurrency(this.currency.getCode(), digitsAfterDecimal, this.currency.getCurrencyInMultiplesOf());
        }

        String currencyCode = this.currency.getCode();
        if (command.isChangeInStringParameterNamed(currencyCodeParamName, currencyCode)) {
            final String newValue = command.stringValueOfParameterNamed(currencyCodeParamName);
            actualChanges.put(currencyCodeParamName, newValue);
            currencyCode = newValue;
            this.currency = new MonetaryCurrency(currencyCode, this.currency.getDigitsAfterDecimal(),
                    this.currency.getCurrencyInMultiplesOf());
        }

        Integer inMultiplesOf = this.currency.getCurrencyInMultiplesOf();
        if (command.isChangeInIntegerParameterNamed(inMultiplesOfParamName, inMultiplesOf)) {
            final Integer newValue = command.integerValueOfParameterNamed(inMultiplesOfParamName);
            actualChanges.put(inMultiplesOfParamName, newValue);
            actualChanges.put(localeParamName, localeAsInput);
            inMultiplesOf = newValue;
            this.currency = new MonetaryCurrency(this.currency.getCode(), this.currency.getDigitsAfterDecimal(), inMultiplesOf);
        }

        if (command.isChangeInBigDecimalParameterNamed(nominalAnnualInterestRateParamName, this.nominalAnnualInterestRate)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed(nominalAnnualInterestRateParamName);
            actualChanges.put(nominalAnnualInterestRateParamName, newValue);
            actualChanges.put(localeParamName, localeAsInput);
            this.nominalAnnualInterestRate = newValue;
        }

        if (command.isChangeInIntegerParameterNamed(interestCompoundingPeriodTypeParamName, this.interestCompoundingPeriodType)) {
            final Integer newValue = command.integerValueOfParameterNamed(interestCompoundingPeriodTypeParamName);
            actualChanges.put(interestCompoundingPeriodTypeParamName, newValue);
            this.interestCompoundingPeriodType = SavingsCompoundingInterestPeriodType.fromInt(newValue).getValue();
        }

        if (command.isChangeInIntegerParameterNamed(interestPostingPeriodTypeParamName, this.interestPostingPeriodType)) {
            final Integer newValue = command.integerValueOfParameterNamed(interestPostingPeriodTypeParamName);
            actualChanges.put(interestPostingPeriodTypeParamName, newValue);
            this.interestPostingPeriodType = SavingsPostingInterestPeriodType.fromInt(newValue).getValue();
        }

        if (command.isChangeInIntegerParameterNamed(interestCalculationTypeParamName, this.interestCalculationType)) {
            final Integer newValue = command.integerValueOfParameterNamed(interestCalculationTypeParamName);
            actualChanges.put(interestCalculationTypeParamName, newValue);
            this.interestCalculationType = SavingsInterestCalculationType.fromInt(newValue).getValue();
        }

        if (command.isChangeInIntegerParameterNamed(interestCalculationDaysInYearTypeParamName, this.interestCalculationDaysInYearType)) {
            final Integer newValue = command.integerValueOfParameterNamed(interestCalculationDaysInYearTypeParamName);
            actualChanges.put(interestCalculationDaysInYearTypeParamName, newValue);
            this.interestCalculationDaysInYearType = SavingsInterestCalculationDaysInYearType.fromInt(newValue).getValue();
        }

        if (command.isChangeInBigDecimalParameterNamedDefaultingZeroToNull(minRequiredOpeningBalanceParamName,
                this.minRequiredOpeningBalance)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamedDefaultToNullIfZero(minRequiredOpeningBalanceParamName);
            actualChanges.put(minRequiredOpeningBalanceParamName, newValue);
            actualChanges.put(localeParamName, localeAsInput);
            this.minRequiredOpeningBalance = newValue;
        }

        if (command.isChangeInIntegerParameterNamedDefaultingZeroToNull(lockinPeriodFrequencyParamName, this.lockinPeriodFrequency)) {
            final Integer newValue = command.integerValueOfParameterNamedDefaultToNullIfZero(lockinPeriodFrequencyParamName);
            actualChanges.put(lockinPeriodFrequencyParamName, newValue);
            actualChanges.put(localeParamName, localeAsInput);
            this.lockinPeriodFrequency = newValue;
        }

        if (command.isChangeInIntegerParameterNamed(lockinPeriodFrequencyTypeParamName, this.lockinPeriodFrequencyType)) {
            final Integer newValue = command.integerValueOfParameterNamed(lockinPeriodFrequencyTypeParamName);
            actualChanges.put(lockinPeriodFrequencyTypeParamName, newValue);
            this.lockinPeriodFrequencyType = newValue != null ? SavingsPeriodFrequencyType.fromInt(newValue).getValue() : newValue;
        }

        // set period type to null if frequency is null
        if (this.lockinPeriodFrequency == null) {
            this.lockinPeriodFrequencyType = null;
        }

        if (command.isChangeInBooleanParameterNamed(withdrawalFeeForTransfersParamName, this.withdrawalFeeApplicableForTransfer)) {
            final boolean newValue = command.booleanPrimitiveValueOfParameterNamed(withdrawalFeeForTransfersParamName);
            actualChanges.put(withdrawalFeeForTransfersParamName, newValue);
            this.withdrawalFeeApplicableForTransfer = newValue;
        }

        if (command.isChangeInIntegerParameterNamed(accountingRuleParamName, this.accountingRule)) {
            final Integer newValue = command.integerValueOfParameterNamed(accountingRuleParamName);
            actualChanges.put(accountingRuleParamName, newValue);
            this.accountingRule = newValue;
        }

        if(this.productGroup != null && command.isChangeInLongParameterNamed(productGroupIdParamName, this.productGroup.getId())){
            final Long newValue = command.longValueOfParameterNamed(productGroupIdParamName);
            actualChanges.put(productGroupIdParamName, newValue);
        }

        // charges
        if (command.hasParameter(chargesParamName)) {
            final JsonArray jsonArray = command.arrayOfParameterNamed(chargesParamName);
            if (jsonArray != null) {
                actualChanges.put(chargesParamName, command.jsonFragment(chargesParamName));
            }
        }

        if (command.isChangeInBooleanParameterNamed(allowOverdraftParamName, this.allowOverdraft)) {
            final boolean newValue = command.booleanPrimitiveValueOfParameterNamed(allowOverdraftParamName);
            actualChanges.put(allowOverdraftParamName, newValue);
            this.allowOverdraft = newValue;
        }

        if (command.isChangeInBigDecimalParameterNamedDefaultingZeroToNull(overdraftLimitParamName, this.overdraftLimit)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamedDefaultToNullIfZero(overdraftLimitParamName);
            actualChanges.put(overdraftLimitParamName, newValue);
            actualChanges.put(localeParamName, localeAsInput);
            this.overdraftLimit = newValue;
        }

        if (command.isChangeInBigDecimalParameterNamedDefaultingZeroToNull(nominalAnnualInterestRateOverdraftParamName, this.nominalAnnualInterestRateOverdraft)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamedDefaultToNullIfZero(nominalAnnualInterestRateOverdraftParamName);
            actualChanges.put(nominalAnnualInterestRateOverdraftParamName, newValue);
            actualChanges.put(localeParamName, localeAsInput);
            this.nominalAnnualInterestRateOverdraft = newValue;
        }
        
        if (command.isChangeInBigDecimalParameterNamedDefaultingZeroToNull(minOverdraftForInterestCalculationParamName, this.minOverdraftForInterestCalculation)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamedDefaultToNullIfZero(minOverdraftForInterestCalculationParamName);
            actualChanges.put(minOverdraftForInterestCalculationParamName, newValue);
            actualChanges.put(localeParamName, localeAsInput);
            this.minOverdraftForInterestCalculation = newValue;
        }
        
        if (!this.allowOverdraft) {
            this.overdraftLimit = null;
            this.nominalAnnualInterestRateOverdraft = null;
            this.minOverdraftForInterestCalculation = null;
        }

        if (command.isChangeInBooleanParameterNamed(enforceMinRequiredBalanceParamName, this.enforceMinRequiredBalance)) {
            final boolean newValue = command.booleanPrimitiveValueOfParameterNamed(enforceMinRequiredBalanceParamName);
            actualChanges.put(enforceMinRequiredBalanceParamName, newValue);
            this.enforceMinRequiredBalance = newValue;
        }

        if (command.isChangeInBigDecimalParameterNamedDefaultingZeroToNull(minRequiredBalanceParamName, this.minRequiredBalance)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamedDefaultToNullIfZero(minRequiredBalanceParamName);
            actualChanges.put(minRequiredBalanceParamName, newValue);
            actualChanges.put(localeParamName, localeAsInput);
            this.minRequiredBalance = newValue;
        }

        if (command.isChangeInBigDecimalParameterNamedDefaultingZeroToNull(minBalanceForInterestCalculationParamName,
                this.minBalanceForInterestCalculation)) {
            final BigDecimal newValue = command
                    .bigDecimalValueOfParameterNamedDefaultToNullIfZero(minBalanceForInterestCalculationParamName);
            actualChanges.put(minBalanceForInterestCalculationParamName, newValue);
            actualChanges.put(localeParamName, localeAsInput);
            this.minBalanceForInterestCalculation = newValue;
        }
        final String dateFormatAsInput = command.dateFormat();

        final String localeParamName = "locale";
        final String dateFormatParamName = "dateFormat";

        final String startDateParamName = "startDate";
        if (command.isChangeInLocalDateParameterNamed(startDateParamName, getStartDate())) {
            final String valueAsInput = command.stringValueOfParameterNamed(startDateParamName);
            actualChanges.put(startDateParamName, valueAsInput);
            actualChanges.put(dateFormatParamName, dateFormatAsInput);
            actualChanges.put(localeParamName, localeAsInput);

            final LocalDate newValue = command.localDateValueOfParameterNamed(startDateParamName);
            if (newValue != null) {
                this.startDate = newValue.toDate();
            } else {
                this.startDate = null;
            }
        }

        final String closeDateParamName = "closeDate";
        if (command.isChangeInLocalDateParameterNamed(closeDateParamName, getCloseDate())) {
            final String valueAsInput = command.stringValueOfParameterNamed(closeDateParamName);
            actualChanges.put(closeDateParamName, valueAsInput);
            actualChanges.put(dateFormatParamName, dateFormatAsInput);
            actualChanges.put(localeParamName, localeAsInput);

            final LocalDate newValue = command.localDateValueOfParameterNamed(closeDateParamName);
            if (newValue != null) {
                this.closeDate = newValue.toDate();
            } else {
                this.closeDate = null;
            }
        }

        validateLockinDetails();
        esnureOverdraftLimitsSetForOverdraftAccounts();

        return actualChanges;
    }

    public void updateInterestCharts(Map<String, Object> changes,final JsonCommand command) {
        final JsonArray interestRateChartArray = command.arrayOfParameterNamed(interestRateCharts);
        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();

        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(SAVINGS_PRODUCT_RESOURCE_NAME);

        if(interestRateChartArray != null){
            for (int i = 0; i <interestRateChartArray.size(); i++) {
                final JsonObject interestRateChartElement = interestRateChartArray.get(i).getAsJsonObject();
                JsonCommand chartCommand = JsonCommand.fromExistingCommand(command, interestRateChartElement);
                if(interestRateChartElement.has(idParamName)){
                    final Long id = interestRateChartElement.get(idParamName).getAsLong();
                    final SavingsProductInterestRateChart chart = this.findProductInterestRateChart(id);
                    if(chart == null) {
                        baseDataValidator.parameter(idParamName).value(id).failWithCode("no.chart.associated.with.id");
                    } else{
                        chart.update(chartCommand,changes,baseDataValidator);
                    }
                }else{
                    //for newly added interest charts to products
                    final SavingsProductInterestRateChart savingsProductInterestRateChart = SavingsProductInterestRateChart.createNewFromJson(this,chartCommand);
                    this.savingsProductInterestRateCharts.add(savingsProductInterestRateChart);

                }
            }
        }
        this.validateProductInterestRate(baseDataValidator);
        if (!dataValidationErrors.isEmpty()) { throw new PlatformApiDataValidationException(dataValidationErrors); }

    }

    public void validateProductInterestRate(DataValidatorBuilder baseDataValidator){
        final Set<SavingsProductInterestRateChart> charts = this.savingsProductInterestRateCharts;
        if(charts.size() > 0 && !charts.isEmpty()){
            for(final SavingsProductInterestRateChart chart: charts){
                this.validateProductInterestRateCharts(baseDataValidator,chart);
            }
        }
    }

    public void validateProductInterestRateCharts(final DataValidatorBuilder baseDataValidator, final SavingsProductInterestRateChart comparingInterestRateChart){
        final Set<SavingsProductInterestRateChart> productInterestRateCharts = this.savingsProductInterestRateCharts;
        if(productInterestRateCharts.size() > 0 && !productInterestRateCharts.isEmpty()){
            for(final SavingsProductInterestRateChart existingInterestRateChart : productInterestRateCharts){
                if(!existingInterestRateChart.equals(comparingInterestRateChart)){
                    if(existingInterestRateChart.getChartFields().isOverlapping(comparingInterestRateChart.getChartFields())){
                        baseDataValidator.failWithCodeNoParameterAddedToErrorCode("chart.overlapping.from.and.end.dates",
                                existingInterestRateChart.getChartFields().getFromDateAsLocalDate(),existingInterestRateChart.getChartFields().getEndDateAsLocalDate(),
                                comparingInterestRateChart.getChartFields().getFromDateAsLocalDate(), comparingInterestRateChart.getChartFields().getEndDateAsLocalDate());
                    }
                }
            }
        }
    }




    private void validateLockinDetails() {

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(SAVINGS_PRODUCT_RESOURCE_NAME);

        if (this.lockinPeriodFrequency == null) {
            baseDataValidator.reset().parameter(lockinPeriodFrequencyTypeParamName).value(this.lockinPeriodFrequencyType).ignoreIfNull()
                    .inMinMaxRange(0, 3);

            if (this.lockinPeriodFrequencyType != null) {
                baseDataValidator.reset().parameter(lockinPeriodFrequencyParamName).value(this.lockinPeriodFrequency).notNull()
                        .integerZeroOrGreater();
            }
        } else {
            baseDataValidator.reset().parameter(lockinPeriodFrequencyParamName).value(this.lockinPeriodFrequencyType)
                    .integerZeroOrGreater();
            baseDataValidator.reset().parameter(lockinPeriodFrequencyTypeParamName).value(this.lockinPeriodFrequencyType).notNull()
                    .inMinMaxRange(0, 3);
        }

        if (!dataValidationErrors.isEmpty()) { throw new PlatformApiDataValidationException(dataValidationErrors); }
    }

    public boolean isCashBasedAccountingEnabled() {
        return AccountingRuleType.CASH_BASED.getValue().equals(this.accountingRule);
    }

    // TODO this entire block is currently unnecessary as Savings does not have
    // accrual accounting
    public boolean isAccrualBasedAccountingEnabled() {
        return isUpfrontAccrualAccounting() || isPeriodicAccrualAccounting();
    }

    public boolean isPeriodicAccrualAccounting() {
        return AccountingRuleType.ACCRUAL_PERIODIC.getValue().equals(this.accountingRule);
    }

    public boolean isUpfrontAccrualAccounting() {
        return AccountingRuleType.ACCRUAL_UPFRONT.getValue().equals(this.accountingRule);
    }

    public Integer getAccountingType() {
        return this.accountingRule;
    }

    public boolean update(final Set<Charge> newSavingsProductCharges) {
        if (newSavingsProductCharges == null) { return false; }

        boolean updated = false;
        if (this.charges != null) {
            final Set<Charge> currentSetOfCharges = new HashSet<>(this.charges);
            final Set<Charge> newSetOfCharges = new HashSet<>(newSavingsProductCharges);

            if (!(currentSetOfCharges.equals(newSetOfCharges))) {
                updated = true;
                this.charges = newSavingsProductCharges;
            }
        } else {
            updated = true;
            this.charges = newSavingsProductCharges;
        }
        return updated;
    }

    public void update(final CodeValue productGroup){
        this.productGroup = productGroup;
    }

    public BigDecimal overdraftLimit() {
        return this.overdraftLimit;
    }

    public boolean isWithdrawalFeeApplicableForTransfer() {
        return this.withdrawalFeeApplicableForTransfer;
    }

    public boolean isAllowOverdraft() {
        return this.allowOverdraft;
    }

    public BigDecimal minRequiredBalance() {
        return this.minRequiredBalance;
    }

    public boolean isMinRequiredBalanceEnforced() {
        return this.enforceMinRequiredBalance;
    }

    public CodeValue getProductGroup(){ return this.productGroup; }

    public Set<Charge> charges() {
        return this.charges;
    }

    public InterestRateChart applicableChart(@SuppressWarnings("unused") final LocalDate target) {
        return null;
    }

    public InterestRateChart findChart(@SuppressWarnings("unused") Long chartId) {
        return null;
    }

    public BigDecimal minBalanceForInterestCalculation() {
        return this.minBalanceForInterestCalculation;
    }

    public String getShortName() {
        return this.shortName;
    }

    public BigDecimal nominalAnnualInterestRateOverdraft() {
		return this.nominalAnnualInterestRateOverdraft;
	}

	public BigDecimal minOverdraftForInterestCalculation() {
		return this.minOverdraftForInterestCalculation;
	}

    public LocalDate getStartDate() {
        LocalDate startLocalDate = null;
        if (this.startDate != null) {
            startLocalDate = LocalDate.fromDateFields(this.startDate);
        }
        return startLocalDate;
    }

    public LocalDate getCloseDate() {
        LocalDate closeLocalDate = null;
        if (this.closeDate != null) {
            closeLocalDate = LocalDate.fromDateFields(this.closeDate);
        }
        return closeLocalDate;
    }

    public Set<ApplyChargesToExistingSavingsAccount> associateApplyChargeToExistingSavingsAccountWith(final Set<ApplyChargesToExistingSavingsAccount> applyChargesToExistingSavingsAccounts){
        for(final ApplyChargesToExistingSavingsAccount applyTo : applyChargesToExistingSavingsAccounts){
            applyTo.updateSavingsProduct(this);
        }
        return applyChargesToExistingSavingsAccounts;
    }

    public void updateSavingsProductInterestRateCharts(Set<SavingsProductInterestRateChart> savingsProductInterestRateCharts) {
        if(savingsProductInterestRateCharts != null){
            for(SavingsProductInterestRateChart interestRateChart : savingsProductInterestRateCharts){
                interestRateChart.updateSavingsProduct(this);
            }
            this.savingsProductInterestRateCharts = savingsProductInterestRateCharts;
        }

    }

    public void updateApplyChargesToExistingSavingsAccount(final Set<ApplyChargesToExistingSavingsAccount> applyChargesToExistingSavingsAccounts){
       

        if(this.applyChargesToExistingSavingsAccounts == null){
            this.applyChargesToExistingSavingsAccounts = this.associateApplyChargeToExistingSavingsAccountWith(applyChargesToExistingSavingsAccounts);
        }else{
            //check existing a new if its exist leave else remove or add
            final List<Long> chargeIds = new ArrayList<>();
            for(ApplyChargesToExistingSavingsAccount  toBeAdded : applyChargesToExistingSavingsAccounts){
                chargeIds.add(toBeAdded.getProductCharge().getId());
                if(!this.findByCharge(toBeAdded.getProductCharge().getId())){
                    toBeAdded.updateSavingsProduct(this);
                    this.applyChargesToExistingSavingsAccounts.add(toBeAdded);
                }

            }

            //has apply in Db but not in newly sent remove it
            List<ApplyChargesToExistingSavingsAccount> idsToRemove = new ArrayList<>();
            for(final ApplyChargesToExistingSavingsAccount existingApplyCharges : this.applyChargesToExistingSavingsAccounts){
                if(!chargeIds.contains(existingApplyCharges.getProductCharge().getId())){
                    idsToRemove.add(existingApplyCharges);
                }
            }
            if(idsToRemove.size() > 0){
                this.applyChargesToExistingSavingsAccounts.removeAll(idsToRemove);
            }



        }

    }

    private boolean findByCharge(final Long chargeId){
         boolean found =false;
        if(this.applyChargesToExistingSavingsAccounts !=null){
            for(final ApplyChargesToExistingSavingsAccount account : this.applyChargesToExistingSavingsAccounts){
                if(account.getProductCharge().getId().equals(chargeId)){
                    found= true;
                    break;
                }
            }
        }
        return found;
    }

    public SavingsProductInterestRateChart findProductInterestRateChart(final Long id){

        SavingsProductInterestRateChart savingsProductInterestRateChart = null;

        for(final SavingsProductInterestRateChart chart : this.savingsProductInterestRateCharts){
            if(chart.getId().equals(id)){
                savingsProductInterestRateChart = chart;
            }
        }
        return savingsProductInterestRateChart;
    }

    public SavingsProductInterestRateChart findCurrentInterestRate(){
        SavingsProductInterestRateChart savingsProductInterestRateChart = null;
        if(this.savingsProductInterestRateCharts != null){
            for(final SavingsProductInterestRateChart interestRateChart : this.savingsProductInterestRateCharts){
                final LocalDate currentDate = new LocalDate();
                final LocalDateInterval thatInterval = LocalDateInterval.create(interestRateChart.getChartFields().getFromDate(), interestRateChart.getChartFields().getEndDate());
                if(thatInterval.contains(currentDate)){
                    savingsProductInterestRateChart = interestRateChart;
                    break;
                }
            }
        }
        return savingsProductInterestRateChart;
    }

    public SavingsProductInterestRateChart findInterestRateFromDateRange(final LocalDate startDate, final LocalDate endDate) {
        SavingsProductInterestRateChart savingsProductInterestRateChart = null;
        if(this.savingsProductInterestRateCharts != null){
            for(final SavingsProductInterestRateChart interestRateChart : this.savingsProductInterestRateCharts){
                final LocalDateInterval thisInterval = LocalDateInterval.create(startDate, endDate);
                final LocalDateInterval thatInterval = LocalDateInterval.create(interestRateChart.getChartFields().getFromDate(), interestRateChart.getChartFields().getEndDate());
                if(thatInterval.contains(endDate)){
                    savingsProductInterestRateChart = interestRateChart;
                    break;
                }
            }
        }

        return savingsProductInterestRateChart;
    }

    public Set<SavingsProductInterestRateChart> getSavingsProductInterestRateCharts() {
        return this.savingsProductInterestRateCharts;
    }



}