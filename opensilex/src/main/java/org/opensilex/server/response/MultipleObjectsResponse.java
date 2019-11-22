//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server.response;

import java.util.*;
import org.opensilex.utils.*;

/**
 *
 * @author vincent
 */
public class MultipleObjectsResponse<T> extends JsonResponse<List<T>> {

    public MultipleObjectsResponse(ListWithPagination<T> paginatedList) {
        super(javax.ws.rs.core.Response.Status.OK);
        this.result = paginatedList.getList(); 
        this.metadata = new Metadata(new Pagination(paginatedList.getPageSize(), paginatedList.getPage(), paginatedList.getTotal()));
        
    }

}
