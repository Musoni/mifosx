package org.mifosplatform.infrastructure.dataqueries.service;

import org.mifosplatform.infrastructure.dataqueries.domain.RegisteredTable;

import java.util.Collection;


public interface RegisteredTableReadPlatformService {

    Collection<RegisteredTable> retrieveAllDataTables();

    Collection<RegisteredTable> retrieveEntityDataTables(String applicationTableName);
}
