/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.core.service;

import java.io.*;
import java.util.List;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.mifosplatform.infrastructure.core.boot.db.TenantDataSourcePortFixService;
import org.mifosplatform.infrastructure.core.domain.MifosPlatformTenant;
import org.mifosplatform.infrastructure.core.domain.MifosPlatformTenantConnection;
import org.mifosplatform.infrastructure.security.service.TenantDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.googlecode.flyway.core.Flyway;
import com.googlecode.flyway.core.api.FlywayException;

/**
 * A service that picks up on tenants that are configured to auto-update their
 * specific schema on application startup.
 */
@Service
public class TenantDatabaseUpgradeService {

    private final TenantDetailsService tenantDetailsService;
    protected final DataSource tenantDataSource;
    protected final TenantDataSourcePortFixService tenantDataSourcePortFixService;
    private final static Logger logger = LoggerFactory.getLogger(TenantDatabaseUpgradeService.class);


    @Autowired
    public TenantDatabaseUpgradeService(final TenantDetailsService detailsService,
            @Qualifier("tenantDataSourceJndi") final DataSource dataSource, TenantDataSourcePortFixService tenantDataSourcePortFixService) {
        this.tenantDetailsService = detailsService;
        this.tenantDataSource = dataSource;
        this.tenantDataSourcePortFixService = tenantDataSourcePortFixService;
    }

    @PostConstruct
    public void upgradeAllTenants() {
        if(!isFlywayEnabled())
        {
            logger.warn("Flyway is disabled on this instance, skipping DB Migrations");
            return;
        }
        upgradeTenantDB();
        final List<MifosPlatformTenant> tenants = this.tenantDetailsService.findAllTenants();
        for (final MifosPlatformTenant tenant : tenants) {
            final MifosPlatformTenantConnection connection = tenant.getConnection();
            if (connection.isAutoUpdateEnabled()) {
                final Flyway flyway = new Flyway();
                flyway.setDataSource(connection.databaseURL(), connection.getSchemaUsername(), connection.getSchemaPassword());
                flyway.setLocations("sql/migrations/core_db");
                flyway.setOutOfOrder(true);
                try {
                    flyway.migrate();
                } catch (FlywayException e) {
                    String betterMessage = e.getMessage() + "; for Tenant DB URL: " + connection.databaseURL() + ", username: "
                            + connection.getSchemaPassword();
                    throw new FlywayException(betterMessage, e.getCause());
                }
            }
        }
    }


    /**
     * check if the flyway.enabled property in the "/var/lib/tomcat7/conf/flyway.properties" file is set to true
     * N/B - This is a temporary fix for the duplicate job scheduling issue resulting from running on a clustered
     * environment
     *
     * @return boolean true if value is true, else false
     **/
    private boolean isFlywayEnabled() {
        // scheduler is enabled by default
        boolean isEnabled = true;
        Properties flywayProperties = new Properties();
        InputStream flywayPropertiesInputStream = null;
        File catalinaBaseConfDirectory = null;
        File flywayPropertiesFile = null;
        String flywayEnabledValue = null;

        try {
            // create a new File instance for the catalina base conf directory
            catalinaBaseConfDirectory = new File(System.getProperty("catalina.base"), "conf");

            // create a new File instance for the flyway properties file
            flywayPropertiesFile = new File(catalinaBaseConfDirectory, "flyway.properties");

            // create file inputstream to the flyway properties file
            flywayPropertiesInputStream = new FileInputStream(flywayPropertiesFile);

            // read property list from input stream 
            flywayProperties.load(flywayPropertiesInputStream);

            flywayEnabledValue = flywayProperties.getProperty("flyway.enabled");

            // make sure it isn't blank, before trying to parse the string as boolean
            if (StringUtils.isNoneBlank(flywayEnabledValue)) {
                isEnabled = Boolean.parseBoolean(flywayEnabledValue);
            }
        }


        catch (FileNotFoundException ex) {
            isEnabled = true;
        }

        catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
        }


        finally {
            if (flywayPropertiesInputStream != null) {
                try {
                    flywayPropertiesInputStream.close();
                }

                catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }

        return isEnabled;
    }

    /**
     * Initializes, and if required upgrades (using Flyway) the Tenant DB
     * itself.
     */
    private void upgradeTenantDB() {
        final Flyway flyway = new Flyway();
        flyway.setDataSource(tenantDataSource);
        flyway.setLocations("sql/migrations/list_db");
        flyway.setOutOfOrder(true);
        flyway.migrate();

        tenantDataSourcePortFixService.fixUpTenantsSchemaServerPort();
    }
}