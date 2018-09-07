//******************************************************************************
//                                       ResultInfrastructure.java
// SILEX-PHIS
// Copyright Â© INRA 2018
// Creation date: 5 sept. 2018
// Contact: vincent.migot@inra.fr anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.brapi.results;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Pagination;
import phis2ws.service.view.manager.Result;
import phis2ws.service.view.model.phis.Infrastructure;

/**
 * A class which represents the result part in the response form, adapted to the infrastructures
 * @author Vincent Migot <vincent.migot@inra.fr>
 */
public class ResultInfrastructure extends Result<Infrastructure> {

    public ResultInfrastructure(ArrayList<Infrastructure> infrastructure) {
        super(infrastructure);
    }
    
    public ResultInfrastructure(ArrayList<Infrastructure> infrastructure, Pagination pagination, boolean paginate) {
        super(infrastructure, pagination, paginate);
    }
}
