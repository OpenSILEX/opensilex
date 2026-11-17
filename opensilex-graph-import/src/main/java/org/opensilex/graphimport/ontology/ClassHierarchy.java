//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.ontology;

import java.net.URI;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The {@code rdfs:subClassOf} links between named classes, as read from the triplestore.
 * <p>
 * Whether the triplestore adds the transitive links or not, {@link #ancestorsOf(URI)} gives the same
 * answer: the closure is computed here.
 *
 * @author Arnaud Charleroy
 */
public final class ClassHierarchy {

    private final Map<URI, Set<URI>> parentsByClass = new HashMap<>();

    /** Records that {@code child} is a direct subclass of {@code parent}. A class is never its own parent. */
    public ClassHierarchy link(URI child, URI parent) {
        if (!child.equals(parent)) {
            parentsByClass.computeIfAbsent(child, key -> new HashSet<>()).add(parent);
        }
        return this;
    }

    /** The classes the given one is a direct subclass of. */
    public Set<URI> parentsOf(URI classUri) {
        return Set.copyOf(parentsByClass.getOrDefault(classUri, Set.of()));
    }

    /** Every class above the given one, the class itself excluded. Safe on cycles. */
    public Set<URI> ancestorsOf(URI classUri) {
        Set<URI> ancestors = new HashSet<>();
        Deque<URI> toVisit = new ArrayDeque<>(parentsByClass.getOrDefault(classUri, Set.of()));
        while (!toVisit.isEmpty()) {
            URI next = toVisit.pop();
            if (!next.equals(classUri) && ancestors.add(next)) {
                toVisit.addAll(parentsByClass.getOrDefault(next, Set.of()));
            }
        }
        return ancestors;
    }

    /** True when {@code ancestor} is strictly above {@code classUri}. */
    public boolean isAncestor(URI ancestor, URI classUri) {
        return ancestorsOf(classUri).contains(ancestor);
    }
}
