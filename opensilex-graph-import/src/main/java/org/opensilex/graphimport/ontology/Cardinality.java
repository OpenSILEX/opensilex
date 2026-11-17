//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.ontology;

/**
 * How many values a node may hold for a property: at least {@code min}, at most {@code max}, no upper
 * bound when {@code max} is null.
 * <p>
 * Built from the raw fields of the OWL restrictions rather than from {@code OwlRestrictionModel.isList()},
 * which answers "list" for an exact cardinality of 1.
 *
 * @author Arnaud Charleroy
 */
public record Cardinality(int min, Integer max) {

    /** What a property without any cardinality allows: any number of values, none required. */
    public static final Cardinality ANY = new Cardinality(0, null);

    public Cardinality {
        if (min < 0) {
            throw new IllegalArgumentException("A minimum cardinality cannot be negative: " + min);
        }
    }

    /**
     * Reads the cardinality fields of one restriction. An exact cardinality sets both bounds; the
     * qualified and unqualified forms count the same.
     *
     * @param exact {@code owl:qualifiedCardinality} or {@code owl:cardinality}, null when absent
     * @param min   {@code owl:minQualifiedCardinality} or {@code owl:minCardinality}, null when absent
     * @param max   {@code owl:maxQualifiedCardinality} or {@code owl:maxCardinality}, null when absent
     */
    public static Cardinality of(Integer exact, Integer min, Integer max) {
        Cardinality bounds = ANY;
        if (exact != null) {
            bounds = bounds.intersect(new Cardinality(exact, exact));
        }
        if (min != null) {
            bounds = bounds.intersect(new Cardinality(min, null));
        }
        if (max != null) {
            bounds = bounds.intersect(new Cardinality(0, max));
        }
        return bounds;
    }

    /**
     * What two restrictions allow together: OWL applies both, so the larger minimum and the smaller
     * maximum hold. The result may allow nothing (see {@link #isSatisfiable()}).
     */
    public Cardinality intersect(Cardinality other) {
        Integer upper;
        if (max == null) {
            upper = other.max;
        } else if (other.max == null) {
            upper = max;
        } else {
            upper = Math.min(max, other.max);
        }
        return new Cardinality(Math.max(min, other.min), upper);
    }

    /** True when a node may hold several values, so that two sheets giving two values add up. */
    public boolean isMultiValued() {
        return max == null || max > 1;
    }

    public boolean isRequired() {
        return min >= 1;
    }

    /** False when the bounds contradict each other, as two restrictions min 2 and max 1 do. */
    public boolean isSatisfiable() {
        return max == null || min <= max;
    }

    public boolean accepts(int count) {
        return count >= min && (max == null || count <= max);
    }

    @Override
    public String toString() {
        return "[" + min + ".." + (max == null ? "*" : max) + "]";
    }
}
