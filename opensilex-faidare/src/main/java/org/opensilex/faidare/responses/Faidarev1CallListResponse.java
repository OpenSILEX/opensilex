package org.opensilex.faidare.responses;

import org.opensilex.brapi.BrapiPaginatedListResponse;
import org.opensilex.faidare.model.Faidarev1CallDTO;
import org.opensilex.utils.ListWithPagination;

public class Faidarev1CallListResponse extends BrapiPaginatedListResponse<Faidarev1CallDTO> {

    public Faidarev1CallListResponse(ListWithPagination<Faidarev1CallDTO> paginatedList) {
        super(paginatedList);
    }
}
