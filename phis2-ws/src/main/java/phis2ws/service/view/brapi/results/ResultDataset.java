//**********************************************************************************************
//                                       ResultDataset.java 
//
// Author(s): Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: October, 18 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  October, 18 2017
// Subject: extend from Resultat, adapted to the dataset object list
//***********************************************************************************************
package phis2ws.service.view.brapi.results;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Pagination;
import phis2ws.service.view.manager.Resultat;
import phis2ws.service.view.model.phis.Dataset;

/**
 * A class which represents the result part in the response form, adapted to the 
 * dataset
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ResultDataset extends Resultat<Dataset> {
    /**
     * @param phenotypes the datasets of the result 
     */
    public ResultDataset(ArrayList<Dataset> phenotypes) {
        super(phenotypes);
    }
    
    /**
     * @param phenotypes
     * @param pagination
     * @param paginate 
     */
    public ResultDataset(ArrayList<Dataset> phenotypes, Pagination pagination, boolean paginate) {
        super(phenotypes, pagination, paginate);
    }
}
