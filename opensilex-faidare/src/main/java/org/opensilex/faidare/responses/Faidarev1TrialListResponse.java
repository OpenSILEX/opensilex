package org.opensilex.faidare.responses;

import org.opensilex.brapi.BrapiPaginatedListResponse;
import org.opensilex.faidare.model.Faidarev1TrialDTO;
import org.opensilex.utils.ListWithPagination;

public class Faidarev1TrialListResponse extends BrapiPaginatedListResponse<Faidarev1TrialDTO> {

    public Faidarev1TrialListResponse(ListWithPagination<Faidarev1TrialDTO> paginatedList) {
        super(paginatedList);
    }
}
