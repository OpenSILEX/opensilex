//******************************************************************************
//                                  Foaf.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 11 Sept. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.ontology;

/**
 * Vocabulary definition from the FOAF ontology.
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