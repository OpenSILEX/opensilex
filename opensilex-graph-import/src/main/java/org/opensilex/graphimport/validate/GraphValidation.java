//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.validate;

import org.opensilex.graphimport.graph.ImportGraph;
import org.opensilex.graphimport.interpret.InterpretationServices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Runs every validator on the graph of an import, in the order of their ids, and reports.
 *
 * @author Arnaud Charleroy
 */
public final class GraphValidation {

    /** How many problems a report lists; all of them are counted. */
    public static final int MAX_PROBLEMS = 500;

    private GraphValidation() {
    }

    public static ValidationReport run(ImportGraph graph, Collection<GraphValidator> validators,
                                       InterpretationServices services) throws Exception {
        return report(graph, problems(graph, validators, services));
    }

    /** Every problem the validators find, none left out. */
    public static List<GraphProblem> problems(ImportGraph graph, Collection<GraphValidator> validators,
                                              InterpretationServices services) throws Exception {
        List<GraphValidator> sorted = new ArrayList<>(validators);
        sorted.sort(Comparator.comparing(GraphValidator::id));
        List<GraphProblem> problems = new ArrayList<>();
        for (GraphValidator validator : sorted) {
            problems.addAll(validator.validate(graph, services));
        }
        return problems;
    }

    /** The report of problems already found. */
    public static ValidationReport report(ImportGraph graph, List<GraphProblem> problems) {
        return ValidationReport.of(graph, problems, MAX_PROBLEMS);
    }
}
