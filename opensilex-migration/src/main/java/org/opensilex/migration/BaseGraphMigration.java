package org.opensilex.migration;

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

        // create a Set that will contain all the graph of the different classes
        Set<String> allGraphToChange = new HashSet<>();

        // if the allGraphToChange is not empty we take the graph values of all the classes and put them in allGraphToChange
        if (!classes.isEmpty()) {
            for (Class c : classes) {
                SPARQLResource annotation = (SPARQLResource) c.getAnnotation(SPARQLResource.class);
                if (Objects.nonNull(annotation) && Objects.nonNull(annotation.graph()) && !Objects.equals(annotation.graph(), "")) {
                    allGraphToChange.add(annotation.graph());
                }
            }
        }

        // if allGraphToChange is not empty we rename all the graph present in allGraphToChange
        if (!allGraphToChange.isEmpty()) {
            for (String graph : allGraphToChange) {
                URI oldGraph = URI.create(sparql.getBaseURI() + "set/" + graph);
                URI newGraph = URI.create(sparql.getBaseGraphURI() + "set/" + graph);
                sparql.renameGraph(oldGraph, newGraph);
            }
        }

    }
}
