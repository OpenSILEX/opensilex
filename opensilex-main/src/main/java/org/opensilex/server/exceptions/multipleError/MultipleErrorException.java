/*
 * *****************************************************************************
 *                         MultipleErrorException.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2025.
 * Last Modification: 24/03/2025 14:53
 * Contact: yvan.roux@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
 * *****************************************************************************
 */
package org.opensilex.server.exceptions.multipleError;

import org.opensilex.server.response.multipleError.MultipleErrorResponse;

import javax.ws.rs.core.Response;

/**
 * Exception representing a bad request
 *
 * @author Valentin RIGOLLE
 */
public class MultipleErrorException extends Exception {
    MultipleErrorObjectList errors;
    String title;

    public MultipleErrorException(String title, MultipleErrorObjectList errors) {
        this.title = title;
        this.errors = errors;
    }

    public Response getResponse(){
        return new MultipleErrorResponse(errors.toDTO()).getResponse();
    }
}
