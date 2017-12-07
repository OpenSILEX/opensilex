//**********************************************************************************************
//                               ResultatAgronomicalObject.java 
//
// Author(s): Morgane VIDAL
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: august 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  January, 2017
// Subject: extend form Resultat adapted to the agronomical object list
//***********************************************************************************************

package phis2ws.service.view.brapi.results;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Pagination;
import phis2ws.service.view.manager.Resultat;
import phis2ws.service.view.model.phis.AgronomicalObject;

public class ResultatAgronomicalObject extends Resultat<AgronomicalObject>{
    
    /**
     * Constructeur qui appelle celui de la classe mère dans le cas d'une liste 
     * à un seul élément
     * @param agronomicalObjectList 
     */
    public ResultatAgronomicalObject(ArrayList<AgronomicalObject> agronomicalObjectList) {
        super(agronomicalObjectList);
    }
    
    /**
     * Constructeur qui appelle celui de la classe mère dans le cas d'une liste 
     * à plusieurs éléments
     * @param agronomicalObjectList
     * @param pagination
     * @param paginate 
     */
    public ResultatAgronomicalObject(ArrayList<AgronomicalObject> agronomicalObjectList, Pagination pagination, boolean paginate) {
        super(agronomicalObjectList, pagination, paginate);
    }
}
