//******************************************************************************
//                          ExperimentModel.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: vincent.migot@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.experiment.dal;

import org.apache.jena.vocabulary.RDFS;
import org.opensilex.core.experiment.factor.dal.FactorModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.organisation.dal.OrganizationModel;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.core.species.dal.SpeciesModel;
import org.opensilex.security.authentication.SecurityOntology;
import org.opensilex.security.group.dal.GroupModel;
import org.opensilex.security.person.dal.PersonModel;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.uri.generation.ClassURIGenerator;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

/**
 * @author Vincent MIGOT
 */
@SPARQLResource(
        ontology = Oeso.class,
        resource = "Experiment",
        graph = ExperimentModel.GRAPH,
        prefix = "expe"
)
public class ExperimentModel extends SPARQLNamedResourceModel<ExperimentModel> implements ClassURIGenerator<ExperimentModel> {

    public static final String GRAPH = "experiment";

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
    List<PersonModel> scientificSupervisors;
    public static final String SCIENTIFIC_SUPERVISOR_FIELD = "scientificSupervisor";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasTechnicalSupervisor"
    )
    List<PersonModel> technicalSupervisors;
    public static final String TECHNICAL_SUPERVISOR_FIELD = "technicalSupervisor";

    @SPARQLProperty(
            ontology = SecurityOntology.class,
            property = "hasGroup"
    )
    List<GroupModel> groups;
    public static final String GROUP_FIELD = "groups";


    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasObjective",
            required = true
    )
    String objective;
    public static final String OBJECTIVE_FIELD = "objective";


    @SPARQLProperty(
            ontology = RDFS.class,
            property = "comment"
    )
    String description;
    public static final String COMMENT_FIELD = "description";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasSpecies",
            ignoreUpdateIfNull = true
    )
    List<SpeciesModel> species;
    public static final String SPECIES_FIELD = "species";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "usesOrganization"
    )
    List<OrganizationModel> infrastructures;
    public static final String INFRASTRUCTURE_FIELD = "infrastructure";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "usesFacility"
    )
    List<FacilityModel> facilities;
    public static final String FACILITY_FIELD = "facilities";


    @SPARQLProperty(
            ontology = Oeso.class,
            property = "isPublic"
    )
    protected Boolean isPublic;
    public static final String IS_PUBLIC_FIELD = "isPublic";

//    @SPARQLProperty(
//            ontology = Oeso.class,
//            property = "measures"
//    )
    List<URI> variables;
    public static final String VARIABLES_FIELD = "variables";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "studyEffectOf",
            cascadeDelete = true
    )
    List<FactorModel> factors;
    public static final String FACTORS_FIELD = "factors";
    public static final String FACTORS_CATEGORIES_FIELD = "factorsCategories";

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

    public List<PersonModel> getScientificSupervisors() {
        return scientificSupervisors;
    }

    public void setScientificSupervisors(List<PersonModel> scientificSupervisors) {
        this.scientificSupervisors = scientificSupervisors;
    }

    public List<PersonModel> getTechnicalSupervisors() {
        return technicalSupervisors;
    }

    public void setTechnicalSupervisors(List<PersonModel> technicalSupervisors) {
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<SpeciesModel> getSpecies() {
        return species;
    }

    public void setSpecies(List<SpeciesModel> species) {
        this.species = species;
    }

    public List<OrganizationModel> getInfrastructures() {
        return infrastructures;
    }

    public void setInfrastructures(List<OrganizationModel> infrastructures) {
        this.infrastructures = infrastructures;
    }

    public List<FacilityModel> getFacilities() {
        return facilities;
    }

    public void setFacilities(List<FacilityModel> facilities) {
        this.facilities = facilities;
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

    public List<FactorModel> getFactors() {
        return factors;
    }

    public void setFactors(List<FactorModel> factors) {
        this.factors = factors;
    }
}