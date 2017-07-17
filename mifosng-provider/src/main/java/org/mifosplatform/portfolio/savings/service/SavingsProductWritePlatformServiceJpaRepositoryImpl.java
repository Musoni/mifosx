/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.portfolio.savings.service;

import static org.mifosplatform.portfolio.savings.SavingsApiConstants.accountingRuleParamName;
import static org.mifosplatform.portfolio.savings.SavingsApiConstants.chargesParamName;
import static org.mifosplatform.portfolio.savings.SavingsApiConstants.interestRateCharts;

import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.mifosplatform.accounting.producttoaccountmapping.service.ProductToGLAccountMappingWritePlatformService;
import org.mifosplatform.infrastructure.core.api.JsonCommand;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResult;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResultBuilder;
import org.mifosplatform.infrastructure.core.domain.Tenant;
import org.mifosplatform.infrastructure.core.exception.AbstractPlatformDomainRuleException;
import org.mifosplatform.infrastructure.core.exception.PlatformApiDataValidationException;
import org.mifosplatform.infrastructure.core.exception.PlatformDataIntegrityException;
import org.mifosplatform.infrastructure.entityaccess.domain.MifosEntityAccessType;
import org.mifosplatform.infrastructure.entityaccess.domain.MifosEntityType;
import org.mifosplatform.infrastructure.entityaccess.service.MifosEntityAccessUtil;
import org.mifosplatform.infrastructure.jobs.annotation.CronTarget;
import org.mifosplatform.infrastructure.jobs.exception.JobExecutionException;
import org.mifosplatform.infrastructure.jobs.service.JobName;
import org.mifosplatform.infrastructure.security.service.PlatformSecurityContext;
import org.mifosplatform.portfolio.charge.domain.Charge;
import org.mifosplatform.portfolio.charge.domain.ChargeCalculationType;
import org.mifosplatform.portfolio.charge.domain.ChargeTimeType;
import org.mifosplatform.portfolio.savings.DepositAccountType;
import org.mifosplatform.portfolio.savings.data.SavingsProductDataValidator;
import org.mifosplatform.portfolio.savings.domain.*;
import org.mifosplatform.portfolio.savings.exception.SavingsProductNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SavingsProductWritePlatformServiceJpaRepositoryImpl implements SavingsProductWritePlatformService {

    private final Logger logger;
    private final PlatformSecurityContext context;
    private final SavingsProductRepository savingProductRepository;
    private final SavingsProductDataValidator fromApiJsonDataValidator;
    private final SavingsProductAssembler savingsProductAssembler;
    private final ProductToGLAccountMappingWritePlatformService accountMappingWritePlatformService;
    private final MifosEntityAccessUtil mifosEntityAccessUtil;
    private final ApplyChargesToExistingSavingsAccountRepository applyChargesToExistingSavingsAccountRepository;
    private final SavingsAccountRepository savingsAccountRepository;

    @Autowired
    public SavingsProductWritePlatformServiceJpaRepositoryImpl(final PlatformSecurityContext context,
            final SavingsProductRepository savingProductRepository, final SavingsProductDataValidator fromApiJsonDataValidator,
            final SavingsProductAssembler savingsProductAssembler,
            final ProductToGLAccountMappingWritePlatformService accountMappingWritePlatformService,
            final MifosEntityAccessUtil mifosEntityAccessUtil,final ApplyChargesToExistingSavingsAccountRepository applyChargesToExistingSavingsAccountRepository,
            final SavingsAccountRepository savingsAccountRepository
            ) {
        this.context = context;
        this.savingProductRepository = savingProductRepository;
        this.fromApiJsonDataValidator = fromApiJsonDataValidator;
        this.savingsProductAssembler = savingsProductAssembler;
        this.logger = LoggerFactory.getLogger(SavingsProductWritePlatformServiceJpaRepositoryImpl.class);
        this.accountMappingWritePlatformService = accountMappingWritePlatformService;
        this.mifosEntityAccessUtil = mifosEntityAccessUtil;
        this.applyChargesToExistingSavingsAccountRepository = applyChargesToExistingSavingsAccountRepository;
        this.savingsAccountRepository = savingsAccountRepository;
    }

    /*
     * Guaranteed to throw an exception no matter what the data integrity issue
     * is.
     */
    private void handleDataIntegrityIssues(final JsonCommand command, final DataAccessException dae) {

        final Throwable realCause = dae.getMostSpecificCause();
        if (realCause.getMessage().contains("sp_unq_name")) {

            final String name = command.stringValueOfParameterNamed("name");
            throw new PlatformDataIntegrityException("error.msg.product.savings.duplicate.name", "Savings product with name `" + name
                    + "` already exists", "name", name);
        } else if (realCause.getMessage().contains("sp_unq_short_name")) {

            final String shortName = command.stringValueOfParameterNamed("shortName");
            throw new PlatformDataIntegrityException("error.msg.product.savings.duplicate.short.name", "Savings product with short name `"
                    + shortName + "` already exists", "shortName", shortName);
        }

        logAsErrorUnexpectedDataIntegrityException(dae);
        throw new PlatformDataIntegrityException("error.msg.savingsproduct.unknown.data.integrity.issue",
                "Unknown data integrity issue with resource.");
    }

    private void logAsErrorUnexpectedDataIntegrityException(final DataAccessException dae) {
        this.logger.error(dae.getMessage(), dae);
    }

    @Transactional
    @Override
    public CommandProcessingResult create(final JsonCommand command) {

        try {
            this.fromApiJsonDataValidator.validateForCreate(command.json());

            final SavingsProduct product = this.savingsProductAssembler.assemble(command);

            this.savingProductRepository.save(product);

            // save accounting mappings
            this.accountMappingWritePlatformService.createSavingProductToGLAccountMapping(product.getId(), command,
                    DepositAccountType.SAVINGS_DEPOSIT);
            
            // check if the office specific products are enabled. If yes, then save this savings product against a specific office
            // i.e. this savings product is specific for this office.
            mifosEntityAccessUtil.checkConfigurationAndAddProductResrictionsForUserOffice(
            		MifosEntityAccessType.OFFICE_ACCESS_TO_SAVINGS_PRODUCTS, 
            		MifosEntityType.SAVINGS_PRODUCT, 
            		product.getId());

            return new CommandProcessingResultBuilder() //
                    .withEntityId(product.getId()) //
                    .build();
        } catch (final DataAccessException e) {
            handleDataIntegrityIssues(command, e);
            return CommandProcessingResult.empty();
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult update(final Long productId, final JsonCommand command) {

        try {
            this.context.authenticatedUser();
            this.fromApiJsonDataValidator.validateForUpdate(command.json());

            final SavingsProduct product = this.savingProductRepository.findOne(productId);
            if (product == null) { throw new SavingsProductNotFoundException(productId); }

            final Map<String, Object> changes = product.update(command);

            if(command.hasParameter(interestRateCharts)){
                product.updateInterestCharts(changes,command);
            }

            if (changes.containsKey(chargesParamName)) {
                final Set<Charge> savingsProductCharges = this.savingsProductAssembler.assembleListOfSavingsProductCharges(command, product
                        .currency().getCode());
                final boolean updated = product.update(savingsProductCharges);
                if (!updated) {
                    changes.remove(chargesParamName);
                }
                /* update ApplyChargesToExistingSavingsAccount  */

                final Set<ApplyChargesToExistingSavingsAccount> applyChargesToExistingSavingsAccounts = this.savingsProductAssembler.assembleApplyChargesToExistingSavingsAccount(command);
                product.updateApplyChargesToExistingSavingsAccount(applyChargesToExistingSavingsAccounts);

            }

            // accounting related changes
            final boolean accountingTypeChanged = changes.containsKey(accountingRuleParamName);
            final Map<String, Object> accountingMappingChanges = this.accountMappingWritePlatformService
                    .updateSavingsProductToGLAccountMapping(product.getId(), command, accountingTypeChanged, product.getAccountingType(),
                            DepositAccountType.SAVINGS_DEPOSIT);
            changes.putAll(accountingMappingChanges);

            if (!changes.isEmpty()) {
                this.savingProductRepository.saveAndFlush(product);
            }

            return new CommandProcessingResultBuilder() //
                    .withEntityId(product.getId()) //
                    .with(changes).build();
        } catch (final DataAccessException e) {
            handleDataIntegrityIssues(command, e);
            return CommandProcessingResult.empty();
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult delete(final Long productId) {

        this.context.authenticatedUser();
        final SavingsProduct product = this.savingProductRepository.findOne(productId);
        if (product == null) { throw new SavingsProductNotFoundException(productId); }

        this.savingProductRepository.delete(product);

        return new CommandProcessingResultBuilder() //
                .withEntityId(product.getId()) //
                .build();
    }

    /**
     * this function adds savings charges of type annual, monthly and withdrawals to an already existing savings accounts
     * when its set to on a product. functions is more like a bulk addSavingsCharge to savings accounts
     */
    @Override
    @CronTarget(jobName = JobName.APPLY_PRODUCT_CHARGE_TO_EXISTING_SAVINGS_ACCOUNT)
    public void applyChargeToExistingSavingsAccount() throws JobExecutionException {
        final StringBuilder sb = new StringBuilder();

        final Collection<ApplyChargesToExistingSavingsAccount> applyChargesToExistingSavingsAccounts = this.applyChargesToExistingSavingsAccountRepository.findAll();
        if(!applyChargesToExistingSavingsAccounts.isEmpty() && applyChargesToExistingSavingsAccounts.size() > 0){
            for(final ApplyChargesToExistingSavingsAccount applyCharge : applyChargesToExistingSavingsAccounts){
                if(applyCharge.isApplyChargeToExistingSavingsAccount()){
                    final List<SavingsAccount> activeSavingsAccountsWithoutCharge = this.savingsAccountRepository.savingsAccountWithoutCharge(applyCharge.getSavingsProduct().getId(),applyCharge.getProductCharge().getId());
                    for(final SavingsAccount account : activeSavingsAccountsWithoutCharge){
                        final Charge charge = applyCharge.getProductCharge();
                        final LocalDate currentDate =  LocalDate.now();
                        final Locale locale = new Locale("en");
                        final String format = "dd MMMM yyyy";
                        final DateTimeFormatter fmt = StringUtils.isNotBlank(format) ? DateTimeFormat.forPattern(format).withLocale(locale)
                                : DateTimeFormat.forPattern("dd MM yyyy");
                        final SavingsAccountCharge savingsAccountCharge = SavingsAccountCharge.createNewWithoutSavingsAccount(charge,charge.getAmount(), ChargeTimeType.fromInt(charge.getChargeTimeType()),
                                ChargeCalculationType.fromInt(charge.getChargeCalculation()),currentDate,true,charge.getFeeOnMonthDay(),charge.feeInterval());
                        savingsAccountCharge.update(account);
                        try{
                            //FIXMe add validation from the add charge to savings account method
                            account.addCharge(fmt,savingsAccountCharge,charge);
                            this.savingsAccountRepository.saveAndFlush(account);
                        }catch(PlatformApiDataValidationException e){
                            sb.append(e.getErrors().get(0).getDeveloperMessage() + " savings account with id  " + account.getId() + ",");
                        }catch(AbstractPlatformDomainRuleException e){
                            sb.append(e.getDefaultUserMessage()  + " savings account with id  " + account.getId() + ",");
                        }catch(RuntimeException e){
                            sb.append(e.toString()  + " savings account with id " + account.getId() + ",");
                        }catch(Exception e){
                            sb.append(e.getCause().getMessage() + " savings account with  id" + account.getId() + ",");
                        }
                    }
                }
            }
        }

        if (sb.length() > 0) { throw new JobExecutionException(sb.toString()); }
    }
}