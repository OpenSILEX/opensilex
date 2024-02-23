package org.opensilex.nosql.mongodb.model;


import org.apache.jena.rdf.model.Resource;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.utils.Ontology;

/**
 * Class used for testing SPARQL/Mongo transaction
 *
 */
@SPARQLResource(
        ontology = SparqlMongoTestOntology.class,
        resource = "TEST_MONGO_SPARQL"
)
public class SparqlMongoTestModel extends SPARQLResourceModel {

}
