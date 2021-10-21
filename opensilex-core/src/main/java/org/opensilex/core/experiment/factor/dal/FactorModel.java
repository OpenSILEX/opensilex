/*
 * ******************************************************************************
 *                                     FactorModel.java
 *  OpenSILEX
 *  Copyright © INRA 2019
 *  Creation date:  17 December, 2019
 *  Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * ******************************************************************************
 */
package org.opensilex.core.experiment.factor.dal;

import org.apache.jena.vocabulary.RDFS;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.SKOSReferencesModel;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.utils.ClassURIGenerator;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Arnaud Charleroy
 */
@SPARQLResource(
        ontology = Oeso.class,
        resource = "Factor",
        graph = FactorModel.GRAPH,
        prefix = "factor"
)
public class FactorModel extends SKOSReferencesModel implements ClassURIGenerator<FactorModel> {

    public static final String GRAPH = "set/factor";

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "label"
    )
    String name;
    public static final String NAME_FIELD = "name";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasCategory"
    )
    FactorCategorySKOSModel category;
    public static final String CATEGORY_FIELD = "category";
    
    @SPARQLProperty(
            ontology = RDFS.class,
            property = "comment"
    )
    String description;
    
    @SPARQLProperty(
            ontology = Oeso.class,
            property = "studiedEffectIn",
            autoUpdate = false
    )
    ExperimentModel experiment;
    public static final String EXPERIMENT_FIELD = "experiment";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasFactor",
            inverse = true,
            cascadeDelete = true,
            autoUpdate = true
    )
    List<FactorLevelModel> factorLevels = new ArrayList<>();
    public static final String FACTORLEVELS_SPARQL_VAR = "factorLevels";
    
    
    @SPARQLProperty(
            ontology = Oeso.class,
            property = "studyEffectOf",
            inverse = true,
            autoUpdate = true
    )
    List<ExperimentModel> associatedExperiments;
    public static final String EXPERIMENTS_FIELD = "experiments";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public FactorCategorySKOSModel getCategory(){
        return category;
    }

    public void setCategory(FactorCategorySKOSModel category) {
        this.category = category;
    }

    public ExperimentModel getExperiment() {
        return experiment;
    }

    public void setExperiment(ExperimentModel experiment) {
        this.experiment = experiment;
    }
    
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<FactorLevelModel> getFactorLevels() {
        return factorLevels;
    }

    public void setFactorLevels(List<FactorLevelModel> factorLevels) {
        this.factorLevels = factorLevels;
    }

    public List<ExperimentModel> getAssociatedExperiments() {
        return associatedExperiments;
    }

    public void setAssociatedExperiments(List<ExperimentModel> associatedExperiments) {
        this.associatedExperiments = associatedExperiments;
    }
    
    @Override
    public String[] getUriSegments(FactorModel instance) {
        return new String[]{
            getExperiment().getName(),
            instance.getName()
        };
    }
}