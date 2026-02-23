/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.security.authentication;

import org.opensilex.server.response.ErrorResponse;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Collection;

public class ForbiddenURIAccessException extends WebApplicationException {

    public ForbiddenURIAccessException(URI uri) {
        super("Forbidden access to URI: " + uri);
    }

    public ForbiddenURIAccessException(URI uri, String message) {
        super(Response.status(Response.Status.FORBIDDEN)
                .entity(new ErrorResponse(
                        Response.Status.FORBIDDEN,
                        "URI access forbidden",
                        String.format("Forbidden access to URI : %s \n %s", uri, message)
                ))
                .type(MediaType.APPLICATION_JSON)
                .build()
        );
    }

    public ForbiddenURIAccessException(Collection<?> uris, String message) {
        super(Response.status(Response.Status.FORBIDDEN)
                .entity(new ErrorResponse(
                        Response.Status.FORBIDDEN,
                        "URI access forbidden",
                        String.format("Forbidden access to URI list : %s \n %s", uris, message)
                ))
                .type(MediaType.APPLICATION_JSON)
                .build()
        );
    }
}
