package org.mifosplatform.infrastructure.dataqueries.service;

import org.mifosplatform.infrastructure.dataqueries.domain.RegisteredTable;
import org.mifosplatform.infrastructure.dataqueries.domain.RegisteredTableMetaData;

import java.util.Collection;


public interface RegisteredTableMetaDataReadPlatformService {

    Collection<RegisteredTableMetaData> retrieveMetaData(RegisteredTable registeredTable);
}
