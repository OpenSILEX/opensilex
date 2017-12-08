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
import static phis2ws.service.authentication.TokenManager.Instance;
import phis2ws.service.view.brapi.Pagination;
import phis2ws.service.view.manager.Resultat;
import phis2ws.service.view.model.phis.Instance;

/**
 *
 * @author lagier
 */
public class ResultatInstance extends Resultat<Instance>{
    /**
     * Constructeur qui appelle celui de la classe mère dans le cas d'une liste 
     * à un seul élément
     * @param instances 
     */
    public ResultatInstance(ArrayList<Instance> instances) {
        super(instances);
    }
    
    /**
     * Constructeur qui appelle celui de la classe mère dans le cas d'une liste 
     * à plusieurs éléments
     * @param instances
     * @param pagination
     * @param paginate 
     */
    public ResultatInstance(ArrayList<Instance> instances, Pagination pagination, boolean paginate) {
        super(instances, pagination, paginate);
    }
}
