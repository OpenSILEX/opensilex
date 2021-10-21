/*
 * ******************************************************************************
 *                                     FactorLevelModel.java
 *  OpenSILEX
 *  Copyright © INRAE 2020
 *  Creation date:  11 March, 2020
 *  Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * ******************************************************************************
 */
package org.opensilex.core.experiment.factor.dal;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.utils.ClassURIGenerator;

/**
 *
 * @author Arnaud Charleroy
 */
@SPARQLResource(
        ontology = Oeso.class,
        resource = "FactorLevel",
        graph = FactorModel.GRAPH,
        prefix = "factor"
)
public class FactorLevelModel extends SPARQLNamedResourceModel<FactorLevelModel> implements ClassURIGenerator<FactorLevelModel> {

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "comment"
    )
    String description;

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasFactor",
            autoUpdate = false
    )
    FactorModel factor;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
            getFactor().getExperiment().getName(),
            getFactor().getName(),
            instance.getName()
        };
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FactorLevelModel)) {
            return false;
        }
        return ((FactorLevelModel) obj).getName().equals(getName())
                && ((FactorLevelModel) obj).getDescription().equals(getDescription());
    }

    @Override
    public int hashCode() {
        HashCodeBuilder builder = new HashCodeBuilder();
        builder.append(getName());
        builder.append(getDescription());
        return builder.hashCode();
    }
}
