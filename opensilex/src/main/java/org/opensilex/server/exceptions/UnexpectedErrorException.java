//******************************************************************************
//                        UnexpectedErrorException.java
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
 * This class represent an unexpected exception (500 - INTERNAL_SERVER_ERROR).
 *
 * It should not be thrown manually anywhere because it's automatically handled
 * by {@code org.opensilex.server.security.AuthenticationFilter} when it catch
 * an exception.
 * </pre>
 *
 * @author Vincent Migot
 */
public class UnexpectedErrorException extends WebApplicationException {

    public UnexpectedErrorException(Throwable ex) {
        super(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse(ex))
                .type(MediaType.APPLICATION_JSON)
                .build()
        );
    }
}
