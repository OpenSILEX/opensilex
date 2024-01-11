package org.opensilex.brapi.responses;

import org.opensilex.brapi.BrapiPaginatedListResponse;
import org.opensilex.brapi.model.BrAPIv1ObservationVariableDTO;
import org.opensilex.utils.ListWithPagination;

public class BrAPIv1ObservationVariableListResponse extends BrapiPaginatedListResponse<BrAPIv1ObservationVariableDTO> {

    public BrAPIv1ObservationVariableListResponse(ListWithPagination<BrAPIv1ObservationVariableDTO> paginatedList) {
        super(paginatedList);
    }
}
