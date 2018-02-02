//**********************************************************************************************
//                                       ResponseFormLabel.java 
//
// Author(s): Eloan LAGIER
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: Feb 1 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  Feb 1, 2018
// Subject: Allows the formating of the result of the queries about Label
//***********************************************************************************************
package phis2ws.service.view.brapi.form;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Metadata;
import phis2ws.service.view.brapi.results.ResultatLabel;
import phis2ws.service.view.manager.ResultForm;
import phis2ws.service.view.model.phis.Label;

/**
 *
 * @author Eloan LAGIER
 */
public class ResponseFormLabel extends ResultForm<Label>{
    /**
     * Initialise les champs metadata et result
     * @param pageSize nombre de résultats par page
     * @param currentPage page demandée
     * @param list liste des résultats
     * @param paginate 
     */   
        public ResponseFormLabel(int pageSize, int currentPage, ArrayList<Label> list, boolean paginate) {
        metadata = new Metadata(pageSize, currentPage, list.size());       
       if (list.size() > 1) {
            result = new ResultatLabel(list, metadata.getPagination(), paginate);
        } else {
            result = new ResultatLabel(list);
        }
    
        }
}
