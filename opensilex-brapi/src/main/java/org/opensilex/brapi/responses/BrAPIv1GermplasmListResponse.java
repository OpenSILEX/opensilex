package org.opensilex.brapi.responses;

import org.opensilex.brapi.BrapiPaginatedListResponse;
import org.opensilex.brapi.model.BrAPIv1GermplasmDTO;
import org.opensilex.utils.ListWithPagination;

public class BrAPIv1GermplasmListResponse extends BrapiPaginatedListResponse<BrAPIv1GermplasmDTO> {

    public BrAPIv1GermplasmListResponse(ListWithPagination<BrAPIv1GermplasmDTO> paginatedList) {
        super(paginatedList);
    }
}
