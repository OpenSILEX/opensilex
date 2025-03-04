/*
 * *****************************************************************************
 *                         MultipleErrorResponse.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2025.
 * Last Modification: 04/03/2025 14:05
 * Contact: yvan.roux@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
 * *****************************************************************************
 */

package org.opensilex.server.response;

import javax.ws.rs.core.Response.Status;
import java.util.Map;

public class MultipleErrorResponse extends JsonResponse<MultipleErrorDTO> {

    public MultipleErrorResponse(Status status, String title, Map<String, String> errors) {
        super(status);
        metadata = new MetadataDTO(new PaginationDTO());
        result = new MultipleErrorDTO(
                title,
                errors
        );
    }
}
