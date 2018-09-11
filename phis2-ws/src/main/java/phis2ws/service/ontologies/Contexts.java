//******************************************************************************
//                                       Contexts.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 11 sept. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.ontologies;

import phis2ws.service.PropertiesFileManager;

/**
 * The contexts used in SILEX-PHIS
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public enum Contexts {
    //The context platform. 
    //The value is equals to the baseURI config param of the service.properties file.
    PLATFORM {
        @Override
        public String toString() {
            return PropertiesFileManager.getConfigFileProperty("service", "baseURI");
        }
    },
}

