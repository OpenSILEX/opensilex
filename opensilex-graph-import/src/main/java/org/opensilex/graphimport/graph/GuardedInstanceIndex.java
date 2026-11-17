//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.graph;

import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.aiimport.resolve.NameSimilarity;
import org.opensilex.aiimport.resolve.NearMatchFinder;
import org.opensilex.aiimport.resolve.ResourceReference;
import org.opensilex.aiimport.workbook.HeaderMatcher;
import org.opensilex.graphimport.ontology.Uris;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLLiteral;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.utils.Ontology;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.apache.jena.arq.querybuilder.Converters.makeVar;

/**
 * Looks names up by {@code rdfs:label}, case aside, among the instances of a class and its
 * subclasses, then keeps what the user may see.
 * <p>
 * The instances of the scopes that live in any graph are found in one query per batch of names, each
 * found instance tagged with the scope its type descends from; a scope bound to a graph, such as the
 * scientific objects of an experiment, has its own query in that graph. Each scope then filters its
 * own instances. The answers are kept for the life of the index, which is one interpretation.
 * <p>
 * Exact names and close names go through that same path, with another filter on the label, so that
 * a suggestion never shows an instance a lookup would hide; so do URIs checked for existence. Values
 * are read only for the instances found visible, in the graph their scope reads.
 *
 * @author Arnaud Charleroy
 */
public class GuardedInstanceIndex implements InstanceIndex {

    /** How many names one query carries. */
    public static final int NAMES_PER_QUERY = 200;

    /** How many names nothing answers to get close names: beyond it, the file has another problem. */
    public static final int NEAR_NAMES = 100;

    /** How many names one query for close names carries. */
    static final int NEAR_NAMES_PER_QUERY = 50;

    /**
     * How many labels one query for close names reads, counted before the user's rights filter them:
     * a class with many instances sharing the first letters of a name may hide a close one past it.
     */
    static final int NEAR_LABELS_PER_QUERY = 2000;

    /** How many first letters a close name shares: a slip in them is not suggested. */
    static final int NEAR_PREFIX = 3;

    private static final ExprFactory EXPR = SPARQLQueryHelper.getExprFactory();
    private static final Var URI_VAR = makeVar("uri");
    private static final Var TYPE = makeVar("type");
    private static final Var ROOT = makeVar("root");
    private static final Var LABEL = makeVar("label");
    private static final Var PROPERTY = makeVar("property");
    private static final Var OBJECT = makeVar("object");

    private final LookupContext context;
    private final List<InstanceScope> scopes;
    private final Map<String, Map<String, Set<URI>>> answers = new HashMap<>();
    private final NearMatchFinder nearMatches = new NearMatchFinder();

    /**
     * The instances found visible so far, with the graph their scope reads them in, null for any: only
     * their values may be read.
     */
    private final Map<URI, URI> graphOfVisible = new HashMap<>();

    public GuardedInstanceIndex(LookupContext context, Collection<InstanceScope> scopes) {
        this.context = context;
        this.scopes = new ArrayList<>(scopes);
        this.scopes.sort(Comparator.comparing(scope -> scope.rootClass().toString()));
    }

    @Override
    public Map<String, Set<URI>> lookUp(URI expectedClass, Collection<String> names) throws Exception {
        Set<String> keys = keysOf(names);
        if (keys.isEmpty()) {
            return Map.of();
        }
        String memo = expectedClass + "|" + String.join("\u0000", keys);
        Map<String, Set<URI>> known = answers.get(memo);
        if (known == null) {
            known = new LinkedHashMap<>();
            List<Expr> filters = new ArrayList<>();
            for (List<String> batch : batches(new ArrayList<>(keys), NAMES_PER_QUERY)) {
                Node[] literals = batch.stream().map(NodeFactory::createLiteral).toArray(Node[]::new);
                filters.add(EXPR.in(EXPR.lcase(EXPR.str(LABEL)), (Object[]) literals));
            }
            for (Map.Entry<URI, Set<String>> found : visibleLabels(Uris.expand(expectedClass), filters, null).entrySet()) {
                for (String label : found.getValue()) {
                    String key = InstanceIndex.key(label);
                    if (keys.contains(key)) {
                        known.computeIfAbsent(key, k -> new LinkedHashSet<>()).add(found.getKey());
                    }
                }
            }
            answers.put(memo, known);
        }
        return known;
    }

    @Override
    public Map<String, List<Suggestion>> near(URI expectedClass, Collection<String> names) throws Exception {
        Map<String, String> nameByKey = new LinkedHashMap<>();
        for (String name : names) {
            String key = InstanceIndex.key(name);
            boolean mayHaveATypo = NameSimilarity.allowedEdits(HeaderMatcher.normalize(name).length()) > 0;
            if (mayHaveATypo && key.length() >= NEAR_PREFIX && nameByKey.size() < NEAR_NAMES) {
                nameByKey.putIfAbsent(key, name);
            }
        }
        Map<String, List<Suggestion>> suggestions = new LinkedHashMap<>();
        for (List<String> batch : batches(new ArrayList<>(nameByKey.keySet()), NEAR_NAMES_PER_QUERY)) {
            List<ResourceReference> candidates = new ArrayList<>();
            visibleLabels(Uris.expand(expectedClass), List.of(closeTo(batch)), NEAR_LABELS_PER_QUERY)
                    .forEach((instance, labels) -> labels.forEach(label ->
                            candidates.add(new ResourceReference(instance, label))));
            for (String key : batch) {
                List<Suggestion> close = nearMatches.suggest(nameByKey.get(key), candidates).stream()
                        .map(found -> new Suggestion(found.getUri(), null, found.getName(), found.getSimilarity()))
                        .toList();
                if (!close.isEmpty()) {
                    suggestions.put(key, close);
                }
            }
        }
        return suggestions;
    }

    @Override
    public Map<URI, Set<URI>> existing(Collection<URI> uris) throws Exception {
        List<String> expanded = uris.stream().map(uri -> Uris.expand(uri).toString()).distinct().sorted().toList();
        List<Expr> filters = new ArrayList<>();
        for (List<String> batch : batches(expanded, NAMES_PER_QUERY)) {
            Node[] nodes = batch.stream().map(NodeFactory::createURI).toArray(Node[]::new);
            filters.add(EXPR.in(URI_VAR, (Object[]) nodes));
        }
        if (filters.isEmpty()) {
            return Map.of();
        }
        Map<URI, Set<URI>> found = new LinkedHashMap<>();
        visibleFacts(null, TYPE, filters, null).forEach((instance, types) ->
                found.put(instance, types.stream()
                        .map(Uris::expand)
                        .collect(Collectors.toCollection(LinkedHashSet::new))));
        return found;
    }

    @Override
    public Map<URI, Map<URI, List<StoredValue>>> storedValues(Collection<URI> instances, Collection<URI> properties)
            throws Exception {
        Map<URI, List<URI>> byGraph = new LinkedHashMap<>();
        instances.stream().map(Uris::expand).distinct()
                .filter(graphOfVisible::containsKey)
                .forEach(instance -> byGraph.computeIfAbsent(graphOfVisible.get(instance), key -> new ArrayList<>())
                        .add(instance));
        Map<URI, Map<URI, List<StoredValue>>> values = new LinkedHashMap<>();
        if (properties.isEmpty()) {
            return values;
        }
        for (Map.Entry<URI, List<URI>> graph : byGraph.entrySet()) {
            for (int start = 0; start < graph.getValue().size(); start += NAMES_PER_QUERY) {
                List<URI> batch = graph.getValue()
                        .subList(start, Math.min(graph.getValue().size(), start + NAMES_PER_QUERY));
                readValues(graph.getKey(), batch, properties, values);
            }
        }
        return values;
    }

    /** Labels that start like one of the names and are no more than two letters longer or shorter. */
    private static Expr closeTo(List<String> keys) {
        Expr label = EXPR.lcase(EXPR.str(LABEL));
        Expr any = null;
        for (String key : keys) {
            int length = key.length();
            Expr one = EXPR.and(EXPR.strstarts(label, NodeValue.makeString(key.substring(0, NEAR_PREFIX))),
                    EXPR.and(EXPR.ge(EXPR.strlen(label), NodeValue.makeInteger(length - 2L)),
                            EXPR.le(EXPR.strlen(label), NodeValue.makeInteger(length + 2L))));
            any = any == null ? one : EXPR.or(any, one);
        }
        return any;
    }

    private static Set<String> keysOf(Collection<String> names) {
        Set<String> keys = new TreeSet<>();
        for (String name : names) {
            String key = InstanceIndex.key(name);
            if (!key.isEmpty()) {
                keys.add(key);
            }
        }
        return keys;
    }

    /**
     * The instances whose label one of the filters accepts, each scope's rules applied.
     *
     * @param filters one query per filter, in each graph
     * @param limit   how many rows a query reads at most; null for all
     * @return the visible instances, as full URIs, with the labels that matched
     */
    private Map<URI, Set<String>> visibleLabels(URI expectedClass, List<Expr> filters, Integer limit)
            throws Exception {
        return visibleFacts(expectedClass, LABEL, filters, limit);
    }

    /**
     * The instances one of the filters accepts, each scope's rules applied, with a fact read about each:
     * its labels, or its types. Each visible instance is remembered with the graph its scope reads, so
     * that its values are read there and nowhere else.
     *
     * @param expectedClass the class they must belong to, subclasses included; null for any
     */
    private Map<URI, Set<String>> visibleFacts(URI expectedClass, Var fact, List<Expr> filters, Integer limit)
            throws Exception {
        Map<URI, InstanceScope> anyGraph = new LinkedHashMap<>();
        Map<URI, Set<String>> factsByInstance = new HashMap<>();
        Map<InstanceScope, Set<URI>> foundByScope = new LinkedHashMap<>();

        for (InstanceScope scope : scopes) {
            if (!scope.canLookUp(context)) {
                continue;
            }
            URI graph = scope.graph(context);
            if (graph == null) {
                anyGraph.put(Uris.expand(scope.rootClass()), scope);
            } else {
                for (Expr filter : filters) {
                    collect(query(expectedClass, fact, List.of(Uris.expand(scope.rootClass())), graph, filter, limit),
                            fact, root -> scope, factsByInstance, foundByScope);
                }
            }
        }
        if (!anyGraph.isEmpty()) {
            for (Expr filter : filters) {
                collect(query(expectedClass, fact, anyGraph.keySet(), null, filter, limit), fact, anyGraph::get,
                        factsByInstance, foundByScope);
            }
        }

        Map<URI, Set<String>> visible = new LinkedHashMap<>();
        for (Map.Entry<InstanceScope, Set<URI>> entry : foundByScope.entrySet()) {
            URI graph = entry.getKey().graph(context);
            for (URI shown : entry.getKey().visible(entry.getValue(), context)) {
                URI instance = Uris.expand(shown);
                graphOfVisible.put(instance, graph == null ? null : Uris.expand(graph));
                visible.computeIfAbsent(instance, key -> new LinkedHashSet<>())
                        .addAll(factsByInstance.getOrDefault(instance, Set.of()));
            }
        }
        return visible;
    }

    private void collect(List<SPARQLResult> results,
                         Var fact,
                         Function<URI, InstanceScope> scopeOfRoot,
                         Map<URI, Set<String>> factsByInstance,
                         Map<InstanceScope, Set<URI>> foundByScope) {
        for (SPARQLResult result : results) {
            URI instance = Uris.expand(result.getStringValue(URI_VAR.getVarName()));
            InstanceScope scope = scopeOfRoot.apply(Uris.expand(result.getStringValue(ROOT.getVarName())));
            if (scope != null) {
                foundByScope.computeIfAbsent(scope, key -> new LinkedHashSet<>()).add(instance);
                factsByInstance.computeIfAbsent(instance, key -> new LinkedHashSet<>())
                        .add(result.getStringValue(fact.getVarName()));
            }
        }
    }

    /**
     * The instances under the given roots whose {@code fact} the filter accepts: their label, or for
     * {@link #TYPE} their type, which needs no label.
     *
     * @param expectedClass the class they must belong to, subclasses included; null for any
     */
    private List<SPARQLResult> query(URI expectedClass, Var fact, Collection<URI> roots, URI graph, Expr filter,
                                     Integer limit) throws Exception {
        WhereBuilder instance = new WhereBuilder().addWhere(URI_VAR, RDF.type, TYPE);
        if (LABEL.equals(fact)) {
            instance.addWhere(URI_VAR, RDFS.label, LABEL);
        }

        SelectBuilder select = new SelectBuilder()
                .setDistinct(true)
                .addVar(URI_VAR)
                .addVar(ROOT)
                .addVar(fact);
        if (graph == null) {
            select.addWhere(instance);
        } else {
            select.addGraph(NodeFactory.createURI(Uris.expand(graph).toString()), instance);
        }
        select.addWhere(TYPE, Ontology.subClassAny, ROOT);
        if (expectedClass != null) {
            select.addWhere(TYPE, Ontology.subClassAny, NodeFactory.createURI(expectedClass.toString()));
        }
        select.addFilter(filter);
        select.addWhereValueVar(ROOT, roots.stream().map(root -> NodeFactory.createURI(root.toString())).toArray());
        if (limit != null) {
            select.setLimit(limit);
        }
        return context.sparql().executeSelectQuery(select);
    }

    /** What the instances hold for the properties, in one graph, or in any graph when it is null. */
    private void readValues(URI graph, List<URI> instances, Collection<URI> properties,
                            Map<URI, Map<URI, List<StoredValue>>> values) throws Exception {
        WhereBuilder triple = new WhereBuilder().addWhere(URI_VAR, PROPERTY, OBJECT);
        SelectBuilder select = new SelectBuilder()
                .setDistinct(true)
                .addVar(URI_VAR)
                .addVar(PROPERTY)
                .addVar(OBJECT);
        if (graph == null) {
            select.addWhere(triple);
        } else {
            select.addGraph(NodeFactory.createURI(Uris.expand(graph).toString()), triple);
        }
        // Two filters rather than two VALUES, which the builder would join into one block of tuples.
        select.addFilter(EXPR.in(URI_VAR, instances.stream()
                .map(uri -> NodeFactory.createURI(uri.toString())).toArray()));
        select.addFilter(EXPR.in(PROPERTY, properties.stream()
                .map(property -> NodeFactory.createURI(Uris.expand(property).toString())).toArray()));
        for (SPARQLResult result : context.sparql().executeSelectQuery(select)) {
            StoredValue value;
            if (result.isURI(OBJECT.getVarName())) {
                value = StoredValue.resource(Uris.expand(result.getStringValue(OBJECT.getVarName())));
            } else {
                Optional<SPARQLLiteral> literal = result.getLiteralValue(OBJECT.getVarName());
                if (literal.isEmpty()) {
                    continue;
                }
                value = StoredValue.literal(literal.get().value(), literal.get().datatype(), literal.get().lang());
            }
            URI instance = Uris.expand(result.getStringValue(URI_VAR.getVarName()));
            URI property = Uris.expand(result.getStringValue(PROPERTY.getVarName()));
            values.computeIfAbsent(instance, key -> new LinkedHashMap<>())
                    .computeIfAbsent(property, key -> new ArrayList<>())
                    .add(value);
        }
    }

    private static List<List<String>> batches(List<String> keys, int size) {
        List<List<String>> batches = new ArrayList<>();
        for (int start = 0; start < keys.size(); start += size) {
            batches.add(keys.subList(start, Math.min(keys.size(), start + size)));
        }
        return batches;
    }
}
