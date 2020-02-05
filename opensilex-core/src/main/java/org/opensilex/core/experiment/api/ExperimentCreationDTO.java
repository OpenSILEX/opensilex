/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.experiment.api;

import org.opensilex.core.experiment.dal.ExperimentDTO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.rest.group.dal.GroupModel;
import org.opensilex.rest.user.dal.UserModel;

import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Vincent MIGOT
 */
public class ExperimentCreationDTO extends ExperimentDTO {


    public ExperimentModel newModel() {

        ExperimentModel model = new ExperimentModel();
        model.setUri(getUri());
        model.setLabel(getLabel());
        model.setStartDate(LocalDate.parse(startDate));
        if (endDate != null) {
            model.setEndDate(LocalDate.parse(endDate));
        }

        model.setObjective(getObjective());
        model.setComment(getComment());
        model.setKeywords(keywords);
        model.setCampaign(campaign);
        model.setSpecies(species);
        model.setInfrastructures(infrastructures);
        model.setDevices(devices);
        model.setIsPublic(isPublic);
        model.setSensors(sensors);

        List<ProjectModel> projectList = new ArrayList<>(projects.size());
        projects.forEach((URI u) -> {
            ProjectModel project = new ProjectModel();
            project.setUri(u);
            projectList.add(project);
        });
        model.setProjects(projectList);

        List<UserModel> scientificList = new ArrayList<>(scientificSupervisors.size());
        scientificSupervisors.forEach((URI u) -> {
            UserModel user = new UserModel();
            user.setUri(u);
            scientificList.add(user);
        });
        model.setScientificSupervisors(scientificList);

        List<UserModel> technicalList = new ArrayList<>(technicalSupervisors.size());
        technicalSupervisors.forEach((URI u) -> {
            UserModel user = new UserModel();
            user.setUri(u);
            technicalList.add(user);
        });
        model.setTechnicalSupervisors(technicalList);

        List<GroupModel> groupList = new ArrayList<>(groups.size());
        groups.forEach((URI u) -> {
            GroupModel group = new GroupModel();
            group.setUri(u);
            groupList.add(group);
        });
        model.setGroups(groupList);

        List<VariableModel> varList = new ArrayList<>(variables.size());
        variables.forEach((URI u) -> {
            VariableModel var = new VariableModel();
            var.setUri(u);
            varList.add(var);
        });
        model.setVariables(varList);

        return model;
    }

}
