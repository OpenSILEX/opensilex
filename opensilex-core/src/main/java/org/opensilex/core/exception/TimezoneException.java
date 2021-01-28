//******************************************************************************
//                          TimezoneException.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.exception;

/**
 *
 * @author Alice Boizet
 */
public class TimezoneException extends DateValidationException  {
    String timezone;
    
    public TimezoneException(String timezone) {
        super("The given timezone doesn't exist : " + timezone);
        
    }
}
