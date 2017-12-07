//**********************************************************************************************
//                                       ResultatUser.java 
//
// Author(s): Morgane VIDAL
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: April 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  April, 2017
// Subject: extended from Resultat adapted to the user list
//***********************************************************************************************
package phis2ws.service.view.brapi.results;

import java.util.ArrayList;
import phis2ws.service.model.User;
import phis2ws.service.view.brapi.Pagination;
import phis2ws.service.view.manager.Resultat;

public class ResultatUser extends Resultat<User> {
    /**
     * appelle le constructeur de la classe mere, dans le cas d'une liste à un seul élément
     * @param users liste des users, contenant un seul élément
     */
    public ResultatUser(ArrayList<User> users) {
        super(users);
    }
    
    /**
     * appelle le constructeur parent dans le cas d'une liste à plusieurs éléments
     * @param users liste des users
     * @param pagination objet pagination permettant de trier les users
     * @param paginate 
     */
    public ResultatUser(ArrayList<User> users, Pagination pagination, boolean paginate) {
        super(users, pagination, paginate);
    }
}
