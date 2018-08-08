//******************************************************************************
//                                       ResponseFormStudy.java
//
// Author(s): boizetal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 19 juil. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  19 juil. 2018
// Subject:
//******************************************************************************
package phis2ws.service.view.brapi.form;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Metadata;
import phis2ws.service.view.brapi.results.ResultStudy;
import phis2ws.service.view.manager.ResultForm;
import phis2ws.service.view.model.phis.StudiesSearch;



public class ResponseFormStudy extends ResultForm {
        /**
     * Initialise les champs metadata et result
     * @param pageSize nombre de résultats par page
     * @param currentPage page demandée
     * @param list liste des résultats
     * @param paginate 
     */
    public ResponseFormStudy(int pageSize, int currentPage, ArrayList<StudiesSearch> list, boolean paginate) {
        metadata = new Metadata(pageSize, currentPage, list.size());
        if (list.size() > 1) {
            result = new ResultStudy(list, metadata.getPagination(), paginate); 
        } else {
            result = new ResultStudy(list);
        }
    }
}
