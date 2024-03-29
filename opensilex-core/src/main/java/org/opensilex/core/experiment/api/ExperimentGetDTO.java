//******************************************************************************
//                          ExperimentGetDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.experiment.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.organisation.dal.OrganizationModel;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.server.rest.validation.Required;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.response.NamedResourceDTO;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.organisation.dal.OrganizationModel;
import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.security.user.api.UserGetDTO;
import org.opensilex.server.rest.validation.Required;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.response.NamedResourceDTO;

/**
 *
 * A basic GetDTO which extends the {@link ExperimentDTO} and which add the conversion from an {@link ExperimentModel} to a {@link ExperimentGetDTO}
 *
 * @author Vincent MIGOT
 * @author Renaud COLIN
 */
public class ExperimentGetDTO {

    
    @JsonProperty("uri")
    protected URI uri;

    @JsonProperty("publisher")
    private UserGetDTO publisher;

    @JsonProperty("publication_date")
    private OffsetDateTime publicationDate;

    @JsonProperty("last_updated_date")
    private OffsetDateTime lastUpdatedDate;

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
    
    @JsonProperty("species")
    protected List<URI> species = new ArrayList<>();
//
//    @JsonProperty("variables")
//    protected List<URI> variables = new ArrayList<>();
    
    @JsonProperty("factors") 
    protected List<URI> factors = new ArrayList<>();
    
    @JsonProperty("organisations") 
    protected List<NamedResourceDTO<OrganizationModel>> organizations = new ArrayList<>();

    @JsonProperty("facilities")
    protected List<NamedResourceDTO<FacilityModel>> facilities = new ArrayList<>();
     
    @JsonProperty("projects")
    protected List<NamedResourceDTO<ProjectModel>> projects = new ArrayList<>();
    
    @JsonProperty("scientific_supervisors")
    protected List<URI> scientificSupervisors = new ArrayList<>();

    @JsonProperty("technical_supervisors")
    protected List<URI> technicalSupervisors = new ArrayList<>();

    @JsonProperty("groups")
    protected List<URI> groups = new ArrayList<>();

    @JsonProperty("is_public")
    protected Boolean isPublic;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
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
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<NamedResourceDTO<ProjectModel>> getProjects() {
        return projects;
    }

    public void setProjects(List<NamedResourceDTO<ProjectModel>>projects) {
        this.projects = projects;
    }

    @NotNull
    @ApiModelProperty(example = "2020-02-20")
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

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public List<URI> getSpecies() {
        return species;
    }

    public void setSpecies(List<URI> species) {
        this.species = species;
    }

    @ApiModelProperty(example = "true")
    public Boolean getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    public List<NamedResourceDTO<OrganizationModel>> getOrganizations() {
        return organizations;
    }

    public void setOrganizations(List<NamedResourceDTO<OrganizationModel>> organizations) {
        this.organizations = organizations;
    }

    public List<NamedResourceDTO<FacilityModel>> getFacilities() {
        return facilities;
    }

    public void setFacilities(List<NamedResourceDTO<FacilityModel>> facilities) {
        this.facilities = facilities;
    }
//
//    public List<URI> getVariables() {
//        return variables;
//    }
//
//    public void setVariables(List<URI> variables) {
//        this.variables = variables;
//    }

    public List<URI> getFactors() {
        return factors;
    }

    public void setFactors(List<URI> factors) {
        this.factors = factors;
    }

    protected static List<URI> getUriList(List<? extends SPARQLResourceModel> models) {

        if (models == null || models.isEmpty()) {
            return Collections.emptyList();
        }
        return models.stream().map(SPARQLResourceModel::getUri)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public static ExperimentGetDTO fromModel(ExperimentModel model) {

        ExperimentGetDTO dto = new ExperimentGetDTO();

        dto.setUri(model.getUri());
        if (Objects.nonNull(model.getPublicationDate())) {
            dto.setPublicationDate(model.getPublicationDate());
        }
        if (Objects.nonNull(model.getLastUpdateDate())) {
            dto.setLastUpdatedDate(model.getLastUpdateDate());
        }
        dto.setName(model.getName());
        dto.setStartDate(model.getStartDate());
        dto.setObjective(model.getObjective());
        dto.setDescription(model.getDescription());
        dto.setIsPublic(model.getIsPublic());
//        dto.setVariables(model.getVariables());

        if (model.getEndDate() != null) {
            dto.setEndDate(model.getEndDate());
        }

        dto.setScientificSupervisors(getUriList(model.getScientificSupervisors()));
        dto.setTechnicalSupervisors(getUriList(model.getTechnicalSupervisors()));
        dto.setGroups(getUriList(model.getGroups()));
        dto.setSpecies(getUriList(model.getSpecies()));
        dto.setFactors(getUriList(model.getFactors()));

        List<NamedResourceDTO<OrganizationModel>> organizationsDTO = new ArrayList<>();
        model.getOrganizations().forEach((orga) -> {
            NamedResourceDTO<OrganizationModel> orgaDTO = NamedResourceDTO.getDTOFromModel(orga);
            organizationsDTO.add(orgaDTO);
        });
        dto.setOrganizations(organizationsDTO);
        
        List<NamedResourceDTO<ProjectModel>> projectsDTO = new ArrayList<>();
        model.getProjects().forEach((project) -> {
            NamedResourceDTO<ProjectModel> projectDTO = NamedResourceDTO.getDTOFromModel(project);
            projectsDTO.add(projectDTO);
        });
        dto.setProjects(projectsDTO);

        List<NamedResourceDTO<FacilityModel>> facilitiesDTO = new ArrayList<>();
        model.getFacilities().forEach((facilityModel) -> {
            facilitiesDTO.add(NamedResourceDTO.getDTOFromModel(facilityModel));
        });
        dto.setFacilities(facilitiesDTO);

        return dto;
    }
}
