package org.opensilex.brapi.responses;

import org.opensilex.brapi.BrapiPaginatedListResponse;
import org.opensilex.brapi.model.BrAPIv1ObservationUnitDTO;
import org.opensilex.utils.ListWithPagination;

public class BrAPIv1ObservationUnitListResponse extends BrapiPaginatedListResponse<BrAPIv1ObservationUnitDTO> {

    public BrAPIv1ObservationUnitListResponse(ListWithPagination<BrAPIv1ObservationUnitDTO> paginatedList) {
        super(paginatedList);
    }
}
