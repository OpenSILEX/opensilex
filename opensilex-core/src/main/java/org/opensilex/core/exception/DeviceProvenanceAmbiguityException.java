//******************************************************************************
//                          TimezoneAmbiguityException.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.exception;

/**
 *
 * @author Julien Bonnefont
 */
public class DeviceProvenanceAmbiguityException extends Exception {
    String provenance;
    
    public DeviceProvenanceAmbiguityException(String provenance) {
        super("This provenance has at least two Devices who fit the choice : " + provenance);
        
    }

}
