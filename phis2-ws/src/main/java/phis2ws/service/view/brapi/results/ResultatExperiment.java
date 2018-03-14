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
import phis2ws.service.view.model.phis.Experiment;


public class ResultatExperiment extends Resultat<Experiment>{
    
    /**
     * Constructeur qui appelle celui de la classe mère dans le cas d'une liste 
     * à un seul élément
     * @param experimentList liste des experimentations contenant un seul élément
     */
    public ResultatExperiment(ArrayList<Experiment> experimentList) {
        super(experimentList);
    }
    
    /**
     * Contructeur qui appelle celui de la classe mère dans le cas d'une liste 
     * à plusieurs éléments
     * @param experimentList liste des expérimentations
     * @param pagination Objet pagination permettant de trier la liste experimentList
     * @param paginate 
     */
    public ResultatExperiment(ArrayList<Experiment> experimentList, Pagination pagination, 
            boolean paginate) {
        super(experimentList, pagination, paginate);
    }
}
