package org.opensilex.faidare.responses;

import org.opensilex.brapi.BrapiPaginatedListResponse;
import org.opensilex.faidare.model.Faidarev1GermplasmDTO;
import org.opensilex.utils.ListWithPagination;

public class Faidarev1GermplasmListResponse extends BrapiPaginatedListResponse<Faidarev1GermplasmDTO> {

    public Faidarev1GermplasmListResponse(ListWithPagination<Faidarev1GermplasmDTO> paginatedList) {
        super(paginatedList);
    }
}
