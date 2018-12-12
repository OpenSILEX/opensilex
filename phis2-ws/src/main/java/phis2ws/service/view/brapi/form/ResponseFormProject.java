//**********************************************************************************************
//                                       ResponseFormProject.java 
//
// Author(s): Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: March 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  March, 2017
// Subject: Allows the formating of the results of the queries about projects
//***********************************************************************************************
package phis2ws.service.view.brapi.form;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Metadata;
import phis2ws.service.view.brapi.results.ResultProject;
import phis2ws.service.view.manager.ResultForm;
import phis2ws.service.view.model.phis.Project;

public class ResponseFormProject extends ResultForm<Project> {
    
    /**
     * Initialise les champs metadata et result
     * @param pageSize nombre de résultats par page
     * @param currentPage page demandée
     * @param projects liste des résultats
     * @param paginate 
     * @param totalCount 
     */
    public ResponseFormProject(int pageSize, int currentPage, ArrayList<Project> projects, boolean paginate, int totalCount) {
        metadata = new Metadata(pageSize, currentPage, totalCount);
        if (projects.size() > 1) {
            result = new ResultProject(projects, metadata.getPagination(), paginate);
        } else {
            result = new ResultProject(projects);
        }
    }
}
