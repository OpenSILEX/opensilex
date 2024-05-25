package org.opensilex.faidare.responses;

import org.opensilex.brapi.BrapiPaginatedListResponse;
import org.opensilex.faidare.model.Faidarev1StudyDTO;
import org.opensilex.utils.ListWithPagination;

public class Faidarev1StudyListResponse extends BrapiPaginatedListResponse<Faidarev1StudyDTO> {

    public Faidarev1StudyListResponse(ListWithPagination<Faidarev1StudyDTO> paginatedList) {
        super(paginatedList);
    }
    public Faidarev1StudyListResponse(){super();}
}
