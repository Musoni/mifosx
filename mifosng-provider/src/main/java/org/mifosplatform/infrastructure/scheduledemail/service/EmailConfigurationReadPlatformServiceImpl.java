/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.scheduledemail.service;

import org.mifosplatform.infrastructure.core.domain.JdbcSupport;
import org.mifosplatform.infrastructure.core.service.RoutingDataSource;
import org.mifosplatform.infrastructure.scheduledemail.exception.EmailConfigurationNotFoundException;
import org.mifosplatform.infrastructure.scheduledemail.data.EmailConfigurationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

@Service
public class EmailConfigurationReadPlatformServiceImpl implements EmailConfigurationReadPlatformService {
	
	private final JdbcTemplate jdbcTemplate;
    private final EmailConfigurationRowMapper emailConfigurationRowMapper;
    
    @Autowired
    public EmailConfigurationReadPlatformServiceImpl(final RoutingDataSource dataSource) {
    	this.jdbcTemplate = new JdbcTemplate(dataSource);
    	this.emailConfigurationRowMapper = new EmailConfigurationRowMapper();
    	
    }
	
	private static final class EmailConfigurationRowMapper implements RowMapper<EmailConfigurationData> {
		
		final String schema;
		
		public EmailConfigurationRowMapper() {
			 final StringBuilder sql = new StringBuilder(300);
	            sql.append("cnf.id as id, ");
	            sql.append("cnf.name as name, ");
	            sql.append("cnf.value as value ");
	            sql.append("from scheduled_email_configuration cnf");
	            
	            this.schema = sql.toString();
		}
		
		public String schema() {
            return this.schema;
        }

		@Override
		public EmailConfigurationData mapRow(ResultSet rs, @SuppressWarnings("unused") int rowNum) throws SQLException {
			
			final Long id = JdbcSupport.getLong(rs, "id");
			final String name = rs.getString("name");
			final String value = rs.getString("value");
			
			return EmailConfigurationData.instance(id, name, value);
		}
		
	}

	@Override
	public Collection<EmailConfigurationData> retrieveAll() {
		final String sql = "select " + this.emailConfigurationRowMapper.schema();

        return this.jdbcTemplate.query(sql, this.emailConfigurationRowMapper, new Object[] {});
	}

	@Override
	public EmailConfigurationData retrieveOne(String name) {
		try {
			final String sql = "select " + this.emailConfigurationRowMapper.schema() + " where cnf.name = ?";

	        return this.jdbcTemplate.queryForObject(sql, this.emailConfigurationRowMapper, name);
		}
		
		catch(final EmptyResultDataAccessException e) {
			
			throw new EmailConfigurationNotFoundException(name);
		}
	}

}
