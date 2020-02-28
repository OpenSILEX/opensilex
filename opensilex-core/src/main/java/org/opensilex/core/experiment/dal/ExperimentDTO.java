//******************************************************************************
//                          ExperimentDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package org.opensilex.core.experiment.dal;

import io.swagger.annotations.ApiModelProperty;
import org.opensilex.rest.validation.Required;
import org.opensilex.rest.validation.date.DateConstraint;

import java.net.URI;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Renaud COLIN
 * A basic DTO class about an {@link ExperimentModel}
 */
public abstract class ExperimentDTO {

    protected URI uri;

    protected String label;

    protected List<URI> projects = new LinkedList<>();

    protected String startDate;

    protected String endDate;

    protected String objective;

    protected String comment;

    protected Integer campaign;

    protected List<String> keywords = new LinkedList<>();

    protected List<URI> scientificSupervisors = new LinkedList<>();

    protected List<URI> technicalSupervisors = new LinkedList<>();

    protected List<URI> groups = new LinkedList<>();

    protected List<URI> infrastructures = new LinkedList<>();

    protected  List<URI> installations = new LinkedList<>();

    protected URI species;

    protected Boolean isPublic;

    protected List<URI> variables = new LinkedList<>();

    protected List<URI> sensors = new LinkedList<>();


    public URI getUri() {
        return uri;
    }

    public ExperimentDTO setUri(URI uri) {
        this.uri = uri;
        return this;
    }

    @Required
    @ApiModelProperty(example = "ZA17")
    public String getLabel() {
        return label;
    }

    public ExperimentDTO setLabel(String label) {
        this.label = label;
        return this;
    }

    @ApiModelProperty(example = "http://www.phenome-fppn.fr/id/species/zeamays")
    public List<URI> getProjects() {
        return projects;
    }

    public ExperimentDTO setProjects(List<URI> projects) {
        this.projects = projects;
        return this;
    }

    @Required
    @ApiModelProperty(example = "2020-02-20")
    @DateConstraint
    public String getStartDate() {
        return startDate;
    }

    public ExperimentDTO setStartDate(String startDate) {
        this.startDate = startDate;
        return this;
    }

    @Required
    @ApiModelProperty(example = "2020-02-20")
    @DateConstraint
    public String getEndDate() {
        return endDate;
    }

    public ExperimentDTO setEndDate(String endDate) {
        this.endDate = endDate;
        return this;
    }

    @ApiModelProperty(example = "objective")
    public String getObjective() {
        return objective;
    }

    public ExperimentDTO setObjective(String objective) {
        this.objective = objective;
        return this;
    }

    @ApiModelProperty(example = "comment")
    public String getComment() {
        return comment;
    }

    public ExperimentDTO setComment(String comment) {
        this.comment = comment;
        return this;
    }

    @ApiModelProperty(example = "2020")
    public Integer getCampaign() {
        return campaign;
    }

    public ExperimentDTO setCampaign(Integer campaign) {
        this.campaign = campaign;
        return this;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public ExperimentDTO setKeywords(List<String> keywords) {
        this.keywords = keywords;
        return this;
    }

    public List<URI> getScientificSupervisors() {
        return scientificSupervisors;
    }

    public ExperimentDTO setScientificSupervisors(List<URI> scientificSupervisors) {
        this.scientificSupervisors = scientificSupervisors;
        return this;
    }

    public List<URI> getTechnicalSupervisors() {
        return technicalSupervisors;
    }

    public ExperimentDTO setTechnicalSupervisors(List<URI> technicalSupervisors) {
        this.technicalSupervisors = technicalSupervisors;
        return this;
    }

    public List<URI> getGroups() {
        return groups;
    }

    public ExperimentDTO setGroups(List<URI> groups) {
        this.groups = groups;
        return this;
    }
    @ApiModelProperty(example = "http://www.phenome-fppn.fr/id/species/zeamays")
    public URI getSpecies() {
        return species;
    }

    public ExperimentDTO setSpecies(URI species) {
        this.species = species;
        return this;
    }

    @ApiModelProperty(example = "true")
    public Boolean getIsPublic() {
        return isPublic;
    }

    public ExperimentDTO setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
        return this;
    }

    public List<URI> getInfrastructures() {
        return infrastructures;
    }

    public ExperimentDTO setInfrastructures(List<URI> infrastructures) {
        this.infrastructures = infrastructures;
        return this;
    }

    public List<URI> getVariables() {
        return variables;
    }

    public ExperimentDTO setVariables(List<URI> variables) {
        this.variables = variables;
        return this;
    }

    public List<URI> getSensors() {
        return sensors;
    }

    public ExperimentDTO setSensors(List<URI> sensors) {
        this.sensors = sensors;
        return this;
    }

    public List<URI> getInstallations() {
        return installations;
    }

    public ExperimentDTO setInstallations(List<URI> installations) {
        this.installations = installations;
        return this;
    }
}
