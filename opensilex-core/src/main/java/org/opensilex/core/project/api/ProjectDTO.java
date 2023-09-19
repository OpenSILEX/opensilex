//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.project.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;
import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.security.user.api.UserGetDTO;
import org.opensilex.server.rest.validation.Required;

/**
 * @author Julien BONNEFONT A basic DTO class about an {@link ProjectModel}
 */
public abstract class ProjectDTO {
    
    @JsonProperty("uri")
    protected URI uri;

    @JsonProperty("publisher")
    protected UserGetDTO publisher;

    @JsonProperty("publication_date")
    protected OffsetDateTime publicationDate;

    @JsonProperty("last_updated_date")
    protected OffsetDateTime lastUpdatedDate;

    @JsonProperty("name")
    protected String name;

    @JsonProperty("shortname")
    protected String shortname;

    @JsonProperty("start_date")
    protected LocalDate startDate;

    @JsonProperty("end_date")
    protected LocalDate endDate;
    
    @JsonProperty("description")
    protected String description;

    @JsonProperty("objective")
    protected String objective;
    
    @JsonProperty("financial_funding")
    protected String financialFunding;

    @JsonProperty("website")
    protected URI homePage;
    
    @JsonProperty("related_projects")
    protected List<URI> relatedProjects = new ArrayList<>();

    @JsonProperty("coordinators")
    protected List<URI> coordinators = new ArrayList<>();
    
    @JsonProperty("scientific_contacts")
    protected List<URI> scientificContacts = new ArrayList<>();

    @JsonProperty("administrative_contacts")
    protected List<URI> administrativeContacts = new ArrayList<>();
    
    @JsonProperty("experiments")
    protected List<URI> experiments = new ArrayList<>();


    public URI getUri() {
        return uri;
    }

    public ProjectDTO setUri(URI uri) {
        this.uri = uri;
        return this;
    }

    public UserGetDTO getPublisher() {
        return publisher;
    }

    public void setPublisher(UserGetDTO publisher) {
        this.publisher = publisher;
    }

    public OffsetDateTime getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(OffsetDateTime publicationDate) {
        this.publicationDate = publicationDate;
    }

    public OffsetDateTime getLastUpdatedDate() {
        return lastUpdatedDate;
    }

    public void setLastUpdatedDate(OffsetDateTime lastUpdatedDate) {
        this.lastUpdatedDate = lastUpdatedDate;
    }

    @Required
    @ApiModelProperty(example = "DROught-tolerant yielding PlantS", required=true)
    public String getName() {
        return name;
    }

    public ProjectDTO setName(String name) {
        this.name = name;
        return this;
    }
    @ApiModelProperty(example = "DROPS")
    public String getShortname() {
        return shortname;
    }

    public ProjectDTO setShortname(String shortname) {
        this.shortname = shortname;
        return this;
    }

    @ApiModelProperty(example = "European Union")
    public String getFinancialFunding() {
        return financialFunding;
    }

    public ProjectDTO setFinancialFunding(String financialFunding) {
        this.financialFunding = financialFunding;
        return this;
    }
    @ApiModelProperty(example = "DROPS aims at developing novel methods....")
    public String getDescription() {
        return description;
    }

    public ProjectDTO setDescription(String description) {
        this.description = description;
        return this;
    }
    @ApiModelProperty(example = "Developing novel methods and strategies for genetic yield improvement under dry environments and for enhanced plant water-use efficiency.")
    public String getObjective() {
        return objective;
    }

    public ProjectDTO setObjective(String objective) {
        this.objective = objective;
        return this;
    }

    @NotNull
    @ApiModelProperty(example = "2010-02-20", required=true)
    public LocalDate getStartDate() {
        return startDate;
    }

    public ProjectDTO setStartDate(LocalDate startDate) {
        this.startDate = startDate;
        return this;
    }

    @ApiModelProperty(example = "2015-02-20")
    public LocalDate getEndDate() {
        return endDate;
    }

    public ProjectDTO setEndDate(LocalDate endDate) {
        this.endDate = endDate;
        return this;
    }

    @ApiModelProperty(example = "https://www.inrae.fr/dropsproject")
    public URI getHomePage() {
        return homePage;
    }

    public ProjectDTO setHomePage(URI homePage) {
        this.homePage = homePage;
        return this;
    }

    public List<URI> getExperiments() {
        return experiments;
    }
    
    public ProjectDTO setExperiments(List<URI> experiments) {
        this.experiments = experiments;
        return this;
    }

    public List<URI> getAdministrativeContacts() {
        return administrativeContacts;
    }

    public ProjectDTO setAdministrativeContacts(List<URI> administrativeContacts) {
        this.administrativeContacts = administrativeContacts;
        return this;
    }

    public List<URI> getCoordinators() {
        return coordinators;
    }

    public ProjectDTO setCoordinators(List<URI> coordinators) {
        this.coordinators = coordinators;
        return this;
    }

    public List<URI> getScientificContacts() {
        return scientificContacts;
    }

    public ProjectDTO setScientificContacts(List<URI> scientificContacts) {
        this.scientificContacts = scientificContacts;
        return this;
    }

    public List<URI> getRelatedProjects() {
        return relatedProjects;
    }

    public ProjectDTO setRelatedProjects(List<URI> relatedProjects) {
        this.relatedProjects = relatedProjects;
        return this;
    }
}
