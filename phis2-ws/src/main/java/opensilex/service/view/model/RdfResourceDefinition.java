//**********************************************************************************************
//                                       RdfResourceDefinition.java 
//
// Author(s): Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: November, 15 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  November, 15 2017
// Subject: Represents the instance definiton view
//***********************************************************************************************
package opensilex.service.view.model;

import java.util.ArrayList;

/**
 * Represents the view of an instance in the triplestore. 
 * With its properties and ontology references
 * @see Property
 * @see OntologyReference
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class RdfResourceDefinition {
    // Instance uri. e.g. http://www.phenome-fppn.fr/diaphen/id/variable/v001
    protected String uri;
    // The rdfs:label of the instance. e.g. sf1
    protected String label;
    // Comment on the instance
    protected String comment;
    
    // List of the ontologies references associated to the instance
    protected ArrayList<OntologyReference> ontologiesReferences = new ArrayList<>();
    
    //List of the properties associated to the instance
    protected ArrayList<Property> properties = new ArrayList<>();
    
    public RdfResourceDefinition() {
        
    }
    
    public RdfResourceDefinition(String uri) {
        this.uri = uri;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public ArrayList<OntologyReference> getOntologiesReferences() {
        return ontologiesReferences;
    }

    public void setOntologiesReferences(ArrayList<OntologyReference> ontologiesReferences) {
        this.ontologiesReferences = ontologiesReferences;
    }
    
    public void addOntologyReference(OntologyReference ontologyReference) {
        ontologiesReferences.add(ontologyReference);
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
    
    public boolean hasProperty(Property property) {
        return properties.contains(property);
    }

    public Property getProperty(Property property) {
        int index = properties.indexOf(property);

        if (index >= 0) {
            return properties.get(index);
        } else {
            return null;
        }
    }
}
