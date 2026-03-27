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

/**
 *
 * @author vmigot
 */
public class InvalidValueException extends WebApplicationException {
    private final String message;

    /**
     * Exception constructor.
     *
     * @param message exception detail
     */
    public InvalidValueException(String message) {
        super(Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(
                        Response.Status.BAD_REQUEST,
                        "Invalid parameter",
                        message))
                .type(MediaType.APPLICATION_JSON)
                .build()
        );
        this.message = message;
    }

    /**
     * @return the error message included in the JSON payload
     */
    public String getErrorMessage() {
        return message;
    }
}
