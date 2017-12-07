//**********************************************************************************************
//                                       ResultatMethod.java 
//
// Author(s): Morgane VIDAL
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: November, 23 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  November, 23 2017
// Subject: extend form Resultat adapted to the methods
//***********************************************************************************************
package phis2ws.service.view.brapi.results;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Pagination;
import phis2ws.service.view.manager.Resultat;
import phis2ws.service.view.model.phis.Method;

public class ResultatMethod extends Resultat<Method> {
    /**
     * Constructeur qui appelle celui de la classe mère dans le cas d'une liste 
     * à un seul élément
     * @param methods 
     */
    public ResultatMethod(ArrayList<Method> methods) {
        super(methods);
    }
    
    /**
     * Constructeur qui appelle celui de la classe mère dans le cas d'une liste 
     * à plusieurs éléments
     * @param methods
     * @param pagination
     * @param paginate 
     */
    public ResultatMethod(ArrayList<Method> methods, Pagination pagination, boolean paginate) {
        super(methods, pagination, paginate);
    }
}
