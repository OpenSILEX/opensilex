package org.opensilex.brapi.responses;

import org.opensilex.brapi.model.BrAPIv1SuperStudyDTO;
import org.opensilex.server.response.SingleObjectResponse;

public class BrAPIv1SingleStudyResponse extends SingleObjectResponse<BrAPIv1SuperStudyDTO> {

    public BrAPIv1SingleStudyResponse(BrAPIv1SuperStudyDTO paginatedList) {
        super(paginatedList);
    }
}
