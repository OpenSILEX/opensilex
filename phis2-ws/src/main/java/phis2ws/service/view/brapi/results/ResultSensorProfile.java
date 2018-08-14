//******************************************************************************
//                                       ResultSensorProfile.java
//
// Author(s): Morgane Vidal <morgane.vidal@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 30 juil. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  30 juil. 2018
// Subject: extend from Resultat, adapted to the sensors profiles object list
//******************************************************************************
package phis2ws.service.view.brapi.results;

import java.util.ArrayList;
import phis2ws.service.resources.dto.SensorProfileDTO;
import phis2ws.service.view.brapi.Pagination;
import phis2ws.service.view.manager.Resultat;

/**
 * A class which represents the result part in the response form, adapted to the sensors profiles
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ResultSensorProfile extends Resultat<SensorProfileDTO> {
    /**
     * @param sensorsProfiles the sensors profiles of the result 
     */
    public ResultSensorProfile(ArrayList<SensorProfileDTO> sensorsProfiles) {
        super(sensorsProfiles);
    }
    
    /**
     * @param sensorsProfiles
     * @param pagination
     * @param paginate 
     */
    public ResultSensorProfile(ArrayList<SensorProfileDTO> sensorsProfiles, Pagination pagination, boolean paginate) {
        super(sensorsProfiles, pagination, paginate);
    }
}

