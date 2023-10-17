package org.opensilex.brapi.responses;

import org.opensilex.brapi.BrapiPaginatedListResponse;
import org.opensilex.brapi.model.BrAPIv1StudyDTO;
import org.opensilex.utils.ListWithPagination;

public class BrAPIv1StudyListResponse extends BrapiPaginatedListResponse<BrAPIv1StudyDTO> {

    public BrAPIv1StudyListResponse(ListWithPagination<BrAPIv1StudyDTO> paginatedList) {
        super(paginatedList);
    }
}
