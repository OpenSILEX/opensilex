//******************************************************************************
//                          ResourceTreeResponse.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.response;

import java.util.List;
import org.opensilex.server.response.MetadataDTO;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.PaginationDTO;
import org.opensilex.utils.ListWithPagination;

public class NamedResourcePaginatedListResponse<T extends SPARQLNamedResourceModel> extends PaginatedListResponse<NamedResourceDTO> {

    public NamedResourcePaginatedListResponse(ListWithPagination<T> paginatedList) {
        super();
        ListWithPagination<NamedResourceDTO> dtoList = paginatedList.convert(NamedResourceDTO.class, NamedResourceDTO::getDTOFromModel);
        this.setResult(dtoList.getList());
        this.metadata = new MetadataDTO(new PaginationDTO(paginatedList.getPageSize(), paginatedList.getPage(), paginatedList.getTotal()));
    }

    public NamedResourcePaginatedListResponse(List<T> list) {
        this(new ListWithPagination<T>(list));
    }

}

