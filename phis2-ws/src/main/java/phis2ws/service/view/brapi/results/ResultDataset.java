//**********************************************************************************************
//                                       ResultDataset.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: October, 18 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//***********************************************************************************************
package phis2ws.service.view.brapi.results;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Pagination;
import phis2ws.service.view.manager.Result;
import phis2ws.service.view.model.phis.Dataset;

/**
 * A class which represents the result part in the response form, adapted to the 
 * dataset
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ResultDataset extends Result<Dataset> {
    /**
     * Constructor which calls the mother-class constructor in the case of a
     * list with only 1 element
     * @param datasets 
     */
    public ResultDataset(ArrayList<Dataset> datasets) {
        super(datasets);
    }
    
    /**
     * Contructor which calls the mother-class constructor in the case of a
     * list with several elements
     * @param datasets
     * @param pagination
     * @param paginate 
     */
    public ResultDataset(ArrayList<Dataset> datasets, Pagination pagination, boolean paginate) {
        super(datasets, pagination, paginate);
    }
}
