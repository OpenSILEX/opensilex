//******************************************************************************
//                          GenericExceptionMapper.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 1 Apr. 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server.rest;

import javax.ws.rs.WebApplicationException;
import org.opensilex.server.response.ErrorResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Handles generic exceptions in web services and displays them as JSON.
 *
 * @author Vincent Migot <vincent.migot@inra.fr>
 */
@Provider
public class ExceptionJsonMapper implements ExceptionMapper<Throwable> {

    /**
     * Converts the exception to JSON
     *
     * @param exception
     * @return JSON error response
     */
    @Override
    public Response toResponse(Throwable exception) {
        if (exception instanceof WebApplicationException) {
            WebApplicationException webAppException = (WebApplicationException) exception;
            Response exceptionResponse = webAppException.getResponse();
            return new ErrorResponse(
                    Status.fromStatusCode(exceptionResponse.getStatus()),
                    exceptionResponse.getStatusInfo().getFamily().toString(),
                    exceptionResponse.getStatusInfo().getReasonPhrase()
                    
            ).getResponse();
        } else {
            return new ErrorResponse(exception).getResponse();
        }
    }

}
