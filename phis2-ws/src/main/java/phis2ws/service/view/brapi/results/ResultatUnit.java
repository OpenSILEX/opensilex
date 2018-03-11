//**********************************************************************************************
//                                       ResultatUnit.java 
//
// Author(s): Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: November, 23 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  November, 23 2017
// Subject: extend form Resultat adapted to the units
//***********************************************************************************************
package phis2ws.service.view.brapi.results;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Pagination;
import phis2ws.service.view.manager.Resultat;
import phis2ws.service.view.model.phis.Unit;

public class ResultatUnit extends Resultat<Unit>{
    /**
     * Constructeur qui appelle celui de la classe mère dans le cas d'une liste 
     * à un seul élément
     * @param units 
     */
    public ResultatUnit(ArrayList<Unit> units) {
        super(units);
    }
    
    /**
     * Constructeur qui appelle celui de la classe mère dans le cas d'une liste 
     * à plusieurs éléments
     * @param units
     * @param pagination
     * @param paginate 
     */
    public ResultatUnit(ArrayList<Unit> units, Pagination pagination, boolean paginate) {
        super(units, pagination, paginate);
    }
}
