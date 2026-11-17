//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.graph;

import java.net.URI;

/**
 * A value an existing instance holds for a property: a resource, or a literal as the triplestore
 * stores it.
 *
 * @param resource the resource it points to, as a full URI; null for a literal
 * @param lexical  the literal's lexical form; null for a resource
 * @param datatype the literal's datatype; null for a resource or a plain literal
 * @param language the literal's language tag; null without one
 * @author Arnaud Charleroy
 */
public record StoredValue(URI resource, String lexical, URI datatype, String language) {

    public static StoredValue resource(URI resource) {
        return new StoredValue(resource, null, null, null);
    }

    public static StoredValue literal(String lexical, URI datatype, String language) {
        return new StoredValue(null, lexical, datatype, language == null || language.isEmpty() ? null : language);
    }

    public boolean isResource() {
        return resource != null;
    }

    /** What the user is shown of it. */
    public String shown() {
        if (isResource()) {
            return resource.toString();
        }
        return language == null ? lexical : lexical + "@" + language;
    }
}
