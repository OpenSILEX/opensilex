//**********************************************************************************************
//                                       ResultatInstance.java 
//
// Author(s): Eloan LAGIER
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: Decembre 8, 2017
// Contact: eloan.lagier@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  Decembre 8, 2017
// Subject: extend form Resultat adapted to the instance
//***********************************************************************************************
package phis2ws.service.view.brapi.results;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Pagination;
import phis2ws.service.view.manager.Resultat;
import phis2ws.service.view.model.phis.Instance;

/**
 *
 * @author lagier
 */
public class ResultatInstance extends Resultat<Instance> {

    /**
     * Builder for a one element-list
     *
     * @param instances
     */
    public ResultatInstance(ArrayList<Instance> instances) {
        super(instances);
    }

    /**
     * Builder for a more-than-one element list
     *
     * @param instances
     * @param pagination
     * @param paginate
     */
    public ResultatInstance(ArrayList<Instance> instances, Pagination pagination, boolean paginate) {
        super(instances, pagination, paginate);
    }
}
