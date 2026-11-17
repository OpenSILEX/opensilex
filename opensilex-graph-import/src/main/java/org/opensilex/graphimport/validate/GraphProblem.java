//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.validate;

import org.opensilex.aiimport.report.ReportMessage;
import org.opensilex.graphimport.graph.CellRef;
import org.opensilex.graphimport.graph.GraphNode;
import org.opensilex.graphimport.graph.Suggestion;

import java.util.List;

/**
 * Something the user has to look at before the import can go on, on the cells it concerns.
 *
 * @param severity    whether it stops the import or only warns
 * @param message     what is wrong, translatable
 * @param cells       the cells concerned, the first one first shown; never empty
 * @param aboutRow    true when it concerns the resource as a whole, such as a missing value, rather
 *                    than the cells of one column
 * @param nodeName    what the user calls the resource concerned
 * @param value       the cell as written, for a problem about one value; null otherwise
 * @param suggestions what the value was probably meant to be, closest first
 * @author Arnaud Charleroy
 */
public record GraphProblem(Severity severity, ReportMessage message, List<CellRef> cells, boolean aboutRow,
                           String nodeName, String value, List<Suggestion> suggestions) {

    /** Whether a problem stops the import. */
    public enum Severity {
        /** The import cannot go on until it is settled. */
        ERROR,
        /** The import may go on; the user should check. */
        WARNING
    }

    public GraphProblem {
        if (cells.isEmpty()) {
            throw new IllegalArgumentException("A problem concerns at least one cell");
        }
        cells = List.copyOf(cells);
        suggestions = List.copyOf(suggestions);
    }

    /** A problem about one cell. */
    static GraphProblem ofValue(Severity severity, ReportMessage message, GraphNode node, CellRef cell, String raw,
                                List<Suggestion> suggestions) {
        return new GraphProblem(severity, message, List.of(cell), false, node.getName(), raw, suggestions);
    }

    /** A problem about several cells of one resource. */
    static GraphProblem ofCells(Severity severity, ReportMessage message, GraphNode node, List<CellRef> cells,
                                boolean aboutRow) {
        return new GraphProblem(severity, message, cells, aboutRow, node.getName(), null, List.of());
    }

    /** The cell the problem is shown on. */
    public CellRef anchor() {
        return cells.get(0);
    }
}
