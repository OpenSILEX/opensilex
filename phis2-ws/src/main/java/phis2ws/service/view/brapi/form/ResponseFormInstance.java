//                                       ResponseFormInstance.java 

// Author(s): Eloan LAGIER
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: Decembre 8, 2017
// Contact: eloan.lagier@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  Decembre 8, 2017
// Subject: Allows the formating of the result of the queries about Instances
//***********************************************************************************************
package phis2ws.service.view.brapi.form;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Metadata;
import phis2ws.service.view.brapi.results.ResultatInstance;
import phis2ws.service.view.manager.ResultForm;
import phis2ws.service.view.model.phis.Instance;



public class ResponseFormInstance extends ResultForm<Instance> {
    /**
     * Initialise les champs metadata et result
     * @param pageSize nombre de résultats par page
     * @param currentPage page demandée
     * @param list liste des résultats
     * @param paginate 
     */   
        public ResponseFormInstance(int pageSize, int currentPage, ArrayList<Instance> list, boolean paginate) {
        metadata = new Metadata(pageSize, currentPage, list.size());
        if (list.size() > 1) {
            result = new ResultatInstance(list, metadata.getPagination(), paginate);
        } else {
            result = new ResultatInstance(list);
        }
    }
}
