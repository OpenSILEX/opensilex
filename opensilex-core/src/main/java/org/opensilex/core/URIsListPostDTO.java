/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core;

import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import java.util.List;
import org.opensilex.server.rest.validation.ValidURI;

/**
 *
 * @author Alice Boizet
 */
public class URIsListPostDTO {
    
    @ValidURI
    protected List<URI> uris;

    public List<URI> getUris() {
        return uris;
    }

    public void setUris(List<URI> uris) {
        this.uris = uris;
    }
    
}
