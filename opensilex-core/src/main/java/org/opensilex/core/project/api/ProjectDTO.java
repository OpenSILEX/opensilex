//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
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
 * @author Julien BONNEFONT A basic DTO class about an {@link ProjectModel}
 */
public abstract class ProjectDTO {

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

    public ProjectDTO setUri(URI uri) {
        this.uri = uri;
        return this;
    }

    public String getLabel() {
        return label;
    }

    public ProjectDTO setLabel(String label) {
        this.label = label;
        return this;
    }

    public String getShortname() {
        return shortname;
    }

    public ProjectDTO setShortname(String shortname) {
        this.shortname = shortname;
        return this;
    }

    public String getHasFinancialFunding() {
        return hasFinancialFunding;
    }

    public ProjectDTO setHasFinancialFunding(String hasFinancialFunding) {
        this.hasFinancialFunding = hasFinancialFunding;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public ProjectDTO setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getObjective() {
        return objective;
    }

    public ProjectDTO setObjective(String objective) {
        this.objective = objective;
        return this;
    }

    @NotNull
    @ApiModelProperty(example = "2020-02-20")
    public LocalDate getStartDate() {
        return startDate;
    }

    public ProjectDTO setStartDate(LocalDate startDate) {
        this.startDate = startDate;
        return this;
    }

    @ApiModelProperty(example = "2020-02-20")
    public LocalDate getEndDate() {
        return endDate;
    }

    public ProjectDTO setEndDate(LocalDate endDate) {
        this.endDate = endDate;
        return this;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public ProjectDTO setKeywords(List<String> keywords) {
        this.keywords = keywords;
        return this;
    }

    public URI getHomePage() {
        return homePage;
    }

    public ProjectDTO setHomePage(URI homePage) {
        this.homePage = homePage;
        return this;
    }

    @ApiModelProperty(example = "http://www.phenome-fppn.fr/id/species/zeamays")
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

//    public List<URI> getGroups() {
//        return groups;
//    }
//
//    public ProjectDTO setGroups(List<URI> groups) {
//        this.groups = groups;
//        return this;
//    }
//    
//     @ApiModelProperty(example = "true")
//    public Boolean getIsPublic() {
//        return isPublic;
//    }
//
//    public ProjectDTO setIsPublic(Boolean isPublic) {
//        this.isPublic = isPublic;
//        return this;
//    }
}
