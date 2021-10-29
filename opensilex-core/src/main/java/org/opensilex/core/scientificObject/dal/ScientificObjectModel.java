package org.opensilex.core.scientificObject.dal;

import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.util.List;

import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.experiment.factor.dal.FactorLevelModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.annotations.SPARQLIgnore;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLTreeModel;
import org.opensilex.sparql.utils.URIGenerator;

@SPARQLResource(
        ontology = Oeso.class,
        resource = "ScientificObject",
        graph = ScientificObjectModel.GRAPH,
        prefix = ScientificObjectModel.PREFIX
)
public class ScientificObjectModel extends SPARQLTreeModel<ScientificObjectModel> {

    public static final String GRAPH = "scientific-object";
    public static final String PREFIX = "so";
    public static final String GENERATION_PREFIX = PREFIX+"-";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "isPartOf",
            useDefaultGraph = false
    )
    protected ScientificObjectModel parent;

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "isPartOf",
            inverse = true,
            ignoreUpdateIfNull = true,
            useDefaultGraph = false
    )
    protected List<ScientificObjectModel> children;

    @SPARQLIgnore
    protected ExperimentModel experiment;
    
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

    public ExperimentModel getExperiment() {
        return experiment;
    }

    public void setExperiment(ExperimentModel experiment) {
        this.experiment = experiment;
    }

    @Override
    public String getInstanceURI(SPARQLTreeModel<ScientificObjectModel> instance) throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();
        if(experiment != null){
            sb.append(experiment.getName()).append("/");
        }

        sb.append(GENERATION_PREFIX);
        if(instance.getName() != null){
            sb.append(URIGenerator.normalize(instance.getName()));
        }
        return sb.toString();
    }
}
