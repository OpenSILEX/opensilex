/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.project.api;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.security.account.dal.AccountModel;

/**
 *
 * @author Julien BONNEFONT
 */
public class ProjectCreationDTO extends ProjectDTO {

    public ProjectModel newModel() {

        ProjectModel model = new ProjectModel();

        model.setUri(getUri());

        model.setShortname(getShortname());
        model.setFinancialFunding(financialFunding);
        model.setStartDate(startDate);
        model.setEndDate(endDate);
        model.setName(name);
        model.setDescription(description);
        model.setObjective(objective);
        model.setHomePage(homePage);
        
        if(administrativeContacts != null){
            List<AccountModel> adminList = new ArrayList<>(administrativeContacts.size());
            administrativeContacts.forEach((URI u) -> {
                AccountModel user = new AccountModel();
                user.setUri(u);
                adminList.add(user);
            });
             model.setAdministrativeContacts(adminList);
        }
       
        if(coordinators != null){
            List<AccountModel> coordList = new ArrayList<>(coordinators.size());
            coordinators.forEach((URI u) -> {
                AccountModel user = new AccountModel();
                user.setUri(u);
                coordList.add(user);
            });
            model.setCoordinators(coordList);
        }

        if(scientificContacts != null){
            List<AccountModel> scientList = new ArrayList<>(scientificContacts.size());
            scientificContacts.forEach((URI u) -> {
                AccountModel user = new AccountModel();
                user.setUri(u);
                scientList.add(user);
            });
            model.setScientificContacts(scientList);
        }

        if(relatedProjects != null){
            List<ProjectModel> projectList = new ArrayList<>(relatedProjects.size());
            relatedProjects.forEach((URI u) -> {
                ProjectModel project = new ProjectModel();
                project.setUri(u);
                projectList.add(project);
            });
            model.setRelatedProjects(projectList);
        }

        return model;
    }

}
