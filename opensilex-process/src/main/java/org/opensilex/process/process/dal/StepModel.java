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
import org.opensilex.sparql.model.time.Time;
import org.opensilex.sparql.model.time.InstantModel;


/**
 * @author Emilie Fernandez
 */
@SPARQLResource(
        ontology = PO2.class,
        resource = "step",
        graph = ProcessModel.GRAPH,
        prefix = "step"
)
public class StepModel extends SPARQLNamedResourceModel<StepModel> implements ClassURIGenerator<StepModel> {

    public static final String GRAPH = "process";

    @SPARQLProperty(
            ontology = Time.class,
            property = "hasBeginning",
            useDefaultGraph = false
    )
    private InstantModel start;
    public static final String START_FIELD = "start";

    @SPARQLProperty(
            ontology = Time.class,
            property = "hasEnd",
            useDefaultGraph = false
    )
    private InstantModel end;
    public static final String END_FIELD = "end";

    @SPARQLProperty(
        ontology = Time.class,
        property = "after"
    )
    URI after;
    public static final String AFTER_FIELD = "after";

    @SPARQLProperty(
            ontology = Time.class,
            property = "before"
    )
    URI before;
    public static final String BEFORE_FIELD = "before";

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

    public URI getAfter() {
        return after;
    }

    public void setAfter(URI after) {
        this.after = after;
    }

    public URI getBefore() {
        return before;
    }

    public void setBefore(URI before) {
        this.before = before;
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
}