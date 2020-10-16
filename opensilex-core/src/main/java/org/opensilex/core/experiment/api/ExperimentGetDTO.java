//******************************************************************************
//                          ExperimentGetDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.experiment.api;

import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.infrastructure.dal.InfrastructureModel;
import org.opensilex.core.project.dal.ProjectModel;
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

    protected URI uri;

    protected String label;

    protected List<NamedResourceDTO<ProjectModel>> projects = new ArrayList<>();

    protected LocalDate startDate;

    protected LocalDate endDate;

    protected String objective;

    protected String comment;

    protected Integer campaign;

    protected List<String> keywords = new ArrayList<>();

    protected List<URI> scientificSupervisors = new ArrayList<>();

    protected List<URI> technicalSupervisors = new ArrayList<>();

    protected List<URI> groups = new ArrayList<>();

    protected List<NamedResourceDTO<InfrastructureModel>> infrastructures = new ArrayList<>();

    protected List<URI> species = new ArrayList<>();

    protected Boolean isPublic;

    protected List<URI> variables = new ArrayList<>();

    protected List<URI> sensors = new ArrayList<>();

    protected List<URI> factors = new ArrayList<>();

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    @Required
    @ApiModelProperty(example = "ZA17")
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
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

    @ApiModelProperty(example = "2020-02-20")
    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    @ApiModelProperty(example = "Genomic prediction of maize yield")
    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    @ApiModelProperty(example = "Genomic prediction of maize yield across European environmental scenarios")
    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @ApiModelProperty(example = "2020")
    public Integer getCampaign() {
        return campaign;
    }

    public void setCampaign(Integer campaign) {
        this.campaign = campaign;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
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

    @ApiModelProperty(example = "http://www.phenome-fppn.fr/id/species/zeamays")
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

    public List<NamedResourceDTO<InfrastructureModel>> getInfrastructures() {
        return infrastructures;
    }

    public void setInfrastructures(List<NamedResourceDTO<InfrastructureModel>> infrastructures) {
        this.infrastructures = infrastructures;
    }

    public List<URI> getVariables() {
        return variables;
    }

    public void setVariables(List<URI> variables) {
        this.variables = variables;
    }

    public List<URI> getSensors() {
        return sensors;
    }

    public void setSensors(List<URI> sensors) {
        this.sensors = sensors;
    }

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
        dto.setLabel(model.getLabel());
        dto.setStartDate(model.getStartDate());
        dto.setCampaign(model.getCampaign());
        dto.setObjective(model.getObjective());
        dto.setComment(model.getComment());
        dto.setKeywords(model.getKeywords());
        dto.setIsPublic(model.getIsPublic());
        dto.setSensors(model.getSensors());
        dto.setVariables(model.getVariables());

        if (model.getEndDate() != null) {
            dto.setEndDate(model.getEndDate());
        }

        dto.setScientificSupervisors(getUriList(model.getScientificSupervisors()));
        dto.setTechnicalSupervisors(getUriList(model.getTechnicalSupervisors()));
        dto.setGroups(getUriList(model.getGroups()));
        dto.setSpecies(getUriList(model.getSpecies()));
        dto.setFactors(getUriList(model.getFactors()));

        List<NamedResourceDTO<InfrastructureModel>> infrastructuresDTO = new ArrayList<>();
        model.getInfrastructures().forEach((infra) -> {
            NamedResourceDTO<InfrastructureModel> infraDTO = NamedResourceDTO.getDTOFromModel(infra);
            infrastructuresDTO.add(infraDTO);
        });
        dto.setInfrastructures(infrastructuresDTO);
        
        List<NamedResourceDTO<ProjectModel>> projectsDTO = new ArrayList<>();
        model.getProjects().forEach((project) -> {
            NamedResourceDTO<ProjectModel> projectDTO = NamedResourceDTO.getDTOFromModel(project);
            projectsDTO.add(projectDTO);
        });
        dto.setProjects(projectsDTO);
        return dto;
    }
}
