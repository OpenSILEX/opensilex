//**********************************************************************************************
//                                       ResponseFormGroup.java 
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
import phis2ws.service.view.brapi.Metadata;
import phis2ws.service.view.brapi.results.ResultGroup;
import phis2ws.service.view.manager.ResultForm;
import phis2ws.service.view.model.phis.Group;

public class ResponseFormGroup extends ResultForm<Group> {
    
    /**
     * Initialise les champs metadata et result
     * @param pageSize nombre de résultats par page
     * @param currentPage page demandée
     * @param groups liste des resultats
     * @param paginate 
     * @param totalCount 
     */
    public ResponseFormGroup(int pageSize, int currentPage, ArrayList<Group> groups, boolean paginate, int totalCount) {
        metadata = new Metadata(pageSize, currentPage, totalCount);
        if (groups.size() > 1) {
            result = new ResultGroup(groups, metadata.getPagination(), paginate);
        } else {
            result = new ResultGroup(groups);
        }
    }
}
