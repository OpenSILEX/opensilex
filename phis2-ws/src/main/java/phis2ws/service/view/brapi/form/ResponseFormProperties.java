//******************************************************************************
//                                       ResponseFormSensorProfile.java
//
// Author(s): Morgane Vidal <morgane.vidal@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 30 juil. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  30 juil. 2018
// Subject: Allows the formating of the result of the request about Sensor Profile
//******************************************************************************
package phis2ws.service.view.brapi.form;

import java.util.ArrayList;
import phis2ws.service.resources.dto.PropertiesDTO;
import phis2ws.service.view.brapi.Metadata;
import phis2ws.service.view.brapi.results.ResultProperties;
import phis2ws.service.view.manager.ResultForm;

/**
 * Allows the formating of the result of the request about Sensor Profile
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ResponseFormProperties extends ResultForm<PropertiesDTO> {
    /**
     * Initialize fields metadata and result
     * @param pageSize results per page
     * @param currentPage current page
     * @param list results list
     * @param paginate 
     */
    public ResponseFormProperties(int pageSize, int currentPage, ArrayList<PropertiesDTO> list, boolean paginate, int totalCount) {
        metadata = new Metadata(pageSize, currentPage, totalCount);
        if (list.size() > 1) {
            result = new ResultProperties(list, metadata.getPagination(), paginate);
        } else {
            result = new ResultProperties(list);
        }
    }
}
