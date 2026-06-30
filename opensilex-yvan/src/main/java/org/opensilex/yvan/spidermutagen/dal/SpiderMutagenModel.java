/*
 * *****************************************************************************
 *                         SpiderMutagenModel.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2026.
 * Last Modification: 29/06/2026 16:52
 * Contact: yvan.roux@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
 * *****************************************************************************
 */

package org.opensilex.yvan.spidermutagen.dal;

import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.yvan.ontology.YvanOntology;

@SPARQLResource(
        ontology = YvanOntology.class,
        resource = "SpiderMutagen",
        graph = SpiderMutagenModel.GRAPH
)
public class SpiderMutagenModel extends SPARQLResourceModel {

    protected static final String GRAPH = "extension/yvan";

    @SPARQLProperty(
            ontology = YvanOntology.class,
            property = "hasExperiment",
            required = true
    )
    private ExperimentModel experiment;

    public ExperimentModel getExperiment() {
        return experiment;
    }

    public void setExperiment(ExperimentModel experiment) {
        this.experiment = experiment;
    }
}
