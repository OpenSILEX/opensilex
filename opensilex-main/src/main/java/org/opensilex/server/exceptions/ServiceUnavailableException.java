//******************************************************************************
//                        ServiceUnavailableException.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2023
// Contact: anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server.exceptions;

import org.opensilex.server.response.ErrorResponse;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Exception that can be caught by the API layer to return it as a serviceUnavailable error response
 * @author Yvan Roux
 */
public class ServiceUnavailableException extends WebApplicationException {

    /**
     * Exception constructor.
     * @param message exception detail
     */
    public ServiceUnavailableException(String message) {
        super(Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity(new ErrorResponse(
                        Response.Status.SERVICE_UNAVAILABLE,
                        "Service Unavailable",
                        message))
                .type(MediaType.APPLICATION_JSON)
                .build()
        );
    }

}
