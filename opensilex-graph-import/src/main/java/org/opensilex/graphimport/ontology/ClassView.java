//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.ontology;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * What a class accepts: its properties, each with its kind, range and cardinality.
 * <p>
 * The restrictions of the class and of its ancestors are combined by two rules:
 * <ul>
 *     <li>the nearest class wins: a restriction of the class on a property replaces those of its
 *     ancestors on the same property, as the platform's forms expect;</li>
 *     <li>restrictions of the same rank add up: two restrictions of one class on one property (a
 *     minimum and a maximum written apart), or of two parents that do not descend from each other,
 *     all apply.</li>
 * </ul>
 * Immutable; built for one import and dropped with it, since an ontology can change at runtime.
 *
 * @author Arnaud Charleroy
 */
public final class ClassView {

    private static final Comparator<RestrictionRow> BY_DOMAIN_THEN_RANGE = Comparator
            .comparing((RestrictionRow row) -> row.domain().toString())
            .thenComparing(row -> Objects.toString(row.range(), ""));

    private final URI uri;
    private final Labels labels;
    private final Set<URI> ancestors;
    private final Map<URI, PropertyView> properties;

    private ClassView(URI uri, Labels labels, Set<URI> ancestors, Map<URI, PropertyView> properties) {
        this.uri = uri;
        this.labels = labels;
        this.ancestors = Set.copyOf(ancestors);
        this.properties = properties;
    }

    /**
     * Combines what was read from the triplestore into the view of one class.
     *
     * @param classUri     the class, as a full URI
     * @param hierarchy    the subclass links of the class and its ancestors
     * @param restrictions the restrictions of the class and of its ancestors, in any order
     * @param labels       the labels of the class and of the properties, by URI
     */
    public static ClassView assemble(
            URI classUri,
            ClassHierarchy hierarchy,
            Collection<RestrictionRow> restrictions,
            Map<URI, Labels> labels) {

        Set<URI> ancestors = hierarchy.ancestorsOf(classUri);
        Set<URI> scope = new HashSet<>(ancestors);
        scope.add(classUri);

        Map<URI, List<RestrictionRow>> rowsByProperty = restrictions.stream()
                .filter(row -> scope.contains(row.domain()))
                .collect(Collectors.groupingBy(RestrictionRow::property));

        Map<URI, PropertyView> properties = new TreeMap<>(Comparator.comparing(URI::toString));
        rowsByProperty.forEach((property, rows) -> {
            PropertyView view = combine(property, nearest(rows, hierarchy), labels.getOrDefault(property, Labels.NONE));
            if (view != null) {
                properties.put(property, view);
            }
        });
        return new ClassView(classUri, labels.getOrDefault(classUri, Labels.NONE), ancestors,
                new LinkedHashMap<>(properties));
    }

    /** The rows of the classes no other restricting class descends from. */
    private static List<RestrictionRow> nearest(List<RestrictionRow> rows, ClassHierarchy hierarchy) {
        Set<URI> domains = rows.stream().map(RestrictionRow::domain).collect(Collectors.toSet());
        Set<URI> overridden = new HashSet<>();
        for (URI domain : domains) {
            overridden.addAll(hierarchy.ancestorsOf(domain));
        }
        List<RestrictionRow> kept = new ArrayList<>();
        for (RestrictionRow row : rows) {
            if (!overridden.contains(row.domain())) {
                kept.add(row);
            }
        }
        kept.sort(BY_DOMAIN_THEN_RANGE);
        return kept;
    }

    /**
     * One property from the restrictions that apply to it. When two of them give two ranges, the one
     * of the first class in URI order is kept, so that the same ontology always gives the same view.
     *
     * @return null when nothing tells whether the property holds literals or links
     */
    private static PropertyView combine(URI property, List<RestrictionRow> rows, Labels labels) {
        Cardinality cardinality = Cardinality.ANY;
        PropertyKind kind = null;
        URI range = null;
        Set<URI> declaredOn = new HashSet<>();
        for (RestrictionRow row : rows) {
            cardinality = cardinality.intersect(row.cardinality());
            declaredOn.add(row.domain());
            if (kind == null) {
                kind = row.kind();
            }
            if (range == null) {
                range = row.range();
            }
        }
        if (kind == null) {
            return null;
        }
        return new PropertyView(property, kind, range, cardinality, declaredOn, labels);
    }

    public URI getUri() {
        return uri;
    }

    public Labels getLabels() {
        return labels;
    }

    /** Every class above this one. */
    public Set<URI> getAncestors() {
        return ancestors;
    }

    /** True when this class is the given one or descends from it. */
    public boolean isA(URI classUri) {
        return uri.equals(classUri) || ancestors.contains(classUri);
    }

    /** The accepted properties, in URI order. */
    public Collection<PropertyView> getProperties() {
        return properties.values();
    }

    /** The property with that full URI, or null when the class does not accept it. */
    public PropertyView getProperty(URI propertyUri) {
        return properties.get(propertyUri);
    }
}
