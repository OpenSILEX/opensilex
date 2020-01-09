/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.experiment.api;

import java.net.URI;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.rest.user.dal.UserModel;
import org.opensilex.sparql.model.SPARQLResourceModel;

/**
 * @author Vincent MIGOT
 */
public class ExperimentGetDTO {

    private URI uri;

    private String label;

    private List<URI> projects = Collections.emptyList();

    private LocalDate startDate;

    private LocalDate endDate;

    private String objective;

    private String comment;

    private Integer campaign;

    private List<String> keywords = Collections.emptyList();

    private List<URI> scientificSupervisors = Collections.emptyList();

    private List<URI> technicalSupervisors = Collections.emptyList();

    private List<URI> groups = Collections.emptyList();

    public List<URI> getGroups() {
        return groups;
    }

    public void setGroups(List<URI> groups) {
        this.groups = groups;
    }

    public void setUri(URI uri) { this.uri = uri; }

    public URI getUri() {
        return uri;
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

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getObjective() { return objective; }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Integer getCampaign() { return campaign; }

    public void setCampaign(Integer campaign) { this.campaign = campaign; }

    public List<String> getKeywords() { return keywords; }

    public void setKeywords(List<String> keywords) { this.keywords = keywords; }

    public List<URI> getScientificSupervisors() { return scientificSupervisors;
    }
    public void setScientificSupervisors(List<URI> scientificSupervisors) { this.scientificSupervisors = scientificSupervisors; }

    public List<URI> getTechnicalSupervisors() { return technicalSupervisors; }

    public void setTechnicalSupervisors(List<URI> technicalSupervisors) { this.technicalSupervisors = technicalSupervisors; }


    public static ExperimentGetDTO fromModel(ExperimentModel model) {

        ExperimentGetDTO dto = new ExperimentGetDTO();

        dto.setUri(model.getUri());
        dto.setLabel(model.getLabel());
        dto.setStartDate(model.getStartDate());
        dto.setEndDate(model.getEndDate());

        dto.setCampaign(model.getCampaign());
        dto.setObjective(model.getObjectives());
        dto.setComment(model.getComment());
        dto.setKeywords(model.getKeywords());

        List<UserModel> scientificSupervisors = model.getScientificSupervisors();
        if (scientificSupervisors != null) {
            dto.setScientificSupervisors(scientificSupervisors.stream()
                .map(SPARQLResourceModel::getUri)
                .collect(Collectors.toList()));
        } else {
            dto.setScientificSupervisors(Collections.emptyList());
        }

        List<UserModel> technicalSupervisors = model.getTechnicalSupervisors();
        if (technicalSupervisors != null) {
            dto.setTechnicalSupervisors(technicalSupervisors.stream()
                .map(SPARQLResourceModel::getUri)
                .collect(Collectors.toList()));
        }
        else {
            dto.setTechnicalSupervisors(Collections.emptyList());
        }

        List<ProjectModel> projectModels = model.getProjects();
        if (projectModels != null) {
            dto.setProjects(projectModels.stream()
                .map(SPARQLResourceModel::getUri)
                .collect(Collectors.toList()));
        }
        else{
            dto.setProjects(Collections.emptyList());
        }

        return dto;
    }
}
