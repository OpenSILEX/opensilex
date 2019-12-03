/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.server.exceptions;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.opensilex.server.response.ErrorResponse;

public class UnauthorizedException extends WebApplicationException {

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
