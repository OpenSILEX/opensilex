//******************************************************************************
//                          NoTimezoneException.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.exception;

/**
 *
 * @author Alice Boizet
 */
public class UnableToParseDateException extends DateValidationException {
    String date;
    
    public UnableToParseDateException(String date) {
        super("unable to parse this date format: " + date + " . Check that the offset or the timezone are given");
    }

}
