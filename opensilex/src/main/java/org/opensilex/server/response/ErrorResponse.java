/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.server.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import javax.ws.rs.core.Response.Status;

/**
 * Internal class used to format the exception, removing useless information.
 */
@JsonInclude(Include.NON_NULL)
public class ErrorResponse extends JsonResponse<ErrorResult> {

    /**
     * Constructor
     *
     * @param exception Exception to wrap into a structured error response
     */
    public ErrorResponse(Throwable exception) {
        super(Status.INTERNAL_SERVER_ERROR);
        metadata = new Metadata(new Pagination());
        result = new ErrorResult(
                "Unexpected internal error - " + exception.getClass().getCanonicalName(),
                exception.getMessage(),
                exception
        );
    }

    public ErrorResponse(Status status, String title, String message) {
        super(status);
        metadata = new Metadata(new Pagination());
        result = new ErrorResult(title, message);
    }

}
