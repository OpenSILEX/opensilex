//******************************************************************************
//                          JsonResponse.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * <pre>
 * This class represent the common base for web services JSON responses with status.
 * All web services response classes should extends this class.
 * See Brapi for details: 
 * https://brapi.docs.apiary.io/#introduction/structure-of-the-response-object:/the-metadata-key
 * https://brapi.docs.apiary.io/#introduction/structure-of-the-response-object:/payload
 * 
 * Produced JSON:
 * {
 *      metadata: {
 *          pagination: {
 *              pageSize: ... page size of the result if paginated request or 0
 *              currentPage: ... current page of the result if paginated request or 0
 *              totalCount: ... total element count if paginated request or 0
 *              totalPages: ... total pages count if paginated request or 0
 *          },
 *          status: [
 *              ... array of custom status as {message: "status message", messageType: ERROR|DEBUG|WARN|INFO}
 *          ],
 *          datafiles: [
 *              ... array of URI in case of resource creation
 *          ]
 *      },
 *      result: {
 *          ... any object properties or array depending of T structure.
 *      }
 * }
 * </pre>
 *
 * @see org.opensilex.server.response.MetadataDTO
 * @author Vincent Migot
 */
public abstract class JsonResponse<T> {

    /**
     * HTTP status
     */
    @JsonIgnore
    protected final Status status;

    /**
     * Response metadata
     */
    protected MetadataDTO metadata;

    /**
     * Response body result
     */
    protected T result;

    /**
     * Constructor with HTTP {@code javax.ws.rs.core.Response.Status}
     *
     * @param status HTTP status
     */
    public JsonResponse(Status status) {
        this.status = status;
    }

    /**
     * <pre>
     * Build and return HTTP {@code javax.ws.rs.core.Response} as JSON with given
     * - status
     * - metadata
     * - result
     * </pre>
     *
     * @return HTTP JSON Response
     */
    @JsonIgnore
    public Response getResponse() {
        return Response.status(status)
                .entity(this)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
