/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.restwebservice;

import java.net.URI;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

public class PlatformRestClient extends RestTemplate {
	protected final String basicAuthenticationCredentials;
	private Integer numberOfHttpRequestAttempts = 0;
	
	private final Integer numberOfHttpRequestRetries = 3;
	
	/**
	 * PlatformRestClient public constructor
	 * 
	 * @param basicAuthenticationCredentials
	 */
	public PlatformRestClient(final String basicAuthenticationCredentials) {
		super();
		
		this.basicAuthenticationCredentials = basicAuthenticationCredentials;
		
		this.configureClient();
	}
	
	/**
	 * PlatformRestClient public constructor
	 * 
	 * @param securityContext
	 */
	public PlatformRestClient(final SecurityContext securityContext) {
		super();
		
		this.basicAuthenticationCredentials = this.createBasicAuthenticationCredentials(securityContext);
	}
	
	/**
	 * Skip SSL certificate verification
	 */
	private void skipSslCertificateVerification() {
		final TrustManager[] trustManager = new TrustManager[] {
	            new X509TrustManager() {
	            	public X509Certificate[] getAcceptedIssuers() {
	                    return null;
	                }
	 
	                public void checkClientTrusted(X509Certificate[] certs, String authType) { }
	 
	                public void checkServerTrusted(X509Certificate[] certs, String authType) { }
	            }
	        };
	        
	        try {
	            // get the SSL context object
	            SSLContext sslContext = SSLContext.getInstance("SSL");
	            
	            // initialize the SSL context
	            sslContext.init(null, trustManager, new SecureRandom());
	            
	            // Set the default SSLSocketFactory
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
	            
	        }  catch (Exception e) {  }
	}
	
	/**
	 * Configure the REST API client class
	 */
	protected void configureClient() {
		this.skipSslCertificateVerification();
		
		// reset the variable
		this.numberOfHttpRequestAttempts = 0;
	}
	
	/**
	 * Creates a new {@link HttpEntity} object for the HTTP request
	 * 
	 * @param entityBody
	 * @param basicAuthenticationCredentials
	 * @param contentMediaType
	 * @param acceptedResponseMediaTypes
	 * @return {@link HttpEntity} object
	 */
	public HttpEntity<String> getHttpRequestEntity(final String entityBody,
			final MediaType contentMediaType, 
			final List<MediaType> acceptedResponseMediaTypes) {
		final HttpHeaders entityHeaders = new HttpHeaders();
		
		entityHeaders.setContentType(contentMediaType);
		entityHeaders.setAccept(acceptedResponseMediaTypes);
        
        if (StringUtils.isNotEmpty(basicAuthenticationCredentials)) {
        	entityHeaders.add("Authorization", "Basic " + this.basicAuthenticationCredentials);
        }
        
        return new HttpEntity<String>(entityBody, entityHeaders);
	}
	
	/**
	 * Creates a new {@link HttpEntity} object for a JSON HTTP request
	 * 
	 * @param entityBody
	 * @return {@link HttpEntity} object
	 */
	public HttpEntity<String> getJsonHttpRequestEntity(final String entityBody) {
		return this.getHttpRequestEntity(entityBody, MediaType.APPLICATION_JSON, 
                Arrays.asList(MediaType.APPLICATION_JSON));
	}
	
	/**
	 * Creates a HTTP basic authentication credentials using the spring {@link SecurityContext} authentication credentials
	 * 
	 * @param securityContext {@link SecurityContext} object
	 * @return basic authentication credentials
	 */
	public String createBasicAuthenticationCredentials(final SecurityContext securityContext) {
		String basicAuthenticationCredentials = null;
		
		if ((securityContext.getAuthentication() != null) 
				&& (securityContext.getAuthentication().getCredentials() != null)) {
			final String username = securityContext.getAuthentication().getName();
	        final String password = securityContext.getAuthentication().getCredentials().toString();
	        
	        basicAuthenticationCredentials = this.createBasicAuthenticationCredentials(username, password);
		}
		
		return basicAuthenticationCredentials;
	}
	
	/**
	 * Creates a HTTP basic authentication credentials
	 * 
	 * @param username
	 * @param password
	 * @return basic authentication credentials
	 */
	public String createBasicAuthenticationCredentials(final String username, final String password) {
		final String concatUsernameAndPassword = username + ":" + password;
		final byte[] encodeConcatUsernameAndPassword = Base64.encode(concatUsernameAndPassword.getBytes());
		
		return new String(encodeConcatUsernameAndPassword);
	}
	
	/**
     * Executes a HTTP request using the spring framework RestTemplate
     * 
     * @param url the URL
     * @param method the HTTP method (GET, POST, etc)
     * @param requestEntity the entity (headers and/or body) to write to the request, may be null
     * @param responseType the type of the return value
     * @return the response as entity
     * @throws InterruptedException
     * @throws RestClientException
     */
    public <T> ResponseEntity<T> executeHttpRequest(final URI url, final HttpMethod method, 
    		final HttpEntity<?> requestEntity, final Class<T> responseType) {
    	final RestTemplate restTemplate = new RestTemplate();
    	
    	HttpStatus statusCode = null;
    	ResponseEntity<T> responseEntity = null;
    	
    	// increment the number of request attempts by 1
    	this.numberOfHttpRequestAttempts++;
    	
    	try {
    		// execute the HTTP request
        	responseEntity = restTemplate.exchange(url, method, 
                    requestEntity, responseType);
        	statusCode = responseEntity.getStatusCode();
        	
        // catch all server HTTP error exceptions
    	} catch (HttpServerErrorException exception) {
    		statusCode = exception.getStatusCode();
    		
    		// if HTTP status is 503 or 504, sleep for 5 seconds and retry
        	if ((statusCode.equals(HttpStatus.SERVICE_UNAVAILABLE) || statusCode.equals(HttpStatus.GATEWAY_TIMEOUT)) && 
        			(this.numberOfHttpRequestAttempts < this.numberOfHttpRequestRetries)) {
        		
        		logger.info("Server returned an error response with status: " + statusCode + ", retrying again in 5 seconds");
        		logger.info("Number of attempts: " + this.numberOfHttpRequestAttempts);
        		
        		try {
        			// sleep for 5 seconds and try again
					Thread.sleep(5000);
					
				} catch (InterruptedException interruptedException) {
					logger.error(interruptedException.getMessage(), interruptedException);
				}
        		
        		// execute HTTP request again
        		this.executeHttpRequest(url, method, requestEntity, responseType);
        		
        	} else {
        		// in other cases, throw back the exception
        		throw exception;
        	}
    	}
    	
    	return responseEntity;
    }
    
    /**
     * Checks if the string is a valid JSON array
     * 
     * @param string
     * @return true if valid, else false
     */
    public static Boolean isJsonArray(final String string) {
    	try {
    		new ObjectMapper().readValue(string, new TypeReference<List<HashMap<String,Object>>>(){});
    		
    	} catch (Exception exception) { 
    		return false;
    	}
    	
    	return true;
    }
    
    /**
     * Checks if the string is a valid JSON object
     * 
     * @param string
     * @return true if valid, else false
     */
    public static Boolean isJsonObject(final String string) {
    	try {
    		new ObjectMapper().readValue(string, HashMap.class);
    		
    	} catch (Exception exception) { 
    		return false;
    	}
    	
    	return true;
    }
}
