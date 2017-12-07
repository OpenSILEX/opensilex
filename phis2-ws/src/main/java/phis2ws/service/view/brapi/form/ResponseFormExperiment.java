//**********************************************************************************************
//                                       ResponseFormExperiment.java 
//
// Author(s): Morgane VIDAL
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: august 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  October, 31 2017 : Passage de trial à experiment
// Subject: Allows the formating of the results of the queries about experiments
//***********************************************************************************************

package phis2ws.service.view.brapi.form;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Metadata;
import phis2ws.service.view.brapi.results.ResultatExperiment;
import phis2ws.service.view.manager.ResultForm;
import phis2ws.service.view.model.phis.Experiment;


public class ResponseFormExperiment extends ResultForm<Experiment>{
    
    /**
     * Initialise les champs metadata et result
     * @param pageSize nombre de résultats par page
     * @param currentPage page demandée
     * @param list liste des résultats
     * @param paginate 
     */
    public ResponseFormExperiment(int pageSize, int currentPage, ArrayList<Experiment> list, boolean paginate) {
        metadata = new Metadata(pageSize, currentPage, list.size());
        if (list.size() > 1) {
            result = new ResultatExperiment(list, metadata.getPagination(), paginate); 
        } else {
            result = new ResultatExperiment(list);
        }
    }
}
