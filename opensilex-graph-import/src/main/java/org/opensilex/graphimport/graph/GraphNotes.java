//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.graph;

import org.opensilex.aiimport.report.ReportMessage;

import java.util.List;

/**
 * What the graph tells the user it left out, and why: the key goes under {@code GraphImport.report.},
 * the parameters fill it in, the English text is the fallback.
 *
 * @author Arnaud Charleroy
 */
final class GraphNotes {

    private static final String SHEET = "sheet";
    private static final String COUNT = "count";

    private GraphNotes() {
    }

    static ReportMessage sheetLeftOut(String sheet) {
        return ReportMessage.of("graph.sheetLeftOut",
                "The sheet " + sheet + " is left out until the class of its rows is chosen.").with(SHEET, sheet);
    }

    static ReportMessage sheetsLeftOut(List<String> sheets) {
        String list = String.join(", ", sheets);
        return ReportMessage.of("graph.sheetsLeftOut",
                        sheets.size() + " sheets are left out until the class of their rows is chosen: " + list + ".")
                .with("sheets", list).with(COUNT, sheets.size());
    }

    static ReportMessage noIdentifier(String sheet) {
        return ReportMessage.of("graph.noIdentifier",
                        "The sheet " + sheet + " has no identifier column: each of its rows is a resource of its own, "
                                + "which no other sheet can complete.")
                .with(SHEET, sheet);
    }

    static ReportMessage emptyIdentifiers(String sheet, int count) {
        return ReportMessage.of("graph.emptyIdentifiers",
                        count + " rows of the sheet " + sheet + " have no identifier: each is a resource of its own.")
                .with(SHEET, sheet).with(COUNT, count);
    }

    static ReportMessage unresolvedTypes(String sheet, int count) {
        return ReportMessage.of("graph.unresolvedTypes",
                        count + " rows of the sheet " + sheet + " have a type that names no known class: they take the "
                                + "class of the sheet.")
                .with(SHEET, sheet).with(COUNT, count);
    }

    static ReportMessage columnsLeftOut(String sheet, List<String> headers) {
        String list = String.join(", ", headers);
        return ReportMessage.of("graph.columnsLeftOut",
                        "Columns of the sheet " + sheet + " left out until their property is chosen: " + list + ".")
                .with(SHEET, sheet).with("headers", list).with(COUNT, headers.size());
    }

    static ReportMessage propertyNotAccepted(String sheet, String header) {
        return ReportMessage.of("graph.propertyNotAccepted",
                        "The column " + header + " of the sheet " + sheet + " is left out: the class of its rows does "
                                + "not accept its property.")
                .with(SHEET, sheet).with("header", header);
    }
}
