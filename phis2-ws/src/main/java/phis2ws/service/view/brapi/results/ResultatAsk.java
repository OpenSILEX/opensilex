//**********************************************************************************************
//                                       ResultatAsk.java 
//
// Author(s): Eloan LAGIER
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: Janvier 30 2018
// Contact: eloan.lagire@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  Janvier 30, 2018
// Subject: extend form Resultat adapted to the Ask
//***********************************************************************************************
package phis2ws.service.view.brapi.results;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Pagination;
import phis2ws.service.view.manager.Resultat;
import phis2ws.service.view.model.phis.Ask;

/**
 *
 * @author Eloan LAGIER
 */
public class ResultatAsk extends Resultat<Ask>{
      /**
     * Constructeur qui appelle celui de la classe mère
     * @param ask 
     */
    public ResultatAsk(ArrayList<Ask> ask) {
        super(ask);
    }
    
         /**
     * Constructeur qui appelle celui de la classe mère dans le cas d'une liste 
     * à plusieurs éléments
     * @param concepts
     * @param pagination
     * @param paginate 
     */
    public ResultatAsk(ArrayList<Ask> concepts, Pagination pagination, boolean paginate) {
        super(concepts, pagination, paginate);
    }
}
