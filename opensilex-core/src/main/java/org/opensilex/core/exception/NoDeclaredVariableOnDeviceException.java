//******************************************************************************
//                          NoVariableDataTypeException.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.exception;

import java.net.URI;

/**
 *
 * @author Alice Boizet
 */
public class NoDeclaredVariableOnDeviceException extends Exception {
    private final URI variable;

    public NoDeclaredVariableOnDeviceException(URI device, URI variable) {
        super("The variable "+ variable.toString() + " is not linked to the device "+ device.toString());
        this.variable = variable;               
    }
    
    
    public NoDeclaredVariableOnDeviceException( URI variable) {
        super("The variable "+ variable.toString() + " is not linked to any device ");
        this.variable = variable;               
    }
    

    public URI getVariable() {
        return variable;
    }
    

}
