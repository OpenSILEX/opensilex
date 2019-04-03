//******************************************************************************
//                             RdfResourceDefinition.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: 15 November 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.model;

import java.util.ArrayList;

/**
 * RDF resource model. 
 * @see Property
 * @see OntologyReference
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class RdfResourceDefinition {
    
    /**
     * Instance URI.
     * @example http://www.phenome-fppn.fr/diaphen/id/variable/v001
     */
    protected String uri;
    
    /**
     * The rdfs:label of the instance. 
     * @example sf1
     */
    protected String label;
    
    /**
     * Comment on the instance.
     */
    protected String comment;
    
    /**
     * List of the ontology references associated to the instance.
     */
    protected ArrayList<OntologyReference> ontologiesReferences = new ArrayList<>();
    
    /**
     * List of the properties associated to the instance
     */
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
