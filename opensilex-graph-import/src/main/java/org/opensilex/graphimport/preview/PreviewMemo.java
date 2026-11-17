//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.preview;

import org.opensilex.aiimport.workbook.WorkbookStructure;
import org.opensilex.graphimport.binding.ImportDecisions;

import java.net.URI;
import java.util.Objects;

/**
 * A preview kept with what it was worked out from, so that turning the pages of the grid does not
 * work it out again. It holds while the workbook is the same object, which the session forgets when
 * a file or a layout changes, and while the decisions and the experiment are equal.
 *
 * @author Arnaud Charleroy
 */
public record PreviewMemo(WorkbookStructure workbook, ImportDecisions decisions, URI experiment,
                          ImportPreview preview) {

    /** True when the preview was worked out from these. */
    public boolean holdsFor(WorkbookStructure currentWorkbook, ImportDecisions currentDecisions,
                            URI currentExperiment) {
        return workbook == currentWorkbook && decisions.equals(currentDecisions)
                && Objects.equals(experiment, currentExperiment);
    }
}
