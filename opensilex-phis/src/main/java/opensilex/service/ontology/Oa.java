//******************************************************************************
//                                Oa.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 11 Sept. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.ontology;

/**
 * Vocabulary definition from the OA ontology.
 * @see https://www.w3.org/ns/oa
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public enum Oa {
    NAMESPACE {
        @Override
        public String toString() {
            return "http://www.w3.org/ns/oa#";
        }
    },
    
    CONCEPT_ANNOTATION {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "Annotation";
        }
    },
    
    CONCEPT_MOTIVATION {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "Motivation";
        }
    },
    
    INSTANCE_DESCRIBING {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "describing";
        }
    },
    
    RELATION_BODY_VALUE {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "bodyValue";
        }
    },
    RELATION_HAS_TARGET {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "hasTarget";
        }
    },
    RELATION_MOTIVATED_BY {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "motivatedBy";
        }
    }
}