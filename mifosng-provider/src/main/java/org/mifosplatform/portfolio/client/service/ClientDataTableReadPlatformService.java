package org.mifosplatform.portfolio.client.service;


import org.mifosplatform.infrastructure.dataqueries.domain.RegisteredTable;

import java.util.Collection;

public interface ClientDataTableReadPlatformService {

    Collection<RegisteredTable> retrieveDataTables();

    

}
