//**********************************************************************************************
//                                       ResultExperiment.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: august 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//***********************************************************************************************

package phis2ws.service.view.brapi.results;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Pagination;
import phis2ws.service.view.manager.Result;
import phis2ws.service.view.model.phis.Experiment;

/**
 * A class which represents the result part in the response form, adapted to the
 * experiments
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ResultExperiment extends Result<Experiment>{
    /**
     * Constructor which calls the mother-class constructor in the case of a
     * list with only 1 element
     * @param experimentList 
     */
    public ResultExperiment(ArrayList<Experiment> experimentList) {
        super(experimentList);
    }
    
    /**
     * Contructor which calls the mother-class constructor in the case of a
     * list with several elements
     * @param experimentList
     * @param pagination
     * @param paginate 
     */
    public ResultExperiment(ArrayList<Experiment> experimentList, Pagination pagination, 
            boolean paginate) {
        super(experimentList, pagination, paginate);
    }
}
