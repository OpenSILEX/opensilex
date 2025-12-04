package org.opensilex.faidare.responses;

import org.opensilex.brapi.BrapiPaginatedListResponse;
import org.opensilex.faidare.model.Faidarev1GeneticResourceDTO;
import org.opensilex.utils.ListWithPagination;

public class Faidarev1GeneticResourceListResponse extends BrapiPaginatedListResponse<Faidarev1GeneticResourceDTO> {

    public Faidarev1GeneticResourceListResponse(ListWithPagination<Faidarev1GeneticResourceDTO> paginatedList) {
        super(paginatedList);
    }
}
