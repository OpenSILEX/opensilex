//******************************************************************************
//                          ProcessModel.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: emilie.fernandez@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.process.process.dal;

import org.opensilex.process.ontology.PO2;

import java.net.URI;
import java.util.List;
import java.time.LocalDate;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.opensilex.sparql.annotations.SPARQLIgnore;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLLabel;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.uri.generation.ClassURIGenerator;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.organisation.dal.InfrastructureModel;
import org.opensilex.core.organisation.dal.InfrastructureFacilityModel;
import java.util.stream.Collectors;


/**
 * @author Emilie Fernandez
 */
@SPARQLResource(
        ontology = PO2.class,
        resource = "Transformation_Process",
        graph = ProcessModel.GRAPH,
        prefix = "process"
)
public class ProcessModel extends SPARQLNamedResourceModel<ProcessModel> implements ClassURIGenerator<ProcessModel> {

    public static final String GRAPH = "process";

    @SPARQLIgnore
    protected ExperimentModel experiment;

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasCreationDate"
    )
    protected LocalDate creationDate;
    public static String CREATION_DATE_FIELD = "creationDate";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasDestructionDate"
    )
    protected LocalDate destructionDate;
    public static String DESTRUCTION_DATE_FIELD = "destructionDate";

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
            ontology = RDFS.class,
            property = "comment"
    )
    String description;
    public static final String COMMENT_FIELD = "description";

    @SPARQLProperty(
        ontology = PO2.class,
        property = "hasForStep"
    )
    List<StepModel> step;
    public static final String STEP_FIELD = "step";

    @SPARQLProperty(
        ontology = Oeso.class,
        property = "usesOrganization"
    )
    List<InfrastructureModel> infrastructures;
    public static final String INFRASTRUCTURE_FIELD = "infrastructure";

    public ExperimentModel getExperiment() {
        return experiment;
    }

    public void setExperiment(ExperimentModel experiment) {
        this.experiment = experiment;
    }        

    public LocalDate getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDate creationDate) {
        this.creationDate = creationDate;
    }

    public LocalDate getDestructionDate() {
        return destructionDate;
    }

    public void setDestructionDate(LocalDate destructionDate) {
        this.destructionDate = destructionDate;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<StepModel> getStep() {
        return step;
    }

    public void setStep(List<StepModel> step) {
        this.step = step;
    }

    public List<URI> getStepUris() {
        return this.step
                .stream()
                .map(StepModel::getUri)
                .collect(Collectors.toList());
    }

    public List<InfrastructureModel> getInfrastructures() {
        return infrastructures;
    }

    public void setInfrastructures(List<InfrastructureModel> infrastructures) {
        this.infrastructures = infrastructures;
    }
}