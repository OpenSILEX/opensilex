//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.graph;

import org.opensilex.aiimport.workbook.ExcelValueParser;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Finds the existing instances a list of names designates.
 *
 * @author Arnaud Charleroy
 */
public interface InstanceIndex {

    /**
     * @param expectedClass the class the instances must belong to, subclasses included
     * @param names         names as written in a file
     * @return for each name that designates at least one visible instance, keyed by
     *         {@link #key(String)}, the instances it designates
     */
    Map<String, Set<URI>> lookUp(URI expectedClass, Collection<String> names) throws Exception;

    /**
     * The visible instances whose name is close to names nothing answers to, as "did you mean …?".
     *
     * @param expectedClass the class the instances must belong to, subclasses included
     * @param names         names no instance answers to
     * @return for each name with close instances, keyed by {@link #key(String)}, the closest first; an
     *         index that cannot tell answers nothing
     */
    default Map<String, List<Suggestion>> near(URI expectedClass, Collection<String> names) throws Exception {
        return Map.of();
    }

    /**
     * The instances among these URIs that exist and that the user may see, whatever their class.
     *
     * @param uris URIs as written in a file, full or prefixed
     * @return for each visible instance, as a full URI, its types; an instance the user may not see is
     *         left out, so that whether it exists is not revealed
     */
    default Map<URI, Set<URI>> existing(Collection<URI> uris) throws Exception {
        return Map.of();
    }

    /**
     * What instances already hold for some properties. Only instances this index found visible, by
     * {@link #lookUp} or {@link #existing}, are read: any other is ignored.
     *
     * @param instances  instances, as full URIs
     * @param properties properties, as full URIs
     * @return the values by instance, then by property
     */
    default Map<URI, Map<URI, List<StoredValue>>> storedValues(Collection<URI> instances, Collection<URI> properties)
            throws Exception {
        return Map.of();
    }

    /** The form a name is compared in: trimmed of invisible characters, lowercase. */
    static String key(String name) {
        return ExcelValueParser.clean(name).toLowerCase(Locale.ROOT);
    }
}
