//******************************************************************************
//                      ExceptionJsonMapper.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.security.authentication;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.opensilex.server.response.ErrorResponse;
import org.opensilex.sparql.exceptions.SPARQLInvalidUriListException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles generic exceptions in web services and displays them as JSON.
 *
 * @see org.opensilex.server.response.ErrorResponse
 * @author Vincent Migot
 */
@Provider
public class ExceptionJsonMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionJsonMapper.class);

    @Context
    UriInfo uriInfo;

    /**
     * Converts the exception to JSON
     *
     * @param exception Exception to convert
     * @return JSON error response
     */
    @Override
    public Response toResponse(Throwable exception) {
        final Response response;
        /*
         * @todo Get rid of this if / else statement
         *   The goal is to make every exception referenced here extend `WebApplicationException`, so that every case
         *   is handled by one condition.
         */
        if (exception instanceof ForbiddenURIAccessException) { //@todo get rid of this
            response = new ErrorResponse(
                    Response.Status.FORBIDDEN, "URI access forbidden",
                    exception.getMessage()
            ).getResponse();
        } else if (exception instanceof NotFoundURIException) { //@todo get rid of this
            response = new ErrorResponse(
                    Response.Status.NOT_FOUND, "URI not found",
                    exception.getMessage()
            ).getResponse();
        } else if(exception instanceof SPARQLInvalidUriListException){ //@todo get rid of this
            response = new ErrorResponse(
                    Response.Status.NOT_FOUND, "URIs not found",
                    exception.getMessage()
            ).getResponse();
        } else if(exception instanceof IllegalArgumentException) { //@todo get rid of this
            response = new ErrorResponse(Status.BAD_REQUEST, Status.BAD_REQUEST.getReasonPhrase(), exception.getMessage())
                    .getResponse();
        }
        else if (exception instanceof WebApplicationException) { //@todo most cases should be handled by this
            WebApplicationException webAppException = (WebApplicationException) exception;
            Response exceptionResponse = webAppException.getResponse();
            Object entity = exceptionResponse.getEntity();
            if (entity instanceof ErrorResponse) {
                response = ((ErrorResponse) entity).getResponse();
            } else {
                response = new ErrorResponse(
                        Status.fromStatusCode(exceptionResponse.getStatus()),
                        exceptionResponse.getStatusInfo().getFamily().toString(),
                        exceptionResponse.getStatusInfo().getReasonPhrase()
                ).getResponse();
            }
        } else if (exception instanceof RuntimeException && exception.getCause() != null) {
            response = new ErrorResponse(exception.getCause()).getResponse();
        } else {
            response = new ErrorResponse(exception).getResponse();
        }
        if (response.getStatus() >= 500) {
            LOGGER.error("Unexpected exception returned to user service call: \n" + uriInfo.getAbsolutePath().toString(), exception);
        }
        return response;
    }

}
