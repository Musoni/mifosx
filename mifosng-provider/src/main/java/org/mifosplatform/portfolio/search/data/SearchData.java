/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.portfolio.search.data;

import org.mifosplatform.infrastructure.core.data.EnumOptionData;

public class SearchData {

    private final Long entityId;
    private final String entityAccountNo;
    private final String entityExternalId;
    private final String entityName;
    private final String entityType;
    private final Long parentId;
    private final String parentName;
    private final String entityMobileNo;
    private final EnumOptionData entityStatus;
    private final String parentType;
    private final Long groupId;
    private final String groupName;
    private final Long officeId;
    private final String officeName;
    private final String parentAccountNo;

    public SearchData(final Long entityId, final String entityAccountNo, final String entityExternalId, final String entityName,
            final String entityType, final Long parentId, final String parentName, final String parentType, 
            final String entityMobileNo, final EnumOptionData entityStatus, final Long groupId, final String groupName, 
            final Long officeId, final String officeName, final String parentAccountNo) {

        this.entityId = entityId;
        this.entityAccountNo = entityAccountNo;
        this.entityExternalId = entityExternalId;
        this.entityName = entityName;
        this.entityType = entityType;
        this.parentId = parentId;
        this.parentName = parentName;
        this.parentType = parentType;
        this.entityMobileNo=entityMobileNo;
        this.entityStatus = entityStatus;
        this.groupId = groupId;
        this.groupName = groupName;
        this.officeId = officeId;
        this.officeName = officeName;
        this.parentAccountNo = parentAccountNo;
    }

    public Long getEntityId() {
        return this.entityId;
    }

    public String getEntityAccountNo() {
        return this.entityAccountNo;
    }

    public String getEntityExternalId() {
        return this.entityExternalId;
    }

    public String getEntityName() {
        return this.entityName;
    }

    public String getEntityType() {
        return this.entityType;
    }

    public Long getParentId() {
        return this.parentId;
    }

    public String getParentName() {
        return this.parentName;
    }
    
    public String getParentType() {
        return this.parentType;
    }

    public String getEntityMobileNo() {
		return this.entityMobileNo;
	}

	public EnumOptionData getEntityStatus() {
        return this.entityStatus;
    }

	/**
	 * @return the groupId
	 */
	public Long getGroupId() {
		return groupId;
	}

	/**
	 * @return the groupName
	 */
	public String getGroupName() {
		return groupName;
	}

	/**
	 * @return the officeId
	 */
	public Long getOfficeId() {
		return officeId;
	}

	/**
	 * @return the officeName
	 */
	public String getOfficeName() {
		return officeName;
	}

	/**
	 * @return the parentAccountNo
	 */
	public String getParentAccountNo() {
		return parentAccountNo;
	}

}
