//******************************************************************************
//                          TimezoneAmbiguityException.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.exception;

import java.time.OffsetDateTime;

/**
 *
 * @author Alice Boizet
 */
public class TimezoneAmbiguityException extends DateValidationException {
    String date;
    
    public TimezoneAmbiguityException(String date) {
        super("The timezone doesn't correspond to the offset filled in the date : " + date);
        
    }

}
