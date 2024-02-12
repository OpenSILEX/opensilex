//******************************************************************************
//                          DeviceProvenanceAmbiguityException.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2021
// Contact: anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.exception;

import org.opensilex.core.data.dal.DataModel;

/**
 *
 * @author Julien Bonnefont
 */
public class DeviceProvenanceAmbiguityException extends Exception {
    
    public DeviceProvenanceAmbiguityException(String provenance, String variable) {
        super("The variable "+ variable + " can't be linked to a specific device in the provenance : " + provenance);
        
    }
    
     public DeviceProvenanceAmbiguityException(String provenance) {
        super("The variable can't be linked to a specific device in the provenance : " + provenance);
        
    }

}
