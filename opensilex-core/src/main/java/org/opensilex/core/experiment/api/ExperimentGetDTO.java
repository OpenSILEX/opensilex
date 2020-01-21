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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.rest.group.dal.GroupModel;
import org.opensilex.rest.serialization.LocalDateDeserializer;
import org.opensilex.rest.serialization.LocalDateSerializer;
import org.opensilex.rest.user.dal.UserModel;
import org.opensilex.sparql.model.SPARQLResourceModel;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * @author Vincent MIGOT
 */
public class ExperimentGetDTO {

    protected URI uri;

    protected String label;

    protected List<URI> projects = Collections.emptyList();

    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    protected LocalDate startDate;

    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    protected LocalDate endDate;

    protected String objective;

    protected String comment;

    protected Integer campaign;

    protected List<String> keywords = Collections.emptyList();

    protected List<URI> scientificSupervisors = Collections.emptyList();

    protected List<URI> technicalSupervisors = Collections.emptyList();

    protected List<URI> groups = Collections.emptyList();

    protected List<URI> species = Collections.emptyList();


    public URI getUri() {
        return uri;
    }

    public String getLabel() {
        return label;
    }

    public List<URI> getProjects() {
        return projects;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public String getObjective() {
        return objective;
    }

    public String getComment() {
        return comment;
    }

    public Integer getCampaign() {
        return campaign;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public List<URI> getScientificSupervisors() {
        return scientificSupervisors;
    }

    public List<URI> getTechnicalSupervisors() {
        return technicalSupervisors;
    }

    public List<URI> getGroups() {
        return groups;
    }

    public List<URI> getSpecies() {
        return species;
    }

    public ExperimentGetDTO setUri(URI uri) {
        this.uri = uri;
        return this;
    }

    public ExperimentGetDTO setLabel(String label) {
        this.label = label;
        return this;
    }

    public ExperimentGetDTO setProjects(List<URI> projects) {
        this.projects = projects;
        return this;
    }

    public ExperimentGetDTO setStartDate(LocalDate startDate) {
        this.startDate = startDate;
        return this;
    }

    public ExperimentGetDTO setEndDate(LocalDate endDate) {
        this.endDate = endDate;
        return this;
    }

    public ExperimentGetDTO setObjective(String objective) {
        this.objective = objective;
        return this;
    }

    public ExperimentGetDTO setComment(String comment) {
        this.comment = comment;
        return this;
    }

    public ExperimentGetDTO setCampaign(Integer campaign) {
        this.campaign = campaign;
        return this;
    }

    public ExperimentGetDTO setKeywords(List<String> keywords) {
        this.keywords = keywords;
        return this;
    }

    public ExperimentGetDTO setScientificSupervisors(List<URI> scientificSupervisors) {
        this.scientificSupervisors = scientificSupervisors;
        return this;
    }

    public ExperimentGetDTO setTechnicalSupervisors(List<URI> technicalSupervisors) {
        this.technicalSupervisors = technicalSupervisors;
        return this;
    }

    public ExperimentGetDTO setGroups(List<URI> groups) {
        this.groups = groups;
        return this;
    }

    public ExperimentGetDTO setSpecies(List<URI> species) {
        this.species = species;
        return this;
    }

    public static ExperimentGetDTO fromModel(ExperimentModel model) {

        ExperimentGetDTO dto = new ExperimentGetDTO();

        dto.setUri(model.getUri())
                .setLabel(model.getLabel())
                .setStartDate(model.getStartDate())
                .setEndDate(model.getEndDate())
                .setCampaign(model.getCampaign())
                .setObjective(model.getObjectives())
                .setComment(model.getComment())
                .setKeywords(model.getKeywords());

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
        } else {
            dto.setTechnicalSupervisors(Collections.emptyList());
        }

        List<ProjectModel> projectModels = model.getProjects();
        if (projectModels != null) {
            dto.setProjects(projectModels.stream()
                    .map(SPARQLResourceModel::getUri)
                    .collect(Collectors.toList()));
        } else {
            dto.setProjects(Collections.emptyList());
        }

        List<GroupModel> groupModels = model.getGroups();
        if (groupModels != null) {
            dto.setGroups(groupModels.stream()
                    .map(SPARQLResourceModel::getUri)
                    .collect(Collectors.toList()));
        } else {
            dto.setGroups(Collections.emptyList());
        }
        return dto;
    }
}
