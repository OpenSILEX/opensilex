/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.server.response;

import java.net.URI;
import javax.ws.rs.core.Response.Status;

/**
 *
 * @author Vincent Migot
 */
public class ObjectCreationResponse extends JsonResponse {

    private URI uri;
    
    public ObjectCreationResponse(URI uri) {
        super(Status.CREATED);
        this.uri = uri;
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }
    
}
