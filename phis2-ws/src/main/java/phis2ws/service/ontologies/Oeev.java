//******************************************************************************
//                                  Oeev.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 14 nov. 2018
// Contact: andreas.garcia@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.ontologies;

/**
 * Elements of the Oeev ontology used in the ws
 * @see http://agroportal.lirmm.fr/ontologies/OEEV
 * @author Andréas Garcia <andreas.garcia@inra.fr>
 */
public enum Oeev {
    NAMESPACE {
        @Override
        public String toString() {
            return "http://www.opensilex.org/vocabulary/oeev#";
        }
    },
    
    CONCEPT_EVENT {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "Event";
        }
    }, 
    
    RELATION_CONCERNS {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "concerns";
        }
    }, 
    
    RELATION_FROM {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "from";
        }
    }, 
    
    RELATION_TO {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "to";
        }
    }
}