//******************************************************************************
//                          ExperimentDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.experiment.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.experiment.dal.ExperimentModel;

import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;
import org.opensilex.server.rest.validation.Required;

/**
 * @author Renaud COLIN A basic DTO class about an {@link ExperimentModel}
 */
public abstract class ExperimentDTO {

    @JsonProperty("uri")
    protected URI uri;

    @JsonProperty("name")
    protected String name;

    @JsonProperty("start_date")
    protected LocalDate startDate;

    @JsonProperty("end_date")
    protected LocalDate endDate;

    @JsonProperty("description")
    protected String description;
    
    @JsonProperty("objective")
    protected String objective;

    @JsonProperty("organisations") 
    protected List<URI> infrastructures = new ArrayList<>();

    @JsonProperty("facilities")
    protected List<URI> facilities = new ArrayList<>();
     
    @JsonProperty("projects")
    protected List<URI> projects = new ArrayList<>();

    @JsonProperty("scientific_supervisors")
    protected List<URI> scientificSupervisors = new ArrayList<>();

    @JsonProperty("technical_supervisors")
    protected List<URI> technicalSupervisors = new ArrayList<>();

    @JsonProperty("groups")
    protected List<URI> groups = new ArrayList<>();
    
    @JsonProperty("factors")
    protected List<URI> factors = new ArrayList<>();

    @JsonProperty("is_public")
    protected Boolean isPublic;



    public URI getUri() {
        return uri;
    }

    public ExperimentDTO setUri(URI uri) {
        this.uri = uri;
        return this;
    }

    @Required
    @ApiModelProperty(required = true)
    public String getName() {
        return name;
    }

    public ExperimentDTO setName(String name) {
        this.name = name;
        return this;
    }

    
    public List<URI> getProjects() {
        return projects;
    }

    public ExperimentDTO setProjects(List<URI> projects) {
        this.projects = projects;
        return this;
    }

    @NotNull
    @ApiModelProperty(example = "2020-02-20")
    public LocalDate getStartDate() {
        return startDate;
    }

    public ExperimentDTO setStartDate(LocalDate startDate) {
        this.startDate = startDate;
        return this;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public ExperimentDTO setEndDate(LocalDate endDate) {
        this.endDate = endDate;
        return this;
    }

    @Required
    @ApiModelProperty(required = true)
    public String getObjective() {
        return objective;
    }

    public ExperimentDTO setObjective(String objective) {
        this.objective = objective;
        return this;
    }
   
    public String getDescription() {
        return description;
    }

    public ExperimentDTO setDescription(String description) {
        this.description = description;
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

    public List<URI> getFacilities() {
        return facilities;
    }

    public ExperimentDTO setFacilities(List<URI> facilities) {
        this.facilities = facilities;
        return this;
    }

    public List<URI> getFactors() {
        return factors;
    }

    public ExperimentDTO setFactors(List<URI> factors) {
        this.factors = factors;
        return this;
    }
}
