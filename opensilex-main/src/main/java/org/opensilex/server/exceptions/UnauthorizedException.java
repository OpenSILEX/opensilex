//******************************************************************************
//                        UnauthorizedException.java
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
 * This class represent an unauthorized exception (401 - UNAUTHORIZED).
 *
 * It should only be used for authentication errors (ie: invalid credentials or token decoding issues)
 *
 * Do not confuse it with {@code org.opensilex.server.exceptions.ForbiddenException}
 * which should be use for all other access rights restrictriction.
 * </pre>
 *
 * @author Vincent Migot
 */
public class UnauthorizedException extends WebApplicationException {

    /**
     * Exception constructor.
     */
    public UnauthorizedException() {
        super(Response.status(Response.Status.UNAUTHORIZED)
                .entity(new ErrorResponse(
                        Response.Status.UNAUTHORIZED,
                        "Access denied",
                        "You must be authenticate and having the right authorizations to access this URL"))
                .type(MediaType.APPLICATION_JSON)
                .build()
        );
    }
}
