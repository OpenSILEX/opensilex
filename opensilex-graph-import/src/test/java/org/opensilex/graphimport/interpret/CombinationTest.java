//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.interpret;

import org.junit.Test;
import org.opensilex.aiimport.report.ReportMessage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Scores are sums of readable weights, and a suggestion is taken only with a clear lead.
 *
 * @author Arnaud Charleroy
 */
public class CombinationTest {

    private static final ReportMessage REASON = ReportMessage.plain("because");
    private static final Combination.Bar BAR = new Combination.Bar(0.75, 0.2);

    @Test
    public void theScoreIsTheSumOfTheWeightsBoundedToOne() {
        List<Candidate<String>> ranked = Combination.rank(
                List.of(clue("a", 0.7), clue("a", 0.5), clue("b", 0.4)),
                List.of(clue("b", -0.6)),
                Comparator.naturalOrder());

        assertEquals("a", ranked.get(0).target());
        assertEquals(1.0, ranked.get(0).score(), 1e-9);
        assertEquals(2, ranked.get(0).evidence().size());
        assertEquals(0.0, ranked.get(1).score(), 1e-9);
    }

    @Test
    public void aModulationNeverMakesACandidate() {
        List<Candidate<String>> ranked = Combination.rank(
                List.of(clue("a", 0.5)), List.of(clue("a", 0.1), clue("z", 0.9)), Comparator.naturalOrder());

        assertEquals(1, ranked.size());
        assertEquals(0.6, ranked.get(0).score(), 1e-9);
    }

    @Test
    public void tiesFollowTheGivenOrder() {
        List<Candidate<String>> ranked = Combination.rank(
                List.of(clue("b", 0.5), clue("a", 0.5)), List.of(), Comparator.naturalOrder());

        assertEquals("a", ranked.get(0).target());
        assertFalse(BAR.isClearedBy(ranked));
    }

    @Test
    public void theBarNeedsAScoreAndALead() {
        assertTrue(BAR.isClearedBy(Combination.rank(List.of(clue("a", 0.8)), List.of(), Comparator.naturalOrder())));
        assertFalse(BAR.isClearedBy(Combination.rank(List.of(clue("a", 0.7)), List.of(), Comparator.naturalOrder())));
        assertFalse(BAR.isClearedBy(Combination.rank(
                List.of(clue("a", 0.9), clue("b", 0.8)), List.of(), Comparator.naturalOrder())));
        assertFalse(BAR.isClearedBy(List.of()));
    }

    @Test
    public void atMostFiveCandidatesAreKept() {
        List<Clue<String>> clues = new ArrayList<>();
        for (String target : List.of("a", "b", "c", "d", "e", "f", "g")) {
            clues.add(clue(target, 0.3));
        }

        assertEquals(Combination.MAX_CANDIDATES, Combination.rank(clues, List.of(), Comparator.naturalOrder()).size());
    }

    private static Clue<String> clue(String target, double weight) {
        return Clue.of(target, "test", weight, REASON);
    }
}
