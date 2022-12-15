package org.opensilex.sparql.model;

import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.uri.generation.ClassURIGenerator;

@SPARQLResource(
        ontology = TEST_ONTOLOGY.class,
        resource = "A"
)
public class UriGeneratedTestModel extends A implements ClassURIGenerator<A> {

    @Override
    public String[] getInstancePathSegments(A instance) {
        return new String[]{instance.getString()};
    }
}
