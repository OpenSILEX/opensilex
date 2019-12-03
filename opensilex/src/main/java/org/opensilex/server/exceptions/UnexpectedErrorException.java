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

public class UnexpectedErrorException extends WebApplicationException {

    public UnexpectedErrorException(Throwable ex) {
        super(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse(ex))
                .type(MediaType.APPLICATION_JSON)
                .build()
        );
    }
}
