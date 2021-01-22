/*
 * ******************************************************************************
 *                                     FactorModel.java
 *  OpenSILEX
 *  Copyright © INRA 2019
 *  Creation date:  17 December, 2019
 *  Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * ******************************************************************************
 */
package org.opensilex.core.factor.dal;

import java.util.ArrayList;
import java.util.List;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.SKOSReferencesModel;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.utils.ClassURIGenerator;

/**
 *
 * @author Arnaud Charleroy
 */
@SPARQLResource(
        ontology = Oeso.class,
        resource = "Factor",
        graph = "set/factors",
        prefix = "factor"
)
public class FactorModel extends SKOSReferencesModel implements ClassURIGenerator<FactorModel> {

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
    String category;
    public static final String CATEGORY_FIELD = "category";
    
    @SPARQLProperty(
            ontology = RDFS.class,
            property = "comment"
    )
    String description;

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
            inverse = true
    )
    List<ExperimentModel> experiments;
    public static final String EXPERMIENTS_FIELD = "experiments";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory(){
        return category;
    }

    public void setCategory(String localName) {
        this.category = localName;
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

    public List<ExperimentModel> getExperiments() {
        return experiments;
    }

    public void setExperiments(List<ExperimentModel> experiments) {
        this.experiments = experiments;
    }
    
    @Override
    public String[] getUriSegments(FactorModel instance) {
        return new String[]{
            instance.getName()
        };
    }
}