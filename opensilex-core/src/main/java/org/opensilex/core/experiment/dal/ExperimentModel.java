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
import org.opensilex.rest.group.dal.GroupModel;
import org.opensilex.rest.user.dal.UserModel;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.utils.ClassURIGenerator;

import java.net.URI;
import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;

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
    public static final String LABEL_VAR = "label";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasProject"
    )
    List<ProjectModel> projects = new LinkedList<>();
    public static final String PROJECT_URI_SPARQL_VAR = "project";


    @SPARQLProperty(
            ontology = Oeso.class,
            property = "startDate",
            required = true
    )
    LocalDate startDate;
    public static final String START_DATE_SPARQL_VAR = "startDate";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "endDate",
            required = true
    )
    LocalDate endDate;
    public static final String END_DATE_SPARQL_VAR = "endDate";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasScientificSupervisor"
    )
    List<UserModel> scientificSupervisors = new LinkedList<>();
    public static final String SCIENTIFIC_SUPERVISOR_SPARQL_VAR = "scientificSupervisor";


    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasTechnicalSupervisor"
    )
    List<UserModel> technicalSupervisors = new LinkedList<>();
    public static final String TECHNICAL_SUPERVISOR_SPARQL_VAR = "technicalSupervisor";


    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasGroup"
    )
    List<GroupModel> groups = new LinkedList<>();
    public static final String GROUP_SPARQL_VAR = "group";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasCampaign"
    )
    Integer campaign;
    public static final String CAMPAIGN_SPARQL_VAR = "campaign";


    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasObjective"
    )
    String objective;
    public static final String OBJECTIVE_SPARQL_VAR = "objective";


    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasKeyword"
    )
    List<String> keywords = new LinkedList<>();
    public static final String KEYWORD_SPARQL_VAR = "keyword";

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "comment"
    )
    String comment;
    public static final String COMMENT_SPARQL_VAR = "comment";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasSpecies"
    )
    URI species;
    public static final String SPECIES_SPARQL_VAR = "species";


    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasInfrastructure"
    )
    List<URI> infrastructures = new LinkedList<>();
    public static final String INFRASTRUCTURE_SPARQL_VAR = "infrastructure";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasDevice"
    )
    List<URI> devices = new LinkedList<>();
    public static final String DEVICES_SPARQL_VAR = "devices";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "isPublic"
    )
    protected Boolean isPublic;
    public static final String IS_PUBLIC_SPARQL_VAR = "isPublic";


    @SPARQLProperty(
            ontology = Oeso.class,
            property = "measures"
    )
    List<URI> variables = new LinkedList<>();
    public static final String VARIABLES_SPARQL_VAR = "variables";
    
    @SPARQLProperty(
            ontology = Oeso.class,
            property = "participatesIn",
            inverse = true
    )
    List<URI> sensors = new LinkedList<>();
    public static final String SENSORS_SPARQL_VAR = "sensors";


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

    public URI getSpecies() {
        return species;
    }

    public void setSpecies(URI species) {
        this.species = species;
    }

    public List<URI> getInfrastructures() {
        return infrastructures;
    }

    public void setInfrastructures(List<URI> infrastructures) {
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
