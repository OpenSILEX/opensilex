/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.List;
import org.opensilex.core.ontology.dal.URITypesModel;

/**
 *
 * @author boizetal
 */
public class URITypesDTO {
    protected URI uri;
    
    @JsonProperty("rdf_types")
    protected List<URI> rdfTypes;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public List<URI> getRdfTypes() {
        return rdfTypes;
    }

    public void setRdfTypes(List<URI> rdfTypes) {
        this.rdfTypes = rdfTypes;
    }
    
    public static URITypesDTO fromModel(URITypesModel model) {
        URITypesDTO dto = new URITypesDTO();
        dto.setUri(model.getUri());
        dto.setRdfTypes(model.getRdfTypes());
        return dto;
    }
    
}
