package org.opensilex.sparql.service.query;

import org.apache.jena.graph.Node;
import org.jetbrains.annotations.NotNull;
import org.opensilex.sparql.exceptions.SPARQLInvalidClassDefinitionException;
import org.opensilex.sparql.exceptions.SPARQLMapperNotFoundException;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * This query search URIs/resource of any {@link SPARQLResourceModel} class
 * @param <T>
 */
public class SparqlMultiClassQuery<T extends SPARQLResourceModel> extends SparqlMultiGraphQuery<T> {

    /**
     * Constructor for SparqlMultiClassQuery which take a Collection of Class to create a new instance from
     * This constructor is used when you have the classes and want to use the automatic generation of types and graphs according to the {@link SPARQLResourceModel} definition
     *
     * @param sparql          The {@link org.opensilex.sparql.service.SPARQLService} to use
     * @param classes         Collection of Class that will be used to create typeAndGraph Map,
     *                        so it's used an OR semantic for search.
     * @param urisSupplier   A {@link Supplier} of {@link Stream} from which URIs are read
     *
     * @see SparqlMultiGraphQuery#SparqlMultiGraphQuery(SPARQLService, Supplier, int) 
     */
    public SparqlMultiClassQuery(SPARQLService sparql, Collection<Class<? extends SPARQLResourceModel>> classes, Supplier<Stream<String>> urisSupplier, int urisSize) throws SPARQLMapperNotFoundException, SPARQLInvalidClassDefinitionException {
        super(sparql, getTypesAndGraphs(sparql, classes), urisSupplier, urisSize);
    }

    /**
     * Constructor for SparqlMultiClassQuery which take a Class and URIs to create a new instance from
     * This constructor is used when you have the class and want to use it manually.
     *
     * @param sparql          The {@link org.opensilex.sparql.service.SPARQLService} to use
     * @param classes         Collection of Class that will be used to create typeAndGraph Map,
     *                        so it's used an OR semantic for search.
     * @param uris           Collection of URIs
     */
    public SparqlMultiClassQuery(SPARQLService sparql, Collection<Class<? extends SPARQLResourceModel>> classes, Collection<URI> uris) throws SPARQLMapperNotFoundException, SPARQLInvalidClassDefinitionException {
        super(sparql, getTypesAndGraphs(sparql, classes), () -> uris.stream().map(URI::toString), uris.size());
    }

    /**
     * @param classes         Collection of Class that will be used to create typeAndGraph Map,
     *                        so it's used an OR semantic for search.
     * @param uris           Collection of URIs into a String format
     * @param sparql          The {@link org.opensilex.sparql.service.SPARQLService} to use
     */
    public SparqlMultiClassQuery(Collection<Class<? extends SPARQLResourceModel>> classes, Collection<String> uris, SPARQLService sparql) throws SPARQLInvalidClassDefinitionException, SPARQLMapperNotFoundException {
        super(sparql, getTypesAndGraphs(sparql, classes), uris::stream, uris.size());
    }

    @NotNull
    private static Map<Node, Node> getTypesAndGraphs(SPARQLService sparql, Collection<Class<? extends SPARQLResourceModel>> classes) throws SPARQLInvalidClassDefinitionException, SPARQLMapperNotFoundException {
        Map<Node, Node> typesAndNodes = new HashMap<>(classes.size());
        for(var clazz : classes){
            var mapper = sparql.getMapperIndex().getForClass(clazz);
            typesAndNodes.put(mapper.getRDFType().asNode(), mapper.getDefaultGraph());
        }
        return typesAndNodes;
    }
}