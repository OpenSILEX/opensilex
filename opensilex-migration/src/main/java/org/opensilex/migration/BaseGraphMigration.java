package org.opensilex.migration;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.opensilex.sparql.SPARQLConfig;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLService;
import org.reflections.Reflections;

import java.net.URI;
import java.util.*;

public class BaseGraphMigration extends DatabaseMigrationModuleUpdate {

    @Override
    public String getDescription() {
        return "Rename graph uris";
    }

    @Override
    protected boolean applyOnSparql(SPARQLService sparql, SPARQLConfig sparqlConfig) {
        return true;
    }

    /**
     * This function renames all the graphs in OpenSilex that extend the SPARQLResourceModel class.
     *
     */
    @Override
    protected void sparqlOperation(SPARQLService sparql, SPARQLConfig sparqlConfig) throws SPARQLException {

        // get all the classes in the whole project that extends SPARQLResourceModel
        Reflections reflections = new Reflections("org.opensilex");
        Set<Class<? extends SPARQLResourceModel>> classes = reflections.getSubTypesOf(SPARQLResourceModel.class);

        // create a Set that will contain all the graph
        Set<String> allGraphToChange = new HashSet<>();

        // if the classes are not empty we take the graph values of all the classes and put them in allGraphToChange
        if (!classes.isEmpty()) {
            for (Class classe : classes) {
                SPARQLResource annotation = (SPARQLResource) classe.getAnnotation(SPARQLResource.class);
                // we check that the graph is not null and not an empty string
                if (Objects.nonNull(annotation) && Objects.nonNull(annotation.graph()) && !annotation.graph().isEmpty()) {
                    allGraphToChange.add(annotation.graph());
                }
            }
        }
        
        // if allGraphToChange is not empty we rename all the graph present in allGraphToChange
        if (!allGraphToChange.isEmpty()) {
            for (String graph : allGraphToChange) {
                Node graphNode = NodeFactory.createURI(sparql.getBaseURI() + "set/" + graph);
                // we check that the graph exist to avoid an unwanted behavior
                if (sparql.graphExists(graphNode)) {
                    URI oldGraph = URI.create(sparql.getBaseURI() + "set/" + graph);
                    URI newGraph = URI.create(sparql.getBaseGraphURI() + "set/" + graph);
                    sparql.renameGraph(oldGraph, newGraph);
                }
            }
        }
    }
}
