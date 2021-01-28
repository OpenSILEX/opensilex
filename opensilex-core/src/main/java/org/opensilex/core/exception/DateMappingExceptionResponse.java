//******************************************************************************
//                          DateMappingExceptionResponse.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import org.opensilex.server.response.ErrorResponse;

/**
 *
 * @author Alice Boizet
 */
@Provider
public class DateMappingExceptionResponse implements ExceptionMapper<DateValidationException> {
    
    @Override
    public Response toResponse(DateValidationException exception) {
        if (exception instanceof TimezoneAmbiguityException) {
            return new ErrorResponse(
                Response.Status.BAD_REQUEST, "TIMEZONE_AMBIGUITY",
                exception
            ).getResponse();
        } else if (exception instanceof UnableToParseDateException) {
            return new ErrorResponse(
                   Response.Status.BAD_REQUEST, "WRONG_DATE_FORMAT",
                   exception
            ).getResponse();
        } else {
            return new ErrorResponse(
                Response.Status.BAD_REQUEST, "TIMEZONE_ERROR",
                exception
            ).getResponse();
        }
    }
}
