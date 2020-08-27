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
    String comment;

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasFactor",
            inverse = true,
            cascadeDelete = true,
            autoUpdate = true
    )
    List<FactorLevelModel> factorLevels = new ArrayList<>();
    public static final String FACTORLEVELS_SPARQL_VAR = "factorLevels";

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

    

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<FactorLevelModel> getFactorLevels() {
        return factorLevels;
    }

    public void setFactorLevels(List<FactorLevelModel> factorLevels) {
        this.factorLevels = factorLevels;
    }

    @Override
    public String[] getUriSegments(FactorModel instance) {
        return new String[]{
            instance.getName()
        };
    }
}