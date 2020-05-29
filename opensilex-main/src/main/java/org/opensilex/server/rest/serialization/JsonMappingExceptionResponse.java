/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.server.rest.serialization;

import com.fasterxml.jackson.databind.JsonMappingException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import org.opensilex.server.response.ErrorResponse;

@Provider
public class JsonMappingExceptionResponse implements ExceptionMapper<JsonMappingException> {

    @Override
    public Response toResponse(JsonMappingException exception) {
        return new ErrorResponse(
                Response.Status.BAD_REQUEST, "Unable to parse JSON input",
                exception
        ).getResponse();
    }
}
