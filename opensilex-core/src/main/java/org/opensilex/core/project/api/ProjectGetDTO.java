/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.project.api;

import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;
import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.server.rest.validation.Required;

/**
 *
 * A basic GetDTO which extends the {@link ProjectDTO} and which add the
 * conversion from an {@link ProjectModel} to a {@link ProjectGetDTO}
 *
 * @author vidalmor
 * @author Julien BONNEFONT
 */
public class ProjectGetDTO {

    protected URI uri;

    protected String label;

    protected String shortname;

    protected String hasFinancialFunding;

    protected String description;

    protected String objective;

    protected LocalDate startDate;

    protected LocalDate endDate;

    protected List<String> keywords = new ArrayList<>();

    protected URI homePage;

    protected List<URI> experiments = new ArrayList<>();

    protected List<URI> administrativeContacts = new ArrayList<>();

    protected List<URI> coordinators = new ArrayList<>();

    protected List<URI> scientificContacts = new ArrayList<>();

    protected List<URI> relatedProjects = new ArrayList<>();

    public URI getUri() {
        return uri;
    }

    public ProjectGetDTO setUri(URI uri) {
        this.uri = uri;
        return this;
    }

    public String getLabel() {
        return label;
    }

    public ProjectGetDTO setLabel(String label) {
        this.label = label;
        return this;
    }

    @Required
    @ApiModelProperty(example = "Blair witch")
    public String getShortname() {
        return shortname;
    }

    public ProjectGetDTO setShortname(String shortname) {
        this.shortname = shortname;
        return this;
    }

    public String getHasFinancialFunding() {
        return hasFinancialFunding;
    }

    public ProjectGetDTO setHasFinancialFunding(String hasFinancialFunding) {
        this.hasFinancialFunding = hasFinancialFunding;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public ProjectGetDTO setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getObjective() {
        return objective;
    }

    public ProjectGetDTO setObjective(String objective) {
        this.objective = objective;
        return this;
    }

    @NotNull
    @ApiModelProperty(example = "2020-02-20")
    public LocalDate getStartDate() {
        return startDate;
    }

    public ProjectGetDTO setStartDate(LocalDate startDate) {
        this.startDate = startDate;
        return this;
    }

    @ApiModelProperty(example = "2020-02-20")
    public LocalDate getEndDate() {
        return endDate;
    }

    public ProjectGetDTO setEndDate(LocalDate endDate) {
        this.endDate = endDate;
        return this;
    }

    public URI getHomePage() {
        return homePage;
    }

    public ProjectGetDTO setHomePage(URI homePage) {
        this.homePage = homePage;
        return this;
    }

    public static ProjectGetDTO fromModel(ProjectModel model) {

        ProjectGetDTO dto = new ProjectGetDTO();

        dto.setUri(model.getUri())
                .setLabel(model.getName())
                .setStartDate(model.getStartDate())
                .setEndDate(model.getEndDate())
                .setShortname(model.getShortname())
                .setHasFinancialFunding(model.getHasFinancialFunding())
                .setDescription(model.getDescription())
                .setObjective(model.getObjective())
                .setHomePage(model.getHomePage());

        return dto;
    }

}
