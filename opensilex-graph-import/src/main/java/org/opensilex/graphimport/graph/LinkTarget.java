//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.graph;

import java.net.URI;
import java.util.List;
import java.util.Set;

/**
 * What a cell of a link column points to. Every answer found is kept: when a row of the file and an
 * existing instance share the name, both are, since the row may be that instance being completed;
 * the preview judges. What nothing answers to carries the close names instead.
 *
 * @param status      how the cell was resolved
 * @param uri         the URI the cell is written as, null otherwise
 * @param nodeKey     the row of the file it names, null when none does
 * @param instances   the existing instances it names, empty when none does
 * @param suggestions close names, for a cell nothing answers to
 * @author Arnaud Charleroy
 */
public record LinkTarget(Status status, URI uri, String nodeKey, Set<URI> instances, List<Suggestion> suggestions) {

    /** How a cell was resolved. */
    public enum Status {
        /** Written as a URI. */
        URI,
        /** The identifier of a row of the file whose class fits. */
        ROW,
        /** The name of one existing instance of the expected class. */
        INSTANCE,
        /** The identifier of a row of the file and the name of existing instances at once. */
        ROW_AND_INSTANCE,
        /** The name of several existing instances, and of no row. */
        AMBIGUOUS,
        /** The identifier of a row of the file whose class does not fit, and of nothing else. */
        WRONG_CLASS,
        /** Nothing answers to it. */
        UNFOUND
    }

    public LinkTarget {
        instances = Set.copyOf(instances);
        suggestions = List.copyOf(suggestions);
    }

    public static LinkTarget written(URI uri, String nodeKey) {
        return new LinkTarget(Status.URI, uri, nodeKey, Set.of(), List.of());
    }

    /** True when the user has nothing to settle. */
    public boolean isResolved() {
        return status == Status.URI || status == Status.ROW || status == Status.INSTANCE
                || status == Status.ROW_AND_INSTANCE;
    }

    /**
     * What two cells pointing to the same thing share, so that the same link given by two sheets counts
     * once. A cell nothing answers to has none.
     */
    public String identity() {
        return switch (status) {
            case URI -> uri.toString();
            case ROW, ROW_AND_INSTANCE -> nodeKey;
            case INSTANCE -> instances.iterator().next().toString();
            default -> null;
        };
    }
}
