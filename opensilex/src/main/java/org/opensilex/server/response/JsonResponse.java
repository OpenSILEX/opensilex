//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server.response;

import com.fasterxml.jackson.annotation.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.*;

/**
 *
 * @author Vincent Migot
 */
public abstract class JsonResponse<T> {

    @JsonIgnore
    protected final Status status;

    protected Metadata metadata;
    
    protected T result;
    
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
