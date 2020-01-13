/*
 * ******************************************************************************
 *                                     FactorModel.java
 *  OpenSILEX
 *  Copyright © INRA 2019
 *  Creation date:  17 December, 2019
 *  Contact: arnaud.charleroy@inra.fr
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
 * @author charlero
 */
@SPARQLResource(
        ontology = Oeso.class,
        resource = "Factor",
        graph = "set/factors",
        prefix = "factor"
)
public class FactorModel extends SPARQLResourceModel implements ClassURIGenerator<FactorModel> {

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

    @Override
    public String[] getUriSegments(FactorModel instance) {
        return new String[]{
            instance.getAlias()
        };
    }
}
