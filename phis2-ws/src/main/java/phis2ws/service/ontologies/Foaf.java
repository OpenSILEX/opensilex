//******************************************************************************
//                                       Foaf.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 11 sept. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.ontologies;

/**
 * The elements of the ontology foaf used.
 * @see http://xmlns.com/foaf/spec/
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public enum Foaf {
    NAMESPACE {
        @Override
        public String toString() {
            return "http://xmlns.com/foaf/0.1/";
        }
    },
    
    CONCEPT_AGENT {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "Agent";
        }
    },
    CONCEPT_GROUP {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "Group";
        }
    },
    CONCEPT_PERSON {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "Person";
        }
    }
}