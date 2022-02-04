/*******************************************************************************
 *                         ConflictException.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2022.
 * Last Modification: 04/01/2022
 * Contact: valentin.rigolle@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/
package org.opensilex.server.exceptions;

import org.opensilex.server.response.ErrorResponse;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * This class represent a conflict exception (409 - CONFLICT).
 * It should be used if the request cannot be executed because
 * it conflicts with the current state.
 *
 * See the <a href="https://httpwg.org/specs/rfc7231.html#status.409">HTTP specification</a>.
 *
 * @author Valentin RIGOLLE
 */
public class ConflictException extends WebApplicationException {
    public ConflictException(String message) {
        super(Response.status(Response.Status.CONFLICT)
                .entity(new ErrorResponse(
                        Response.Status.CONFLICT,
                        "Conflict",
                        message))
                .type(MediaType.APPLICATION_JSON)
                .build()
        );
    }
}
