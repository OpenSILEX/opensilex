//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.interpret;

import org.apache.jena.vocabulary.RDFS;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Header words that the ontology's labels do not cover: {@code rdfs:label} and {@code rdfs:comment}
 * have no label in the OpenSILEX vocabulary, yet "nom" is the most common column of all. Words are in
 * {@code HeaderMatcher.normalize} form.
 *
 * @author Arnaud Charleroy
 */
public final class CommonHeaderWords {

    private static final URI NAME = URI.create(RDFS.label.getURI());
    private static final URI COMMENT = URI.create(RDFS.comment.getURI());

    private static final Map<String, URI> PROPERTY_BY_WORD = Map.ofEntries(
            Map.entry("nom", NAME), Map.entry("name", NAME), Map.entry("label", NAME),
            Map.entry("libelle", NAME), Map.entry("intitule", NAME), Map.entry("designation", NAME),
            Map.entry("commentaire", COMMENT), Map.entry("commentaires", COMMENT), Map.entry("comment", COMMENT),
            Map.entry("comments", COMMENT), Map.entry("description", COMMENT), Map.entry("remarque", COMMENT),
            Map.entry("remarques", COMMENT), Map.entry("note", COMMENT), Map.entry("notes", COMMENT));

    private static final Set<String> IDENTIFIER_WORDS = Set.of(
            "id", "identifiant", "identifier", "uri", "code", "ref", "reference",
            "nom", "name", "label", "libelle");

    private CommonHeaderWords() {
    }

    /** The property a common header word names. */
    public static Optional<URI> propertyFor(String normalizedHeader) {
        return Optional.ofNullable(PROPERTY_BY_WORD.get(normalizedHeader));
    }

    /** True when the header is a word people give the column identifying the rows. */
    public static boolean namesIdentifier(String normalizedHeader) {
        return IDENTIFIER_WORDS.contains(normalizedHeader);
    }
}
