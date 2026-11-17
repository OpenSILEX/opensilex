//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.interpret;

import org.opensilex.aiimport.report.ReportMessage;

import java.util.List;

/**
 * What a workbook holds, sheet by sheet, as the evidence and the user's decisions say.
 *
 * @param sheets every sheet, in workbook order
 * @param notes  what the user should know about the whole import
 * @author Arnaud Charleroy
 */
public record Interpretation(List<SheetInterpretation> sheets, List<ReportMessage> notes) {

    public Interpretation {
        sheets = List.copyOf(sheets);
        notes = List.copyOf(notes);
    }
}
