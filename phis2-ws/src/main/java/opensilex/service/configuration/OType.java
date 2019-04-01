//******************************************************************************
//                                       OType.java
//
// Author(s): Morgane Vidal <morgane.vidal@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 7 mars 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  7 mars 2018
// Subject: an enum used for the possible values of the type of an object in the 
// tripletDTO
//******************************************************************************
package opensilex.service.configuration;

/**
 * Used to get the values of the o_type in the TripletDTO.
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
