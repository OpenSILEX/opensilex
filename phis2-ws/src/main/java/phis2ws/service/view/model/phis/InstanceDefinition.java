//**********************************************************************************************
//                                       InstanceDefinition.java 
//
// Author(s): Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: November, 15 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  November, 15 2017
// Subject: Represents the instance definiton view
//***********************************************************************************************
package phis2ws.service.view.model.phis;

import java.util.ArrayList;

public class InstanceDefinition {
    
    /**
     * @attribute uri l'uri de l'instance (ex. http://www.phenome-fppn.fr/diaphen/id/variable/v001)
     * @attribute label le label de la variable (ex. surface foliaire)
     * @attribute comment un commentaire sur l'instance
     * @attribute ontologiesReferences des références vers des ontologies externes (ex. une référence skos vers la crop onto)
     */
    protected String uri;
    protected String label;
    protected String comment;
    
    ArrayList<OntologyReference> ontologiesReferences = new ArrayList<>();
    
    public InstanceDefinition() {
        
    }
    
    public InstanceDefinition(String uri) {
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
}
