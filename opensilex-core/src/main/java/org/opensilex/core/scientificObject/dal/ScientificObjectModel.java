package org.opensilex.core.scientificObject.dal;

import java.time.OffsetDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import org.opensilex.core.experiment.factor.dal.FactorLevelModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;

@SPARQLResource(
        ontology = Oeso.class,
        resource = "ScientificObject",
        graph = "set/scientific-objects",
        prefix = "so"
)
public class ScientificObjectModel extends ExperimentalObjectModel {

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasCreationDate"
    )
    protected OffsetDateTime creationDate;
    public static String CREATION_DATE_FIELD = "creationDate";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasDestructionDate"
    )
    protected OffsetDateTime destructionDate;
    public static String DESTRUCTION_DATE_FIELD = "destructionDate";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasFactorLevel"
    )
    protected List<FactorLevelModel> factorLevels;
    public static String FACTOR_LEVEL_FIELD = "factorLevels";

    public OffsetDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(OffsetDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public OffsetDateTime getDestructionDate() {
        return destructionDate;
    }

    public void setDestructionDate(OffsetDateTime destructionDate) {
        this.destructionDate = destructionDate;
    }

    public List<FactorLevelModel> getFactorLevels() {
        return factorLevels;
    }

    public void setFactorLevels(List<FactorLevelModel> factorLevels) {
        this.factorLevels = factorLevels;
    }

}
