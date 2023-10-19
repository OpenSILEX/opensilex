package org.opensilex.brapi.responses;

import org.opensilex.brapi.BrapiPaginatedListResponse;
import org.opensilex.brapi.model.BrAPIv1CallDTO;
import org.opensilex.utils.ListWithPagination;

public class BrAPIv1CallListResponse extends BrapiPaginatedListResponse<BrAPIv1CallDTO> {

    public BrAPIv1CallListResponse(ListWithPagination<BrAPIv1CallDTO> paginatedList) {
        super(paginatedList);
    }
}
