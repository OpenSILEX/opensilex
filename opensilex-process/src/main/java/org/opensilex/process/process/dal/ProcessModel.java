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
import org.opensilex.core.organisation.dal.InfrastructureFacilityModel;
import java.util.stream.Collectors;
import org.opensilex.sparql.model.time.Time;
import org.opensilex.sparql.model.time.InstantModel;

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
    protected List<ExperimentModel> experiment;

    @SPARQLProperty(
            ontology = Time.class,
            property = "hasBeginning",
            useDefaultGraph = false  // InstantModel stored in GRAPH
    )
    private InstantModel start;
    public static final String START_FIELD = "start";

    @SPARQLProperty(
            ontology = Time.class,
            property = "hasEnd",
            useDefaultGraph = false // InstantModel stored in GRAPH
    )
    private InstantModel end;
    public static final String END_FIELD = "end";

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
            property = "usesFacility"
    )
    List<InfrastructureFacilityModel> facilities;
    public static final String FACILITY_FIELD = "facilities";    

    public List<ExperimentModel> getExperiment() {
        return experiment;
    }

    public void setExperiment(List<ExperimentModel> experiment) {
        this.experiment = experiment;
    }    

    public InstantModel getStart() {
        return start;
    }

    public void setStart(InstantModel start) {
        this.start = start;
    }

    public InstantModel getEnd() {
        return end;
    }

    public void setEnd(InstantModel end) {
        this.end = end;
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

    public List<InfrastructureFacilityModel> getFacilities() {
        return facilities;
    }

    public void setFacilities(List<InfrastructureFacilityModel> facilities) {
        this.facilities = facilities;
    }
}
