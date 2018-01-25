//**********************************************************************************************
//                                       ResultatConcept.java 
//
// Author(s): Eloan LAGIER
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: Janvier 25, 2018
// Contact: eloan.lagier@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date: Janvier 5, 2018
// Subject: extend form Resultat adapted to the Concept
//***********************************************************************************************
package phis2ws.service.view.brapi.results;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Pagination;
import phis2ws.service.view.manager.Resultat;
import phis2ws.service.view.model.phis.Concept;

/**
 *
 * @author Eloan LAGIER
 */
public class ResultatConcept extends Resultat<Concept>{
    /**
     * Constructeur qui appelle celui de la classe mère dans le cas d'une liste 
     * à un seul élément
     * @param concepts 
     */
    public ResultatConcept(ArrayList<Concept> concepts) {
        super(concepts);
    }
    
     /**
     * Constructeur qui appelle celui de la classe mère dans le cas d'une liste 
     * à plusieurs éléments
     * @param concepts
     * @param pagination
     * @param paginate 
     */
    public ResultatConcept(ArrayList<Concept> concepts, Pagination pagination, boolean paginate) {
        super(concepts, pagination, paginate);
    }
    
    
}
