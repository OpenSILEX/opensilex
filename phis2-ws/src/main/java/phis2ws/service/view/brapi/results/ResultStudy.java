//******************************************************************************
//                                       ResultStudy.java
//
// Author(s): boizetal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 18 juil. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  18 juil. 2018
// Subject: extended from Resultat adapted to the studies list
//******************************************************************************
package phis2ws.service.view.brapi.results;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Pagination;
import phis2ws.service.view.manager.Resultat;
import phis2ws.service.view.model.phis.Experiment;
import phis2ws.service.view.model.phis.StudiesSearch;


public class ResultStudy extends Resultat<StudiesSearch>{
    
    /**
     * Constructeur qui appelle celui de la classe mère dans le cas d'une liste 
     * à un seul élément
     * @param studiesList liste des calls contenant un seul élément
     */
    public ResultStudy(ArrayList<StudiesSearch> studiesList) {
        super(studiesList);
    }
    
    /**
     * Contructeur qui appelle celui de la classe mère dans le cas d'une liste 
     * à plusieurs éléments
     * @param studiesList liste des study
     * @param pagination Objet pagination permettant de trier la liste experimentList
     * @param paginate 
     */
    public ResultStudy(ArrayList<StudiesSearch> studiesList, Pagination pagination, 
            boolean paginate) {
        super(studiesList, pagination, paginate);
    }
}
