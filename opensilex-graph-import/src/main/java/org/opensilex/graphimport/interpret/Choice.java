//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.interpret;

import org.opensilex.aiimport.report.ReportMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * What was settled for one question (the property of a column, the class of a sheet, its identifier
 * column), how, and what else was possible.
 *
 * @param chosen     the answer, or null while {@link Status#UNDECIDED}
 * @param status     how the answer was reached
 * @param candidates the answers considered, best first, with their evidence
 * @param notes      what the user should know about this question
 * @param <T>        the kind of answer
 * @author Arnaud Charleroy
 */
public record Choice<T>(T chosen, Status status, List<Candidate<T>> candidates, List<ReportMessage> notes) {

    /** How an answer was reached. */
    public enum Status {
        /** The user chose it. */
        CHOSEN,
        /** The file said it, such as a header written as the predicate. */
        DECIDED,
        /** The evidence cleared the bar; the user may still change it. */
        SUGGESTED,
        /** The user has to choose. */
        UNDECIDED,
        /** The user left it out: no answer, and none suggested again. */
        LEFT_OUT
    }

    public Choice {
        candidates = List.copyOf(candidates);
        notes = List.copyOf(notes);
    }

    public static <T> Choice<T> undecided(List<Candidate<T>> candidates, List<ReportMessage> notes) {
        return new Choice<>(null, Status.UNDECIDED, candidates, notes);
    }

    /** The same choice, sent back to the user with one more note. */
    public Choice<T> reopened(ReportMessage note) {
        List<ReportMessage> more = new ArrayList<>(notes);
        more.add(note);
        return new Choice<>(null, Status.UNDECIDED, candidates, more);
    }

    /** The answer when the evidence clears the bar, else no answer. */
    static <T> Choice<T> fromEvidence(List<Candidate<T>> ranked, Combination.Bar bar, List<ReportMessage> notes) {
        if (bar.isClearedBy(ranked)) {
            return new Choice<>(ranked.get(0).target(), Status.SUGGESTED, ranked, notes);
        }
        return undecided(ranked, notes);
    }
}
