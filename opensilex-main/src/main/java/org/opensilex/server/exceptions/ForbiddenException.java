//******************************************************************************
//                        ForbiddenException.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server.exceptions;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.opensilex.server.response.ErrorResponse;

/**
 * <pre>
 * This class represent a forbidden exception (403 - FORBIDDEN).
 * 
 * It should be use for any access rights restrictriction.
 * 
 * Do not confuse it with {@code org.opensilex.server.exceptions.UnauthorizedException} 
 * which should only be used for authentication errors (ie: invalid credentials or token decoding issues)
 * </pre>
 *
 * @author Vincent Migot
 */
public class ForbiddenException extends WebApplicationException {

    public ForbiddenException(String message) {
        super(Response.status(Response.Status.FORBIDDEN)
                .entity(new ErrorResponse(
                        Response.Status.FORBIDDEN,
                        "Access denied",
                        message))
                .type(MediaType.APPLICATION_JSON)
                .build()
        );
    }

}
