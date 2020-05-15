/*
 * ******************************************************************************
 *                                     FactorLevelModel.java
 *  OpenSILEX
 *  Copyright © INRAE 2020
 *  Creation date:  11 March, 2020
 *  Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * ******************************************************************************
 */
package org.opensilex.core.factor.dal;

import org.apache.jena.vocabulary.RDFS;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.utils.ClassURIGenerator;

/**
 *
 * @author Arnaud Charleroy
 */
@SPARQLResource(
        ontology = Oeso.class,
        resource = "FactorLevel",
        graph = "set/factorsLevels",
        prefix = "factorLevel"
)
public class FactorLevelModel extends SPARQLResourceModel implements ClassURIGenerator<FactorLevelModel> {

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "label",
            required = true
    )
    String name;
    public static final String NAME_FIELD = "name";

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "comment"
    )
    String comment;

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasFactorLevel",
            inverse = true
    )
    FactorModel factor;

    public String getName() {
        return name;
    }

    public void setName(String alias) {
        this.name = alias;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public FactorModel getFactor() {
        return factor;
    }

    public void setFactor(FactorModel factor) {
        this.factor = factor;
    }

    @Override
    public String[] getUriSegments(FactorLevelModel instance) {
        return new String[]{
            getFactor().getName().toString(),
            instance.getName()
        };
    }
}
