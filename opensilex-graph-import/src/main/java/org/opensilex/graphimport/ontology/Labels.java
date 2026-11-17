//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.ontology;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * The {@code rdfs:label} of a class or a property in each language it has, the empty key holding the
 * label without a language tag.
 *
 * @author Arnaud Charleroy
 */
public record Labels(Map<String, String> byLanguage) {

    public static final String NO_LANGUAGE = "";

    /** The language a label falls back to after the asked one and the untagged one. */
    private static final String FALLBACK_LANGUAGE = "en";

    public static final Labels NONE = new Labels(Map.of());

    public Labels {
        // Sorted by language, so that every walk over the labels sees them in the same order.
        byLanguage = Collections.unmodifiableMap(new TreeMap<>(byLanguage));
    }

    /**
     * The label in the given language, else the untagged one, else the English one, else null. The
     * same rule on every ontology store, since the labels are read from the triplestore itself.
     */
    public String in(String language) {
        String label = language == null ? null : byLanguage.get(language);
        if (label == null) {
            label = byLanguage.get(NO_LANGUAGE);
        }
        if (label == null) {
            label = byLanguage.get(FALLBACK_LANGUAGE);
        }
        return label;
    }

    /** Every label, whatever its language, for matching a column header against them. */
    public Collection<String> all() {
        return byLanguage.values();
    }
}
