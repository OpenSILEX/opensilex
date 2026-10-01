//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.resolve;

import org.junit.Test;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Choosing, among what an instance already has, the few resources a misspelt name meant.
 *
 * @author Arnaud Charleroy
 */
public class NearMatchFinderTest {

    private final NearMatchFinder finder = new NearMatchFinder();

    @Test
    public void theClosestNameComesFirst() {
        List<ResourceReference> suggestions = finder.suggest("Chardonay", Arrays.asList(
                reference("merlot", "Merlot"),
                reference("chardonnay-b", "Chardonnay Blanc"),
                reference("chardonnay", "Chardonnay")));

        assertEquals(1, suggestions.size());
        assertEquals("Chardonnay", suggestions.get(0).getName());
        assertTrue(suggestions.get(0).getSimilarity() > 0.8);
    }

    @Test
    public void noMoreThanThreeAreOffered() {
        List<ResourceReference> suggestions = finder.suggest("Sauvignon blanc", Arrays.asList(
                reference("a", "Sauvignon blanc"),
                reference("b", "Sauvignon blanx"),
                reference("c", "Sauvignon bllanc"),
                reference("d", "Sauvigon blanc"),
                reference("e", "Sauvignonblanc")));

        assertEquals(NearMatchFinder.MAX_SUGGESTIONS, suggestions.size());
        assertEquals("the identical spelling first", 1.0, suggestions.get(0).getSimilarity(), 0.0);
    }

    /**
     * A variable is known by its name and its alternative name, and appears once per name among
     * the candidates. It is offered once, at its closest.
     */
    @Test
    public void aResourceKnownByTwoNamesIsOfferedOnce() {
        List<ResourceReference> suggestions = finder.suggest("Berry_sugar", Arrays.asList(
                reference("v1", "Berry sugar content"),
                reference("v1", "Berry sugr")));

        assertEquals(1, suggestions.size());
        assertEquals("Berry sugr", suggestions.get(0).getName());
    }

    @Test
    public void nothingCloseMeansNoSuggestion() {
        assertTrue(finder.suggest("Chardonay", Arrays.asList(
                reference("merlot", "Merlot"), reference("syrah", "Syrah"))).isEmpty());
    }

    /**
     * The candidates may be shared between several names of the file, so the finder must not
     * write its scores into them.
     */
    @Test
    public void theCandidatesAreLeftUntouched() {
        ResourceReference candidate = reference("chardonnay", "Chardonnay");
        finder.suggest("Chardonay", Arrays.asList(candidate));
        assertNull(candidate.getSimilarity());
    }

    @Test
    public void plotCodesAreNotMatchedBySpelling() {
        assertFalse(ReportCategory.SCIENTIFIC_OBJECTS.allowsNearMatching());
        assertTrue(ReportCategory.GERMPLASM.allowsNearMatching());
        assertTrue(ReportCategory.VARIABLES.allowsNearMatching());
    }

    private ResourceReference reference(String id, String name) {
        return new ResourceReference(URI.create("http://opensilex.test/id/" + id), name);
    }
}
