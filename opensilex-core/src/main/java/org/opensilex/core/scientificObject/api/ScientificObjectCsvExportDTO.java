/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.scientificObject.api;

import java.net.URI;
import java.util.List;
import org.opensilex.server.rest.validation.ValidURI;

/**
 *
 * @author vince
 */
public class ScientificObjectCsvExportDTO {

    @ValidURI
    private List<URI> objectURIs;

    @ValidURI
    private URI contextURI;

    public List<URI> getObjectURIs() {
        return objectURIs;
    }

    public void setObjectURIs(List<URI> objectURIs) {
        this.objectURIs = objectURIs;
    }

    public URI getContextURI() {
        return contextURI;
    }

    public void setContextURI(URI contextURI) {
        this.contextURI = contextURI;
    }

}
