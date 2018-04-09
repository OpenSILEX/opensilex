//******************************************************************************
//                                       ResponseFormVector.java
//
// Author(s): Morgane Vidal <morgane.vidal@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 6 avr. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  6 avr. 2018
// Subject: Allows the formating of the result of the request about vector
//******************************************************************************
package phis2ws.service.view.brapi.form;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Metadata;
import phis2ws.service.view.brapi.results.ResultVector;
import phis2ws.service.view.manager.ResultForm;
import phis2ws.service.view.model.phis.Vector;

/**
 *Allows the formating of the result of the request about Vector
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ResponseFormVector extends ResultForm<Vector> {
    /**
     * Initialize fields metadata and result
     * @param pageSize results per page
     * @param currentPage current page
     * @param list results list
     * @param paginate 
     */
    public ResponseFormVector(int pageSize, int currentPage, ArrayList<Vector> list, boolean paginate) {
        metadata = new Metadata(pageSize, currentPage, list.size());
        if (list.size() > 1) {
            result = new ResultVector(list, metadata.getPagination(), paginate);
        } else {
            result = new ResultVector(list);
        }
    }
}
