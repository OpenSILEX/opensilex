//******************************************************************************
//                          TreeResponse.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server.response;

import java.util.List;
import java.util.TreeSet;
import javax.ws.rs.core.Response.Status;

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
public class TreeResponse<T> extends JsonResponse<TreeSet<T>> {

    public TreeResponse(TreeSet<T> result) {
        this();
        this.result = result;
    }

    public TreeResponse(){
        super(Status.OK);
        this.metadata = new MetadataDTO(new PaginationDTO());
    }

}
