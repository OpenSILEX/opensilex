//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server.rest;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import org.opensilex.server.response.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Handles generic exceptions in web services and displays them as JSON.
 *
 * @author Vincent Migot
 */
@Provider
public class ExceptionJsonMapper implements ExceptionMapper<Throwable> {

    final private static Logger LOGGER = LoggerFactory.getLogger(ExceptionJsonMapper.class);

    /**
     * Converts the exception to JSON
     *
     * @param exception Exception to convert
     * @return JSON error response
     */
    @Override
    public Response toResponse(Throwable exception) {
        final Response response;
        if (exception instanceof WebApplicationException) {
            WebApplicationException webAppException = (WebApplicationException) exception;
            Response exceptionResponse = webAppException.getResponse();
            response = new ErrorResponse(
                    Status.fromStatusCode(exceptionResponse.getStatus()),
                    exceptionResponse.getStatusInfo().getFamily().toString(),
                    exceptionResponse.getStatusInfo().getReasonPhrase()
            ).getResponse();
        } else {
            response = new ErrorResponse(exception).getResponse();
        }

        LOGGER.debug("Exception returned to user service call", exception);
        return response;
    }

}
