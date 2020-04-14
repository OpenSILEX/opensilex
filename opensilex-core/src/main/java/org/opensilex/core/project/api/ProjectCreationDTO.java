/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.project.api;

import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.security.user.dal.UserModel;

/**
 *
 * @author Julien BONNEFONT
 */
public class ProjectCreationDTO extends ProjectDTO {

    public ProjectModel newModel() {

        ProjectModel model = new ProjectModel();
        
        model.setUri(getUri());
        model.setLabel(getLabel());
        model.setStartDate(LocalDate.parse(startDate));
        if (endDate != null) {
            model.setEndDate(LocalDate.parse(endDate));
        }
        model.setShortname(shortname);
        model.setDescription(description);
        model.setObjective(objective);
        
        List<String> keywordsList = new ArrayList<>(keywords.size());
        keywords.forEach((String u) -> {
            keywordsList.add(u);
        });
        model.setKeywords(keywordsList);
        
        model.setHomePage(homePage);
        
        List<UserModel> adminList= new ArrayList<>(administrativeContacts.size());
        administrativeContacts.forEach((URI u )-> {
            UserModel user = new UserModel();
            user.setUri(uri);
            adminList.add(user);
        });
        model.setAdministrativeContacts(adminList);
        
        List<UserModel> coordList= new ArrayList<>(coordinators.size());
        coordinators.forEach((URI u )-> {
            UserModel user = new UserModel();
            user.setUri(uri);
            coordList.add(user);
        });
        model.setCoordinators(coordList);
        
        List<UserModel> scientList= new ArrayList<>(scientificContacts.size());
        scientificContacts.forEach((URI u )-> {
            UserModel user = new UserModel();
            user.setUri(uri);
            scientList.add(user);
        });
        model.setCoordinators(scientList);
        
        List<ProjectModel> projectList = new ArrayList<>(relatedProjects.size());
        relatedProjects.forEach((URI u) -> {
            ProjectModel project = new ProjectModel();
            project.setUri(u);
            projectList.add(project);
        });
        model.setRelatedProjects(projectList);
        
        List<ExperimentModel> expeList = new ArrayList<>(experiments.size());
        experiments.forEach((URI u) -> {
            ExperimentModel expe = new ExperimentModel();
            expe.setUri(u);
            expeList.add(expe);
        });
        model.setExperiments(expeList);
        
//         List<GroupModel> groupList = new ArrayList<>(groups.size());
//        groups.forEach((URI u) -> {
//            GroupModel group = new GroupModel();
//            group.setUri(u);
//            groupList.add(group);
//        });
//        model.setGroups(groupList);
        

        return model;
    }

}
