package org.opensilex.core.scientificObject.dal;

import java.time.LocalDate;
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
    protected LocalDate creationDate;
    public static String CREATION_DATE_FIELD = "creationDate";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasDestructionDate"
    )
    protected LocalDate destructionDate;
    public static String DESTRUCTION_DATE_FIELD = "destructionDate";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasFactorLevel"
    )
    protected List<FactorLevelModel> factorLevels;
    public static String FACTOR_LEVEL_FIELD = "factorLevels";

    public LocalDate getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDate creationDate) {
        this.creationDate = creationDate;
    }

    public LocalDate getDestructionDate() {
        return destructionDate;
    }

    public void setDestructionDate(LocalDate destructionDate) {
        this.destructionDate = destructionDate;
    }

    public List<FactorLevelModel> getFactorLevels() {
        return factorLevels;
    }

    public void setFactorLevels(List<FactorLevelModel> factorLevels) {
        this.factorLevels = factorLevels;
    }

}
