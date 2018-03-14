//**********************************************************************************************
//                                       ResultatProject.java 
//
// Author(s): Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: March 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  March, 2017
// Subject: extended from Resultat adapted to the project list
//***********************************************************************************************
package phis2ws.service.view.brapi.results;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Pagination;
import phis2ws.service.view.manager.Resultat;
import phis2ws.service.view.model.phis.Project;

public class ResultatProject extends Resultat<Project> {
    
    /**
     * appelle le constructeur de la classe mere, dans le cas d'une liste à un seul éléments
     * @param projects liste des projets, contenant un seul élément
     */
    public ResultatProject(ArrayList<Project> projects) {
        super(projects);
    }
    
    /**
     * appelle le constructeur parent dans le cas d'une liste à plusieurs éléments
     * @param projects liste des projets
     * @param pagination objet pagination permettant de trier projects
     * @param paginate 
     */
    public ResultatProject(ArrayList<Project> projects, Pagination pagination, boolean paginate) {
        super(projects, pagination, paginate);
    }
}
