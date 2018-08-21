//******************************************************************************
//                                       ResponseFormStudies.java
// SILEX-PHIS
// Copyright © INRA 2018
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.brapi.form;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Metadata;
import phis2ws.service.view.brapi.results.ResultStudies;
import phis2ws.service.view.manager.ResultForm;
import phis2ws.service.view.model.phis.StudiesSearch;


public class ResponseFormStudies extends ResultForm {
     /**
     * Initializes Metadata and Results fields
     * @param pageSize number of results per page
     * @param currentPage requested page
     * @param list List of studies
     * @param paginate 
     */
    public ResponseFormStudies(int pageSize, int currentPage, ArrayList<StudiesSearch> list, boolean paginate) {
        metadata = new Metadata(pageSize, currentPage, list.size());
        if (list.size() > 1) {
            result = new ResultStudies(list, metadata.getPagination(), paginate); 
        } else {
            result = new ResultStudies(list);
        }
    }
}
