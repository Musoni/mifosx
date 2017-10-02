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
import org.mifosplatform.portfolio.group.api.GroupsApiResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.UriInfo;

@Component
public class CreateGroupCommandStrategy  implements CommandStrategy {

    private final GroupsApiResource groupsApiResource;

    @Autowired
    private CreateGroupCommandStrategy(GroupsApiResource groupsApiResource) {
        this.groupsApiResource = groupsApiResource;
    }

    @Override
    public BatchResponse execute(BatchRequest batchRequest, UriInfo uriInfo) {
        final BatchResponse response = new BatchResponse();
        final String responseBody;

        response.setRequestId(batchRequest.getRequestId());
        response.setHeaders(batchRequest.getHeaders());

        // Try-catch blocks to map exceptions to appropriate status codes
        try {

            responseBody = groupsApiResource.create(batchRequest.getBody());

            response.setStatusCode(200);
            // Sets the body of the response after Group is successfully created
            response.setBody(responseBody);

        } catch (RuntimeException e) {

            // Gets an object of type ErrorInfo, containing information about
            // raised exception
            ErrorInfo ex = ErrorHandler.handler(e);

            response.setStatusCode(ex.getStatusCode());
            response.setBody(ex.getMessage());
        }

        return response;
    }
}
