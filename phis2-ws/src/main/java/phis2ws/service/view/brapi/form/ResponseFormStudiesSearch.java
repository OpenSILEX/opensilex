//******************************************************************************
//                                       ResponseFormStudiesSearch.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 22 août 2018
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.brapi.form;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Metadata;
import phis2ws.service.view.brapi.results.ResultStudiesSearch;
import phis2ws.service.view.manager.ResultForm;
import phis2ws.service.view.model.phis.StudiesSearch;

/**
 * Formating the result of the request on studies-search
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class ResponseFormStudiesSearch extends ResultForm {
     /**
     * Initializes Metadata and Results fields
     * @param pageSize number of results per page
     * @param currentPage requested page
     * @param list List of studies
     * @param paginate 
     */
    public ResponseFormStudiesSearch(int pageSize, int currentPage, ArrayList<StudiesSearch> list, boolean paginate) {
        metadata = new Metadata(pageSize, currentPage, list.size());
        if (list.size() > 1) {
            result = new ResultStudiesSearch(list, metadata.getPagination(), paginate); 
        } else {
            result = new ResultStudiesSearch(list);
        }
    }
}
