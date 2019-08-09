/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.server.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 *
 * @author vincent
 */
public abstract class JsonResponse {

    protected final Status status;

    public JsonResponse(Status status) {
        this.status = status;
    }

    @JsonIgnore
    public Response getResponse() {
        return Response.status(status)
                .entity(this)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
