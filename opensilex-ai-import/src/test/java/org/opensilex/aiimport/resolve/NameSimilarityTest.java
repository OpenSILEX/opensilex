//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.resolve;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * When two names are close enough for one to be a misspelling of the other — and, just as much,
 * when they are not.
 * <p>
 * The cases on the refusing side matter more than the others. A suggestion is only a suggestion,
 * but a user in a hurry clicks it, and a plot or a clone filed against its neighbour is data
 * nobody will ever find to be wrong.
 *
 * @author Arnaud Charleroy
 */
public class NameSimilarityTest {

    @Test
    public void aMissingLetterIsRecognised() {
        assertTrue(NameSimilarity.similarity("Chardonay", "Chardonnay").isPresent());
    }

    /**
     * The commonest slip of the fingers, and the case plain Levenshtein gets wrong: two swapped
     * neighbours are one edit here, two there, and an eight-letter name tolerates one.
     */
    @Test
    public void aSwapOfTwoNeighboursCountsOnce() {
        assertEquals(1, NameSimilarity.distance("genotpye", "genotype", 2));
        assertTrue(NameSimilarity.similarity("Genotpye", "Genotype").isPresent());
    }

    @Test
    public void caseAccentsAndSeparatorsAreNotDifferences() {
        assertEquals(1.0, NameSimilarity.similarity("Pinot_Noir", "pinot noir").getAsDouble(), 0.0);
        assertEquals(1.0, NameSimilarity.similarity("Cépage", "cepage").getAsDouble(), 0.0);
    }

    /**
     * {@code A1} and {@code A10} are one edit apart and are two plots. Short names leave no room to
     * tell a typo from a different name, so they are compared exactly.
     */
    @Test
    public void shortNamesAreComparedExactly() {
        assertFalse(NameSimilarity.similarity("A1", "A10").isPresent());
        assertFalse(NameSimilarity.similarity("Bloc", "Blic").isPresent());
    }

    /**
     * A number in a name is an identifier; a different number is a different thing.
     */
    @Test
    public void namesThatDifferByADigitAreDifferentThings() {
        assertFalse(NameSimilarity.similarity("Clone 115", "Clone 116").isPresent());
        assertFalse(NameSimilarity.similarity("Essai 2019", "Essai 2020").isPresent());
        // The same number, misspelt around it, is still a typo.
        assertTrue(NameSimilarity.similarity("Chardonay 2019", "Chardonnay 2019").isPresent());
    }

    @Test
    public void theToleranceGrowsWithTheLength() {
        assertEquals(0, NameSimilarity.allowedEdits(4));
        assertEquals(1, NameSimilarity.allowedEdits(5));
        assertEquals(1, NameSimilarity.allowedEdits(8));
        assertEquals(2, NameSimilarity.allowedEdits(9));

        // Two slips in a long name are forgiven; in a short one they make another word.
        assertTrue(NameSimilarity.similarity("Sauvignnon blnc", "Sauvignon blanc").isPresent());
        assertFalse(NameSimilarity.similarity("Merlto", "Merlot noir").isPresent());
    }

    @Test
    public void closerNamesScoreHigher() {
        double oneSlip = NameSimilarity.similarity("Chardonay", "Chardonnay").getAsDouble();
        double twoSlips = NameSimilarity.similarity("Chardonai", "Chardonnay").getAsDouble();
        assertTrue(oneSlip > twoSlips);
    }

    /**
     * The bound is what makes comparing a name against thousands of candidates affordable: the
     * computation stops as soon as the answer is known to be "too far".
     */
    @Test
    public void theDistanceStopsOnceItIsTooFar() {
        assertEquals(3, NameSimilarity.distance("vitis", "malus domestica", 2));
        assertEquals(3, NameSimilarity.distance("abcdef", "uvwxyz", 2));
    }

    @Test
    public void anEmptyNameIsNeverClose() {
        assertFalse(NameSimilarity.similarity("", "Chardonnay").isPresent());
        assertFalse(NameSimilarity.similarity("---", "Chardonnay").isPresent());
    }
}
