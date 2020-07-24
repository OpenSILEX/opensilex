/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front.datatypes;

import java.net.URI;
import java.net.URISyntaxException;
import org.opensilex.sparql.deserializer.SPARQLDeserializer;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author vmigot
 */
public class DatatypeComponents {

    private final static Logger LOGGER = LoggerFactory.getLogger(DatatypeComponents.class);

    private URI uri;

    private String inputComponent;

    private String viewComponent;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public String getInputComponent() {
        return inputComponent;
    }

    public void setInputComponent(String inputComponent) {
        this.inputComponent = inputComponent;
    }

    public String getViewComponent() {
        return viewComponent;
    }

    public void setViewComponent(String viewComponent) {
        this.viewComponent = viewComponent;
    }

    public static DatatypeComponents fromString(String uri, String inputComponent, String viewComponent) {
        try {
            DatatypeComponents dtc = new DatatypeComponents();
            dtc.setUri(SPARQLDeserializers.formatURI(new URI(uri)));
            dtc.setInputComponent(inputComponent);
            dtc.setViewComponent(viewComponent);
            
            return dtc;
        } catch (URISyntaxException ex) {
            LOGGER.error("Invalid primitive datatype URI (should never happend): " + uri, ex);
        }
        
        return null;
    }

}
