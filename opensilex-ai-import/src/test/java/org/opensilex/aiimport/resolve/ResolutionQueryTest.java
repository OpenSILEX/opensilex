//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.resolve;

import org.junit.Test;

import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The regular expressions the resolution sends to the triplestore.
 *
 * @author Arnaud Charleroy
 */
public class ResolutionQueryTest {

    /**
     * A misspelling spoils one or two three-letter fragments of a name, never all of them, so the
     * correct spelling is among what the fragments find.
     */
    @Test
    public void theFragmentsOfAMisspeltNameFindItsCorrectSpelling() {
        String pattern = InstanceLookups.fragmentPattern("Chardonay");

        assertTrue(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher("Chardonnay").find());
        assertFalse(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher("Merlot").find());
    }

    @Test
    public void aNameTooShortToBeMatchedBySpellingIsNotSearchedAtAll() {
        assertNull(InstanceLookups.fragmentPattern("A1"));
    }

    /**
     * Escaped character by character: the quoting form of Java regular expressions is not part of
     * the XPath regular expressions SPARQL specifies.
     */
    @Test
    public void aLiteralIsEscapedPortably() {
        String escaped = InstanceLookups.escapeRegex("j.dupont+vigne@inrae.fr");

        assertFalse(escaped.contains("\\Q"));
        assertEquals("j\\.dupont\\+vigne@inrae\\.fr", escaped);
        assertTrue(Pattern.compile(escaped).matcher("j.dupont+vigne@inrae.fr").matches());
        assertFalse(Pattern.compile(escaped).matcher("jXdupont+vigne@inrae.fr").matches());
    }
}
