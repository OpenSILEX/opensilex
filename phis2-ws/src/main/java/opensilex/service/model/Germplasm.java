//******************************************************************************
//                                Accession.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 1 juil. 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.model;

import java.util.ArrayList;
import opensilex.service.resource.dto.rdfResourceDefinition.PropertyPostDTO;

/**
 * Model of the germplasm
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class Germplasm {
    private String uri;
    private String rdfType;
    private String label;
    private ArrayList<Property> properties = new ArrayList<>();       

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getRdfType() {
        return rdfType;
    }

    public void setRdfType(String rdfType) {
        this.rdfType = rdfType;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public ArrayList<Property> getProperties() {
        return properties;
    }

    public void setProperties(ArrayList<Property> properties) {
        this.properties = properties;
    }
    
    public void addProperty(Property property) {
        this.properties.add(property);
    }
}
