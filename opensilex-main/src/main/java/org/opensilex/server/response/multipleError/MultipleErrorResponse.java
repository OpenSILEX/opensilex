/*
 * *****************************************************************************
 *                         MultipleErrorResponse.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2025.
 * Last Modification: 11/03/2025 11:32
 * Contact: yvan.roux@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
 * *****************************************************************************
 */

package org.opensilex.server.response.multipleError;

import org.opensilex.server.response.JsonResponse;
import org.opensilex.server.response.MetadataDTO;
import org.opensilex.server.response.PaginationDTO;

import javax.ws.rs.core.Response.Status;

public class MultipleErrorResponse extends JsonResponse<MultipleErrorListDTO> {

    public MultipleErrorResponse(MultipleErrorListDTO result) {
        super(Status.BAD_REQUEST);
        metadata = new MetadataDTO(new PaginationDTO());
        this.result = result;
    }
}
