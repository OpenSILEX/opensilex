/*******************************************************************************
 *                         BadRequestException.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2021.
 * Last Modification: 07/12/2021
 * Contact: valentin.rigolle@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/
package org.opensilex.server.exceptions;

import org.opensilex.server.response.MultipleErrorResponse;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

/**
 * Exception representing a bad request
 *
 * @author Valentin RIGOLLE
 */
public class MultipleErrorException extends Exception {
    MultipleErrorObject errors;
    String title;

    public MultipleErrorException(String title, MultipleErrorObject errors) {
        this.title = title;
        this.errors = errors;
    }

    public Response getResponse(){
        return new MultipleErrorResponse(title, errors.getErrors()).getResponse();
    }

    public MultipleErrorObject getErrors() {
        return errors;
    }

    public String getTitle() {
        return title;
    }
}
