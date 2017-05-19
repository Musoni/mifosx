/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.batch.command.internal;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minidev.json.parser.JSONParser;
import org.mifosplatform.batch.command.CommandStrategy;
import org.mifosplatform.batch.domain.BatchRequest;
import org.mifosplatform.batch.domain.BatchResponse;
import org.mifosplatform.batch.exception.ErrorHandler;
import org.mifosplatform.batch.exception.ErrorInfo;
import org.mifosplatform.portfolio.loanaccount.api.LoansApiResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.UriInfo;
import java.text.ParseException;

@Component
public class CollectLoanTemplateCommandStrategy implements CommandStrategy {

    private final LoansApiResource loansApiResource;

    @Autowired
    private CollectLoanTemplateCommandStrategy(final LoansApiResource loansApiResource) {
        this.loansApiResource = loansApiResource;
    }

    @Override
    public BatchResponse execute(BatchRequest request, @SuppressWarnings("unused") UriInfo uriInfo) {
        final BatchResponse response = new BatchResponse();
        final String responseBody;

        response.setRequestId(request.getRequestId());
        response.setHeaders(request.getHeaders());


        // Pluck out the loanId out of the relative path
        final String body = request.getBody();
        JsonParser parse = new JsonParser();
        JsonObject jsonBody = (JsonObject) parse.parse(body);

        final Long clientId = (jsonBody.has("clientId")) ? jsonBody.get("clientId").getAsLong() : null ;
        final Long productId =  (jsonBody.has("productId")) ? jsonBody.get("productId").getAsLong() : null;
        final String templateType = (jsonBody.has("templateType")) ? jsonBody.get("templateType").getAsString() : null;
        final Long groupId  = (jsonBody.has("groupId")) ? jsonBody.get("groupId").getAsLong() : null;


        // Try-catch blocks to map exceptions to appropriate status codes
        try {

            // Calls 'retrieveAllLoanCharges' function from
            // 'LoanChargesApiResource' to Collect
            // Charges for a loan
            responseBody = loansApiResource.template(clientId,groupId,productId,templateType,false,true,uriInfo);

            response.setStatusCode(200);
            // Sets the body of the response after Charges have been
            // successfully collected
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
