/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.dataexport.data;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class DataExportEntityColumnName {
	public static final String TRANSFER_TO_OFFICE_ID = "transfer_to_office_id";
	public static final String VERSION = "version";
	public static final String IMAGE_ID = "image_id";
	public static final String ACCOUNT_TYPE_ENUM = "account_type_enum";
	public static final String DEPOSIT_TYPE_ENUM = "deposit_type_enum";
	public static final String SUB_STATUS = "sub_status";
	
	public static final Set<String> COLUMNS_TO_BE_REMOVED_FROM_LISTS_OF_ENTITY_COLUMNS = new HashSet<>(Arrays.asList(
			TRANSFER_TO_OFFICE_ID, VERSION, IMAGE_ID, ACCOUNT_TYPE_ENUM, DEPOSIT_TYPE_ENUM, SUB_STATUS));
}
