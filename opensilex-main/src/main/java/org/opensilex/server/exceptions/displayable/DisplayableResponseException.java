/*******************************************************************************
 *                         DisplayableException.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2021.
 * Last Modification: 07/12/2021
 * Contact: valentin.rigolle@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/
package org.opensilex.server.exceptions.displayable;

import org.opensilex.server.response.ErrorResponse;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

/**
 * An exception that will be sent as an error response and can be displayed to the user, using a translation key
 * (and values).
 *
 * @author Valentin RIGOLLE
 */
public class DisplayableResponseException extends WebApplicationException {
    private final String translationKey;
    private final Map<String, String> translationValues;

    public DisplayableResponseException(String message, Response.Status status, String title, String translationKey, Map<String, String> translationValues) {
        this(message,
                Response.status(status)
                        .entity(new ErrorResponse(status, title, message, translationKey, translationValues))
                        .type(MediaType.APPLICATION_JSON)
                        .build(),
                translationKey,
                translationValues);
    }

    public DisplayableResponseException(String message, Response response, String translationKey, Map<String, String> translationValues) {
        super(message, response);
        this.translationKey = translationKey;
        this.translationValues = translationValues;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public Map<String, String> getTranslationValues() {
        return translationValues;
    }
}
