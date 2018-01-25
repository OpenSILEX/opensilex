//**********************************************************************************************
//                                       ResponseFormConcept.java 
//
// Author(s): Eloan LAGIER
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: Janvier, 25 2018
// Contact: eloan.lagier@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  Janvier, 5 2018
// Subject: Allows the formating of the result of the queries about Concept
//***********************************************************************************************
package phis2ws.service.view.brapi.form;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Metadata;
import phis2ws.service.view.brapi.results.ResultatConcept;
import phis2ws.service.view.manager.ResultForm;
import phis2ws.service.view.model.phis.Concept;

/**
 *
 * @author Eloan LAGIER
 */
public class ResponseFormConcept extends ResultForm<Concept>{
    /**
     * Initialise les champs metadata et result
     * @param pageSize nombre de résultats par page
     * @param currentPage page demandée
     * @param list liste des résultats
     * @param paginate 
     */   
        public ResponseFormConcept(int pageSize, int currentPage, ArrayList<Concept> list, boolean paginate) {
        metadata = new Metadata(pageSize, currentPage, list.size());
        result = new ResultatConcept(list, metadata.getPagination(), paginate);
       
       if (list.size() > 1) {
            result = new ResultatConcept(list, metadata.getPagination(), paginate);
        } else {
            result = new ResultatConcept(list);
        }
    
        }
}
