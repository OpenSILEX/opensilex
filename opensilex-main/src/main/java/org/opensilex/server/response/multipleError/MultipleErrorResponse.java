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

/*
 * <pre>
 * Json response to represent many lists of errors, each corresponding to an object (a creation or update DTO).
 *
 * for each ocject (DTO) that has been given and contain an error, the response will contain an error list identified
 *  by the index of the object in the original list.
 *
 * exemple:
 * Original DTO list :
 * [ {value: "error"},
 *   {value: "ok"}]
 * ]
 *
 * Error response:
 * [ {index: 0, errors: ["error"]} ]
 *
 * explanation:
 * The first object (index 0) has an error, so it is represented by an error list with the index 0 and the error message.
 * </pre>
 *
 * @see MultipleErrorListDTO and @see MultipleErrorDTO for more information about the structure of the response.
 */
public class MultipleErrorResponse extends JsonResponse<MultipleErrorListDTO> {

    public MultipleErrorResponse(MultipleErrorListDTO result) {
        super(Status.BAD_REQUEST);
        metadata = new MetadataDTO(new PaginationDTO());
        this.result = result;
    }
}
