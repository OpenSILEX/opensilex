//******************************************************************************
//                                       DublinCore.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 11 sept. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.ontologies;

/**
 * The elements of the ontology dublin core used.
 * @see http://dublincore.org/
 * @see http://dublincore.org/2012/06/14/dcterms
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public enum DublinCore {
    NAMESPACE {
        @Override
        public String toString() {
            return "http://purl.org/dc/terms/";
        }
    },
    
    RELATION_DATE {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "date";
        }
    },
    RELATION_CREATED {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "created";
        }
    },
    RELATION_CREATOR {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "creator";
        }
    },
    RELATION_FORMAT {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "format";
        }
    },
    RELATION_LANGUAGE {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "language";
        }
    },
    RELATION_TITLE {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "title";
        }
    },
}
