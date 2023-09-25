package org.opensilex.server.exceptions;

import org.opensilex.server.response.ErrorResponse;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class BadGatewayException extends WebApplicationException {

    public BadGatewayException(String message) {
        super(Response.status(Response.Status.BAD_GATEWAY)
                .entity(new ErrorResponse(
                        Response.Status.BAD_GATEWAY,
                        Response.Status.BAD_GATEWAY.getReasonPhrase(),
                        message))
                .type(MediaType.APPLICATION_JSON)
                .build()
        );
    }
}
