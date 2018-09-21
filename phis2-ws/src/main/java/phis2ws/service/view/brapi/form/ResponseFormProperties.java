//******************************************************************************
//                                       ResponseFormProperties.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 7 sept. 2018
// Contact: vincent.migot@inra.fr anne.tireau@inra.fr, pascal.neveu@inra.fr
// Subject: Allows the formating of the result of the request about a generic list of properties
//******************************************************************************
package phis2ws.service.view.brapi.form;

import java.util.ArrayList;
import phis2ws.service.resources.dto.PropertiesDTO;
import phis2ws.service.resources.dto.PropertyLabelsDTO;
import phis2ws.service.view.brapi.Metadata;
import phis2ws.service.view.brapi.results.ResultProperties;
import phis2ws.service.view.manager.ResultForm;

/**
 * Allows the formating of the result of the request about a generic list of properties
 * @author Vincent Migot <vincent.migot@inra.fr>
 */
public class ResponseFormProperties extends ResultForm<PropertiesDTO<PropertyLabelsDTO>> {
    /**
     * Initialize fields metadata and result
     * @param pageSize results per page
     * @param currentPage current page
     * @param list results list
     * @param paginate 
     */
    public ResponseFormProperties(int pageSize, int currentPage, ArrayList<PropertiesDTO<PropertyLabelsDTO>> list, boolean paginate, int totalCount) {
        metadata = new Metadata(pageSize, currentPage, totalCount);
        if (list.size() > 1) {
            result = new ResultProperties(list, metadata.getPagination(), paginate);
        } else {
            result = new ResultProperties(list);
        }
    }
}
