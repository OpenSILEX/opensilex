//******************************************************************************
//                          ExperimentModel.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: vincent.migot@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.experiment.dal;

import org.apache.jena.vocabulary.RDFS;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.utils.ClassURIGenerator;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import org.opensilex.core.factor.dal.FactorModel;
import org.opensilex.core.infrastructure.dal.InfrastructureModel;
import org.opensilex.core.species.dal.SpeciesModel;
import org.opensilex.security.authentication.SecurityOntology;
import org.opensilex.security.group.dal.GroupModel;
import org.opensilex.security.user.dal.UserModel;

/**
 * @author Vincent MIGOT
 */
@SPARQLResource(
        ontology = Oeso.class,
        resource = "Experiment",
        graph = "set/experiments",
        prefix = "expe"
)
public class ExperimentModel extends SPARQLResourceModel implements ClassURIGenerator<ExperimentModel> {

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "label",
            required = true
    )
    String label;
    public static final String LABEL_FIELD = "label";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasProject"
    )
    List<ProjectModel> projects;
    public static final String PROJECT_URI_FIELD = "project";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "startDate",
            required = true
    )
    LocalDate startDate;
    public static final String START_DATE_FIELD = "startDate";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "endDate"
    )
    LocalDate endDate;
    public static final String END_DATE_FIELD = "endDate";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasScientificSupervisor"
    )
    List<UserModel> scientificSupervisors;
    public static final String SCIENTIFIC_SUPERVISOR_FIELD = "scientificSupervisor";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasTechnicalSupervisor"
    )
    List<UserModel> technicalSupervisors;
    public static final String TECHNICAL_SUPERVISOR_FIELD = "technicalSupervisor";

    @SPARQLProperty(
            ontology = SecurityOntology.class,
            property = "hasGroup"
    )
    List<GroupModel> groups;
    public static final String GROUP_FIELD = "groups";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasCampaign"
    )
    Integer campaign;
    public static final String CAMPAIGN_FIELD = "campaign";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasObjective",
            required = true
    )
    String objective;
    public static final String OBJECTIVE_FIELD = "objective";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasKeyword"
    )
    List<String> keywords;
    public static final String KEYWORD_FIELD = "keyword";

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "comment"
    )
    String comment;
    public static final String COMMENT_FIELD = "comment";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasSpecies"
    )
    List<SpeciesModel> species;
    public static final String SPECIES_FIELD = "species";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasInfrastructure"
    )
    List<InfrastructureModel> infrastructures;
    public static final String INFRASTRUCTURE_FIELD = "infrastructure";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasDevice"
    )
    List<URI> devices;
    public static final String DEVICES_FIELD = "devices";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "isPublic"
    )
    protected Boolean isPublic;
    public static final String IS_PUBLIC_FIELD = "isPublic";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "measures"
    )
    List<URI> variables;
    public static final String VARIABLES_FIELD = "variables";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "participatesIn",
            inverse = true
    )
    List<URI> sensors;
    public static final String SENSORS_FIELD = "sensors";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "studyEffectOf"
    )
    List<FactorModel> factors;
    public static final String FACTORS_FIELD = "factors";
    
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<ProjectModel> getProjects() {
        return projects;
    }

    public void setProjects(List<ProjectModel> projects) {
        this.projects = projects;
    }

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

    public List<UserModel> getScientificSupervisors() {
        return scientificSupervisors;
    }

    public void setScientificSupervisors(List<UserModel> scientificSupervisors) {
        this.scientificSupervisors = scientificSupervisors;
    }

    public List<UserModel> getTechnicalSupervisors() {
        return technicalSupervisors;
    }

    public void setTechnicalSupervisors(List<UserModel> technicalSupervisors) {
        this.technicalSupervisors = technicalSupervisors;
    }

    public List<GroupModel> getGroups() {
        return groups;
    }

    public void setGroups(List<GroupModel> groups) {
        this.groups = groups;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Integer getCampaign() {
        return campaign;
    }

    public void setCampaign(Integer campaign) {
        this.campaign = campaign;
    }

    public List<SpeciesModel> getSpecies() {
        return species;
    }

    public void setSpecies(List<SpeciesModel> species) {
        this.species = species;
    }

    public List<InfrastructureModel> getInfrastructures() {
        return infrastructures;
    }

    public void setInfrastructures(List<InfrastructureModel> infrastructures) {
        this.infrastructures = infrastructures;
    }

    public Boolean getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
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

    public List<FactorModel> getFactors() {
        return factors;
    }

    public void setFactors(List<FactorModel> factors) {
        this.factors = factors;
    }

    public List<URI> getDevices() {
        return devices;
    }

    public void setDevices(List<URI> devices) {
        this.devices = devices;
    }

    @Override
    public String[] getUriSegments(ExperimentModel instance) {
        return new String[]{
            instance.getLabel()
        };
    }
}
