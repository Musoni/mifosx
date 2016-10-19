/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.batch.command.internal;

import org.mifosplatform.batch.command.CommandStrategy;
import org.mifosplatform.batch.domain.BatchRequest;
import org.mifosplatform.batch.domain.BatchResponse;
import org.mifosplatform.batch.exception.ErrorHandler;
import org.mifosplatform.batch.exception.ErrorInfo;
import org.mifosplatform.commands.exception.UnsupportedCommandException;
import org.mifosplatform.portfolio.group.api.GroupsApiResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.UriInfo;

/**
 * This command strategy is used to assign a role to a group.
 *
 * @author Ishan Khanna
 */
@Component
public class AssignRoleToGroupCommandStrategy implements CommandStrategy {

    /**
     * Command expected in the batch request to assign the role to a group.
     */
    private static final String ASSIGN_ROLE_TO_GROUP_COMMAND = "assignRole";

    /**
     * The API Resource that allows us to execute the command to assign role.
     */
    private final GroupsApiResource groupsApiResource;

    @Autowired
    public AssignRoleToGroupCommandStrategy(final GroupsApiResource groupsApiResource) {
        this.groupsApiResource = groupsApiResource;
    }

    @Override
    public BatchResponse execute(final BatchRequest batchRequest, @SuppressWarnings("unused") UriInfo uriInfo) {

        final BatchResponse batchResponse = new BatchResponse();
        final String responseBody;

        batchResponse.setRequestId(batchRequest.getRequestId());
        batchResponse.setHeaders(batchRequest.getHeaders());

        final String[] pathParameters = batchRequest.getRelativeUrl().split("/");

        // Fetching the group ID and command from the path which should look something like this
        // /groups/{groupID}?command=assignRole

        Long groupId = Long.parseLong(pathParameters[1].substring(0, pathParameters[1].indexOf("?")));

        final String command = pathParameters[1].substring(pathParameters[1].indexOf("=")+1, pathParameters[1].length());

        try {

            if (command.equals(ASSIGN_ROLE_TO_GROUP_COMMAND)) {

                    responseBody = groupsApiResource.activateOrGenerateCollectionSheet(groupId, command, null,
                            batchRequest.getBody(), uriInfo);
                    batchResponse.setStatusCode(200);
                    batchResponse.setBody(responseBody);

            } else {

                // Maybe the command was not provided or an invalid command was provided.
                throw new UnsupportedCommandException(command);

            }

        } catch (RuntimeException e) {

            // Gets an object of type ErrorInfo, containing information about
            // raised exception
            ErrorInfo ex = ErrorHandler.handler(e);

            batchResponse.setStatusCode(ex.getStatusCode());
            batchResponse.setBody(ex.getMessage());

        }

        return batchResponse;
    }
}
