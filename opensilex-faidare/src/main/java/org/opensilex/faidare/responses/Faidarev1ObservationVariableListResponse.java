package org.opensilex.faidare.responses;

import org.opensilex.brapi.BrapiPaginatedListResponse;
import org.opensilex.faidare.model.Faidarev1ObservationVariableDTO;
import org.opensilex.utils.ListWithPagination;

public class Faidarev1ObservationVariableListResponse extends BrapiPaginatedListResponse<Faidarev1ObservationVariableDTO> {

    public Faidarev1ObservationVariableListResponse(ListWithPagination<Faidarev1ObservationVariableDTO> paginatedList) {
        super(paginatedList);
    }
}
