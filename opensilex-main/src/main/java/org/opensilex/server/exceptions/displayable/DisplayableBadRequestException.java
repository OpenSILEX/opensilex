/*******************************************************************************
 *                         DisplayableBadRequestException.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2021.
 * Last Modification: 07/12/2021
 * Contact: valentin.rigolle@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/
package org.opensilex.server.exceptions.displayable;

import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.Map;

/**
 * A bad request exception with a message meant to be displayed to the user
 *
 * @author Valentin RIGOLLE
 */
public class DisplayableBadRequestException extends DisplayableResponseException {
    public DisplayableBadRequestException(String message, String translationKey) {
        this(message, translationKey, Collections.emptyMap());
    }

    public DisplayableBadRequestException(String message, String translationKey, Map<String, String> translationValues) {
        super(message,
                Response.Status.BAD_REQUEST,
                Response.Status.BAD_REQUEST.getReasonPhrase(),
                translationKey,
                translationValues);
    }
}
