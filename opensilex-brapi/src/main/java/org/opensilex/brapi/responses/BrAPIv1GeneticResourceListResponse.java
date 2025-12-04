package org.opensilex.brapi.responses;

import org.opensilex.brapi.BrapiPaginatedListResponse;
import org.opensilex.brapi.model.BrAPIv1GeneticResourceDTO;
import org.opensilex.utils.ListWithPagination;

public class BrAPIv1GeneticResourceListResponse extends BrapiPaginatedListResponse<BrAPIv1GeneticResourceDTO> {

    public BrAPIv1GeneticResourceListResponse(ListWithPagination<BrAPIv1GeneticResourceDTO> paginatedList) {
        super(paginatedList);
    }
}
