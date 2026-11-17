//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.validate;

import org.opensilex.aiimport.report.ReportMessage;
import org.opensilex.graphimport.graph.CellRef;
import org.opensilex.graphimport.graph.DateColumn;
import org.opensilex.graphimport.graph.ImportGraph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * What the graph of an import holds and what is wrong with it.
 * <p>
 * The problems are counted by kind before the list is cut, and the list takes them in turn from each
 * kind, so that a column wrong on every row does not hide the one problem of another.
 *
 * @param nodeCount   how many resources
 * @param valueCount  how many values, links included
 * @param linkCount   how many links
 * @param counts      the problems by sheet, column and kind, all of them counted; a problem whose cells
 *                    lie in two places counts in both
 * @param problems    at most {@link GraphValidation#MAX_PROBLEMS} problems
 * @param total       how many problems there are
 * @param notes       what the graph left out
 * @param dateColumns the columns of dates, with how they were read
 * @author Arnaud Charleroy
 */
public record ValidationReport(int nodeCount, int valueCount, int linkCount, List<ProblemCount> counts,
                               List<GraphProblem> problems, int total, List<ReportMessage> notes,
                               List<DateColumn> dateColumns) {

    private static final Comparator<GraphProblem> BY_PLACE = Comparator
            .comparing(GraphProblem::anchor, CellRef.READING_ORDER)
            .thenComparing(problem -> Objects.toString(problem.message().getKey(), ""));

    public ValidationReport {
        counts = List.copyOf(counts);
        problems = List.copyOf(problems);
        notes = List.copyOf(notes);
        dateColumns = List.copyOf(dateColumns);
    }

    /** The kind a problem is counted under: where it is, what it says, about which property. */
    private record Kind(String sheet, Integer column, GraphProblem.Severity severity, String key, String property) {
    }

    /**
     * The kinds a problem counts under: one per sheet and column its cells touch, so that the two cells of
     * a conflict given by two sheets are each counted where the user looks; the first is where it is listed.
     */
    private static Set<Kind> kindsOf(GraphProblem problem) {
        Set<Kind> kinds = new LinkedHashSet<>();
        for (CellRef cell : problem.cells()) {
            kinds.add(new Kind(cell.sheet(), problem.aboutRow() ? null : cell.column(), problem.severity(),
                    problem.message().getKey(), problem.message().getParams().get("property")));
        }
        return kinds;
    }

    static ValidationReport of(ImportGraph graph, List<GraphProblem> all, int max) {
        List<GraphProblem> sorted = new ArrayList<>(all);
        sorted.sort(BY_PLACE);
        Map<Kind, Deque<GraphProblem>> byKind = new LinkedHashMap<>();
        Map<Kind, Deque<GraphProblem>> touched = new LinkedHashMap<>();
        for (GraphProblem problem : sorted) {
            boolean first = true;
            for (Kind kind : kindsOf(problem)) {
                touched.computeIfAbsent(kind, k -> new ArrayDeque<>()).add(problem);
                if (first) {
                    byKind.computeIfAbsent(kind, k -> new ArrayDeque<>()).add(problem);
                    first = false;
                }
            }
        }
        List<ProblemCount> counts = new ArrayList<>();
        touched.forEach((kind, problems) -> counts.add(new ProblemCount(kind.sheet(), kind.column(), kind.severity(),
                problems.size(), problems.peekFirst().message())));

        List<GraphProblem> kept = new ArrayList<>();
        List<Deque<GraphProblem>> queues = new ArrayList<>(byKind.values());
        while (kept.size() < max && queues.stream().anyMatch(queue -> !queue.isEmpty())) {
            for (Deque<GraphProblem> queue : queues) {
                if (!queue.isEmpty() && kept.size() < max) {
                    kept.add(queue.pollFirst());
                }
            }
        }
        kept.sort(BY_PLACE);
        return new ValidationReport(graph.nodes().size(), graph.valueCount(), graph.linkCount(), counts, kept,
                all.size(), graph.notes(), graph.dateColumns());
    }
}
