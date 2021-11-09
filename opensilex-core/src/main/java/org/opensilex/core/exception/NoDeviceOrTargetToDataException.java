//******************************************************************************
//                          DateValidationException.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.exception;

/**
 *
 * @author Alice Boizet
 */
public class NoDeviceOrTargetToDataException extends Exception {
    
    public NoDeviceOrTargetToDataException() {
        super("The Data isn't linked to any target or device ");
    }
    
    
}
