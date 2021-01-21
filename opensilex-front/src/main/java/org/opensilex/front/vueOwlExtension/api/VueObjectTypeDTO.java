/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front.vueOwlExtension.api;

import java.net.URI;
import java.util.Map;
import org.opensilex.core.ontology.api.RDFClassDTO;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;

/**
 *
 * @author vmigot
 */
public class VueObjectTypeDTO {

    protected URI uri;
    protected URI shortUri;
    protected String inputComponent;
    protected Map<String, String> inputComponentsByProperty;
    protected String viewComponent;
    protected String label;

    protected RDFClassDTO rdfClass;

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

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public RDFClassDTO getRdfClass() {
        return rdfClass;
    }

    public void setRdfClass(RDFClassDTO rdfClass) {
        this.rdfClass = rdfClass;
    }

}
