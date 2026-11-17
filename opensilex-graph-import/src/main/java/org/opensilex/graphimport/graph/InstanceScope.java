//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.graph;

import java.net.URI;
import java.util.Collection;
import java.util.Set;

/**
 * The instances of one class, and its subclasses, that a user may find by name, with the platform's
 * rules on who sees what.
 * <p>
 * An instance is looked up only under a scope its own type descends from: an instance under no scope
 * is never found, whatever the class a column expects. A module adds a scope through
 * {@link org.opensilex.graphimport.GraphImportExtension}.
 *
 * @author Arnaud Charleroy
 */
public interface InstanceScope {

    /** The class whose instances, subclasses included, this scope covers. */
    URI rootClass();

    /** Whether the instances can be looked up at all in this context. */
    default boolean canLookUp(LookupContext context) {
        return true;
    }

    /** The named graph the instances live in, or null when they may live in any graph. */
    default URI graph(LookupContext context) {
        return null;
    }

    /**
     * Keeps the instances the user may see. Never throws for an instance the user may not see: it is
     * left out, so that whether it exists is not revealed.
     *
     * @param found instances found by name, as full URIs
     * @return those the user may see, as full URIs
     */
    Set<URI> visible(Collection<URI> found, LookupContext context) throws Exception;
}
