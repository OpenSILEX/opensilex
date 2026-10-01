//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.resolve;

import org.opensilex.aiimport.workbook.HeaderMatcher;

import java.util.OptionalDouble;

/**
 * Says whether two names are close enough for one to be a misspelling of the other.
 * <p>
 * Used to suggest, never to decide: a close name is offered to the user as "did you mean …?" and
 * becomes a match only once they confirm it. That is why the rules below lean towards staying
 * silent. A missed suggestion costs the user one search; a wrong one invites them to file their
 * data against somebody else's resource.
 * <p>
 * Three rules, each for a case that a plain edit distance gets wrong:
 * <ul>
 *   <li><b>Transpositions count once.</b> "Genotpye" is one slip of the fingers, and the
 *       restricted Damerau-Levenshtein distance used here says so, where plain Levenshtein counts
 *       two edits and would reject it.</li>
 *   <li><b>Short names are compared exactly.</b> {@code A1} and {@code A10} are one edit apart and
 *       are two different plots. Below five characters there is no room for a typo to be told
 *       apart from a different name.</li>
 *   <li><b>Digits must agree.</b> "Clone 115" and "Clone 116" are one edit apart and are two
 *       different clones. A number in a name is an identifier, and a different number is a
 *       different thing, whatever the distance says.</li>
 * </ul>
 * Case, accents and separators are ignored beforehand, through the same normalisation the header
 * matching uses, so "Pinot_Noir" and "pinot noir" are identical rather than close.
 *
 * @author Arnaud Charleroy
 */
public final class NameSimilarity {

    /**
     * Up to this normalised length, only an identical name matches.
     */
    static final int EXACT_ONLY_UP_TO = 4;

    /**
     * Up to this normalised length, one edit is tolerated; beyond it, two.
     */
    static final int ONE_EDIT_UP_TO = 8;

    private NameSimilarity() {
    }

    /**
     * @return how many edits a name of this normalised length may carry and still be recognised
     */
    public static int allowedEdits(int length) {
        if (length <= EXACT_ONLY_UP_TO) {
            return 0;
        }
        return length <= ONE_EDIT_UP_TO ? 1 : 2;
    }

    /**
     * @return a similarity between 0 and 1 when {@code b} could be what {@code a} meant to say,
     *         empty when the two are simply different names
     */
    public static OptionalDouble similarity(String a, String b) {
        String left = HeaderMatcher.normalize(a);
        String right = HeaderMatcher.normalize(b);
        if (left.isEmpty() || right.isEmpty()) {
            return OptionalDouble.empty();
        }
        if (!digitsOf(left).equals(digitsOf(right))) {
            return OptionalDouble.empty();
        }

        // The shorter of the two sets the tolerance, so the rule is the same whichever side the
        // typo is on.
        int allowed = allowedEdits(Math.min(left.length(), right.length()));
        int distance = distance(left, right, allowed);
        if (distance > allowed) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(1.0 - (double) distance / Math.max(left.length(), right.length()));
    }

    /**
     * Restricted Damerau-Levenshtein distance — the optimal string alignment variant — which counts
     * an insertion, a deletion, a substitution or a swap of two neighbours as one edit.
     * <p>
     * Bounded: it stops as soon as every alignment of the current row exceeds {@code limit}, since
     * none of them can come back under it. Comparing a name against a few thousand candidates
     * then costs little more than reading them.
     *
     * @return the distance, or {@code limit + 1} as soon as it is known to exceed {@code limit}
     */
    static int distance(String a, String b, int limit) {
        if (Math.abs(a.length() - b.length()) > limit) {
            return limit + 1;
        }
        int columns = b.length();
        int[] twoRowsUp = new int[columns + 1];
        int[] rowUp = new int[columns + 1];
        int[] row = new int[columns + 1];
        for (int j = 0; j <= columns; j++) {
            rowUp[j] = j;
        }

        for (int i = 1; i <= a.length(); i++) {
            row[0] = i;
            int rowMinimum = row[0];
            for (int j = 1; j <= columns; j++) {
                int substitution = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                int value = Math.min(Math.min(rowUp[j] + 1, row[j - 1] + 1),
                        rowUp[j - 1] + substitution);
                boolean swapped = i > 1 && j > 1
                        && a.charAt(i - 1) == b.charAt(j - 2)
                        && a.charAt(i - 2) == b.charAt(j - 1);
                if (swapped) {
                    value = Math.min(value, twoRowsUp[j - 2] + 1);
                }
                row[j] = value;
                rowMinimum = Math.min(rowMinimum, value);
            }
            if (rowMinimum > limit) {
                return limit + 1;
            }
            int[] recycled = twoRowsUp;
            twoRowsUp = rowUp;
            rowUp = row;
            row = recycled;
        }
        return Math.min(rowUp[columns], limit + 1);
    }

    private static String digitsOf(String normalised) {
        StringBuilder digits = new StringBuilder();
        for (char c : normalised.toCharArray()) {
            if (Character.isDigit(c)) {
                digits.append(c);
            }
        }
        return digits.toString();
    }
}
