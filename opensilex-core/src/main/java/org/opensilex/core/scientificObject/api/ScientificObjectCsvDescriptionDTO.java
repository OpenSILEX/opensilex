/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.scientificObject.api;

import java.net.URI;

/**
 *
 * @author vmigot
 */
public class ScientificObjectCsvDescriptionDTO {

    private URI experimentURI;

    private URI scientificObjectType;

    public URI getExperimentURI() {
        return experimentURI;
    }

    public void setExperimentURI(URI experimentURI) {
        this.experimentURI = experimentURI;
    }

    public URI getScientificObjectType() {
        return scientificObjectType;
    }

    public void setScientificObjectType(URI scientificObjectType) {
        this.scientificObjectType = scientificObjectType;
    }

}
