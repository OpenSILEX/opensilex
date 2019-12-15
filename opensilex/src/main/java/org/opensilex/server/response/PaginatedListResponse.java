//******************************************************************************
//                          PaginatedListResponse.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server.response;

import java.util.List;
import org.opensilex.utils.ListWithPagination;

/**
 * <pre>
 * Response model for paginated list response (for search request by example).
 *
 * Automatically define pagination metadata and result body as an array.
 * </pre>
 *
 * @see org.opensilex.utils.ListWithPagination
 * @see org.opensilex.server.response.JsonResponse
 * @author Vincent Migot
 */
public class PaginatedListResponse<T> extends JsonResponse<List<T>> {

    public PaginatedListResponse(ListWithPagination<T> paginatedList) {
        super(javax.ws.rs.core.Response.Status.OK);
        this.result = paginatedList.getList();
        this.metadata = new MetadataDTO(new PaginationDTO(paginatedList.getPageSize(), paginatedList.getPage(), paginatedList.getTotal()));

    }

}
