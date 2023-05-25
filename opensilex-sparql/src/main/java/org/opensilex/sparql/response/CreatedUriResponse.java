/*******************************************************************************
 *                         CreatedUriResponse.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2023.
 * Last Modification: 17/05/2023
 * Contact: valentin.rigolle@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/
package org.opensilex.sparql.response;

import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.StatusDTO;
import org.opensilex.server.response.StatusLevel;
import org.opensilex.sparql.deserializer.URIDeserializer;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 *     Response for the creation of a resource. This class extends {@link ObjectUriResponse} and adds a warning in the
 *     case the created URI has a prefix that is not known by the instance.
 * </p>
 *
 * @author Valentin Rigolle
 */
public class CreatedUriResponse extends ObjectUriResponse {
    public static final String UNKNOWN_PREFIX_WARNING = "Prefix \"%s\" is unknown : you may have forgotten to declare it " +
            "as a namespace in the triple store, or may need to restart the instance.";
    public static final String UNKNOWN_PREFIX_WARNING_KEY = "server.warnings.unknown-prefix";
    public static final String UNKNOWN_PREFIX_WARNING_PREFIX = "prefix";

    public CreatedUriResponse(URI uri) {
        super(Response.Status.CREATED, uri);

        checkUnknownPrefix(uri);
    }

    public CreatedUriResponse(List<URI> uriList) {
        super(Response.Status.CREATED, uriList);

        for (URI uri : uriList) {
            checkUnknownPrefix(uri);
        }
    }

    private void checkUnknownPrefix(URI uri) {
        if (!URIDeserializer.hasKnownPrefix(uri) && Objects.isNull(uri.getAuthority())) {
            addMetadataStatus(new StatusDTO(
                    String.format(UNKNOWN_PREFIX_WARNING, uri.getScheme()),
                    StatusLevel.WARNING,
                    UNKNOWN_PREFIX_WARNING_KEY,
                    Collections.singletonMap(UNKNOWN_PREFIX_WARNING_PREFIX, uri.getScheme())
            ));
        }
    }
}
