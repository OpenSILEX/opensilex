/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.experiment.api;

import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.server.validation.Required;

/**
 *
 * @author vidalmor
 */
public class ExperimentCreationDTO {

    private URI uri;

    @Required
    private String alias;

    private List<URI> projects;

    @Required
    private String startDate;

    private String endDate;

    private List<URI> scientificSupervisors;

    private List<URI> technicalSupervisors;

    private List<URI> groups;

    private String objectives;

    private List<String> keywords;

    private String comment;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
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

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public ExperimentModel newModel() {
        ExperimentModel model = new ExperimentModel();
        model.setUri(getUri());
        model.setAlias(getAlias());

        List<ProjectModel> ps = new ArrayList<>();
        getProjects().forEach((URI u) -> {
            ProjectModel project = new ProjectModel();
            project.setUri(u);
            ps.add(project);
        });
        model.setProjects(ps);

        model.setStartDate(LocalDate.parse(getStartDate()));
        model.setEndDate(LocalDate.parse(getEndDate()));
        
        model.setObjectives(getObjectives());
        model.setComment(getComment());
        
        return model;
    }

}
