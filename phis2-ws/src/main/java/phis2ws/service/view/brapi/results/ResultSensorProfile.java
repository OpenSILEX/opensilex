//******************************************************************************
//                                       ResultSensorProfile.java
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: 30 juil. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.brapi.results;

import java.util.ArrayList;
import phis2ws.service.resources.dto.SensorProfileDTO;
import phis2ws.service.view.brapi.Pagination;
import phis2ws.service.view.manager.Result;

/**
 * A class which represents the result part in the response form, adapted to the sensors profiles
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ResultSensorProfile extends Result<SensorProfileDTO> {
    /**
     * Constructor which calls the mother-class constructor in the case of a
     * list with only 1 element
     * @param sensorsProfiles 
     */
    public ResultSensorProfile(ArrayList<SensorProfileDTO> sensorsProfiles) {
        super(sensorsProfiles);
    }
    
    /**
     * Contructor which calls the mother-class constructor in the case of a
     * list with several elements
     * @param sensorsProfiles
     * @param pagination
     * @param paginate 
     */
    public ResultSensorProfile(ArrayList<SensorProfileDTO> sensorsProfiles, Pagination pagination, boolean paginate) {
        super(sensorsProfiles, pagination, paginate);
    }
}

