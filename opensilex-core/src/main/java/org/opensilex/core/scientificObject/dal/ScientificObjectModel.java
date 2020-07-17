package org.opensilex.core.scientificObject.dal;

import java.util.List;
import org.opensilex.core.factor.dal.FactorLevelModel;
import org.opensilex.core.germplasm.dal.GermplasmModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLTreeModel;

@SPARQLResource(
        ontology = Oeso.class,
        resource = "ScientificObject",
        graph = "scientific-objects",
        prefix = "so"
)
public class ScientificObjectModel extends SPARQLTreeModel<ScientificObjectModel> {

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "isPartOf"
    )
    protected ScientificObjectModel parent;

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "isPartOf",
            inverse = true,
            ignoreUpdateIfNull = true
    )
    protected List<ScientificObjectModel> children;

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasFactorLevel"
    )
    protected List<FactorLevelModel> factorLevels;

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasGermplasm"
    )
    protected List<GermplasmModel> germplasms;

    public List<FactorLevelModel> getFactorLevels() {
        return factorLevels;
    }

    public void setFactorLevels(List<FactorLevelModel> factorLevels) {
        this.factorLevels = factorLevels;
    }

    public List<GermplasmModel> getGermplasms() {
        return germplasms;
    }

    public void setGermplasms(List<GermplasmModel> germplasms) {
        this.germplasms = germplasms;
    }

}
