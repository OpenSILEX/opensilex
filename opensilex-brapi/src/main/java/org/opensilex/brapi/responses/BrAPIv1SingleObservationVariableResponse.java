package org.opensilex.brapi.responses;

import org.opensilex.brapi.model.BrAPIv1ObservationVariableDTO;
import org.opensilex.server.response.SingleObjectResponse;

public class BrAPIv1SingleObservationVariableResponse extends SingleObjectResponse<BrAPIv1ObservationVariableDTO> {

    public BrAPIv1SingleObservationVariableResponse(BrAPIv1ObservationVariableDTO paginatedList) {
        super(paginatedList);
    }
}
