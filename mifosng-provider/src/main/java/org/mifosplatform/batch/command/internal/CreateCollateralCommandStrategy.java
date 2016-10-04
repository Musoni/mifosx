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
import org.mifosplatform.portfolio.collateral.api.CollateralsApiResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.UriInfo;

/**
 * Created by ishan on 04/10/16.
 */
@Component
public class CreateCollateralCommandStrategy implements CommandStrategy {

    private final CollateralsApiResource collateralsApiResource;

    @Autowired
    public CreateCollateralCommandStrategy(final CollateralsApiResource collateralsApiResource) {
        this.collateralsApiResource = collateralsApiResource;
    }

    @Override
    public BatchResponse execute(BatchRequest batchRequest, UriInfo uriInfo) {

        final BatchResponse response = new BatchResponse();
        final String responseBody;

        response.setRequestId(batchRequest.getRequestId());
        response.setHeaders(batchRequest.getHeaders());

        final String[] pathParameters = batchRequest.getRelativeUrl().split("/");
        Long loanId = Long.parseLong(pathParameters[1]);

        try {

            responseBody = collateralsApiResource.createCollateral(loanId, batchRequest.getBody());
            response.setBody(responseBody);
            response.setStatusCode(200);

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
