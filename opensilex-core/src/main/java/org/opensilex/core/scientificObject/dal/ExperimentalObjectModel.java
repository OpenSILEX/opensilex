package org.opensilex.core.scientificObject.dal;

import java.util.List;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLTreeModel;

@SPARQLResource(
        ontology = Oeso.class,
        resource = "ExperimentalObject"
)
public class ExperimentalObjectModel extends SPARQLTreeModel<ExperimentalObjectModel> {

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "isPartOf"
    )
    protected ExperimentalObjectModel parent;

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "isPartOf",
            inverse = true,
            ignoreUpdateIfNull = true,
            useDefaultGraph = false
    )
    protected List<ExperimentalObjectModel> children;

}
