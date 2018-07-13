//**********************************************************************************************
//                                       ResultatExperiment.java 
//
// Author(s): Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: august 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  January, 2017
// Subject: extended from Resultat adapted to the experiment list
//***********************************************************************************************

package phis2ws.service.view.brapi.results;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Pagination;
import phis2ws.service.view.manager.Resultat;
import phis2ws.service.view.model.phis.Calls;;


public class ResultCalls extends Resultat<Calls>{
    
    /**
     * Constructeur qui appelle celui de la classe mère dans le cas d'une liste 
     * à un seul élément
     * @param callsList liste des calls contenant un seul élément
     */
    public ResultCalls(ArrayList<Calls> callsList) {
        super(callsList);
    }
    
    /**
     * Contructeur qui appelle celui de la classe mère dans le cas d'une liste 
     * à plusieurs éléments
     * @param callsList liste des calls
     * @param pagination Objet pagination permettant de trier la liste experimentList
     * @param paginate 
     */
    public ResultCalls(ArrayList<Calls> callsList, Pagination pagination, 
            boolean paginate) {
        super(callsList, pagination, paginate);
    }
}
