//******************************************************************************
//                          PaginatedListResponse.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server.response;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.pagination.PaginatedSearchStrategy;
import org.opensilex.utils.pagination.StreamWithPagination;

import javax.ws.rs.core.Response.Status;

/**
 * <pre>
 * Response model for paginated list response (for search request by example).Automatically define pagination metadata and result body as an array.
 * </pre>
 *
 * @see org.opensilex.utils.ListWithPagination
 * @see org.opensilex.server.response.JsonResponse
 * @author Vincent Migot
 * @param <T> response list generic type
 */
public class PaginatedListResponse<T> extends JsonResponse<List<T>> {

    /**
     * Constructor.
     */
    public PaginatedListResponse(ListWithPagination<T> paginatedList) {
        this(Status.OK, paginatedList);
    }

    public PaginatedListResponse(StreamWithPagination<T> paginatedStream){
        super(Status.OK);

        this.metadata = new MetadataDTO(paginatedStream.getPagination());

        List<T> resultList = paginatedStream.getSource()
                .collect(Collectors.toList());

        if(paginatedStream.getPaginationStrategy() == PaginatedSearchStrategy.HAS_NEXT_PAGE){

            // Page size limit reached, delete an element
            if(resultList.size() > paginatedStream.getPageSize()){
                this.result = resultList.subList(0, resultList.size() - 1);
                this.metadata.getPagination().setHasNextPage(true);
            }else{
                this.result = resultList;
                this.metadata.getPagination().setHasNextPage(false);
            }
        }else{
            this.result = resultList;
        }
    }

                                 /**
     * Constructor with specific status.
     *
     * @param status
     * @param paginatedList
     */
    public PaginatedListResponse(Status status, ListWithPagination<T> paginatedList) {
        super(status);
        this.result = paginatedList.getList();
        this.metadata = new MetadataDTO(paginatedList.getPagination());
    }


    /**
     * Constructor for an empty list.
     */
    public PaginatedListResponse() {
        this(Status.OK, new ArrayList<>());
    }

    /**
     * Constructor for a list without pagination with a specific status.
     *
     * @param status
     * @param list
     */
    public PaginatedListResponse(Status status, List<T> list) {
        super(status);
        setResult(list);
        setMetadata(new MetadataDTO(new PaginationDTO(list.size(), 0, list.size())));
    }

    /**
     * Constructor for a list without pagination with OK status.
     *
     * @param list
     */
    public PaginatedListResponse(List<T> list) {
        this(Status.OK, list);
    }

    @Override
    public PaginatedListResponse<T> setResult(List<T> list) {
        this.result = list;
        return this;
    }

    /**
     *
     * @return
     * Use @{@link JsonIgnore} to prevent the serialization of the method result when building a Response to an API client
     */
    @JsonIgnore
    public ListWithPagination<T> getResultWithPagination() {
        return new ListWithPagination<>(result,
                (int) metadata.getPagination().getCurrentPage(),
                (int) metadata.getPagination().getPageSize(),
                (int) metadata.getPagination().getTotalCount());
    }
}
