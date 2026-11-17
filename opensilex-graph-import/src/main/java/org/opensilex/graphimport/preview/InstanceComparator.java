//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.preview;

import org.apache.jena.vocabulary.RDFS;
import org.opensilex.aiimport.report.ReportMessage;
import org.opensilex.graphimport.graph.CellRef;
import org.opensilex.graphimport.graph.GraphNode;
import org.opensilex.graphimport.graph.GraphValue;
import org.opensilex.graphimport.graph.ImportGraph;
import org.opensilex.graphimport.graph.InstanceIndex;
import org.opensilex.graphimport.graph.LinkTarget;
import org.opensilex.graphimport.graph.StoredValue;
import org.opensilex.graphimport.interpret.InterpretationServices;
import org.opensilex.graphimport.ontology.ClassView;
import org.opensilex.graphimport.ontology.PropertyView;
import org.opensilex.graphimport.ontology.Uris;
import org.opensilex.graphimport.ontology.literal.LiteralReader;
import org.opensilex.graphimport.ontology.literal.LiteralReading;
import org.opensilex.graphimport.ontology.literal.ReadingContext;
import org.opensilex.graphimport.validate.GraphProblem;
import org.opensilex.graphimport.validate.NodeClasses;
import org.opensilex.graphimport.validate.OwlRestrictionGraphValidator;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Compares the resources of an import with the instance, in two passes.
 * <p>
 * First each resource is matched to what exists, through the index and so within the user's rights: a
 * URI the file writes, if it exists with a class that fits; otherwise its name among the resources of
 * its class. Several resources for one name is a conflict, never a choice. A row without identifier,
 * or whose sheets give unrelated classes, is matched to nothing.
 * <p>
 * Then each value is compared with what the matched resource holds, both read the same way, so that
 * {@code 1.50} stored and {@code 1,5} written are one value; links are compared once every row is
 * matched, a link to a row pointing to the resource that row is. The cardinality of the property
 * decides: a value the resource lacks is added while the property takes it, and replaces one beyond.
 * <p>
 * The identifier of a resource matched by its name is the value it was matched by: written {@code p1}
 * for a resource named {@code P1}, or without the language its name is stored with, it is that name,
 * not another one that would replace it.
 *
 * @author Arnaud Charleroy
 */
public class InstanceComparator {

    private static final URI LITERAL = URI.create(RDFS.Literal.getURI());

    /** Stored literals are canonical already: no date order or zone to guess. */
    private final LiteralReader storedReader = new LiteralReader(ReadingContext.UNKNOWN);

    private final InterpretationServices services;

    public InstanceComparator(InterpretationServices services) {
        this.services = services;
    }

    /** What a resource is matched to. */
    private record Match(URI existing, Set<URI> candidates, ReportMessage conflict) {

        static final Match NONE = new Match(null, Set.of(), null);

        /** The resource it is, when nothing stands in the way of comparing their values. */
        URI clean() {
            return conflict == null ? existing : null;
        }
    }

    /** A value as compared: what it is once read, where it points, or why it cannot be compared. */
    private record Read(String identity, URI target, boolean ambiguous) {
    }

    /**
     * @param problems every problem the validators found on the graph
     * @return the resources, in the graph's order, compared
     */
    public List<NodeComparison> compare(ImportGraph graph, List<GraphProblem> problems) throws Exception {
        Map<String, Match> matches = match(graph.nodes());
        Map<URI, Map<URI, List<StoredValue>>> stored = storedValues(graph.nodes(), matches);
        Map<String, List<GraphProblem>> problemsByNode = byNode(graph, problems);
        Map<String, GraphNode> byKey = graph.byKey();

        List<NodeComparison> compared = new ArrayList<>();
        for (int index = 0; index < graph.nodes().size(); index++) {
            GraphNode node = graph.nodes().get(index);
            compared.add(compareNode(index, node, matches, stored, byKey,
                    problemsByNode.getOrDefault(node.getKey(), List.of())));
        }
        return compared;
    }

    //#region matching

    private Map<String, Match> match(List<GraphNode> nodes) throws Exception {
        Map<String, Match> matches = new HashMap<>();
        Map<URI, List<GraphNode>> named = new LinkedHashMap<>();
        Map<GraphNode, URI> written = new LinkedHashMap<>();
        for (GraphNode node : nodes) {
            Optional<URI> classUri = NodeClasses.mostSpecific(node, services.catalog());
            if (!node.isIdentified()) {
                matches.put(node.getKey(), Match.NONE);
            } else if (classUri.isEmpty()) {
                matches.put(node.getKey(), new Match(null, Set.of(),
                        PreviewMessages.fileClasses(NodeClasses.labels(node.getClasses(), services))));
            } else if (node.getUri() != null) {
                written.put(node, classUri.get());
            } else {
                named.computeIfAbsent(classUri.get(), key -> new ArrayList<>()).add(node);
            }
        }
        for (Map.Entry<URI, List<GraphNode>> entry : named.entrySet()) {
            Map<String, Set<URI>> found = services.instances()
                    .lookUp(entry.getKey(), entry.getValue().stream().map(GraphNode::getName).toList());
            for (GraphNode node : entry.getValue()) {
                matches.put(node.getKey(), byName(node, found.getOrDefault(node.getKey(), Set.of())));
            }
        }
        Map<URI, Set<URI>> existing = written.isEmpty() ? Map.of() : services.instances().existing(written.keySet()
                .stream().map(GraphNode::getUri).toList());
        for (Map.Entry<GraphNode, URI> entry : written.entrySet()) {
            URI uri = Uris.expand(entry.getKey().getUri());
            matches.put(entry.getKey().getKey(), byUri(uri, existing.get(uri), entry.getValue()));
        }
        return matches;
    }

    private static Match byName(GraphNode node, Set<URI> found) {
        if (found.isEmpty()) {
            return Match.NONE;
        }
        Set<URI> expanded = new LinkedHashSet<>();
        found.forEach(uri -> expanded.add(Uris.expand(uri)));
        if (expanded.size() == 1) {
            return new Match(expanded.iterator().next(), Set.of(), null);
        }
        return new Match(null, expanded, PreviewMessages.ambiguousMatch(node.getName(), expanded.size()));
    }

    /** A written URI is the resource when it exists with a class that is the file's, or above or below it. */
    private Match byUri(URI uri, Set<URI> types, URI classUri) throws Exception {
        if (types == null) {
            return Match.NONE;
        }
        for (URI type : types) {
            if (services.views().get(type).isA(classUri) || services.views().get(classUri).isA(type)) {
                return new Match(uri, Set.of(), null);
            }
        }
        return new Match(uri, Set.of(), PreviewMessages.classMismatch(uri.toString(),
                NodeClasses.labels(types, services), NodeClasses.labels(List.of(classUri), services)));
    }

    /** What the matched resources hold, for the properties the file gives and those their class requires. */
    private Map<URI, Map<URI, List<StoredValue>>> storedValues(List<GraphNode> nodes, Map<String, Match> matches)
            throws Exception {
        Set<URI> instances = new LinkedHashSet<>();
        Set<URI> properties = new LinkedHashSet<>();
        for (GraphNode node : nodes) {
            URI existing = matches.get(node.getKey()).clean();
            if (existing != null) {
                instances.add(existing);
                properties.addAll(node.getValues().keySet());
                ClassView view = view(node);
                if (view != null) {
                    view.getProperties().stream()
                            .filter(property -> property.cardinality().min() > 0)
                            .forEach(property -> properties.add(property.uri()));
                }
            }
        }
        if (instances.isEmpty()) {
            return Map.of();
        }
        return services.instances().storedValues(instances, properties);
    }

    //#endregion

    //#region comparing

    private NodeComparison compareNode(int index, GraphNode node, Map<String, Match> matches,
                                       Map<URI, Map<URI, List<StoredValue>>> stored, Map<String, GraphNode> byKey,
                                       List<GraphProblem> problems) throws Exception {
        Match match = matches.get(node.getKey());
        URI existing = match.clean();
        ClassView view = view(node);
        Map<URI, List<StoredValue>> held = existing == null ? Map.of() : stored.getOrDefault(existing, Map.of());

        List<ValueComparison> values = new ArrayList<>();
        Map<URI, Set<String>> fileIdentities = new HashMap<>();
        Set<CellRef> identifierCells = existing != null && node.getUri() == null
                ? new HashSet<>(node.getRows())
                : Set.of();
        for (Map.Entry<URI, List<GraphValue>> entry : node.getValues().entrySet()) {
            PropertyView property = view == null ? null : view.getProperty(entry.getKey());
            URI datatype = datatype(entry.getValue(), property);
            Map<String, StoredValue> heldValues = identities(held.getOrDefault(entry.getKey(), List.of()), datatype);
            List<Read> reads = new ArrayList<>();
            Set<String> given = new LinkedHashSet<>();
            for (GraphValue value : entry.getValue()) {
                Read read = read(value, matches, byKey);
                if (identifierCells.contains(value.cell())) {
                    read = asMatched(value, read, heldValues);
                }
                reads.add(read);
                if (read.identity() != null) {
                    given.add(read.identity());
                }
            }
            fileIdentities.put(entry.getKey(), given);
            Set<String> together = new LinkedHashSet<>(heldValues.keySet());
            together.addAll(given);
            Integer max = property == null ? null : property.cardinality().max();
            boolean beyond = max != null && together.size() > max && !heldValues.isEmpty();
            List<String> heldInstead = heldValues.entrySet().stream()
                    .filter(kept -> !given.contains(kept.getKey()))
                    .map(kept -> kept.getValue().shown())
                    .toList();
            for (int position = 0; position < entry.getValue().size(); position++) {
                values.add(compareValue(entry.getKey(), entry.getValue().get(position), reads.get(position), existing,
                        heldValues.keySet(), beyond, heldInstead));
            }
        }

        List<String> missing = missing(view, held, fileIdentities, node);
        List<GraphProblem> own = problems.stream()
                .filter(problem -> !OwlRestrictionGraphValidator.REQUIRED_MISSING.equals(problem.message().getKey()))
                .toList();
        NodeState state = state(match, values);
        boolean toFix = own.stream().anyMatch(problem -> problem.severity() == GraphProblem.Severity.ERROR)
                || values.stream().anyMatch(value -> value.state() == ValueState.AMBIGUOUS)
                || (state == NodeState.CREATED && !missing.isEmpty());
        return new NodeComparison(index, node, NodeClasses.mostSpecific(node, services.catalog()).orElse(null), state,
                match.existing(), match.candidates(), match.conflict(), values, missing, own, toFix);
    }

    private static ValueComparison compareValue(URI property, GraphValue value, Read read, URI existing,
                                                Set<String> held, boolean beyond, List<String> heldInstead) {
        if (read.ambiguous()) {
            return new ValueComparison(property, value, ValueState.AMBIGUOUS, null, List.of());
        }
        if (read.identity() == null) {
            return new ValueComparison(property, value, ValueState.UNUSABLE, null, List.of());
        }
        if (existing == null) {
            return new ValueComparison(property, value, ValueState.NEW, read.target(), List.of());
        }
        if (held.contains(read.identity())) {
            return new ValueComparison(property, value, ValueState.PRESENT, read.target(), List.of());
        }
        if (beyond) {
            return new ValueComparison(property, value, ValueState.CONFLICT, read.target(), heldInstead);
        }
        return new ValueComparison(property, value, ValueState.NEW, read.target(), List.of());
    }

    private static NodeState state(Match match, List<ValueComparison> values) {
        if (match.conflict() != null) {
            return NodeState.CONFLICT;
        }
        if (match.existing() == null) {
            return NodeState.CREATED;
        }
        if (values.stream().anyMatch(value -> value.state() == ValueState.CONFLICT)) {
            return NodeState.CONFLICT;
        }
        return values.stream().anyMatch(value -> value.state() == ValueState.NEW)
                ? NodeState.COMPLETED
                : NodeState.UNCHANGED;
    }

    /** The properties the resource needs and neither the file nor the instance gives enough values of. */
    private List<String> missing(ClassView view, Map<URI, List<StoredValue>> held, Map<URI, Set<String>> given,
                                 GraphNode node) {
        List<String> missing = new ArrayList<>();
        if (view == null) {
            return missing;
        }
        for (PropertyView property : view.getProperties()) {
            int min = property.cardinality().min();
            if (min == 0) {
                continue;
            }
            Set<String> together = new LinkedHashSet<>(given.getOrDefault(property.uri(), Set.of()));
            together.addAll(identities(held.getOrDefault(property.uri(), List.of()),
                    datatype(node.getValues().getOrDefault(property.uri(), List.of()), property)).keySet());
            if (together.size() < min) {
                missing.add(NodeClasses.label(property, services));
            }
        }
        return missing;
    }

    /**
     * What a value of the file is once read. A link to a row is the resource the row is matched to, or
     * the row itself when it would be created; a name that is both a row and an existing resource is
     * that resource only when the row is matched to it.
     */
    private static Read read(GraphValue value, Map<String, Match> matches, Map<String, GraphNode> byKey) {
        if (!value.isUsable()) {
            return new Read(null, null, false);
        }
        if (!value.isLink()) {
            LiteralReading literal = value.literal();
            return new Read(literalIdentity(literal.lexical(), literal.datatype(), value.language()), null, false);
        }
        LinkTarget link = value.link();
        return switch (link.status()) {
            case URI -> resource(Uris.expand(link.uri()));
            case INSTANCE -> resource(Uris.expand(link.instances().iterator().next()));
            case ROW -> row(link.nodeKey(), matches, byKey);
            case ROW_AND_INSTANCE -> {
                URI rowIs = matches.get(link.nodeKey()).clean();
                boolean same = rowIs != null && link.instances().stream().map(Uris::expand).anyMatch(rowIs::equals);
                yield same ? resource(rowIs) : new Read(null, null, true);
            }
            default -> new Read(null, null, false);
        };
    }

    /** The name a resource was matched by, read as the stored value it matched, case and language aside. */
    private static Read asMatched(GraphValue value, Read read, Map<String, StoredValue> held) {
        if (read.identity() == null || value.isLink()) {
            return read;
        }
        String key = InstanceIndex.key(value.raw());
        for (Map.Entry<String, StoredValue> stored : held.entrySet()) {
            if (!stored.getValue().isResource() && InstanceIndex.key(stored.getValue().lexical()).equals(key)) {
                return new Read(stored.getKey(), null, false);
            }
        }
        return read;
    }

    private static Read row(String nodeKey, Map<String, Match> matches, Map<String, GraphNode> byKey) {
        URI matched = matches.get(nodeKey).clean();
        if (matched != null) {
            return resource(matched);
        }
        URI written = byKey.get(nodeKey).getUri();
        return written != null ? resource(Uris.expand(written)) : new Read("row:" + nodeKey, null, false);
    }

    private static Read resource(URI uri) {
        return new Read(resourceIdentity(uri), uri, false);
    }

    /** The values a resource holds, by what they are once read the way the file's values are. */
    private Map<String, StoredValue> identities(List<StoredValue> values, URI datatype) {
        Map<String, StoredValue> identities = new LinkedHashMap<>();
        for (StoredValue value : values) {
            String identity;
            if (value.isResource()) {
                identity = resourceIdentity(value.resource());
            } else {
                LiteralReading reading = storedReader.read(value.lexical(), datatype);
                String lexical = reading.isRefused() || reading.isEmpty() ? value.lexical() : reading.lexical();
                identity = literalIdentity(lexical, datatype, value.language());
            }
            identities.putIfAbsent(identity, value);
        }
        return identities;
    }

    private static String resourceIdentity(URI uri) {
        return "resource:" + uri;
    }

    private static String literalIdentity(String lexical, URI datatype, String language) {
        return "literal:" + lexical + "^^" + datatype
                + (language == null ? "" : "@" + language.toLowerCase(Locale.ROOT));
    }

    /** The datatype values of a property are read for: the file's, else the property's range. */
    private static URI datatype(List<GraphValue> values, PropertyView property) {
        for (GraphValue value : values) {
            if (!value.isLink() && value.literal() != null) {
                return value.literal().datatype();
            }
        }
        return property == null || property.range() == null ? LITERAL : property.range();
    }

    private ClassView view(GraphNode node) throws Exception {
        Optional<URI> classUri = NodeClasses.mostSpecific(node, services.catalog());
        return classUri.isEmpty() ? null : services.views().get(classUri.get());
    }

    /** The problems of each resource, by the cells they lie on. */
    private static Map<String, List<GraphProblem>> byNode(ImportGraph graph, Collection<GraphProblem> problems) {
        Map<CellRef, String> owner = new HashMap<>();
        for (GraphNode node : graph.nodes()) {
            node.getRows().forEach(row -> owner.put(row, node.getKey()));
            node.getValues().values()
                    .forEach(values -> values.forEach(value -> owner.put(value.cell(), node.getKey())));
        }
        Map<String, List<GraphProblem>> byNode = new HashMap<>();
        for (GraphProblem problem : problems) {
            problem.cells().stream()
                    .map(owner::get)
                    .filter(Objects::nonNull)
                    .distinct()
                    .forEach(key -> byNode.computeIfAbsent(key, k -> new ArrayList<>()).add(problem));
        }
        return byNode;
    }

    //#endregion
}
