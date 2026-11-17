//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.graph;

import java.net.URI;
import java.util.Comparator;

/**
 * What a name nothing answers to was probably meant to be: an existing instance, or a row of the
 * file, whose name is close. Offered to the user, never applied.
 *
 * @param uri        the existing instance; null for a row of the file
 * @param nodeKey    the row of the file, as its {@link GraphNode#getKey() key}; null for an instance
 * @param name       the name it answers to
 * @param similarity how close the names are, from 0 to 1
 * @author Arnaud Charleroy
 */
public record Suggestion(URI uri, String nodeKey, String name, double similarity) {

    /** Closest first, then by name, so that the same file always gives the same list. */
    public static final Comparator<Suggestion> CLOSEST_FIRST = Comparator
            .comparingDouble(Suggestion::similarity).reversed()
            .thenComparing(Suggestion::name, String.CASE_INSENSITIVE_ORDER);
}
