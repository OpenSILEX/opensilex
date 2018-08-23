//**********************************************************************************************
//                                       ResultProject.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: March 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//***********************************************************************************************
package phis2ws.service.view.brapi.results;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Pagination;
import phis2ws.service.view.manager.Result;
import phis2ws.service.view.model.phis.Project;

/**
 * A class which represents the result part in the response form, adapted to the
 * projects
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ResultProject extends Result<Project> {
    /**
     * Constructor which calls the mother-class constructor in the case of a
     * list with only 1 element
     * @param projects 
     */
    public ResultProject(ArrayList<Project> projects) {
        super(projects);
    }
    
    /**
     * Contructor which calls the mother-class constructor in the case of a
     * list with several elements
     * @param projects
     * @param pagination
     * @param paginate 
     */
    public ResultProject(ArrayList<Project> projects, Pagination pagination, boolean paginate) {
        super(projects, pagination, paginate);
    }
}
