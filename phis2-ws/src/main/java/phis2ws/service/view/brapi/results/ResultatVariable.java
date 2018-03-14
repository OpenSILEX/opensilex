//**********************************************************************************************
//                                       ResulatVariable.java 
//
// Author(s): Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: Novemnber, 23 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  November, 23 2017
// Subject: extend form Resultat adapted to the variables
//***********************************************************************************************
package phis2ws.service.view.brapi.results;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Pagination;
import phis2ws.service.view.manager.Resultat;
import phis2ws.service.view.model.phis.Variable;

public class ResultatVariable extends Resultat<Variable>{
    
    /**
     * Constructeur qui appelle celui de la classe mère dans le cas d'une liste 
     * à un seul élément
     * @param variables 
     */
    public ResultatVariable(ArrayList<Variable> variables) {
        super(variables);
    }
    
    /**
     * Constructeur qui appelle celui de la classe mère dans le cas d'une liste 
     * à plusieurs éléments
     * @param variables
     * @param pagination
     * @param paginate 
     */
    public ResultatVariable(ArrayList<Variable> variables, Pagination pagination, boolean paginate) {
        super(variables, pagination, paginate);
    }
}