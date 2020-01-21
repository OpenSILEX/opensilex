/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.experiment.api;

import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.core.species.SpeciesModel;
import org.opensilex.rest.group.dal.GroupModel;
import org.opensilex.rest.user.dal.UserModel;
import org.opensilex.rest.validation.Required;
import org.opensilex.rest.validation.date.DateConstraint;

import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Vincent MIGOT
 */
public class ExperimentCreationDTO {

    private URI uri;

    private Integer campaign;

    @Required
    private String label;

    private List<URI> projects = Collections.emptyList();

    @Required
    @ApiModelProperty(example = "2015-08-07")
    @DateConstraint()
    private String startDate;

    @ApiModelProperty(example = "2015-08-07")
    @DateConstraint()
    private String endDate;

    private List<URI> scientificSupervisors = Collections.emptyList();

    private List<URI> technicalSupervisors = Collections.emptyList();

    private List<URI> groups = Collections.emptyList();

    List<URI> species = Collections.emptyList();

    private String objectives;

    private List<String> keywords;

    private String comment;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<URI> getProjects() {
        return projects;
    }

    public void setProjects(List<URI> projects) {
        this.projects = projects;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public List<URI> getScientificSupervisors() {
        return scientificSupervisors;
    }

    public void setScientificSupervisors(List<URI> scientificSupervisors) {
        this.scientificSupervisors = scientificSupervisors;
    }

    public List<URI> getTechnicalSupervisors() {
        return technicalSupervisors;
    }

    public void setTechnicalSupervisors(List<URI> technicalSupervisors) {
        this.technicalSupervisors = technicalSupervisors;
    }

    public List<URI> getGroups() {
        return groups;
    }

    public void setGroups(List<URI> groups) {
        this.groups = groups;
    }

    public String getObjectives() {
        return objectives;
    }

    public void setObjectives(String objectives) {
        this.objectives = objectives;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public Integer getCampaign() {
        return campaign;
    }

    public void setCampaign(Integer campaign) {
        this.campaign = campaign;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<URI> getSpecies() {
        return species;
    }

    public void setSpecies(List<URI> species) {
        this.species = species;
    }

    public ExperimentModel newModel() {

        ExperimentModel model = new ExperimentModel();
        model.setUri(getUri());
        model.setLabel(getLabel());
        model.setStartDate(LocalDate.parse(startDate));

        if (endDate != null) {
            model.setEndDate(LocalDate.parse(endDate));
        }
        model.setObjectives(getObjectives());
        model.setComment(getComment());
        model.setKeywords(keywords);
        model.setCampaign(campaign);

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

        List<SpeciesModel> speciesList = new ArrayList<>(species.size());
        groups.forEach((URI u) -> {
            SpeciesModel species = new SpeciesModel();
            species.setUri(u);
            speciesList.add(species);
        });
        model.setSpecies(speciesList);
        return model;
    }

}
