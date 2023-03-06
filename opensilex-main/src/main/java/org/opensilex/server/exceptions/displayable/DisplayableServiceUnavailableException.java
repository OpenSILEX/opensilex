/*******************************************************************************
 *                         ExternalServiceConnectionException.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2023.
 * Last Modification: 16/01/2023
 * Contact: valentin.rigolle@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/
package org.opensilex.server.exceptions.displayable;

import javax.ws.rs.core.Response;
import java.util.Map;

/**
 * A displayable Service Unavailable (error code 503) exception.
 *
 * @author Valentin Rigolle
 */
public class DisplayableServiceUnavailableException extends DisplayableResponseException {

    public DisplayableServiceUnavailableException(String message, String translationKey, Map<String, String> translationValues) {
        super(message,
                Response.Status.SERVICE_UNAVAILABLE,
                Response.Status.SERVICE_UNAVAILABLE.getReasonPhrase(),
                translationKey,
                translationValues);
    }
}
