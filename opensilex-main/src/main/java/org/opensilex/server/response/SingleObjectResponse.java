//******************************************************************************
//                          SingleObjectResponse.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server.response;

import javax.ws.rs.core.Response.Status;

/**
 * <pre>
 * Single Object URI response (for get by uri result by example).Automatically set given object to result body with no metadata.
 * </pre>
 *
 * @see org.opensilex.server.response.JsonResponse
 * @author Vincent Migot
 * @param <T> response object type
 */
public class SingleObjectResponse<T> extends JsonResponse<T> {

    /**
     * Constructor for single object response.
     *
     * @param result object.
     */
    public SingleObjectResponse(T result) {
        this();
        this.result = result;
    }

    /**
     * Private constructor to initialize empty metadata.
     */
    private SingleObjectResponse() {
        super(Status.OK);
        this.metadata = new MetadataDTO(new PaginationDTO());
    }

}
