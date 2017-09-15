/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.sms.scheduler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.*;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.lang3.StringUtils;
import org.mifosplatform.infrastructure.configuration.data.ExternalServicesPropertiesData;
import org.mifosplatform.infrastructure.configuration.service.ExternalServicesConstants;
import org.mifosplatform.infrastructure.configuration.service.ExternalServicesPropertiesReadPlatformService;
import org.mifosplatform.infrastructure.core.domain.MifosPlatformTenant;
import org.mifosplatform.infrastructure.core.service.ThreadLocalContextUtil;
import org.mifosplatform.infrastructure.jobs.annotation.CronTarget;
import org.mifosplatform.infrastructure.jobs.service.JobName;
import org.mifosplatform.infrastructure.reportmailingjob.data.ReportMailingJobValidator;
import org.mifosplatform.infrastructure.reportmailingjob.helper.IPv4Helper;
import org.mifosplatform.infrastructure.scheduledemail.data.EmailMessageWithAttachmentData;
import org.mifosplatform.infrastructure.scheduledemail.service.EmailMessageJobEmailService;
import org.mifosplatform.infrastructure.sms.data.SmsConfigurationData;
import org.mifosplatform.infrastructure.sms.data.SmsData;
import org.mifosplatform.infrastructure.sms.data.SmsMessageApiQueueResourceData;
import org.mifosplatform.infrastructure.sms.data.SmsMessageApiReportResourceData;
import org.mifosplatform.infrastructure.sms.data.SmsMessageApiResponseData;
import org.mifosplatform.infrastructure.sms.data.SmsMessageDeliveryReportData;
import org.mifosplatform.infrastructure.sms.data.TenantSmsConfiguration;
import org.mifosplatform.infrastructure.sms.domain.SmsConfiguration;
import org.mifosplatform.infrastructure.sms.domain.SmsConfigurationRepository;
import org.mifosplatform.infrastructure.sms.domain.SmsMessage;
import org.mifosplatform.infrastructure.sms.domain.SmsMessageRepository;
import org.mifosplatform.infrastructure.sms.domain.SmsMessageStatusType;
import org.mifosplatform.infrastructure.sms.service.SmsConfigurationReadPlatformService;
import org.mifosplatform.infrastructure.sms.service.SmsReadPlatformService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

/**
 * Scheduled job services that send SMS messages and get delivery reports for the sent SMS messages
 **/
@Service
public class SmsMessageScheduledJobServiceImpl implements SmsMessageScheduledJobService {
	
	private final SmsMessageRepository smsMessageRepository;
	private final SmsConfigurationRepository smsConfigurationRepository;
	private final SmsReadPlatformService smsReadPlatformService;
	private final SmsConfigurationReadPlatformService configurationReadPlatformService;
	private static final Logger logger = LoggerFactory.getLogger(SmsMessageScheduledJobServiceImpl.class);
	private final RestTemplate restTemplate = new RestTemplate();
	private final EmailMessageJobEmailService emailMessageJobEmailService;
	private final ExternalServicesPropertiesReadPlatformService externalServicePropertiesReadPlatformService;
	private final ReportMailingJobValidator reportMailingJobValidator;


	/**
	 * SmsMessageScheduledJobServiceImpl constructor
	 **/
	@Autowired
	public SmsMessageScheduledJobServiceImpl(SmsMessageRepository smsMessageRepository,
			 SmsConfigurationReadPlatformService readPlatformService,
			 SmsReadPlatformService smsReadPlatformService,
			 SmsConfigurationRepository smsConfigurationRepository,
			 EmailMessageJobEmailService emailMessageJobEmailService,
			 ExternalServicesPropertiesReadPlatformService externalServicePropertiesReadPlatformService,
			 ReportMailingJobValidator reportMailingJobValidator) {
		this.smsMessageRepository = smsMessageRepository;
		this.smsConfigurationRepository = smsConfigurationRepository;
		this.configurationReadPlatformService = readPlatformService;
		this.smsReadPlatformService = smsReadPlatformService;
		this.emailMessageJobEmailService = emailMessageJobEmailService;
		this.externalServicePropertiesReadPlatformService = externalServicePropertiesReadPlatformService;
		this.reportMailingJobValidator = reportMailingJobValidator;
	}
	
	/** 
     * get the tenant's sms configuration 
     * 
     * @return {@link TenantSmsConfiguration} object
     **/
    private TenantSmsConfiguration getTenantSmsConfiguration() {
    	Collection<SmsConfigurationData> configurationDataCollection = configurationReadPlatformService.retrieveAll();
    	
    	return TenantSmsConfiguration.instance(configurationDataCollection);
    }
	
	/** 
	 * get a new HttpEntity with the provided body
	 **/
	private HttpEntity<String> getHttpEntity(String body, String apiAuthUsername, String apiAuthPassword) {
		HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        String authorization = apiAuthUsername + ":" + apiAuthPassword;

        byte[] encodedAuthorisation = Base64.encode(authorization.getBytes());
        headers.add("Authorization", "Basic " + new String(encodedAuthorisation));
		
		return new HttpEntity<String>(body, headers);
	}
	
	/** 
	 * prevents the SSL security certificate check 
	 **/
	private void trustAllSSLCertificates() {
		TrustManager[] trustAllCerts = new TrustManager[] {
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
 
                public void checkClientTrusted(
                    X509Certificate[] certs, String authType) {
                }
 
                public void checkServerTrusted(
                    X509Certificate[] certs, String authType) {
                }
            }
        };
		
		try {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            
            // Create all-trusting host name verifier
    		HostnameVerifier hostnameVerifier = new HostnameVerifier() {
    			@Override
    			public boolean verify(String hostname, SSLSession session) {
    				return true;
    			}
    		};

    		// Install the all-trusting host verifier
    		HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);
        } 
		
		catch (Exception e) { 
			// do nothing
		}
	}
	
	/** 
     * Format destination phone number so it is in international format without the leading
     * "0" or "+", example: 31612345678 
     * 
     * @param phoneNumber the phone number to be formated
     * @param countryCallingCode the country calling code
     * @return phone number in international format
     **/
    private String formatDestinationPhoneNumber(String phoneNumber, String countryCallingCode) {
    	String formatedPhoneNumber = "";
    	
    	try {
    		// trim the string
    		phoneNumber = StringUtils.trim(phoneNumber);
    		
    		Long phoneNumberToLong = Long.parseLong(phoneNumber);
    		Long countryCallingCodeToLong = Long.parseLong(countryCallingCode);
    		formatedPhoneNumber = Long.toString(countryCallingCodeToLong) + Long.toString(phoneNumberToLong);
    	}
    	
    	catch(Exception e) {
    		logger.error("Invalid phone number or country calling code, must contain only numbers", e);
    	}
    	
    	return formatedPhoneNumber;
    }

	/**
	 * Send batches of SMS messages to the SMS gateway (or intermediate gateway)
	 **/
	@Override
	@Transactional
	@CronTarget(jobName = JobName.SEND_MESSAGES_TO_SMS_GATEWAY)
	public void sendMessages() {
	    if (IPv4Helper.applicationIsNotRunningOnLocalMachine() && this.isSmsEnabledInSmsPropertiesFile()) {
	        final TenantSmsConfiguration tenantSmsConfiguration = this.getTenantSmsConfiguration();
	        final String apiAuthUsername = tenantSmsConfiguration.getApiAuthUsername();
	        final String apiAuthPassword = tenantSmsConfiguration.getApiAuthPassword();
	        final String apiBaseUrl = tenantSmsConfiguration.getApiBaseUrl();
	        final String sourceAddress = tenantSmsConfiguration.getSourceAddress();
	        final String countryCallingCode = tenantSmsConfiguration.getCountryCallingCode();
	        final MifosPlatformTenant tenant = ThreadLocalContextUtil.getTenant();
	        final int httpEntityLimit = 500;
	        final int httpEntityLimitMinusOne = httpEntityLimit - 1;
	        
	        Integer smsCredits = tenantSmsConfiguration.getSmsCredits();
	        Integer smsSqlLimit = 5000;

			final Integer smsCreditEmailReminder = this.smsCreditEmailReminder();
	        
	        if(smsCredits > 0) {
	            try{
	                smsSqlLimit = (smsSqlLimit > smsCredits) ? smsCredits : smsSqlLimit;
	                final Collection<SmsData> pendingMessages = this.smsReadPlatformService.retrieveAllPending(smsSqlLimit);
	                
	                if(pendingMessages.size() > 0) {
	                    Iterator<SmsData> pendingMessageIterator = pendingMessages.iterator();
	                    int index = 0;
	                    
	                    // ====================== start point json string ======================================
	                    StringBuilder httpEntity = new StringBuilder("[");
	                    
	                    while(pendingMessageIterator.hasNext()) {
	                        SmsData smsData = pendingMessageIterator.next();
	                        SmsMessageApiQueueResourceData apiQueueResourceData = 
	                                SmsMessageApiQueueResourceData.instance(smsData.getId(), tenant.getTenantIdentifier(), 
	                                        null, sourceAddress, formatDestinationPhoneNumber(smsData.getMobileNo(), countryCallingCode), 
	                                        smsData.getMessage());
	                        
	                        httpEntity.append(apiQueueResourceData.toJsonString());
	                        
	                        index++;
	                        
	                        if (index == httpEntityLimitMinusOne) {
	                            httpEntity.append("]");
	                            
	                            smsCredits = this.sendMessages(httpEntity, apiAuthUsername, apiAuthPassword, apiBaseUrl, smsCredits, 
	                                    sourceAddress,smsCreditEmailReminder);
	                            
	                            index = 0;
	                            httpEntity = new StringBuilder("[");
	                        }
	                        
	                        // add comma separation if iterator has more elements
	                        if(pendingMessageIterator.hasNext() && (index > 0)) {
	                            httpEntity.append(", ");
	                        }
	                    }
	                    
	                    httpEntity.append("]");
	                    // ====================== end point json string ====================================
	                    
	                    smsCredits = this.sendMessages(httpEntity, apiAuthUsername, apiAuthPassword, apiBaseUrl, smsCredits, sourceAddress,smsCreditEmailReminder);


	                    logger.info(pendingMessages.size() + " pending message(s) successfully sent to the intermediate gateway - mlite-sms");
	                    
	                    SmsConfiguration smsConfiguration = this.smsConfigurationRepository.findByName("SMS_CREDITS");
	                    smsConfiguration.setValue(smsCredits.toString());
	                    
	                    // save the SmsConfiguration entity
	                    this.smsConfigurationRepository.save(smsConfiguration);


					}
	            }
	            
	            catch(Exception e) {
	                logger.error(e.getMessage(), e);
	            }
	        }
	    }
	}
	
	/**
	 * handles the sending of messages to the intermediate gateway and updating of the external ID, status and sources address
	 * of each message
	 * 
	 * @param httpEntity
	 * @param apiAuthUsername
	 * @param apiAuthPassword
	 * @param apiBaseUrl
	 * @param smsCredits
	 * @param sourceAddress
	 * @return the number of SMS credits left
	 */
	private Integer sendMessages(final StringBuilder httpEntity, final String apiAuthUsername, 
	        final String apiAuthPassword, final String apiBaseUrl, Integer smsCredits, final String sourceAddress,final Integer smsCreditReminder) {
	    // trust all SSL certificates
        trustAllSSLCertificates();
        
        // make request
        final ResponseEntity<SmsMessageApiResponseData> entity = restTemplate.postForEntity(apiBaseUrl + "/queue",
                getHttpEntity(httpEntity.toString(), apiAuthUsername, apiAuthPassword), 
                SmsMessageApiResponseData.class);
        
        final List<SmsMessageDeliveryReportData> smsMessageDeliveryReportDataList = entity.getBody().getData();
        final Iterator<SmsMessageDeliveryReportData> deliveryReportIterator = smsMessageDeliveryReportDataList.iterator();
        
        while(deliveryReportIterator.hasNext()) {
            SmsMessageDeliveryReportData smsMessageDeliveryReportData = deliveryReportIterator.next();
            
            SmsMessage smsMessage = this.smsMessageRepository.findOne(smsMessageDeliveryReportData.getId());
            
            if(!smsMessageDeliveryReportData.getHasError()) {
                
                // initially set the status type enum to 100
                Integer statusType = SmsMessageStatusType.PENDING.getValue();
                
                switch(smsMessageDeliveryReportData.getDeliveryStatus()) {
                    case 100:
                    case 200:
                        statusType = SmsMessageStatusType.SENT.getValue();
                        break;
                        
                    case 300:
                        statusType = SmsMessageStatusType.DELIVERED.getValue();
                        break;
                        
                    case 400:
                        statusType = SmsMessageStatusType.FAILED.getValue();
                        break;
                        
                    default:
                        statusType = SmsMessageStatusType.INVALID.getValue();
                        break;
                }
                
                // update the externalId of the SMS message
                smsMessage.setExternalId(smsMessageDeliveryReportData.getExternalId());
                
                // update the SMS message sender
                smsMessage.setSourceAddress(sourceAddress);
                
                // update the status Type enum
                smsMessage.setStatusType(statusType);
                
                // deduct one credit from the tenant's SMS credits
                smsCredits--;
				if(smsCreditReminder.compareTo(smsCredits) == 0){this.sendEmailLowSmsCreditReminder(smsCredits);}
				
            } else {
            	Integer statusType = SmsMessageStatusType.FAILED.getValue();
            	
            	// update the status Type enum
                smsMessage.setStatusType(statusType);
            }
            
            // save the SmsMessage entity
            this.smsMessageRepository.save(smsMessage);
        }
        
        return smsCredits;
	}

	/**
	 * get SMS message delivery reports from the SMS gateway (or intermediate gateway)
	 **/
	@Override
	@Transactional
	@CronTarget(jobName = JobName.GET_DELIVERY_REPORTS_FROM_SMS_GATEWAY)
	public void getDeliveryReports() {
	    if (IPv4Helper.applicationIsNotRunningOnLocalMachine() && this.isSmsEnabledInSmsPropertiesFile()) {
	        final TenantSmsConfiguration tenantSmsConfiguration = this.getTenantSmsConfiguration();
	        final String apiAuthUsername = tenantSmsConfiguration.getApiAuthUsername();
	        final String apiAuthPassword = tenantSmsConfiguration.getApiAuthPassword();
	        final String apiBaseUrl = tenantSmsConfiguration.getApiBaseUrl();
	        final MifosPlatformTenant tenant = ThreadLocalContextUtil.getTenant();
	        
	        try{
	            List<Long> smsMessageExternalIds = this.smsReadPlatformService.retrieveExternalIdsOfAllSent(0);
	            SmsMessageApiReportResourceData smsMessageApiReportResourceData = 
	                    SmsMessageApiReportResourceData.instance(smsMessageExternalIds, tenant.getTenantIdentifier());
	            
	            // only proceed if there are sms message with status type enum 200
	            if(smsMessageExternalIds.size() > 0) {
	                // trust all SSL certificates
	                trustAllSSLCertificates();
	                
	                // make request
	                ResponseEntity<SmsMessageApiResponseData> entity = restTemplate.postForEntity(apiBaseUrl + "/report",
	                        getHttpEntity(smsMessageApiReportResourceData.toJsonString(), apiAuthUsername, apiAuthPassword), 
	                        SmsMessageApiResponseData.class);
	                
	                List<SmsMessageDeliveryReportData> smsMessageDeliveryReportDataList = entity.getBody().getData();
	                Iterator<SmsMessageDeliveryReportData> iterator1 = smsMessageDeliveryReportDataList.iterator();
	                
	                while(iterator1.hasNext()) {
	                    SmsMessageDeliveryReportData smsMessageDeliveryReportData = iterator1.next();
	                    Integer deliveryStatus = smsMessageDeliveryReportData.getDeliveryStatus();
	                    
	                    if(!smsMessageDeliveryReportData.getHasError() && (deliveryStatus != 100 && deliveryStatus != 200)) {
	                        SmsMessage smsMessage = this.smsMessageRepository.findOne(smsMessageDeliveryReportData.getId());
	                        Integer statusType = smsMessage.getStatusType();
	                        boolean statusChanged = false;
	                        
	                        switch(deliveryStatus) {
	                            case 0:
	                                statusType = SmsMessageStatusType.INVALID.getValue();
	                                break;
	                            case 300:
	                                statusType = SmsMessageStatusType.DELIVERED.getValue();
	                                break;
	                                
	                            case 400:
	                                statusType = SmsMessageStatusType.FAILED.getValue();
	                                break;
	                                
	                            default:
	                                statusType = smsMessage.getStatusType();
	                                break;
	                        }
	                        
	                        statusChanged = !statusType.equals(smsMessage.getStatusType());
	                        
	                        // update the status Type enum
	                        smsMessage.setStatusType(statusType);
	                        
	                        // save the SmsMessage entity
	                        this.smsMessageRepository.save(smsMessage);
	                        
	                        if (statusChanged) {
	                            logger.info("Status of SMS message id: " + smsMessage.getId() + " successfully changed to " + statusType);
	                        }
	                    }
	                }
	                
	                if(smsMessageDeliveryReportDataList.size() > 0) {
	                    logger.info(smsMessageDeliveryReportDataList.size() + " "
	                            + "delivery report(s) successfully received from the intermediate gateway - mlite-sms");
	                }
	            }
	        }
	        
	        catch(Exception e) {
	            logger.error(e.getMessage(), e);
	        }
	    }
	}
	
	/**
	 * Checks if the SMS module (sending sms and retrieving delivery reports) is enable in the sms properties file
	 * 
	 * @return true if sms module is enabled, else false
	 */
	private Boolean isSmsEnabledInSmsPropertiesFile() {
		Boolean isEnabled = true;
		
		Properties smsProperties = new Properties();
        InputStream smsPropertiesInputStream = null;
        File catalinaBaseConfDirectory = null;
        File smsPropertiesFile = null;
        String smsDotEnablePropertyValue = null;
        
        try {
            // create a new File instance for the catalina base conf directory
            catalinaBaseConfDirectory = new File(System.getProperty("catalina.base"), "conf");
            
            // create a new File instance for the quartz properties file
            smsPropertiesFile = new File(catalinaBaseConfDirectory, "sms.properties");
            
            // create file inputstream to the quartz properties file
            smsPropertiesInputStream = new FileInputStream(smsPropertiesFile);
            
            // read property list from input stream 
            smsProperties.load(smsPropertiesInputStream);
            
            smsDotEnablePropertyValue = smsProperties.getProperty("sms.enabled");
            
            // make sure it isn't blank, before trying to parse the string as boolean
            if (StringUtils.isNoneBlank(smsDotEnablePropertyValue)) {
                isEnabled = Boolean.parseBoolean(smsDotEnablePropertyValue); 
            }
            
        } catch (FileNotFoundException ex) {
            
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }

        finally {
            if (smsPropertiesInputStream != null) {
                try {
                	smsPropertiesInputStream.close();
                	
                }  catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        }
		
		return isEnabled;
	}

	private void sendEmailLowSmsCreditReminder(final Integer smsCredits){

		String port = null;
		String username  = null;
		String password = null;
		String host   = null;
		String to  = null;

		final Collection<ExternalServicesPropertiesData> externalServicesPropertiesDatas = this.externalServicePropertiesReadPlatformService.retrieveOne("SMTP");
		for(final ExternalServicesPropertiesData externalServicesProperty : externalServicesPropertiesDatas){
			if(externalServicesProperty.getName().equals(ExternalServicesConstants.SMTP_HOST)){
				host = externalServicesProperty.getValue();
			}
			if(externalServicesProperty.getName().equals(ExternalServicesConstants.SMTP_PORT)){
				port = externalServicesProperty.getValue();
			}
			if(externalServicesProperty.getName().equals(ExternalServicesConstants.SMTP_USERNAME)){
				username = externalServicesProperty.getValue();
			}
			if(externalServicesProperty.getName().equals(ExternalServicesConstants.SMTP_PASSWORD)){
				password = externalServicesProperty.getValue();
			}
			if(externalServicesProperty.getName().equals(ExternalServicesConstants.SMTP_TARTGET_EMAIL_FOR_LOW_SMS_CREDIT)){
				to = externalServicesProperty.getValue();
			}
		}

		if(port !=null && username !=null && host !=null && password !=null){

			if(to == null){to = username;}
			String text = "Please be aware that you have a limited number of SMS messages remaining. If you would like to continue sending out automated messages to your clients, please make sure to top up your SMS balance as soon as possible.\n" +
					"Thank you,";
			String subject = "Musoni SMS Balance Low";
			Set<String> emailRecipients =  this.reportMailingJobValidator.validateEmailRecipients(to);

			for(final String sendTo : emailRecipients){
				EmailMessageWithAttachmentData emailMessageWithAttachmentData = EmailMessageWithAttachmentData.createNew(sendTo,
						text,subject,null);
				this.emailMessageJobEmailService.sendEmailWithAttachment(emailMessageWithAttachmentData);
			}

		}

	}



	private Integer smsCreditEmailReminder(){
		Integer smsCreditEmailReminder = 0;
		final Collection<ExternalServicesPropertiesData> externalServicesPropertiesDatas = this.externalServicePropertiesReadPlatformService.retrieveOne("SMTP");
		for(final ExternalServicesPropertiesData externalServicesProperty : externalServicesPropertiesDatas){
			if(externalServicesProperty.getName().equals(ExternalServicesConstants.SMTP_CREDIT_EMAIL_REMINDER)){
				smsCreditEmailReminder = Integer.valueOf(externalServicesProperty.getValue());
			}
		}
		return smsCreditEmailReminder;
	}
}
