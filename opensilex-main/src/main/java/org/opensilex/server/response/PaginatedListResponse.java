//******************************************************************************
//                          PaginatedListResponse.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server.response;

import java.util.ArrayList;
import java.util.List;
import org.opensilex.utils.ListWithPagination;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

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
        this(Status.OK, paginatedList);
    }

    public PaginatedListResponse(Status status, ListWithPagination<T> paginatedList) {
        super(status);
        this.result = paginatedList.getList();
        this.metadata = new MetadataDTO(new PaginationDTO(paginatedList.getPageSize(), paginatedList.getPage(), paginatedList.getTotal()));
    }

    public PaginatedListResponse() {
        this(Status.OK, new ArrayList<>());
    }

    public PaginatedListResponse(Status status, List<T> list) {
        super(status);
        setResult(list);
    }

    public PaginatedListResponse(List<T> list) {
        this(Status.OK, list);
    }

    @Override
    public PaginatedListResponse<T> setResult(List<T> list) {
        this.result = list;
        this.metadata = new MetadataDTO(new PaginationDTO(list.size(), 0, list.size()));
        return this;
    }

}
