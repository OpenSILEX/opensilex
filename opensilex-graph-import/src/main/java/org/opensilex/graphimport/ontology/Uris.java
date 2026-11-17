//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.ontology;

import org.opensilex.sparql.deserializer.URIDeserializer;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Optional;

/**
 * One form for every URI the module handles: the full one. The platform hands out prefixed URIs or
 * full ones depending on its configuration and on the call; two forms of one URI must compare equal.
 *
 * @author Arnaud Charleroy
 */
public final class Uris {

    private Uris() {
    }

    public static URI expand(String uri) {
        return URI.create(URIDeserializer.getExpandedURI(uri));
    }

    public static URI expand(URI uri) {
        return expand(uri.toString());
    }

    /**
     * A cell read as a URI: written in full ({@code http://…}, {@code urn:…}) or with a prefix the platform
     * knows. A name such as {@code P1} or {@code Plot:12} is not one.
     *
     * @return the URI in its full form, or empty when the cell is a name
     */
    public static Optional<URI> asUri(String cell) {
        if (cell == null || cell.isBlank() || cell.chars().anyMatch(Character::isWhitespace)) {
            return Optional.empty();
        }
        String text = cell.startsWith("<") && cell.endsWith(">") ? cell.substring(1, cell.length() - 1) : cell;
        try {
            String expanded = URIDeserializer.getExpandedURI(text);
            String lower = expanded.toLowerCase(Locale.ROOT);
            if (lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("urn:")) {
                return Optional.of(new URI(expanded));
            }
        } catch (URISyntaxException | IllegalArgumentException e) {
            // A cell that only looks like a URI is a name.
        }
        return Optional.empty();
    }
}
