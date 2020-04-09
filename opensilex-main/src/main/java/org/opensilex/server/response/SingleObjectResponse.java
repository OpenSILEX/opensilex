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
 * Single Object URI response (for get by uri result by example).
 *
 * Automatically set given object to result body with no metadata.
 * </pre>
 *
 * @see org.opensilex.server.response.JsonResponse
 * @author Vincent Migot
 */
public class SingleObjectResponse<T> extends JsonResponse<T> {

    public SingleObjectResponse(T result) {
        this();
        this.result = result;
    }

    public SingleObjectResponse(){
        super(Status.OK);
        this.metadata = new MetadataDTO(new PaginationDTO());
    }

}
