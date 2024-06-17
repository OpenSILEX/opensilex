//******************************************************************************
//                          DataExportInformation.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.bll;

import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.provenance.dal.ProvenanceModel;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Class that contains all information needed to perform a data export pretty print (Variable models, Object models, Provenance models and Experiment models).
 * Contained in URI -> Model maps as this is how both export functions use this information.
 */
public abstract class DataExportInformation {
    private Map<URI, VariableModel> variables;
    private Map<URI, SPARQLNamedResourceModel> objects;
    private Map<URI, ProvenanceModel> provenances;
    private Map<URI, ExperimentModel> experiments;

    public Map<URI, SPARQLNamedResourceModel> getObjects() {
        return objects;
    }

    public DataExportInformation setObjects(Map<URI, SPARQLNamedResourceModel> objects) {
        this.objects = objects;
        return this;
    }

    public Map<URI, VariableModel> getVariables() {
        return variables;
    }

    public DataExportInformation setVariables(Map<URI, VariableModel> variables) {
        this.variables = variables;
        return this;
    }

    public Map<URI, ProvenanceModel> getProvenances() {
        return provenances;
    }

    public DataExportInformation setProvenances(Map<URI, ProvenanceModel> provenances) {
        this.provenances = provenances;
        return this;
    }

    public Map<URI, ExperimentModel> getExperiments() {
        return experiments;
    }

    public DataExportInformation setExperiments(Map<URI, ExperimentModel> experiments) {
        this.experiments = experiments;
        return this;
    }
}
