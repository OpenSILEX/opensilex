/*******************************************************************************
 *                         BadRequestException.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2021.
 * Last Modification: 07/12/2021
 * Contact: valentin.rigolle@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/
package org.opensilex.server.exceptions;

import org.opensilex.server.response.ErrorResponse;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Exception representing a bad request
 *
 * @author Valentin RIGOLLE
 */
public class BadRequestException extends WebApplicationException {

    public BadRequestException(String message) {
        super(Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(
                        Response.Status.BAD_REQUEST,
                        Response.Status.BAD_REQUEST.getReasonPhrase(),
                        message))
                .type(MediaType.APPLICATION_JSON)
                .build()
        );
    }
}
