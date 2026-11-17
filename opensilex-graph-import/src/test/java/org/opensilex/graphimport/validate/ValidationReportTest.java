//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.validate;

import org.junit.Test;
import org.opensilex.aiimport.report.ReportMessage;
import org.opensilex.graphimport.graph.CellRef;
import org.opensilex.graphimport.graph.ImportGraph;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The list of problems is cut, never the counts, and the cut keeps a problem of every kind.
 *
 * @author Arnaud Charleroy
 */
public class ValidationReportTest {

    @Test
    public void aColumnWrongOnEveryRowDoesNotHideTheOneProblemOfAnother() {
        List<GraphProblem> problems = new ArrayList<>();
        for (int row = 0; row < 600; row++) {
            problems.add(problem("Parcelles", row, 1, "literal.notADate"));
        }
        problems.add(problem("Parcelles", 599, 2, "problem.linkUnfound"));

        ValidationReport report = ValidationReport.of(new ImportGraph(List.of(), List.of(), List.of()), problems, 500);

        assertEquals(500, report.problems().size());
        assertEquals(601, report.total());
        assertTrue(report.problems().stream().anyMatch(problem -> problem.anchor().column() == 2));
        assertEquals(2, report.counts().size());
        assertEquals(600, report.counts().get(0).count());
        assertEquals(1, report.counts().get(1).count());
        assertEquals("the list stays in reading order", 0, report.problems().get(0).anchor().row());
    }

    private static GraphProblem problem(String sheet, int row, int column, String key) {
        return new GraphProblem(GraphProblem.Severity.ERROR, ReportMessage.of(key, key), List.of(new CellRef(sheet, row,
                column)), false, "P" + row, "x", List.of());
    }
}
