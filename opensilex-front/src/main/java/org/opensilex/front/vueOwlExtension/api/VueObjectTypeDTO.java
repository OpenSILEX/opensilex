/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front.vueOwlExtension.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Map;
import org.opensilex.core.ontology.api.RDFTypeDTO;
import org.opensilex.core.ontology.api.RDFTypeTranslatedDTO;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;

/**
 *
 * @author vmigot
 */
public class VueObjectTypeDTO {

    @ValidURI
    protected URI uri;

    @ValidURI
    @JsonProperty("short_uri")
    protected URI shortUri;
    
    @JsonProperty("input_component")
    protected String inputComponent;
    
    @JsonProperty("input_components_by_property")
    protected Map<String, String> inputComponentsByProperty;
    
    @JsonProperty("view_component")
    protected String viewComponent;
    
    protected String name;

    @JsonProperty("rdf_type")
    protected RDFTypeTranslatedDTO rdfClass;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public URI getShortUri() {
        return SPARQLDeserializers.formatURI(getUri());
    }

    public String getInputComponent() {
        return inputComponent;
    }

    public void setInputComponent(String inputComponent) {
        this.inputComponent = inputComponent;
    }

    public Map<String, String> getInputComponentsByProperty() {
        return inputComponentsByProperty;
    }

    public void setInputComponentsByProperty(Map<String, String> inputComponentsByProperty) {
        this.inputComponentsByProperty = inputComponentsByProperty;
    }

    public String getViewComponent() {
        return viewComponent;
    }

    public void setViewComponent(String viewComponent) {
        this.viewComponent = viewComponent;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RDFTypeTranslatedDTO getRdfClass() {
        return rdfClass;
    }

    public void setRdfClass(RDFTypeTranslatedDTO rdfClass) {
        this.rdfClass = rdfClass;
    }

}
