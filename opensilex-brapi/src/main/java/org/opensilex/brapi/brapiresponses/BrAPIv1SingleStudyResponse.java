package org.opensilex.brapi.brapiresponses;

import org.opensilex.brapi.model.BrAPIv1StudyDTO;
import org.opensilex.server.response.SingleObjectResponse;

public class BrAPIv1SingleStudyResponse extends SingleObjectResponse<BrAPIv1StudyDTO> {

    public BrAPIv1SingleStudyResponse(BrAPIv1StudyDTO paginatedList) {
        super(paginatedList);
    }
}
