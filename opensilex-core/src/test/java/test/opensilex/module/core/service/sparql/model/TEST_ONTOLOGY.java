/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test.opensilex.module.core.service.sparql.model;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.opensilex.utils.ontology.Ontology;

/**
 *
 * @author vincent
 */
public class TEST_ONTOLOGY {
    
    public static final Path FILE_PATH = Paths.get("ontologies", "test.owl");
    public static final Lang FILE_FORMAT = RDFLanguages.RDFXML;
    public static final String NAMESPACE = "http://test.opensilex.org/";
    public static final Node GRAPH = NodeFactory.createURI(NAMESPACE);
    
    public static final Path DATA_FILE_PATH = Paths.get("ontologies", "test_data.ttl");
    public static final Lang DATA_FILE_FORMAT = RDFLanguages.TURTLE;
    public static final String DATA_NAMESPACE = NAMESPACE + "data/";
    public static final Node DATA_GRAPH = NodeFactory.createURI(DATA_NAMESPACE);
    
    public static final Resource A = Ontology.resource(NAMESPACE, "A");
    public static final Resource A1 = Ontology.resource(NAMESPACE, "A1");
    public static final Resource A2 = Ontology.resource(NAMESPACE, "A2");
    public static final Resource A11 = Ontology.resource(NAMESPACE, "A11");
    public static final Resource A21 = Ontology.resource(NAMESPACE, "A21");

    public static final Resource B = Ontology.resource(NAMESPACE, "B");

}
