//******************************************************************************
//                                  time.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 11 dec. 2018
// Contact: andreas.garcia@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.ontologies;

/**
 * Time ontology
 * @see https://www.w3.org/TR/owl-time/
 * @author Andréas Garcia <andreas.garcia@inra.fr>
 */
public enum Time {
    NAMESPACE {
        @Override
        public String toString() {
            return "http://www.w3.org/2006/time#";
        }
    }, 
    
    RELATION_HAS_TIME {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "hasTime";
        }
    }, 
    
    RELATION_IN_XSD_DATETIMESTAMP {
        @Override
        public String toString() {
            return NAMESPACE.toString() + "inXSDDateTimeStamp";
        }
    }
}