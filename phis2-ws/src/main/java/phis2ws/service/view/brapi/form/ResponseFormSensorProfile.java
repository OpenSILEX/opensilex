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
import phis2ws.service.resources.dto.SensorProfileDTO;
import phis2ws.service.view.brapi.Metadata;
import phis2ws.service.view.brapi.results.ResultSensorProfile;
import phis2ws.service.view.manager.ResultForm;

/**
 * Allows the formating of the result of the request about Sensor Profile
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ResponseFormSensorProfile extends ResultForm<SensorProfileDTO> {
    /**
     * Initialize fields metadata and result
     * @param pageSize results per page
     * @param currentPage current page
     * @param list results list
     * @param paginate 
     */
    public ResponseFormSensorProfile(int pageSize, int currentPage, ArrayList<SensorProfileDTO> list, boolean paginate) {
        metadata = new Metadata(pageSize, currentPage, list.size());
        if (list.size() > 1) {
            result = new ResultSensorProfile(list, metadata.getPagination(), paginate);
        } else {
            result = new ResultSensorProfile(list);
        }
    }
}
