package org.opensilex.faidare.responses;

import org.opensilex.brapi.BrapiPaginatedListResponse;
import org.opensilex.faidare.model.Faidarev1LocationDTO;
import org.opensilex.utils.ListWithPagination;

public class Faidarev1LocationListResponse extends BrapiPaginatedListResponse<Faidarev1LocationDTO> {

    public Faidarev1LocationListResponse(ListWithPagination<Faidarev1LocationDTO> paginatedList) {
        super(paginatedList);
    }
}
