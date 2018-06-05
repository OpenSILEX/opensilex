//**********************************************************************************************
//                                       ResponseFormUser.java 
//
// Author(s): Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: April 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  April, 2017
// Subject: Allows the formating of the results of the queries about groups
//***********************************************************************************************
package phis2ws.service.view.brapi.form;

import java.util.ArrayList;
import phis2ws.service.model.User;
import phis2ws.service.view.brapi.Metadata;
import phis2ws.service.view.brapi.results.ResultatUser;
import phis2ws.service.view.manager.ResultForm;

public class ResponseFormUser extends ResultForm<User> {
    
    /**
     * Initialise les champs metadata et result
     * @param pageSize nombre de résultats par page
     * @param currentPage page demandée
     * @param groups liste des resultats
     * @param paginate 
     */
    public ResponseFormUser(int pageSize, int currentPage, ArrayList<User> groups, boolean paginate) {
        metadata = new Metadata(pageSize, currentPage, groups.size());
        if (groups.size() > 1) {
            result = new ResultatUser(groups, metadata.getPagination(), paginate);
        } else {
            result = new ResultatUser(groups);
        }
    }
}
