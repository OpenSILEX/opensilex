package org.opensilex.sparql.service.query;

import org.apache.commons.collections4.MapUtils;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.vocabulary.RDF;
import org.opensilex.server.rest.serialization.uri.UriFormater;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.Ontology;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.opensilex.sparql.service.SPARQLService.TYPE_VAR;
import static org.opensilex.sparql.service.SPARQLService.URI_VAR;

/**
 * A SPARQL query which can find the existing/non-existing resource inside zero or multiple graph
 * The query take a List of URI as input
 *
 * @author rcolin
 */
public class SparqlMultiGraphQuery<T extends SPARQLResourceModel> extends AbstractSparqlUrisQuery<T> {

    /**
     * Map of (types, graph) where the query will look for resources.
     * The query return a model which match any given types/graph, so it's use a OR semantic
     */
    private final Map<Node, Node> typesAndGraphs;

    /**
     * A Supplier of Stream from which URIs are read.
     * We use a {@link Supplier} here since the URI Stream can be read two time (one for query and once again for validation if required).
     * The goal is to provide flexibility depending on the data structure you use for storing URIs and the datatype of the URI (URI, String) without having to perform convert (CPU/RAM expensive)
     */
    private final Supplier<Stream<String>> urisSupplier;

    /**
     * Size of URIs in {@link Stream} which is read from {@code urisSupplier}.
     */
    private final int urisSize;

    /**
     * Function that will transform SPARQLResult into URI
     */
    private final Function<SPARQLResult, URI> uriHandler;

    /**
     * @param typesAndGraphs Map of (types, graph) (The query return a model which match any given types/graph, so it's use a OR semantic)
     * @param urisSupplier   A {@link Supplier} of {@link Stream} from which URIs are read
     *                       <ul>
     *                           <li>We use a {@link Supplier} here since the URI Stream can be read two time (one for query and once again for validation if required).</li>
     *                           <li>The goal is to provide flexibility depending of the data structure you use for storing URIs and the datatype of the URI (URI, String) without having to perform convert (CPU/RAM expensive)</li>
     *                       </ul>
     * @throws IllegalArgumentException if Any {@link Class} from {@code classes} doesn't have a corresponding graph. This method only works for model with a defined graph.
     * @apiNote <ul>
     * <li>If any URI is stored inside a different graph than the graph defined inside model, then the query can't find it </li>
     * <li>If the URI is declared inside two or more of the graph associated with {@code classes}, then more results than input URIs will be returned</li>
     * <li>
     * The produced SPARQL query looks like
     * <pre>
     * {@code
     * SELECT DISTINCT ?uri ?type where {
     *    {
     *    ?type rdfs:subClassOf* vocabulary:Germplasm .
     *    GRAPH <http://opensilex.dev/set/germplasm> {
     *      ?uri a ?type
     *    }
     *  }
     *  UNION
     *  {
     *    ?type rdfs:subClassOf* vocabulary:Method .
     *    GRAPH <http://opensilex.dev/set/variable> {
     *      ?uri a ?type
     *    }
     *  }
     *  }
     *
     *  VALUES ?uri   {
     *    vocabulary:standard_method
     *    <http://aims.fao.org/aos/agrovoc/c_291281>
     *  }
     * }
     * </pre>
     * <li>When searching for unknown URIs, then a {@code FILTER NOT EXISTS} clause is added and the query return the list of unknown URIs from input</li>
     * </li>
     * </ul>
     */
    public SparqlMultiGraphQuery(SPARQLService sparql, @Nullable Map<Node, Node> typesAndGraphs, @NotNull Supplier<Stream<String>> urisSupplier, int urisSize) {
        super(sparql);
        Objects.requireNonNull(urisSupplier);
        if (urisSize <= 0) {
            throw new IllegalArgumentException("urisSize must be greater than 0");
        }
        this.typesAndGraphs = typesAndGraphs;
        this.urisSupplier = urisSupplier;
        this.urisSize = urisSize;
        uriHandler = (result -> UriFormater.formatURI(result.getStringValue(SPARQLResourceModel.URI_FIELD)));
    }

    /**
     * Constructs a new instance of SparqlMultiGraphQuery with (type, graph) and uris.
     *
     * @param sparql         The {@link org.opensilex.sparql.service.SPARQLService} to use
     * @param typesAndGraphs Map of (types, graph) where the query will look for resources.
     *                       The query return a model which match any given types/graph, so it's use a OR semantic.
     * @param uris           Collection of URIs into a String format
     * @see SparqlMultiGraphQuery#SparqlMultiGraphQuery(SPARQLService, Supplier, int)
     */
    public SparqlMultiGraphQuery(SPARQLService sparql, Map<Node, Node> typesAndGraphs, Collection<String> uris) {
        this(sparql, typesAndGraphs, uris::stream, uris.size());
    }

    /**
     * Constructs a new instance of SparqlMultiGraphQuery with (type, graph) and uris.
     *
     * @param sparql         The {@link org.opensilex.sparql.service.SPARQLService} to use
     * @param typesAndGraphs Map of (types, graph) where the query will look for resources.
     *                       The query return a model which match any given types/graph, so it's use a OR semantic.
     * @param uris           Collection of URIs
     * @see SparqlMultiGraphQuery#SparqlMultiGraphQuery(SPARQLService, Supplier, int)
     */
    public SparqlMultiGraphQuery(SPARQLService sparql, Collection<URI> uris, Map<Node, Node> typesAndGraphs) {
        this(sparql, typesAndGraphs, () -> uris.stream().map(URI::toString), uris.size());
    }


    /**
     * Constructs a new instance of SparqlMultiGraphQuery with URIs and no (type, graph).
     * In this case, the query will look inside the whole database since no graph is specified
     *
     * @param sparql The {@link org.opensilex.sparql.service.SPARQLService} to use
     * @apiNote Pay attention to query performance when no graph is defined since according database indexing, this can lead to the read
     * of the whole repository
     * @see SparqlMultiGraphQuery#SparqlMultiGraphQuery(SPARQLService, Supplier, int)
     */
    public SparqlMultiGraphQuery(SPARQLService sparql, Supplier<Stream<String>> urisSupplier, int urisSize) {
        this(sparql, null, urisSupplier, urisSize);
    }

    @Override
    public Function<SPARQLResult, URI> uriHandler() {
        return uriHandler;
    }

    @Override
    public WhereBuilder getWhere() {

        WhereBuilder where = new WhereBuilder();

        // Only check if the resource has a defined rdf:type
        if (MapUtils.isEmpty(typesAndGraphs)) {
            return where.addWhere(URI_VAR, RDF.type.asNode(), TYPE_VAR);
        }

        // Add a UNION part for each (type,graph)
        typesAndGraphs.forEach((type, graph) -> where.addUnion(new WhereBuilder()
                .addWhere(TYPE_VAR, Ontology.subClassAny, type)
                .addGraph(graph, URI_VAR, RDF.type.asNode(), TYPE_VAR)
        ));
        return where;
    }


    @Override
    public SelectBuilder getSelect() {
        return new SelectBuilder()
                .setDistinct(true)
                .addVar(URI_VAR)
                .addVar(TYPE_VAR);
    }

    @Override
    public Stream<String> getUniqueUriStringStream() {
        return urisSupplier.get();
    }

    @Override
    public int getQuerySize() {
        return urisSize;
    }

}
