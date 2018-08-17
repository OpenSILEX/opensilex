//******************************************************************************
//                                       GroupLevels.java
// SILEX-PHIS
// Copyright © INRA 2018
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.configuration;

/**
 * The list of the authorized group levels
 * @author Arnaud Charleroy<arnaud.charleroy@inra.fr>, Morgane Vidal <morgane.vidal@inra.fr>
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
