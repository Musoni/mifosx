/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.core.service;

import org.apache.commons.lang.BooleanUtils;
import org.mifosplatform.infrastructure.core.domain.MifosPlatformTenant;
import org.springframework.util.Assert;

/**
 *
 */
public class ThreadLocalContextUtil {

    public static final String CONTEXT_TENANTS = "tenants";

    private static final ThreadLocal<String> contextHolder = new ThreadLocal<>();

    private static final ThreadLocal<MifosPlatformTenant> tenantcontext = new ThreadLocal<>();
    
    private static final ThreadLocal<String> authTokenContext = new ThreadLocal<>();
    
    private static final ThreadLocal<Boolean> skipPasswordExpirationCheck = newBooleanThreadLocal(false);
    
    public static void setTenant(final MifosPlatformTenant tenant) {
        Assert.notNull(tenant, "tenant cannot be null");
        tenantcontext.set(tenant);
    }

    public static MifosPlatformTenant getTenant() {
        return tenantcontext.get();
    }

    public static void clearTenant() {
        tenantcontext.remove();
    }

    public static String getDataSourceContext() {
        return contextHolder.get();
    }

    public static void setDataSourceContext(final String dataSourceContext) {
        contextHolder.set(dataSourceContext);
    }

    public static void clearDataSourceContext() {
        contextHolder.remove();
    }
    
    public static void setAuthToken(final String authToken) {
    	authTokenContext.set(authToken);
    }

    public static String getAuthToken() {
        return authTokenContext.get();
    }
    
    /**
     * Switches the data source to the tenants (`mifosplatform-tenants`) database
     */
    public static void switchToPrimaryDataSource() {
        setDataSourceContext(CONTEXT_TENANTS);
    }
    
    /**
     * Switches the data source to the specified tenant's database (e.g. - mifostenant-default)
     * 
     * @param mifosPlatformTenant
     */
    public static void switchToTenantSpecificDataSource(final MifosPlatformTenant mifosPlatformTenant) {
        setDataSourceContext(null);
        setTenant(mifosPlatformTenant);
    }
    
    /**
     * Creates a new boolean thread local variable
     * 
     * @param initialValue
     * @return {@link ThreadLocal}
     */
    private static ThreadLocal<Boolean> newBooleanThreadLocal(
            final Boolean initialValue) {
        ThreadLocal<Boolean> threadLocal = new ThreadLocal<Boolean>() {
            @Override
            protected Boolean initialValue() {
                return initialValue;
            }
        };
        
        return threadLocal;
    }

    /**
     * Sets the value of the "skipPasswordExpirationCheck" thread local variable
     * 
     * @param skip
     */
    public static void setSkipPasswordExpirationCheck(final Boolean skip) {
    	skipPasswordExpirationCheck.set(skip);
    }
    
    /**
     * Returns true if the value of the "skipPasswordExpirationCheck" thread local variable is true, else false
     * 
     * @return true/false
     */
    public static Boolean skipPasswordExpirationCheck() {
    	return BooleanUtils.isNotFalse(skipPasswordExpirationCheck.get());
    }
    
    /**
     * Returns true if the value of the "skipPasswordExpirationCheck" thread local variable is false, else false
     * 
     * @return true/false
     */
    public static Boolean doPasswordExpirationCheck() {
    	return BooleanUtils.isFalse(skipPasswordExpirationCheck.get());
    }
}