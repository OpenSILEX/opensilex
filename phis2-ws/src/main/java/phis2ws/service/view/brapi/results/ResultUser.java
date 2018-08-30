//**********************************************************************************************
//                                       ResultUser.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: April 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//***********************************************************************************************
package phis2ws.service.view.brapi.results;

import java.util.ArrayList;
import phis2ws.service.model.User;
import phis2ws.service.view.brapi.Pagination;
import phis2ws.service.view.manager.Result;

/**
 * A class which represents the result part in the response form, adapted to the
 * users
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ResultUser extends Result<User> {
    /**
     * Constructor which calls the mother-class constructor in the case of a
     * list with only 1 element
     * @param users 
     */
    public ResultUser(ArrayList<User> users) {
        super(users);
    }
    
    /**
     * Contructor which calls the mother-class constructor in the case of a
     * list with several elements
     * @param users
     * @param pagination
     * @param paginate 
     */
    public ResultUser(ArrayList<User> users, Pagination pagination, boolean paginate) {
        super(users, pagination, paginate);
    }
}
