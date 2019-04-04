//******************************************************************************
//                                Skos.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 11 Sept. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.ontology;

/**
 * Vocabulary definition from the SKOS ontology.
 * @see http://www.w3.org/2008/05/skos
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public enum Skos {
    NAMESPACE {
        @Override
        public String toString() {
            return "http://www.w3.org/2008/05/skos#";
        }
    },
    
    RELATION_BROADER {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "broader";
        }
    },
    RELATION_CLOSE_MATCH {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "closeMatch";
        }
    },
    RELATION_EXACT_MATCH {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "exactMatch";
        }
    },
    RELATION_NARROWER {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "narrower";
        }
    },
    RELATION_PREF_LABEL {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "prefLabel";
        }
    }
}
