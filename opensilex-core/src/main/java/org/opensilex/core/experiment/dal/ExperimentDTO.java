//******************************************************************************
//                          ExperimentDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package org.opensilex.core.experiment.dal;

import org.opensilex.rest.validation.Required;

import java.net.URI;
import java.util.Collections;
import java.util.List;

/**
 * @author Renaud COLIN
 * A basic DTO class about an {@link ExperimentModel}
 */
public abstract class ExperimentDTO {

    protected URI uri;

    @Required
    protected String label;

    protected List<URI> projects = Collections.emptyList();

    @Required
    protected String startDate;

    protected String endDate;

    protected String objective;

    protected String comment;

    protected Integer campaign;

    protected List<String> keywords = Collections.emptyList();

    protected List<URI> scientificSupervisors = Collections.emptyList();

    protected List<URI> technicalSupervisors = Collections.emptyList();

    protected List<URI> groups = Collections.emptyList();

    protected List<URI> infrastructures = Collections.emptyList();

    protected  List<URI> installations = Collections.emptyList();

    protected URI species;

    protected Boolean isPublic;

    protected List<URI> variables = Collections.emptyList();

    protected List<URI> sensors = Collections.emptyList();


    public URI getUri() {
        return uri;
    }

    public ExperimentDTO setUri(URI uri) {
        this.uri = uri;
        return this;
    }

    public String getLabel() {
        return label;
    }

    public ExperimentDTO setLabel(String label) {
        this.label = label;
        return this;
    }

    public List<URI> getProjects() {
        return projects;
    }

    public ExperimentDTO setProjects(List<URI> projects) {
        this.projects = projects;
        return this;
    }

    public String getStartDate() {
        return startDate;
    }

    public ExperimentDTO setStartDate(String startDate) {
        this.startDate = startDate;
        return this;
    }

    public String getEndDate() {
        return endDate;
    }

    public ExperimentDTO setEndDate(String endDate) {
        this.endDate = endDate;
        return this;
    }

    public String getObjective() {
        return objective;
    }

    public ExperimentDTO setObjective(String objective) {
        this.objective = objective;
        return this;
    }

    public String getComment() {
        return comment;
    }

    public ExperimentDTO setComment(String comment) {
        this.comment = comment;
        return this;
    }

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

    public URI getSpecies() {
        return species;
    }

    public ExperimentDTO setSpecies(URI species) {
        this.species = species;
        return this;
    }

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
