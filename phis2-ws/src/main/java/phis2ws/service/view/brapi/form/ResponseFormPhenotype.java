//**********************************************************************************************
//                                       ResponseFormPhenotype.java 
//
// Author(s): Morgane VIDAL
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: October, 18 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  October, 18 2017
// Subject: Allows the formating of the result of the request about Phenotypes
//***********************************************************************************************
package phis2ws.service.view.brapi.form;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Metadata;
import phis2ws.service.view.brapi.results.ResultatPhenotype;
import phis2ws.service.view.manager.ResultForm;
import phis2ws.service.view.model.phis.Phenotype;

public class ResponseFormPhenotype extends ResultForm<Phenotype> {
    /**
     * Initialise les champs metadata et result
     * @param pageSize nombre de résultats par page
     * @param currentPage page demandée
     * @param list liste des résultats
     * @param paginate 
     */
    public ResponseFormPhenotype(int pageSize, int currentPage, ArrayList<Phenotype> list, boolean paginate) {
        metadata = new Metadata(pageSize, currentPage, list.size());
        if (list.size() > 1) {
            result = new ResultatPhenotype(list, metadata.getPagination(), paginate);
        } else {
            result = new ResultatPhenotype(list);
        }
    }
}
