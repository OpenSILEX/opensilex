//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.interpret;

import org.opensilex.graphimport.ontology.ClassHierarchy;
import org.opensilex.graphimport.ontology.Labels;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * The classes a sheet may hold: the root classes the assistant looks instances up in, and all their
 * subclasses, with their labels and their subclass links.
 *
 * @author Arnaud Charleroy
 */
public final class ClassCatalog {

    private final Set<URI> roots;
    private final Map<URI, Labels> labels;
    private final ClassHierarchy hierarchy;

    public ClassCatalog(Set<URI> roots, Map<URI, Labels> labels, ClassHierarchy hierarchy) {
        this.roots = Set.copyOf(roots);
        this.labels = Map.copyOf(labels);
        this.hierarchy = hierarchy;
    }

    public Set<URI> getRoots() {
        return roots;
    }

    /** Every class of the catalog, in URI order. */
    public Set<URI> getClasses() {
        TreeSet<URI> classes = new TreeSet<>(labels.keySet());
        classes.addAll(roots);
        return classes;
    }

    public boolean contains(URI classUri) {
        return roots.contains(classUri) || labels.containsKey(classUri);
    }

    public Labels labelsOf(URI classUri) {
        return labels.getOrDefault(classUri, Labels.NONE);
    }

    /** The label in the language, else the local name of the URI. */
    public String labelOf(URI classUri, String language) {
        String label = labelsOf(classUri).in(language);
        return label != null ? label : localName(classUri);
    }

    /** True when {@code classUri} is {@code ancestor} or descends from it. */
    public boolean isA(URI classUri, URI ancestor) {
        return classUri.equals(ancestor) || hierarchy.isAncestor(ancestor, classUri);
    }

    /** The classes of the catalog that are direct subclasses of the given one, in URI order. */
    public Set<URI> childrenOf(URI classUri) {
        Set<URI> children = new TreeSet<>();
        for (URI candidate : getClasses()) {
            if (!candidate.equals(classUri) && hierarchy.parentsOf(candidate).contains(classUri)) {
                children.add(candidate);
            }
        }
        return children;
    }

    /** The deepest class every given class is or descends from, or null when they share none. */
    public URI commonClass(Set<URI> classes) {
        URI deepest = null;
        int deepestDepth = -1;
        for (URI candidate : getClasses()) {
            boolean common = classes.stream().allMatch(classUri -> isA(classUri, candidate));
            int depth = hierarchy.ancestorsOf(candidate).size();
            if (common && depth > deepestDepth) {
                deepest = candidate;
                deepestDepth = depth;
            }
        }
        return deepest;
    }

    /** The part of a URI after its last {@code #} or {@code /}. */
    public static String localName(URI uri) {
        String text = uri.toString();
        int cut = Math.max(text.lastIndexOf('#'), text.lastIndexOf('/'));
        return cut >= 0 ? text.substring(cut + 1) : text;
    }
}
