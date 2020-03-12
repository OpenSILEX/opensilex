/*
 * ******************************************************************************
 *                                     FactorLevelModel.java
 *  OpenSILEX
 *  Copyright © INRAE 2020
 *  Creation date:  11 March, 2020
 *  Contact: arnaud.charleroy@inra.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * ******************************************************************************
 */
package org.opensilex.core.factor.dal;

import java.net.URI;
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
    String alias;
    public static final String ALIAS_FIELD = "alias";

 
    @SPARQLProperty(
            ontology = RDFS.class,
            property = "comment"
    )
    String comment;

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasFactor"
    )
    URI hasFactor;
    public static final String HAS_FACTOR_FIELD = "hasFactor";

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }


    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public URI getHasFactor() {
        return hasFactor;
    }

    public void setHasFactor(URI hasFactor) {
        this.hasFactor = hasFactor;
    }
    
    @Override
    public String[] getUriSegments(FactorLevelModel instance) {
        return new String[]{
            instance.getAlias()
        };
    }
}
