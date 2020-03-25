/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.rest.sparql.dto;

import java.net.URI;

/**
 *
 * @author vince
 */
public class ResourceGetDTO {
       
    protected URI uri;
    
    protected URI type;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public URI getType() {
        return type;
    }

    public void setType(URI type) {
        this.type = type;
    }
    
    
}
