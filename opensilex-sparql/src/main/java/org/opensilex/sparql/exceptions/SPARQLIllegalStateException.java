/*******************************************************************************
 *                         SPARQLIllegalStateException.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2023.
 * Last Modification: 08/02/2023
 * Contact: valentin.rigolle@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/
package org.opensilex.sparql.exceptions;

/**
 * Exception thrown when the current state of the triple store doesn't match the constraints of the model or the
 * business logic.
 *
 * @author Valentin Rigolle
 */
public class SPARQLIllegalStateException extends SPARQLException {
    public SPARQLIllegalStateException() {
    }

    public SPARQLIllegalStateException(String message) {
        super(message);
    }
}
