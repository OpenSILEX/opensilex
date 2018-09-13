//******************************************************************************
//                            ResponseFormUser.java
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: Apr, 2017
// Contact: morgane.vidal@inra.fr,arnaud.charleroy@inra.fr, anne.tireau@inra.fr, 
//          pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.brapi.form;

import java.util.ArrayList;
import phis2ws.service.model.User;
import phis2ws.service.view.brapi.Metadata;
import phis2ws.service.view.brapi.results.ResultUser;
import phis2ws.service.view.manager.ResultForm;

/**
 * Allows the formating of the results of the queries about users.
 * @author Morgane Vidal <morgane.vidal@inra.fr>, Arnaud Charleroy <arnaud.charleroy@inra.fr>
 * @update [Arnaud Charleroy] 13 Sept, 2018 : add paginated results constructor
 */
public class ResponseFormUser extends ResultForm<User> {
    
    /**
     * Initiate metadata and result response fields
     * @param pageSize results per page
     * @param currentPage current page
     * @param users users results list
     * @param paginate need to be paginate or not
     */
    public ResponseFormUser(int pageSize, int currentPage, ArrayList<User> users, boolean paginate) {
        metadata = new Metadata(pageSize, currentPage, users.size());
        if (users.size() > 1) {
            result = new ResultUser(users, metadata.getPagination(), paginate);
        } else {
            result = new ResultUser(users);
        }
    }
    
    /**
     * Initiate metadata and result response fields
     * @param pageSize results per page
     * @param currentPage current page
     * @param users users results list
     * @param paginate need to be paginate or not
     * @param totalCount the number of the items returned by the query
     */
    public ResponseFormUser(int pageSize, int currentPage, ArrayList<User> users, boolean paginate, int totalCount) {
        metadata = new Metadata(pageSize, currentPage, totalCount);
        if (users.size() > 1) {
            result = new ResultUser(users, metadata.getPagination(), paginate);
        } else {
            result = new ResultUser(users);
        }
    }
}
