//******************************************************************************
//                               Rdfs.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 11 Sept. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.ontology;

/**
 * Vocabulary definition from the RDFS ontology.
 * @see https://www.w3.org/TR/rdf-schema/
 * @see https://www.w3.org/2000/01/rdf-schema#
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public enum Rdfs {
    NAMESPACE {
        @Override
        public String toString() {
            return "http://www.w3.org/2000/01/rdf-schema#";
        }
    },
    RELATION_DOMAIN {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "domain";
        }
    },
    RELATION_RANGE {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "range";
        }
    },
    RELATION_COMMENT {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "comment";
        }
    },
    RELATION_LABEL {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "label";
        }
    },
    RELATION_SEE_ALSO {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "seeAlso";
        }
    },
    RELATION_SUBCLASS_OF {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "subClassOf";
        }
    },
    RELATION_SUBPROPERTY_OF {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "subPropertyOf";
        }
    }
}
