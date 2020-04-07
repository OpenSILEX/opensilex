//******************************************************************************
//                              OType.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 7 March 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.configuration;

/**
 * Object type values in the triplet DTO.
 * @see TripletDTO
 * @see https://www.w3.org/wiki/JSON_Triple_Sets
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public enum OType {
    LITERAL {
        @Override
        public String toString() {
            return "literal";
        }
    },
    URI {
        @Override
        public String toString() {
            return "uri";
        }
    }
}
