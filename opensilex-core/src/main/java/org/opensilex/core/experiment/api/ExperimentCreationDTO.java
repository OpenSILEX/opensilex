//******************************************************************************
//                          ExperimentCreationDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.experiment.api;

import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.project.dal.ProjectModel;

import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;
import org.opensilex.core.infrastructure.dal.InfrastructureModel;
import org.opensilex.core.species.dal.SpeciesModel;
import org.opensilex.security.group.dal.GroupModel;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.rest.validation.Required;

/**
 * @author Vincent MIGOT
 */
public class ExperimentCreationDTO {

    protected URI uri;

    protected String label;

    protected List<URI> projects = new ArrayList<>();

    protected LocalDate startDate;

    protected LocalDate endDate;

    protected String objective;

    protected String comment;

    protected Integer campaign;

    protected List<String> keywords = new ArrayList<>();

    protected List<URI> scientificSupervisors = new ArrayList<>();

    protected List<URI> technicalSupervisors = new ArrayList<>();

    protected List<URI> groups = new ArrayList<>();

    protected List<URI> infrastructures = new ArrayList<>();

    protected List<URI> installations = new ArrayList<>();

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

    @ApiModelProperty(example = "http://www.phenome-fppn.fr/id/species/zeamays")
    public List<URI> getProjects() {
        return projects;
    }

    public void setProjects(List<URI> projects) {
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

    @ApiModelProperty(example = "objective")
    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    @ApiModelProperty(example = "comment")
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

    public List<URI> getInfrastructures() {
        return infrastructures;
    }

    public void setInfrastructures(List<URI> infrastructures) {
        this.infrastructures = infrastructures;
    }

    public ExperimentModel newModel() {

        ExperimentModel model = new ExperimentModel();
        model.setUri(getUri());
        model.setLabel(getLabel());
        model.setStartDate(startDate);
        model.setEndDate(endDate);

        model.setObjective(getObjective());
        model.setComment(getComment());
        model.setKeywords(keywords);
        model.setCampaign(campaign);
        model.setDevices(installations);
        model.setIsPublic(isPublic);
        model.setSensors(sensors);
        model.setVariables(variables);
        model.setFactors(factors);

        List<SpeciesModel> speciesList = new ArrayList<>(species.size());
        species.forEach((URI u) -> {
            SpeciesModel species = new SpeciesModel();
            species.setUri(u);
            speciesList.add(species);
        });
        model.setSpecies(speciesList);

        List<InfrastructureModel> infrastructuresList = new ArrayList<>(infrastructures.size());
        infrastructures.forEach((URI u) -> {
            InfrastructureModel infrastructure = new InfrastructureModel();
            infrastructure.setUri(u);
            infrastructuresList.add(infrastructure);
        });
        model.setInfrastructures(infrastructuresList);

        List<ProjectModel> projectList = new ArrayList<>(projects.size());
        projects.forEach((URI u) -> {
            ProjectModel project = new ProjectModel();
            project.setUri(u);
            projectList.add(project);
        });
        model.setProjects(projectList);

        List<UserModel> scientificList = new ArrayList<>(scientificSupervisors.size());
        scientificSupervisors.forEach((URI u) -> {
            UserModel user = new UserModel();
            user.setUri(u);
            scientificList.add(user);
        });
        model.setScientificSupervisors(scientificList);

        List<UserModel> technicalList = new ArrayList<>(technicalSupervisors.size());
        technicalSupervisors.forEach((URI u) -> {
            UserModel user = new UserModel();
            user.setUri(u);
            technicalList.add(user);
        });
        model.setTechnicalSupervisors(technicalList);

        List<GroupModel> groupList = new ArrayList<>(groups.size());
        groups.forEach((URI u) -> {
            GroupModel group = new GroupModel();
            group.setUri(u);
            groupList.add(group);
        });
        model.setGroups(groupList);

        return model;
    }

}
