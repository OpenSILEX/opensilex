//******************************************************************************
//                               Owl.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 11 Sept. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.ontology;

/**
 * Vocabulary definition from the OWL ontology.
 * @see https://www.w3.org/OWL/
 * @see http://www.w3.org/2002/07/owl#
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public enum Owl {
    NAMESPACE {
        @Override
        public String toString() {
            return "http://www.w3.org/2002/07/owl#";
        }
    },
    
    CONCEPT_RESTRICTION {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "Restriction";
        }
    },
    
    RELATION_CARDINALITY {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "cardinality";
        }
    },
    RELATION_MAX_CARDINALITY {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "maxCardinality";
        }
    },
    RELATION_MIN_CARDINALITY {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "minCardinality";
        }
    },
    RELATION_ON_PROPERTY {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "onProperty";
        }
    },
    RELATION_QUALIFIED_CARDINALITY {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "qualifiedCardinality";
        }
    },
    RELATION_UNION_OF {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "unionOf";
        }
    }
}
