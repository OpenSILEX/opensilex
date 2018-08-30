//**********************************************************************************************
//                                       ResultGroup.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: April 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//***********************************************************************************************
package phis2ws.service.view.brapi.results;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Pagination;
import phis2ws.service.view.manager.Result;
import phis2ws.service.view.model.phis.Group;

/**
 * A class which represents the result part in the response form, adapted to the
 * groups.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ResultGroup extends Result<Group> {
    /**
     * Constructor which calls the mother-class constructor in the case of a
     * list with only 1 element
     * @param groups 
     */
    public ResultGroup(ArrayList<Group> groups) {
        super(groups);
    }
    
    /**
     * Contructor which calls the mother-class constructor in the case of a
     * list with several elements
     * @param groups
     * @param pagination
     * @param paginate 
     */
    public ResultGroup(ArrayList<Group> groups, Pagination pagination, boolean paginate) {
        super(groups, pagination, paginate);
    }
}

