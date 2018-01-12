//**********************************************************************************************
//                                       ResponseFormDataset.java 
//
// Author(s): Morgane VIDAL
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: October, 18 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  October, 18 2017
// Subject: Allows the formating of the result of the request about datasets
//***********************************************************************************************
package phis2ws.service.view.brapi.form;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Metadata;
import phis2ws.service.view.brapi.results.ResultDataset;
import phis2ws.service.view.manager.ResultForm;
import phis2ws.service.view.model.phis.Dataset;

/**
 * format the result of the request about datasets
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 * @see phis2ws.service.view.manager.ResultForm
 */
public class ResponseFormDataset extends ResultForm<Dataset> {
    /**
     * Initialize metadata and result
     * @param pageSize number of results per page
     * @param currentPage current page number
     * @param list results list
     * @param paginate 
     */
    public ResponseFormDataset(int pageSize, int currentPage, ArrayList<Dataset> list, boolean paginate) {
        metadata = new Metadata(pageSize, currentPage, list.size());
        if (list.size() > 1) {
            result = new ResultDataset(list, metadata.getPagination(), paginate);
        } else {
            result = new ResultDataset(list);
        }
    }
}
