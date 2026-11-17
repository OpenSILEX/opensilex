//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.graph;

import org.opensilex.aiimport.resolve.NameSimilarity;
import org.opensilex.aiimport.resolve.NearMatchFinder;
import org.opensilex.graphimport.interpret.InterpretationServices;
import org.opensilex.graphimport.ontology.Uris;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;

/**
 * Settles what each cell of a link column points to, in this order: a URI as written, the identifier
 * of a row of the file whose class fits, the name of existing instances of the expected class. Every
 * answer is kept; what nothing answers to gets close names, from the file and from the instance.
 * <p>
 * Runs once the rows are merged, since whether a row fits depends on every class its sheets give it.
 *
 * @author Arnaud Charleroy
 */
final class LinkResolver {

    /** How much longer or shorter a close name may be: the most edits {@link NameSimilarity} allows. */
    private static final int LENGTH_GAP = 2;

    /**
     * A link cell waiting for its target.
     *
     * @param range the class the property expects; null when the ontology does not say
     */
    record PendingLink(GraphNode node, URI property, URI range, CellRef cell, String raw) {
    }

    private final InterpretationServices services;

    LinkResolver(InterpretationServices services) {
        this.services = services;
    }

    /**
     * @param nodes the merged rows of the file, by key
     * @return the target of each link, in the order given
     */
    List<LinkTarget> resolve(List<PendingLink> links, Map<String, GraphNode> nodes) throws Exception {
        Map<URI, List<Integer>> byRange = new LinkedHashMap<>();
        for (int index = 0; index < links.size(); index++) {
            byRange.computeIfAbsent(links.get(index).range(), range -> new ArrayList<>()).add(index);
        }
        List<LinkTarget> targets = new ArrayList<>(links.size());
        links.forEach(link -> targets.add(null));
        for (Map.Entry<URI, List<Integer>> group : byRange.entrySet()) {
            resolveRange(group.getKey(), group.getValue(), links, nodes, targets);
        }
        return targets;
    }

    private void resolveRange(URI range, List<Integer> indexes, List<PendingLink> links, Map<String, GraphNode> nodes,
                              List<LinkTarget> targets) throws Exception {
        List<String> names = new ArrayList<>();
        for (int index : indexes) {
            String raw = links.get(index).raw();
            if (Uris.asUri(raw).isEmpty()) {
                names.add(raw);
            }
        }
        Map<String, Set<URI>> found = range == null || names.isEmpty()
                ? Map.of()
                : services.instances().lookUp(range, names);

        Set<String> unfound = new LinkedHashSet<>();
        for (int index : indexes) {
            LinkTarget target = target(links.get(index).raw(), range, nodes, found);
            targets.set(index, target);
            if (target.status() == LinkTarget.Status.UNFOUND) {
                unfound.add(links.get(index).raw());
            }
        }
        if (unfound.isEmpty()) {
            return;
        }
        Map<String, List<Suggestion>> close = closeNames(range, unfound, nodes);
        for (int index : indexes) {
            LinkTarget target = targets.get(index);
            List<Suggestion> suggestions = close.get(InstanceIndex.key(links.get(index).raw()));
            if (target.status() == LinkTarget.Status.UNFOUND && suggestions != null) {
                targets.set(index, new LinkTarget(LinkTarget.Status.UNFOUND, null, null, Set.of(), suggestions));
            }
        }
    }

    private LinkTarget target(String raw, URI range, Map<String, GraphNode> nodes, Map<String, Set<URI>> found)
            throws Exception {
        Optional<URI> written = Uris.asUri(raw);
        if (written.isPresent()) {
            String key = written.get().toString();
            return LinkTarget.written(written.get(), nodes.containsKey(key) ? key : null);
        }
        String key = InstanceIndex.key(raw);
        GraphNode row = nodes.get(key);
        if (row != null && !row.isIdentified()) {
            row = null;
        }
        boolean fits = row != null && fits(row, range);
        Set<URI> instances = found.getOrDefault(key, Set.of());
        if (fits) {
            LinkTarget.Status status = instances.isEmpty() ? LinkTarget.Status.ROW : LinkTarget.Status.ROW_AND_INSTANCE;
            return new LinkTarget(status, null, key, instances, List.of());
        }
        if (instances.size() == 1) {
            return new LinkTarget(LinkTarget.Status.INSTANCE, null, null, instances, List.of());
        }
        if (instances.size() > 1) {
            return new LinkTarget(LinkTarget.Status.AMBIGUOUS, null, null, instances, List.of());
        }
        if (row != null) {
            return new LinkTarget(LinkTarget.Status.WRONG_CLASS, null, key, Set.of(), List.of());
        }
        return new LinkTarget(LinkTarget.Status.UNFOUND, null, null, Set.of(), List.of());
    }

    /**
     * Whether a row may be the target: one of its classes is the range or descends from it, all its
     * ancestors counted, as the evidence that suggested the property counts them.
     */
    private boolean fits(GraphNode node, URI range) throws Exception {
        if (range == null) {
            return true;
        }
        for (URI classUri : node.getClasses()) {
            if (services.views().get(classUri).isA(range)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The closest names, rows of the file and visible instances together, for at most
     * {@link GuardedInstanceIndex#NEAR_NAMES} names.
     */
    private Map<String, List<Suggestion>> closeNames(URI range, Set<String> unfound, Map<String, GraphNode> nodes)
            throws Exception {
        List<String> names = unfound.stream().limit(GuardedInstanceIndex.NEAR_NAMES).toList();
        Map<String, List<Suggestion>> byKey = new LinkedHashMap<>();
        if (range != null) {
            services.instances().near(range, names)
                    .forEach((key, suggestions) -> byKey.computeIfAbsent(key, k -> new ArrayList<>()).addAll(suggestions));
        }
        List<GraphNode> candidates = new ArrayList<>();
        for (GraphNode node : nodes.values()) {
            if (node.isIdentified() && node.getUri() == null && fits(node, range)) {
                candidates.add(node);
            }
        }
        for (String name : names) {
            for (GraphNode node : candidates) {
                if (Math.abs(node.getName().length() - name.length()) <= LENGTH_GAP) {
                    OptionalDouble similarity = NameSimilarity.similarity(name, node.getName());
                    if (similarity.isPresent()) {
                        byKey.computeIfAbsent(InstanceIndex.key(name), k -> new ArrayList<>())
                                .add(new Suggestion(null, node.getKey(), node.getName(), similarity.getAsDouble()));
                    }
                }
            }
        }
        Map<String, List<Suggestion>> closest = new LinkedHashMap<>();
        byKey.forEach((key, suggestions) -> closest.put(key, suggestions.stream()
                .sorted(Suggestion.CLOSEST_FIRST)
                .limit(NearMatchFinder.MAX_SUGGESTIONS)
                .toList()));
        return closest;
    }
}
