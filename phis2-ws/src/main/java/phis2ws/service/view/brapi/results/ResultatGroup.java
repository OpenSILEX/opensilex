//**********************************************************************************************
//                                       ResultatGroup.java 
//
// Author(s): Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: April 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  April, 2017
// Subject: extended from Resultat adapted to the group list
//***********************************************************************************************
package phis2ws.service.view.brapi.results;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Pagination;
import phis2ws.service.view.manager.Resultat;
import phis2ws.service.view.model.phis.Group;

public class ResultatGroup extends Resultat<Group> {
    
    /**
     * appelle le constructeur de la classe mere, dans le cas d'une liste à un seul élément
     * @param groups liste des groupes, contenant un seul élément
     */
    public ResultatGroup(ArrayList<Group> groups) {
        super(groups);
    }
    
    /**
     * appelle le constructeur parent dans le cas d'une liste à plusieurs éléments
     * @param groups liste des groupes
     * @param pagination objet pagination permettant de trier les groupes
     * @param paginate 
     */
    public ResultatGroup(ArrayList<Group> groups, Pagination pagination, boolean paginate) {
        super(groups, pagination, paginate);
    }
}

