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
public class NoVariableDataTypeException extends Exception {
    private final URI variable;

    public NoVariableDataTypeException(URI variable) {
        super("The variable "+ variable.toString() + " has no datatype defined");
        this.variable = variable;               
    }

    public URI getVariable() {
        return variable;
    }

}
