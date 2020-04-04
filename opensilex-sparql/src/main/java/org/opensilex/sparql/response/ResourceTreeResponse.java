//******************************************************************************
//                          ResourceTreeResponse.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.response;

import java.util.List;
import javax.ws.rs.core.Response.Status;
import org.opensilex.sparql.response.ResourceTreeDTO;
import org.opensilex.server.response.JsonResponse;
import org.opensilex.server.response.MetadataDTO;
import org.opensilex.server.response.PaginationDTO;

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

    public ResourceTreeResponse(List<ResourceTreeDTO> result) {
        this();
        this.result = result;
    }

    public ResourceTreeResponse(){
        super(Status.OK);
        this.metadata = new MetadataDTO(new PaginationDTO());
    }

}
