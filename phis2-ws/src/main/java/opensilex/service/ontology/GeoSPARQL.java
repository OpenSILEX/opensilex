//******************************************************************************
//                                GeoSPARQL.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 11 Sept. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.ontology;

/**
 * Vocabulary definition from the GeoSPARQL ontology.
 * @see http://www.opengis.net/ont/geosparql#
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public enum GeoSPARQL {
    NAMESPACE {
        @Override
        public String toString() {
            return "http://www.opengis.net/ont/geosparql#";
        }
    },
    
    RELATION_CONTAINS {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "contains";
        }
    },
    RELATION_CONTAINS_MULTIPLE {
        @Override
        public String toString() {
            return RELATION_CONTAINS.toString() + "*";
        }
    }
}
