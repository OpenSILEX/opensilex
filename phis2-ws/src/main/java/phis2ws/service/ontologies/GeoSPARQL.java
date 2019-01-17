//******************************************************************************
//                                       GeoSPARQL.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 11 sept. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.ontologies;

/**
 * Used to get the vocabulary of GeoSPARQL.
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
            return NAMESPACE.toString() + RELATION_CONTAINS.toString() + "*";
        }
    }
}
