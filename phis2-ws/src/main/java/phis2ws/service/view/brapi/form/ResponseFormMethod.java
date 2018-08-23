//**********************************************************************************************
//                                       ResponseFormMethod.java 
//
// Author(s): Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: November, 23 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  November, 23 2017
// Subject: Allows the formating of the result of the queries about methods
//***********************************************************************************************
package phis2ws.service.view.brapi.form;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Metadata;
import phis2ws.service.view.brapi.results.ResultMethod;
import phis2ws.service.view.manager.ResultForm;
import phis2ws.service.view.model.phis.Method;

public class ResponseFormMethod extends ResultForm<Method> {
    /**
     * Initialise les champs metadata et result
     * @param pageSize nombre de résultats par page
     * @param currentPage page demandée
     * @param list liste des résultats
     * @param paginate 
     */
    public ResponseFormMethod(int pageSize, int currentPage, ArrayList<Method> list, boolean paginate) {
        metadata = new Metadata(pageSize, currentPage, list.size());
        if (list.size() > 1) {
            result = new ResultMethod(list, metadata.getPagination(), paginate);
        } else {
            result = new ResultMethod(list);
        }
    }
}
