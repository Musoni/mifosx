/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.reportmailingjob.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;
import org.joda.time.LocalDate;
import org.mifosplatform.infrastructure.core.api.JsonCommand;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResult;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResultBuilder;
import org.mifosplatform.infrastructure.core.domain.MifosPlatformTenant;
import org.mifosplatform.infrastructure.core.exception.PlatformDataIntegrityException;
import org.mifosplatform.infrastructure.core.service.DateUtils;
import org.mifosplatform.infrastructure.core.service.ThreadLocalContextUtil;
import org.mifosplatform.infrastructure.dataqueries.domain.Report;
import org.mifosplatform.infrastructure.dataqueries.domain.ReportRepositoryWrapper;
import org.mifosplatform.infrastructure.dataqueries.service.ReadReportingService;
import org.mifosplatform.infrastructure.documentmanagement.contentrepository.FileSystemContentRepository;
import org.mifosplatform.infrastructure.jobs.annotation.CronTarget;
import org.mifosplatform.infrastructure.jobs.exception.JobExecutionException;
import org.mifosplatform.infrastructure.jobs.service.JobName;
import org.mifosplatform.infrastructure.reportmailingjob.ReportMailingJobConstants;
import org.mifosplatform.infrastructure.reportmailingjob.data.ReportMailingJobEmailData;
import org.mifosplatform.infrastructure.reportmailingjob.data.ReportMailingJobValidator;
import org.mifosplatform.infrastructure.reportmailingjob.domain.ReportMailingJob;
import org.mifosplatform.infrastructure.reportmailingjob.domain.ReportMailingJobEmailAttachmentFileFormat;
import org.mifosplatform.infrastructure.reportmailingjob.domain.ReportMailingJobPreviousRunStatus;
import org.mifosplatform.infrastructure.reportmailingjob.domain.ReportMailingJobRepository;
import org.mifosplatform.infrastructure.reportmailingjob.domain.ReportMailingJobRepositoryWrapper;
import org.mifosplatform.infrastructure.reportmailingjob.domain.ReportMailingJobRunHistory;
import org.mifosplatform.infrastructure.reportmailingjob.domain.ReportMailingJobRunHistoryRepository;
import org.mifosplatform.infrastructure.reportmailingjob.domain.ReportMailingJobStretchyReportParamDateOption;
import org.mifosplatform.infrastructure.reportmailingjob.helper.IPv4Helper;
import org.mifosplatform.infrastructure.reportmailingjob.helper.ReportMailingJobStretchyReportDateHelper;
import org.mifosplatform.infrastructure.security.service.BasicAuthTenantDetailsService;
import org.mifosplatform.infrastructure.security.service.PlatformSecurityContext;
import org.mifosplatform.portfolio.calendar.service.CalendarUtils;
import org.mifosplatform.useradministration.domain.AppUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportMailingJobWritePlatformServiceImpl implements ReportMailingJobWritePlatformService {
    
    private final static Logger logger = LoggerFactory.getLogger(ReportMailingJobWritePlatformServiceImpl.class);
    private final ReportRepositoryWrapper reportRepositoryWrapper;
    private final ReportMailingJobValidator reportMailingJobValidator;
    private final ReportMailingJobRepositoryWrapper reportMailingJobRepositoryWrapper;
    private final ReportMailingJobRepository reportMailingJobRepository;
    private final PlatformSecurityContext platformSecurityContext;
    private final ReportMailingJobEmailService reportMailingJobEmailService;
    private final ReadReportingService readReportingService;
    private final ReportMailingJobRunHistoryRepository reportMailingJobRunHistoryRepository;
    private final BasicAuthTenantDetailsService basicAuthTenantDetailsService;
    
    @Autowired
    public ReportMailingJobWritePlatformServiceImpl(final ReportRepositoryWrapper reportRepositoryWrapper, 
            final ReportMailingJobValidator reportMailingJobValidator, 
            final ReportMailingJobRepositoryWrapper reportMailingJobRepositoryWrapper, 
            final ReportMailingJobRepository reportMailingJobRepository, 
            final PlatformSecurityContext platformSecurityContext, 
            final ReportMailingJobEmailService reportMailingJobEmailService,  
            final ReadReportingService readReportingService, 
            final ReportMailingJobRunHistoryRepository reportMailingJobRunHistoryRepository, 
            final BasicAuthTenantDetailsService basicAuthTenantDetailsService) {
        this.reportRepositoryWrapper = reportRepositoryWrapper;
        this.reportMailingJobValidator = reportMailingJobValidator;
        this.reportMailingJobRepositoryWrapper = reportMailingJobRepositoryWrapper;
        this.reportMailingJobRepository = reportMailingJobRepositoryWrapper.getReportMailingJobRepository();
        this.platformSecurityContext = platformSecurityContext;
        this.reportMailingJobEmailService = reportMailingJobEmailService;
        this.readReportingService = readReportingService;
        this.reportMailingJobRunHistoryRepository = reportMailingJobRunHistoryRepository;
        this.basicAuthTenantDetailsService = basicAuthTenantDetailsService;
    }

    @Override
    @Transactional
    public CommandProcessingResult createReportMailingJob(JsonCommand jsonCommand) {
        try {
            // validate the create request
            this.reportMailingJobValidator.validateCreateRequest(jsonCommand);
            
            final AppUser appUser = this.platformSecurityContext.authenticatedUser();
            
            // get the stretchy Report object
            final Report stretchyReport = this.reportRepositoryWrapper.findOneThrowExceptionIfNotFound(jsonCommand.longValueOfParameterNamed(
                    ReportMailingJobConstants.STRETCHY_REPORT_ID_PARAM_NAME));
            
            // create an instance of ReportMailingJob class from the JsonCommand object
            final ReportMailingJob reportMailingJob = ReportMailingJob.instance(jsonCommand, stretchyReport, new LocalDate(), appUser, appUser);
            
            // save entity
            this.reportMailingJobRepository.save(reportMailingJob);
            
            return new CommandProcessingResultBuilder().withCommandId(jsonCommand.commandId()).
                    withEntityId(reportMailingJob.getId()).build();
        }
        
        catch (final DataIntegrityViolationException dve) {
            handleDataIntegrityIssues(jsonCommand, dve);
            
            return CommandProcessingResult.empty();
        }
    }

    @Override
    @Transactional
    public CommandProcessingResult updateReportMailingJob(Long reportMailingJobId, JsonCommand jsonCommand) {
        try {
            // validate the update request
            this.reportMailingJobValidator.validateUpdateRequest(jsonCommand);
            
            // retrieve the ReportMailingJob object from the database
            final ReportMailingJob reportMailingJob = this.reportMailingJobRepositoryWrapper.findOneThrowExceptionIfNotFound(reportMailingJobId);
            
            final Map<String, Object> changes = reportMailingJob.update(jsonCommand);
            
            // get the recurrence rule string
            final String recurrence = reportMailingJob.getRecurrence();
            
            // get the next run DateTime from the ReportMailingJob entity
            DateTime nextRunDateTime = reportMailingJob.getNextRunDateTime();
            
            // get the start DateTime from the ReportMailingJob entity
            DateTime startDateTime = reportMailingJob.getStartDateTime();
            
            if (!changes.isEmpty()) {
                // check if the stretchy report id was updated
                if (changes.containsKey(ReportMailingJobConstants.STRETCHY_REPORT_ID_PARAM_NAME)) {
                    final Long stretchyReportId = (Long) changes.get(ReportMailingJobConstants.STRETCHY_REPORT_ID_PARAM_NAME);
                    final Report stretchyReport = this.reportRepositoryWrapper.findOneThrowExceptionIfNotFound(stretchyReportId);
                    
                    // update the stretchy report
                    reportMailingJob.update(stretchyReport);
                }
                
                // check if the recurrence was updated
                if (changes.containsKey(ReportMailingJobConstants.RECURRENCE_PARAM_NAME)) {
                    
                    // go ahead if the recurrence is not null
                    if (StringUtils.isNotBlank(recurrence)) {
                        // check if the start DateTime was updated
                        if (changes.containsKey(ReportMailingJobConstants.START_DATE_TIME_PARAM_NAME)) {
                            // get the updated start DateTime
                            startDateTime = reportMailingJob.getStartDateTime();
                        }
                        
                        // get the next recurring DateTime
                        final DateTime nextRecurringDateTime = this.createNextRecurringDateTime(recurrence, startDateTime);
                        
                        // update the next run time property
                        reportMailingJob.updateNextRunDateTime(nextRecurringDateTime);
                    }
                    
                    // check if the next run DateTime is not empty and the recurrence is empty
                    else if (StringUtils.isBlank(recurrence) && (nextRunDateTime != null)) {
                        // the next run DateTime should be set to null
                        reportMailingJob.updateNextRunDateTime(null);
                    }
                }
                
                if (changes.containsKey(ReportMailingJobConstants.START_DATE_TIME_PARAM_NAME)) {
                    startDateTime = reportMailingJob.getStartDateTime();
                    
                    // initially set the next recurring date time to the new start date time
                    DateTime nextRecurringDateTime = startDateTime;
                    
                    // ensure that the recurrence pattern string is not empty
                    if (StringUtils.isNotBlank(recurrence)) {
                        // get the next recurring DateTime
                        nextRecurringDateTime = this.createNextRecurringDateTime(recurrence, startDateTime);
                    }
                    
                    // update the next run time property
                    reportMailingJob.updateNextRunDateTime(nextRecurringDateTime);
                }
                
                // save and flush immediately so any data integrity exception can be handled in the "catch" block
                this.reportMailingJobRepository.saveAndFlush(reportMailingJob);
            }
            
            return new CommandProcessingResultBuilder().
                    withCommandId(jsonCommand.commandId()).
                    withEntityId(reportMailingJob.getId()).
                    with(changes).
                    build();
        }
        
        catch (final DataIntegrityViolationException dve) {
            handleDataIntegrityIssues(jsonCommand, dve);
            
            return CommandProcessingResult.empty();
        }
    }

    @Override
    @Transactional
    public CommandProcessingResult deleteReportMailingJob(Long reportMailingJobId) {
        // retrieve the ReportMailingJob object from the database
        final ReportMailingJob reportMailingJob = this.reportMailingJobRepositoryWrapper.findOneThrowExceptionIfNotFound(reportMailingJobId);
        
        // delete the report mailing job by setting the isDeleted property to 1 and altering the name
        reportMailingJob.delete();
        
        // save the report mailing job entity
        this.reportMailingJobRepository.save(reportMailingJob);
        
        return new CommandProcessingResultBuilder().withEntityId(reportMailingJobId).build();
    }
    
    /**
     * Switches the database to the slave read only database
     */
    private void switchToReadOnlyDatabase() {
        ThreadLocalContextUtil.switchToPrimaryDataSource();
        
        final String tenantIdentifier = ThreadLocalContextUtil.getTenant().getTenantIdentifier();
        final MifosPlatformTenant platformTenant = this.basicAuthTenantDetailsService.loadTenantById(
                tenantIdentifier, true);
        
        ThreadLocalContextUtil.switchToTenantSpecificDataSource(platformTenant);
    }
    
    /**
     * Switches the database to the master write database
     */
    private void switchToWriteDatabase() {
        ThreadLocalContextUtil.switchToPrimaryDataSource();
        
        final String tenantIdentifier = ThreadLocalContextUtil.getTenant().getTenantIdentifier();
        final MifosPlatformTenant platformTenant = this.basicAuthTenantDetailsService.loadTenantById(
                tenantIdentifier, false);
        
        ThreadLocalContextUtil.switchToTenantSpecificDataSource(platformTenant);
    }
    
    @Override
    @CronTarget(jobName = JobName.EXECUTE_REPORT_MAILING_JOBS)
    public void executeReportMailingJobs() throws JobExecutionException {
        if (IPv4Helper.applicationIsNotRunningOnLocalMachine()) {
            final Collection<ReportMailingJob> reportMailingJobCollection = this.reportMailingJobRepository.findAll();
            
            for (ReportMailingJob reportMailingJob : reportMailingJobCollection) {
                // get the tenant's date as a DateTime object
                final DateTime localDateTimeOftenant = DateUtils.getLocalDateTimeOfTenant().toDateTime();
                
                final DateTime nextRunDateTime = reportMailingJob.getNextRunDateTime();
                
                if (nextRunDateTime != null && reportMailingJob.isActive() && reportMailingJob.isNotDeleted()) {
                    
                    if (nextRunDateTime.isBefore(localDateTimeOftenant) || nextRunDateTime.isEqual(localDateTimeOftenant)) {
                        // get the emailAttachmentFileFormat enum object
                        final ReportMailingJobEmailAttachmentFileFormat emailAttachmentFileFormat = ReportMailingJobEmailAttachmentFileFormat.
                                instance(reportMailingJob.getEmailAttachmentFileFormat());
                        
                        if (emailAttachmentFileFormat != null && Arrays.asList(ReportMailingJobEmailAttachmentFileFormat.validValues()).
                                contains(emailAttachmentFileFormat.getId())) {
                            final Report stretchyReport = reportMailingJob.getStretchyReport();
                            final String reportName = (stretchyReport != null) ? stretchyReport.getReportName() : null;
                            final StringBuilder errorLog = new StringBuilder();
                            final Map<String, String> validateStretchyReportParamMap = this.reportMailingJobValidator.
                                    validateStretchyReportParamMap(reportMailingJob.getStretchyReportParamMap());
                            Map<String, String> reportParams = new HashMap<>();
                            
                            if (validateStretchyReportParamMap != null) {
                                Iterator<Map.Entry<String, String>> validateStretchyReportParamMapEntries = validateStretchyReportParamMap.entrySet().iterator();
                                
                                while (validateStretchyReportParamMapEntries.hasNext()) {
                                    Map.Entry<String, String> validateStretchyReportParamMapEntry = validateStretchyReportParamMapEntries.next();
                                    String key = validateStretchyReportParamMapEntry.getKey();
                                    String value = validateStretchyReportParamMapEntry.getValue();
                                    Object[] stretchyReportParamDateOptionsList = ReportMailingJobStretchyReportParamDateOption.validValues();
                                    
                                    if (StringUtils.containsIgnoreCase(key, "date")) {
                                        if (Arrays.asList(stretchyReportParamDateOptionsList).contains(value)) {
                                            ReportMailingJobStretchyReportParamDateOption enumOption = ReportMailingJobStretchyReportParamDateOption.instance(value);
                                            
                                            value = ReportMailingJobStretchyReportDateHelper.getDateAsString(enumOption);
                                        }
                                    }
                                    
                                    reportParams.put(key, value);
                                }
                            }
                            
                            // generate the pentaho report output stream, method in turn call another that sends the file to the email recipients
                            this.generatePentahoReportOutputStream(reportMailingJob, emailAttachmentFileFormat, reportParams, reportName, errorLog);
                            
                            // TODO - write a helper method to handle the generation of the pentaho report file
                            this.updateReportMailingJobAfterJobExecution(reportMailingJob, errorLog, localDateTimeOftenant);
                        }
                    }
                }
            }
        }
    }
    
    /** 
     * update the report mailing job entity after job execution 
     * 
     * @param reportMailingJob -- the report mailing job entity
     * @param errorLog -- StringBuilder object containing the error log if any
     * @param jobStartDateTime -- the start DateTime of the job
     * @return None
     **/
    private void updateReportMailingJobAfterJobExecution(final ReportMailingJob reportMailingJob, final StringBuilder errorLog, 
            final DateTime jobStartDateTime) {
        this.switchToWriteDatabase();
        
        final String recurrence = reportMailingJob.getRecurrence();
        final DateTime startDateTime = reportMailingJob.getStartDateTime();
        ReportMailingJobPreviousRunStatus reportMailingJobPreviousRunStatus = ReportMailingJobPreviousRunStatus.SUCCESS;
        
        reportMailingJob.updatePreviousRunErrorLog(null);
        
        if (errorLog != null && errorLog.length() > 0) {
            reportMailingJobPreviousRunStatus = ReportMailingJobPreviousRunStatus.ERROR;
            reportMailingJob.updatePreviousRunErrorLog(errorLog.toString());
        }
        
        reportMailingJob.increaseNumberOfRunsByOne();
        reportMailingJob.updatePreviousRunStatus(reportMailingJobPreviousRunStatus.getValue());
        reportMailingJob.updatePreviousRunDateTime(reportMailingJob.getNextRunDateTime());
        
        // check if the job has a recurrence pattern, if not deactivate the job. The job will only run once
        if (StringUtils.isEmpty(recurrence)) {
            // deactivate job
            reportMailingJob.deactivate();
            
            // job will only run once, no next run time
            reportMailingJob.updateNextRunDateTime(null);
        }
        
        else if (startDateTime != null) {
            final DateTime nextRecurringDateTime = this.createNextRecurringDateTime(recurrence, startDateTime);
            
            // finally update the next run date time property
            reportMailingJob.updateNextRunDateTime(nextRecurringDateTime);
        }
        
        // save the ReportMailingJob entity
        this.reportMailingJobRepository.save(reportMailingJob);
        
        // create a new report mailing job run history entity
        this.createReportMailingJobRunHistroryAfterJobExecution(reportMailingJob, errorLog, jobStartDateTime, 
                reportMailingJobPreviousRunStatus.getValue());
    }
    
    /**
     * create the next recurring DateTime from recurrence pattern, start DateTime and current DateTime
     * 
     * @param recurrencePattern
     * @param startDateTime
     * @return DateTime object
     */
    private DateTime createNextRecurringDateTime(final String recurrencePattern, final DateTime startDateTime) {
        DateTime nextRecurringDateTime = null;
        
        // the recurrence pattern/rule cannot be empty
        if (StringUtils.isNotBlank(recurrencePattern) && startDateTime != null) {
            final LocalDate currentDate = DateUtils.getLocalDateTimeOfTenant().toLocalDate();
            // use the previous day as the start date for calculating the next run date
            final LocalDate nextRecurringLocalDate = CalendarUtils.getNextRecurringDate(recurrencePattern, startDateTime.toLocalDate(), 
                    currentDate.minusDays(1));
            final int currentYearIntValue = nextRecurringLocalDate.get(DateTimeFieldType.year());
            final int currentMonthIntValue = nextRecurringLocalDate.get(DateTimeFieldType.monthOfYear());
            final int currentDayIntValue = nextRecurringLocalDate.get(DateTimeFieldType.dayOfMonth());
            
            nextRecurringDateTime = startDateTime.withDate(currentYearIntValue, currentMonthIntValue, currentDayIntValue);
        }
        
        return nextRecurringDateTime;
    }
    
    /** 
     * create a new report mailing job run history entity after job execution
     * 
     * @param reportMailingJob -- the report mailing job entity
     * @param errorLog -- StringBuilder object containing the error log if any
     * @param jobStartDateTime -- the start DateTime of the job
     * @param jobRunStatus -- the status of the job (success/error)
     * @return None
     **/
    private void createReportMailingJobRunHistroryAfterJobExecution(final ReportMailingJob reportMailingJob, final StringBuilder errorLog, 
            final DateTime jobStartDateTime, final String jobRunStatus) {
        final DateTime jobEndDateTime = DateUtils.getLocalDateTimeOfTenant().toDateTime();
        final String errorLogToString = (errorLog != null) ? errorLog.toString() : null;
        final ReportMailingJobRunHistory reportMailingJobRunHistory = ReportMailingJobRunHistory.instance(reportMailingJob, jobStartDateTime, 
                jobEndDateTime, jobRunStatus, null, errorLogToString);
        
        this.reportMailingJobRunHistoryRepository.save(reportMailingJobRunHistory);
    }

    /** 
     * Handle any SQL data integrity issue 
     *
     * @param jsonCommand -- JsonCommand object
     * @param dve -- data integrity exception object
     * @return None
     **/
    private void handleDataIntegrityIssues(final JsonCommand jsonCommand, final DataIntegrityViolationException dve) {
        final Throwable realCause = dve.getMostSpecificCause();
        
        if (realCause.getMessage().contains(ReportMailingJobConstants.NAME_PARAM_NAME)) {
            final String name = jsonCommand.stringValueOfParameterNamed(ReportMailingJobConstants.NAME_PARAM_NAME);
            throw new PlatformDataIntegrityException("error.msg.report.mailing.job.duplicate.name", "Report mailing job with name `" + name + "` already exists",
                    ReportMailingJobConstants.NAME_PARAM_NAME, name);
        }

        logger.error(dve.getMessage(), dve);
        
        throw new PlatformDataIntegrityException("error.msg.charge.unknown.data.integrity.issue",
                "Unknown data integrity issue with resource: " + realCause.getMessage());
    }
    
    /** 
     * generate the Pentaho report output stream
     * 
     * @return StringBuilder object -- the error log StringBuilder object
     **/
    private StringBuilder generatePentahoReportOutputStream(final ReportMailingJob reportMailingJob, final ReportMailingJobEmailAttachmentFileFormat emailAttachmentFileFormat, 
            final Map<String, String> reportParams, final String reportName, final StringBuilder errorLog) {
        
        try {
            this.switchToReadOnlyDatabase();
            
            final ByteArrayOutputStream byteArrayOutputStream = this.readReportingService.generatePentahoReportAsOutputStream(reportName, 
                    emailAttachmentFileFormat.getValue(), reportParams, null, reportMailingJob.getRunAsUser(), errorLog);
            final MifosPlatformTenant mifosPlatformTenant = ThreadLocalContextUtil.getTenant();
            final String fileLocation = FileSystemContentRepository.MIFOSX_BASE_DIR + File.separator + 
            		mifosPlatformTenant.getTenantIdentifier() + File.separator + "reportmailingjob";
            final String fileNameWithoutExtension = fileLocation + File.separator + reportName;
            
            // check if file directory exists, if not create directory
            if (!new File(fileLocation).isDirectory()) {
                new File(fileLocation).mkdirs();
            }
            
            if (byteArrayOutputStream.size() == 0) {
                errorLog.append("Pentaho report processing failed, empty output stream created");
            }
            
            else if (errorLog.length() == 0 && (byteArrayOutputStream.size() > 0)) {
                final String fileName = fileNameWithoutExtension + "." + emailAttachmentFileFormat.getValue();
                
                // send the file to email recipients
                this.sendPentahoReportFileToEmailRecipients(reportMailingJob, fileName, byteArrayOutputStream, errorLog);
            }
        }
        
        catch (Exception e) {
            // do nothing for now
            errorLog.append(e);
        }
        
        return errorLog;
    }
    
    /** 
     * send Pentaho report file to email recipients
     * 
     * @return None
     **/
    private void sendPentahoReportFileToEmailRecipients(final ReportMailingJob reportMailingJob, final String fileName, 
            final ByteArrayOutputStream byteArrayOutputStream, final StringBuilder errorLog) {
        final Set<String> emailRecipients = this.reportMailingJobValidator.validateEmailRecipients(reportMailingJob.getEmailRecipients());
        
        try {
            final File file = new File(fileName);
            final FileOutputStream outputStream = new FileOutputStream(file);
            byteArrayOutputStream.writeTo(outputStream);
            
            for (String emailRecipient : emailRecipients) {
                final ReportMailingJobEmailData reportMailingJobEmailData = new ReportMailingJobEmailData(emailRecipient, 
                        reportMailingJob.getEmailMessage(), reportMailingJob.getEmailSubject(), file);
                
                this.reportMailingJobEmailService.sendEmailWithAttachment(reportMailingJobEmailData);
            }
            
        } catch (IOException e) {
            errorLog.append("The ReportMailingJobWritePlatformServiceImpl.executeReportMailingJobs threw an IOException "
                    + "exception: " + e.getMessage() + " ---------- ");
        }
    }
}
