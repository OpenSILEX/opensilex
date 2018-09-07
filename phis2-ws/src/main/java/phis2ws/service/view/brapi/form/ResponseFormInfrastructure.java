//******************************************************************************
//                                       ResponseFromInfrastructure.java
// SILEX-PHIS
// Copyright Â© INRA 2018
// Creation date: 5 sept. 2018
// Contact: vincent.migot@inra.fr anne.tireau@inra.fr, pascal.neveu@inra.fr
// Allows the formating of the result of the request about Infrastructure
//******************************************************************************
package phis2ws.service.view.brapi.form;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Metadata;
import phis2ws.service.view.brapi.results.ResultInfrastructure;
import phis2ws.service.view.manager.ResultForm;
import phis2ws.service.view.model.phis.Infrastructure;

/**
 * Allows the formating of the result of the request about Infrastructure
 * @author Vincent Migot <vincent.migot@inra.fr>
 */
public class ResponseFormInfrastructure extends ResultForm<Infrastructure> {

    public ResponseFormInfrastructure(int pageSize, int currentPage, ArrayList<Infrastructure> list, boolean paginate, int totalCount) {
        metadata = new Metadata(pageSize, currentPage, totalCount);
        if (totalCount > 1) {
            result = new ResultInfrastructure(list, metadata.getPagination(), paginate);
        } else {
            result = new ResultInfrastructure(list);
        }
    }

}
