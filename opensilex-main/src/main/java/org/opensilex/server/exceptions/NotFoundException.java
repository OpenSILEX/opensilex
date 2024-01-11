//******************************************************************************
//                        NotFoundException.java
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
 * This class represent a not fount exception (404 - NOT_FOUND).
 *
 * It should be use if requested resource does not exists.
 * </pre>
 *
 * @author Vincent Migot
 */
public class NotFoundException extends WebApplicationException {

    /**
     * Exception constructor.
     *
     * @param message exception detail
     */
    public NotFoundException(String message) {
       this(message, "Resource not found");
    }

    public NotFoundException(String message, String title) {
        super(Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse(
                        Response.Status.NOT_FOUND,
                        title,
                        message))
                .type(MediaType.APPLICATION_JSON)
                .build()
        );
    }

}
