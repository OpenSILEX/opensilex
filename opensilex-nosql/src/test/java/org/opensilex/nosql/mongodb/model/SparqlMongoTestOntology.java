package org.opensilex.nosql.mongodb.model;

import org.apache.jena.rdf.model.Resource;
import org.opensilex.sparql.utils.Ontology;

public class SparqlMongoTestOntology {

    public static final String NAMESPACE = "http://test.opensilex.org/test/sparql-mongo/";

    public static final Resource TEST_MONGO_SPARQL = Ontology.resource(NAMESPACE, "TEST_MONGO_SPARQL");

}
