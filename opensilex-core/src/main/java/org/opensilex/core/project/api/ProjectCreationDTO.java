/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.project.api;

import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.security.person.dal.PersonModel;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

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
            List<PersonModel> adminList = new ArrayList<>(administrativeContacts.size());
            administrativeContacts.forEach((URI u) -> {
                PersonModel person = new PersonModel();
                person.setUri(u);
                adminList.add(person);
            });
             model.setAdministrativeContacts(adminList);
        }
       
        if(coordinators != null){
            List<PersonModel> coordList = new ArrayList<>(coordinators.size());
            coordinators.forEach((URI u) -> {
                PersonModel person = new PersonModel();
                person.setUri(u);
                coordList.add(person);
            });
            model.setCoordinators(coordList);
        }

        if(scientificContacts != null){
            List<PersonModel> scientList = new ArrayList<>(scientificContacts.size());
            scientificContacts.forEach((URI u) -> {
                PersonModel person = new PersonModel();
                person.setUri(u);
                scientList.add(person);
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
