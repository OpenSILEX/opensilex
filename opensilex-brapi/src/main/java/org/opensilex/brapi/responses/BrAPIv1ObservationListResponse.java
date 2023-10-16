package org.opensilex.brapi.responses;

import org.opensilex.brapi.BrapiPaginatedListResponse;
import org.opensilex.brapi.model.BrAPIv1ObservationDTO;
import org.opensilex.utils.ListWithPagination;

public class BrAPIv1ObservationListResponse extends BrapiPaginatedListResponse<BrAPIv1ObservationDTO> {

    public BrAPIv1ObservationListResponse(ListWithPagination<BrAPIv1ObservationDTO> paginatedList) {
        super(paginatedList);
    }
}
