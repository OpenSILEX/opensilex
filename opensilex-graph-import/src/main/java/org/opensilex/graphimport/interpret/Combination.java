//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.interpret;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Adds the evidence up, so that the user can read the score of a suggestion as the sum of its reasons.
 * <p>
 * Only a proposing clue makes a candidate. A modulating clue, such as "the values are dates", adds to
 * or takes from a candidate something else proposed, and never brings one by itself: every value
 * reads as text, so a text property would otherwise be proposed for every column.
 *
 * @author Arnaud Charleroy
 */
public final class Combination {

    /** How many candidates a choice keeps for the user to pick from. */
    public static final int MAX_CANDIDATES = 5;

    private Combination() {
    }

    /**
     * A score a suggestion must reach, and the lead it must have over the next one, to be taken
     * without asking.
     */
    public record Bar(double threshold, double margin) {

        /** Whether the best candidate of a ranked list clears the bar. */
        public boolean isClearedBy(List<? extends Candidate<?>> ranked) {
            if (ranked.isEmpty() || ranked.get(0).score() < threshold) {
                return false;
            }
            return ranked.size() == 1 || ranked.get(0).score() - ranked.get(1).score() >= margin;
        }
    }

    /**
     * @param proposals   clues that make candidates
     * @param modulations clues that only weigh on candidates the proposals made
     * @param order       breaks ties, so that the same file always gives the same ranking
     * @return the candidates, best first, at most {@link #MAX_CANDIDATES}
     */
    public static <T> List<Candidate<T>> rank(
            Collection<Clue<T>> proposals,
            Collection<Clue<T>> modulations,
            Comparator<T> order) {

        Map<T, List<Evidence>> byTarget = new LinkedHashMap<>();
        for (Clue<T> clue : proposals) {
            byTarget.computeIfAbsent(clue.target(), key -> new ArrayList<>()).add(clue.evidence());
        }
        for (Clue<T> clue : modulations) {
            List<Evidence> evidence = byTarget.get(clue.target());
            if (evidence != null) {
                evidence.add(clue.evidence());
            }
        }

        List<Candidate<T>> candidates = new ArrayList<>();
        byTarget.forEach((target, evidence) -> candidates.add(new Candidate<>(target, score(evidence), evidence)));
        candidates.sort(Comparator.<Candidate<T>>comparingDouble(Candidate::score).reversed()
                .thenComparing(Candidate::target, order));
        return candidates.size() > MAX_CANDIDATES ? candidates.subList(0, MAX_CANDIDATES) : candidates;
    }

    private static double score(List<Evidence> evidence) {
        double sum = 0;
        for (Evidence piece : evidence) {
            sum += piece.weight();
        }
        return Math.max(0, Math.min(1, sum));
    }
}
