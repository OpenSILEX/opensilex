//******************************************************************************
//                          BrapiPaginatedListResponse.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.brapi;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.Response;
import org.opensilex.server.response.JsonResponse;
import org.opensilex.server.response.MetadataDTO;
import org.opensilex.server.response.PaginationDTO;
import org.opensilex.utils.ListWithPagination;

/**
 *
 * @author Alice Boizet
 */
public class BrapiPaginatedListResponse<T> extends JsonResponse<BrapiDataResponsePart<List<T>>> {

    /**
     * Constructor.
     *
     * @param paginatedList
     */
    public BrapiPaginatedListResponse(ListWithPagination<T> paginatedList) {
        this(Response.Status.OK, paginatedList);
    }

    /**
     * Constructor with specific status.
     *
     * @param status
     * @param paginatedList
     */
    public BrapiPaginatedListResponse(Response.Status status, ListWithPagination<T> paginatedList) {
        super(status);
        this.result = new BrapiDataResponsePart(paginatedList.getList());
        this.metadata = new MetadataDTO(new PaginationDTO(paginatedList.getPageSize(), paginatedList.getPage(), paginatedList.getTotal()));
    }

    /**
     * Constructor for an empty list.
     */
    public BrapiPaginatedListResponse() {
        this(Response.Status.OK, new ArrayList<>());
    }

    /**
     * Constructor for a list without pagination with a specific status.
     *
     * @param status
     * @param list
     */
    public BrapiPaginatedListResponse(Response.Status status, List<T> list) {
        super(status);
        setResult(new BrapiDataResponsePart(list));
    }

    /**
     * Constructor for a list without pagination with OK status.
     *
     * @param list
     */
    public BrapiPaginatedListResponse(List<T> list) {
        this(Response.Status.OK, list);
    }

    @Override
    public BrapiPaginatedListResponse<T> setResult(BrapiDataResponsePart<List<T>> data) {
        this.result = data;
        this.metadata = new MetadataDTO(new PaginationDTO(data.getData().size(), 0, data.getData().size()));
        return this;
    }
    
}
