//******************************************************************************
//                                       ResultSensor.java
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: 14 mars 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.brapi.results;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Pagination;
import phis2ws.service.view.manager.Result;
import phis2ws.service.view.model.phis.Sensor;

/**
 * A class which represents the result part in the response form, adapted to the sensors
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ResultSensor extends Result<Sensor> {
    /**
     * Constructor which calls the mother-class constructor in the case of a
     * list with only 1 element
     * @param sensors 
     */
    public ResultSensor(ArrayList<Sensor> sensors) {
        super(sensors);
    }
    
    /**
     * Contructor which calls the mother-class constructor in the case of a
     * list with several elements
     * @param sensors
     * @param pagination
     * @param paginate 
     */
    public ResultSensor(ArrayList<Sensor> sensors, Pagination pagination, boolean paginate) {
        super(sensors, pagination, paginate);
    }
}
