//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.ontology;

import org.junit.Test;
import org.opensilex.core.ontology.Oeso;

import java.net.URI;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

/**
 * Headers written as predicates are recognised by their form alone.
 *
 * @author Arnaud Charleroy
 */
public class HeaderResolverTest {

    private static final String RDFS = "http://www.w3.org/2000/01/rdf-schema#";
    private static final String RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";

    private final HeaderResolver resolver = new HeaderResolver(Map.of("dev", "http://dev.opensilex.org/"));

    @Test
    public void aFullUriNamesItsProperty() {
        assertNames(HeaderReading.Form.FULL_URI, Oeso.NS + "hasCreationDate", null,
                resolver.read(" " + Oeso.NS + "hasCreationDate "));
        assertNames(HeaderReading.Form.FULL_URI, Oeso.NS + "isPartOf", null,
                resolver.read("<" + Oeso.NS + "isPartOf>"));
    }

    @Test
    public void bothPrefixesOfTheVocabularyAreRecognised() {
        assertNames(HeaderReading.Form.PREFIXED_NAME, Oeso.NS + "hasFactorLevel", null,
                resolver.read("oeso:hasFactorLevel"));
        assertNames(HeaderReading.Form.PREFIXED_NAME, Oeso.NS + "hasFactorLevel", null,
                resolver.read("vocabulary:hasFactorLevel"));
    }

    @Test
    public void thePlatformPrefixesAreRecognisedWhateverTheirCase() {
        assertNames(HeaderReading.Form.PREFIXED_NAME, "http://dev.opensilex.org/so-1", null, resolver.read("dev:so-1"));
        assertNames(HeaderReading.Form.PREFIXED_NAME, RDFS + "comment", null, resolver.read("RDFS:comment"));
    }

    @Test
    public void aLanguageSuffixIsKeptApart() {
        assertNames(HeaderReading.Form.PREFIXED_NAME, RDFS + "label", "fr", resolver.read("rdfs:label@fr"));
        assertNames(HeaderReading.Form.FULL_URI, RDFS + "comment", "en-gb",
                resolver.read("<" + RDFS + "comment>@en-GB"));
    }

    @Test
    public void theTypeColumnIsRecognised() {
        assertEquals(HeaderReading.Form.TYPE, resolver.read("rdf:type").form());
        assertEquals(HeaderReading.Form.TYPE, resolver.read("a").form());
        assertEquals(URI.create(RDF_TYPE), resolver.read("<" + RDF_TYPE + ">").property());
    }

    @Test
    public void anUnknownPrefixIsReported() {
        HeaderReading reading = resolver.read("ex:height");

        assertEquals(HeaderReading.Form.UNKNOWN_PREFIX, reading.form());
        assertEquals("ex", reading.prefix());
        assertFalse(reading.names());
    }

    @Test
    public void anOrdinaryHeaderIsLeftToTheEvidence() {
        for (String header : new String[]{"Hauteur (cm)", "plot id", "", "   ", "contact@inrae.fr", "type"}) {
            HeaderReading reading = resolver.read(header);
            assertEquals(header, HeaderReading.Form.PLAIN, reading.form());
            assertNull(reading.property());
        }
        assertEquals(HeaderReading.Form.PLAIN, resolver.read(null).form());
    }

    @Test
    public void aSuffixOnAnOrdinaryHeaderIsNotALanguage() {
        HeaderReading reading = resolver.read("note@fr");

        assertEquals(HeaderReading.Form.PLAIN, reading.form());
        assertNull(reading.language());
    }

    private static void assertNames(HeaderReading.Form form, String property, String language, HeaderReading reading) {
        assertEquals(form, reading.form());
        assertEquals(URI.create(property), reading.property());
        assertEquals(language, reading.language());
    }
}
