//******************************************************************************
//                      ErrorResponse.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import javax.ws.rs.core.Response.Status;
import java.util.Map;

/**
 * Error JSON message.
 * <pre>
 * This class represents an HTTP error response (Status &gt;= 400) in JSON.
 * Response status is automatically set to 500 - INTERNAL_SERVER_ERROR if error comes from a source exception.
 * Otherwise response status is defined by constructor parameter.
 *
 * Produced JSON:
 * {
 *      metadata: {
 *          pagination: {
 *              pageSize: 0,
 *              currentPage: 0,
 *              totalCount: 0,
 *              totalPages: 0
 *          },
 *          status: [],
 *          datafiles: []
 *      },
 *      result: {
 *          title: Title of the error
 *          message: Message of the error
 *          stack: Stack trace array of the source exception filtered by packages org.opensilex.* (ONLY IN DEBUG MODE)
 *          fullstack: Complete stack trace array of the source exception (ONLY IN DEBUG MODE)
 *      }
 * }
 * </pre>
 *
 * @see org.opensilex.server.response.ErrorDTO
 * @author Vincent Migot
 */
@JsonInclude(Include.NON_NULL)
public class ErrorResponse extends JsonResponse<ErrorDTO> {

    public ErrorResponse(){

    }

    /**
     * Constructor.
     *
     * @param exception Exception to wrap into a structured error response
     */
    public ErrorResponse(Throwable exception) {
        super(Status.INTERNAL_SERVER_ERROR);
        metadata = new MetadataDTO(new PaginationDTO());
        result = new ErrorDTO(
                "Unexpected internal error - " + exception.getClass().getCanonicalName(),
                exception.getMessage(),
                exception
        );
    }

    public ErrorResponse(Status status, String title, Throwable exception) {
        super(status);
        metadata = new MetadataDTO(new PaginationDTO());
        result = new ErrorDTO(
                title,
                exception.getMessage(),
                exception
        );
    }

    /**
     * Constructor.
     *
     * @param status errror status code
     * @param title error title
     * @param message errror message
     */
    public ErrorResponse(Status status, String title, String message) {
        super(status);
        metadata = new MetadataDTO(new PaginationDTO());
        result = new ErrorDTO(title, message);
    }

    /**
     * Constructs a displayable error response (with a translation key)
     *
     * @param status error status code
     * @param title error title
     * @param message error message (not for the user)
     * @param translationKey error translation key (for the user)
     * @param translationValues error translation values (if needed in the translation key)
     */
    public ErrorResponse(Status status, String title, String message, String translationKey, Map<String, String> translationValues) {
        super(status);
        metadata = new MetadataDTO(new PaginationDTO());
        result = new ErrorDTO(title, message, translationKey, translationValues, null);
    }
}
