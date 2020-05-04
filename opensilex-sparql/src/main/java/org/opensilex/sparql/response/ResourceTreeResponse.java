//******************************************************************************
//                          ResourceTreeResponse.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.response;

import java.util.ArrayList;
import java.util.List;
import org.opensilex.server.response.JsonResponse;

/**
 * <pre>
 * Tree response
 *
 * Automatically set given object to result body with no metadata.
 * </pre>
 *
 * @see org.opensilex.server.response.JsonResponse
 * @author Vincent Migot
 */
public class ResourceTreeResponse extends JsonResponse<List<ResourceTreeDTO>> {

    public ResourceTreeResponse() {
        this(new ArrayList<>());
    }

    public ResourceTreeResponse(List<ResourceTreeDTO> result) {
        super();
        this.result = result;
    }

}
