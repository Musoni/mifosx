/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.template.restwebservice;

import java.net.URI;
import java.util.Arrays;

import javax.ws.rs.core.UriBuilder;

import org.mifosplatform.restwebservice.PlatformRestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContext;

public class TemplateRestClient extends PlatformRestClient {
	private final static Logger logger = LoggerFactory.getLogger(PlatformRestClient.class);
	
	/**
	 * TemplateRestClient public constructor
	 *
	 * @param securityContext
	 */
	public TemplateRestClient(final SecurityContext securityContext) {
		super(securityContext);
	}
	
	/**
	 * Retrieves string data from the specified url
	 * 
	 * @param url
	 * @return string data
	 */
	public String retrieveDataFromUrl(final String url) {
		this.configureClient();
		
		String data = null;
		
		try {
			// build the URI
            final URI uri = UriBuilder.fromUri(url).build();
            
            // HTTP request entity consisting of HTTP Authorization header 
            final HttpEntity<String> requestEntity = this.getHttpRequestEntity(null, MediaType.APPLICATION_JSON, 
                    Arrays.asList(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, MediaType.TEXT_HTML));
            
            // execute the HTTP request
            final ResponseEntity<String> responseEntity = this.executeHttpRequest(uri, HttpMethod.GET, requestEntity, 
                    String.class);
            
            if (responseEntity != null) {
            	data = responseEntity.getBody();
            }
			
		} catch (final Exception exception) {
			logger.error(exception.getMessage(), exception);
		} 
		
		return data;
	}
}
