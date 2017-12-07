//**********************************************************************************************
//                                       ResultatPhenotype.java 
//
// Author(s): Morgane VIDAL
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: October, 18 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  October, 18 2017
// Subject: extend from Resultat, adapted to the phenotype object list
//***********************************************************************************************
package phis2ws.service.view.brapi.results;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Pagination;
import phis2ws.service.view.manager.Resultat;
import phis2ws.service.view.model.phis.Phenotype;

public class ResultatPhenotype extends Resultat<Phenotype> {
    /**
     * Constructeur qui appelle celui de la classe mère dans le caqs d'une liste 
     * à un seul élément
     * @param phenotypes 
     */
    public ResultatPhenotype(ArrayList<Phenotype> phenotypes) {
        super(phenotypes);
    }
    
    /**
     * Constructeur qui appelle celui de la classe mère dans le cas d'une liste 
     * à plusieurs éléments
     * @param phenotypes
     * @param pagination
     * @param paginate 
     */
    public ResultatPhenotype(ArrayList<Phenotype> phenotypes, Pagination pagination, boolean paginate) {
        super(phenotypes, pagination, paginate);
    }
}
