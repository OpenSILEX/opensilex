//******************************************************************************
//                          StepModel.java
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
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.core.organisation.dal.InfrastructureFacilityModel;
import org.opensilex.core.ontology.Oeso;


/**
 * @author Emilie Fernandez
 */
@SPARQLResource(
        ontology = PO2.class,
        resource = "Step",
        graph = ProcessModel.GRAPH,
        prefix = "step"
)
public class StepModel extends SPARQLNamedResourceModel<StepModel> implements ClassURIGenerator<StepModel> {

    public static final String GRAPH = "step";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "startDate"
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
            ontology = RDFS.class,
            property = "comment"
    )
    String description;
    public static final String COMMENT_FIELD = "description";

    @SPARQLProperty(
            ontology = PO2.class,
            property = "hasInput"
    )
    List<ScientificObjectModel> input;
    public static final String INPUT_FIELD = "input";

    @SPARQLProperty(
        ontology = PO2.class,
        property = "hasOutput"
    )
    List<ScientificObjectModel> output;
    public static final String OUTPUT_FIELD = "output";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "usesFacility"
    )
    List<InfrastructureFacilityModel> facilities;
    public static final String FACILITY_FIELD = "facilities";       

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<ScientificObjectModel> getInput() {
        return input;
    }

    public void setInput(List<ScientificObjectModel> input) {
        this.input = input;
    }

    public List<ScientificObjectModel> getOutput() {
        return output;
    }

    public void setOutput(List<ScientificObjectModel> output) {
        this.output = output;
    }

    public List<InfrastructureFacilityModel> getFacilities() {
        return facilities;
    }

    public void setFacilities(List<InfrastructureFacilityModel> facilities) {
        this.facilities = facilities;
    }
}