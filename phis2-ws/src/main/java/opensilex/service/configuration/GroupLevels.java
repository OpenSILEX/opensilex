//******************************************************************************
//                              GroupLevels.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 6 Aug. 2018
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.configuration;

/**
 * List of authorized group levels.
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>, Morgane Vidal <morgane.vidal@inra.fr>
 */
public enum GroupLevels {
    GUEST {
        @Override
        public String toString(){
            return "Guest";
        }
    },
    OWNER {
        @Override
        public String toString(){
            return "Owner";
        }
    }
}
