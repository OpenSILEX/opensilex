package org.opensilex.sparql.mapping;

import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLResourceModel;
import test.opensilex.sparql.model.TEST_ONTOLOGY;

@SPARQLResource(
    ontology = TEST_ONTOLOGY.class,
    resource = "Fail"
)
public class NoGetterClass extends SPARQLResourceModel {

    @SPARQLProperty(
        ontology = TEST_ONTOLOGY.class,
        property = "hasString"
    )
    private String string;

    public void setString(String string) {
        this.string = string;
    }
}
