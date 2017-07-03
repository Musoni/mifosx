/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.template.service;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ser.std.StdArraySerializers;
import org.codehaus.jackson.type.TypeReference;
import org.mifosplatform.template.domain.Template;
import org.mifosplatform.template.restwebservice.TemplateRestClient;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class TemplateMergeService {
	private final static Logger logger = LoggerFactory.getLogger(TemplateMergeService.class);
	
    // private final FromJsonHelper fromApiJsonHelper;
    private Map<String, Object> scopes;
    private String authToken;

    // @Autowired
    // public TemplateMergeService(final FromJsonHelper fromApiJsonHelper) {
    // this.fromApiJsonHelper = fromApiJsonHelper;
    //

    public void setAuthToken(final String authToken) {
        //final String auth = ThreadLocalContextUtil.getAuthToken();
    	this.authToken =  authToken;
    }


    

    public String compile(final Template template, final Map<String, Object> scopes) throws MalformedURLException, IOException, ParseException {
        this.scopes = scopes;
        this.scopes.put("static", TemplateMergeService.now());

        final MustacheFactory mf = new DefaultMustacheFactory();
        final Mustache mustache = mf.compile(new StringReader(template.getText()), template.getName());

        final Map<String, Object> mappers = getCompiledMapFromMappers(template.getMappersAsMap());
        this.scopes.putAll(mappers);

        expandMapArrays(scopes);

        final StringWriter stringWriter = new StringWriter();
        mustache.execute(stringWriter, this.scopes);

        return stringWriter.toString();
    }
    
    public static String now() {
        final DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");
        final Date date = new Date();

        return dateFormat.format(date);
    }

    public String formatDate(String stringDate) throws ParseException {
        final DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");

        final DateFormat formatTo = new SimpleDateFormat("dd MMMM yyyy");
        Date date = dateFormat.parse(stringDate);
        return formatTo.format(date);

    }

    public String formatDouble (Double removeZeros){
        DecimalFormat decimalFormat = new DecimalFormat("#,###.##");
        return decimalFormat.format(removeZeros);
    }

    private void formatDecimalDigits(HashMap<String,Object> objs) {
        for(Map.Entry<String,Object> entrySet: objs.entrySet()){
            final String key = entrySet.getKey();
            final Object value = entrySet.getValue();
            if(value instanceof Double){
                String doubleFormatted = this.formatDouble((Double) value);
                objs.put(key,doubleFormatted);
            }else if (value instanceof LinkedHashMap){
                this.formatDecimalDigits((HashMap) value);
            }
        }
    }



	private Map<String, Object> getCompiledMapFromMappers(final Map<String, String> data) throws ParseException {
        final MustacheFactory mf = new DefaultMustacheFactory();

        if (data != null) {
            for (final Map.Entry<String, String> entry : data.entrySet()) {
                final Mustache mappersMustache = mf.compile(new StringReader(entry.getValue()), "");
                final StringWriter stringWriter = new StringWriter();

                mappersMustache.execute(stringWriter, this.scopes);
                String url = stringWriter.toString();
                if (!url.startsWith("http")) {
                    url = this.scopes.get("BASE_URI") + url;
                }
                try {
                    final List<HashMap<String,Object>> mapFromUrl = getMapFromUrl(url);
                    /** this function changes the date format of [1997,7-1] to 1-7-1997 **/
                    for(final HashMap<String,Object> dateFormat : mapFromUrl){
                        for(Map.Entry<String,Object> map: dateFormat.entrySet()){
                            final Object obj  = map.getValue();
                            final String key = map.getKey();
                            if(obj instanceof ArrayList && (key.contains("date") || key.contains("Date")) && ((ArrayList) obj).size() == 3){
                                String changeArrayDateToStringDate =  ((ArrayList) obj).get(2).toString() +"-"+((ArrayList) obj).get(1).toString() +"-"+((ArrayList) obj).get(0).toString();
                                dateFormat.put(key,this.formatDate(changeArrayDateToStringDate));
                            }
                            if(obj instanceof LinkedHashMap){
                                this.formatArrayDate((HashMap) obj);
                                this.formatDecimalDigits((HashMap) obj);
                            }

                            if(obj instanceof Double){
                                String doubleFormatted = this.formatDouble((Double) obj);
                                dateFormat.put(key,doubleFormatted);
                            }
                        }
                    }
                    /** this function handles single maps from main entity ex loan and client else handles many to many relationships
                     * where mustache has to loop the info and display it
                     * */
                    if(mapFromUrl.size() == 1){
                        this.scopes.put(entry.getKey(), mapFromUrl.get(0));
                    }else{
                        this.scopes.put(entry.getKey(), mapFromUrl);
                    }
                } catch (final IOException e) {
                	logger.error("getCompiledMapFromMappers() failed", e);
                }
            }
        }
        return this.scopes;
    }

    private void formatArrayDate(HashMap<String,Object> objs) throws ParseException{
         for(Map.Entry<String,Object> entrySet: objs.entrySet()){
             final String key = entrySet.getKey();
             final Object value = entrySet.getValue();
             if(value instanceof ArrayList && (key.contains("date") || key.contains("Date")) && ((ArrayList) value).size() == 3){
                 String changeArrayDateToStringDate =  ((ArrayList) value).get(2).toString() +"-"+((ArrayList) value).get(1).toString() +"-"+((ArrayList) value).get(0).toString();
                 objs.put(key,this.formatDate(changeArrayDateToStringDate));
             }else if (value instanceof LinkedHashMap){
                 this.formatArrayDate((HashMap) value);
             }
         }
    }

    @SuppressWarnings("unchecked")
    private List<HashMap<String, Object>> getMapFromUrl(final String url) throws MalformedURLException, IOException {
    	final TemplateRestClient templateRestClient = new TemplateRestClient(SecurityContextHolder.getContext());
    	final String data = templateRestClient.retrieveDataFromUrl(url);
    	
    	List<HashMap<String, Object>> result = new ArrayList<>();
        HashMap<String,Object> hashMap  = new HashMap<>();
        
        if (TemplateRestClient.isJsonArray(data)) {
        	result = new ObjectMapper().readValue(data, new TypeReference<List<HashMap<String,Object>>>(){});
        	
        } else if (TemplateRestClient.isJsonObject(data)) {
        	hashMap = new ObjectMapper().readValue(data, HashMap.class);
        	
            result.add(hashMap);
        	
        } else {
        	hashMap.put("src", data);
        }
        
        return result;
    }
    
	@SuppressWarnings("unchecked")
	private void expandMapArrays(Object value) {
		if (value instanceof Map) {
			Map<String, Object> valueAsMap = (Map<String, Object>) value;
			//Map<String, Object> newValue = null;
			Map<String,Object> valueAsMap_second = new HashMap<>();
			for (Entry<String, Object> valueAsMapEntry : valueAsMap.entrySet()) {
				Object valueAsMapEntryValue = valueAsMapEntry.getValue();
				if (valueAsMapEntryValue instanceof Map) { // JSON Object
					expandMapArrays(valueAsMapEntryValue);
				} else if (valueAsMapEntryValue instanceof Iterable) { // JSON Array
					Iterable<Object> valueAsMapEntryValueIterable = (Iterable<Object>) valueAsMapEntryValue;
					String valueAsMapEntryKey = valueAsMapEntry.getKey();
					int i = 0;
					for (Object object : valueAsMapEntryValueIterable) {
						valueAsMap_second.put(valueAsMapEntryKey + "#" + i, object);
						++i;
						expandMapArrays(object);
						
					}
				}

			}
			valueAsMap.putAll(valueAsMap_second);

		}		
	}

	/*
	 * Gets the object from a runReport query
	 */
	private List<HashMap<String,Object>> getRunReportObject(final String url) throws MalformedURLException, IOException{
		final TemplateRestClient templateRestClient = new TemplateRestClient(SecurityContextHolder.getContext());
    	final String data = templateRestClient.retrieveDataFromUrl(url);
    	
    	List<HashMap<String, Object>> result = new ArrayList<HashMap<String, Object>>();
    	
    	if (TemplateRestClient.isJsonArray(data)) {
    		result = new ObjectMapper().readValue(data, new TypeReference<List<HashMap<String,Object>>>(){});
    	}

	    return result;
	}
	
	public Map<String, List<HashMap<String,Object>>> compileMappers(final Map<String, String> templateMappers,Map<String,Object> smsParams) {
	    final MustacheFactory mf = new DefaultMustacheFactory();

	    final Map<String,List<HashMap<String,Object>>> runReportObject = new HashMap<String, List<HashMap<String,Object>>>();

	    if(templateMappers !=null){
	        for(Map.Entry<String,String> entry : templateMappers.entrySet()){
	            /*
	                "mapperkey": "runreports",
	                "mappervalue": "runreports/{{runreportId}}?associations=all&tenantIdentifier={{tenantIdentifier}}",
	                entry.getValue represents mapperValue
	             */
	            final Mustache urlMustache = mf.compile(new StringReader(entry.getValue()),"");

	            final StringWriter stringWriter = new StringWriter();
	            //execute to replace params in the mapperValue above ex {{loanId}} = 4
	            urlMustache.execute(stringWriter,smsParams);
	            String url = stringWriter.toString(); //holds the url to query for object from runReport
	            if (!url.startsWith("http")) {
	                url = smsParams.get("BASE_URI") + url;
	            }
	            try{
	                runReportObject.put(entry.getKey(), getRunReportObject(url));
	            }catch(final MalformedURLException e){
	                //TODO throw something here
	            }catch (final IOException e){
	               // TODO throw something here
	            }
	        }

	    }

	    return runReportObject; //contains list of runReport object runReport,{Object}
	}
}
