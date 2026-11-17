//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.ontology;

import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Recognises a column header written as a predicate: a full URI, a prefixed name, {@code rdf:type},
 * each with an optional {@code @fr} language suffix. Such a header needs no suggestion.
 * <p>
 * The prefixes are those of the platform plus a few local aliases ({@code oeso:} and
 * {@code vocabulary:} for the OpenSILEX vocabulary, {@code rdf:}, {@code rdfs:}, {@code xsd:}), which
 * are never added to the platform's own table.
 *
 * @author Arnaud Charleroy
 */
public class HeaderResolver {

    /** The prefix people write for the OpenSILEX vocabulary, which the platform registers as {@code vocabulary:}. */
    public static final String OESO_ALIAS = "oeso";

    private static final Pattern LANGUAGE_SUFFIX = Pattern.compile("^(.+)@([a-zA-Z]{2,3}(?:-[a-zA-Z0-9]{2,8})*)$");
    private static final Pattern FULL_URI = Pattern.compile("^<?((?:https?|urn):[^\\s<>]+)>?$");
    private static final Pattern PREFIXED_NAME = Pattern.compile("^([A-Za-z][\\w.-]*)?:([\\w][\\w.-]*)$");
    private static final String TYPE_SHORTHAND = "a";

    private final Map<String, String> namespaces;

    /** @param prefixes prefix to namespace, as the platform holds them */
    public HeaderResolver(Map<String, String> prefixes) {
        Map<String, String> all = new HashMap<>(prefixes);
        all.putIfAbsent(OESO_ALIAS, Oeso.NS);
        all.putIfAbsent("vocabulary", Oeso.NS);
        all.putIfAbsent("rdf", RDF.getURI());
        all.putIfAbsent("rdfs", RDFS.getURI());
        all.putIfAbsent("xsd", XSD.NS);
        this.namespaces = Map.copyOf(all);
    }

    /** A resolver on the prefixes the platform knows now. */
    public static HeaderResolver ofPlatformPrefixes() {
        return new HeaderResolver(SPARQLService.getPrefixes());
    }

    public HeaderReading read(String header) {
        if (header == null || header.isBlank()) {
            return HeaderReading.plain();
        }
        String text = header.trim();
        Matcher suffix = LANGUAGE_SUFFIX.matcher(text);
        if (suffix.matches()) {
            String language = suffix.group(2).toLowerCase(Locale.ROOT);
            HeaderReading withoutSuffix = readPredicate(suffix.group(1).trim(), language);
            if (withoutSuffix.names()) {
                return withoutSuffix;
            }
        }
        return readPredicate(text, null);
    }

    private HeaderReading readPredicate(String text, String language) {
        if (TYPE_SHORTHAND.equals(text)) {
            return new HeaderReading(HeaderReading.Form.TYPE, URI.create(RDF.type.getURI()), null, null);
        }
        Matcher full = FULL_URI.matcher(text);
        if (full.matches()) {
            return named(HeaderReading.Form.FULL_URI, full.group(1), language);
        }
        Matcher prefixed = PREFIXED_NAME.matcher(text);
        if (!prefixed.matches()) {
            return HeaderReading.plain();
        }
        String prefix = prefixed.group(1) == null ? "" : prefixed.group(1);
        String namespace = namespaces.get(prefix);
        if (namespace == null) {
            namespace = namespaces.get(prefix.toLowerCase(Locale.ROOT));
        }
        if (namespace == null) {
            return new HeaderReading(HeaderReading.Form.UNKNOWN_PREFIX, null, null, prefix);
        }
        return named(HeaderReading.Form.PREFIXED_NAME, namespace + prefixed.group(2), language);
    }

    private static HeaderReading named(HeaderReading.Form form, String uri, String language) {
        URI property;
        try {
            property = URI.create(uri);
        } catch (IllegalArgumentException e) {
            return HeaderReading.plain();
        }
        if (property.toString().equals(RDF.type.getURI())) {
            return new HeaderReading(HeaderReading.Form.TYPE, property, null, null);
        }
        return new HeaderReading(form, property, language, null);
    }
}
