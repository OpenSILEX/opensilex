package org.opensilex.sparql.mapping;

import org.opensilex.sparql.annotations.SPARQLManualLoading;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.model.TEST_ONTOLOGY;

@SPARQLResource(
        ontology = TEST_ONTOLOGY.class,
        resource = "Fail"
)
@SPARQLManualLoading
public class NoSetterClass extends SPARQLResourceModel {

    @SPARQLProperty(
            ontology = TEST_ONTOLOGY.class,
            property = "hasString"
    )
    private String string;

    public String getString() {
        return string;
    }
}
