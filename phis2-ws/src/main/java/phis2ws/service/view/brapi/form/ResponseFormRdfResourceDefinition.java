//******************************************************************************
//                                       ResponseFormRdfResourceDefinition.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 7 sept. 2018
// Contact: vincent.migot@inra.fr anne.tireau@inra.fr, pascal.neveu@inra.fr
// Subject: Allows the formating of the result of the request about a generic list of rdf Resources Definition
//******************************************************************************
package phis2ws.service.view.brapi.form;

import java.util.ArrayList;
import phis2ws.service.resources.dto.rdfResourceDefinition.RdfResourceDefinitionDTO;
import phis2ws.service.view.brapi.Metadata;
import phis2ws.service.view.brapi.results.ResultRdfResourceDefinition;
import phis2ws.service.view.manager.ResultForm;
import phis2ws.service.view.model.phis.RdfResourceDefinition;

/**
 * Allows the formating of the result of the request about a generic list of rdf Resources Definition
 * @author Vincent Migot <vincent.migot@inra.fr>
 */
public class ResponseFormRdfResourceDefinition extends ResultForm<RdfResourceDefinitionDTO> {
    /**
     * Initialize fields metadata and result
     * @param pageSize results per page
     * @param currentPage current page
     * @param list results list
     * @param paginate 
     */
    public ResponseFormRdfResourceDefinition(int pageSize, int currentPage, ArrayList<RdfResourceDefinitionDTO> list, boolean paginate, int totalCount) {
        metadata = new Metadata(pageSize, currentPage, totalCount);
        if (list.size() > 1) {
            result = new ResultRdfResourceDefinition(list, metadata.getPagination(), paginate);
        } else {
            result = new ResultRdfResourceDefinition(list);
        }
    }
}
