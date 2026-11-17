//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.write;

import org.opensilex.aiimport.report.ReportMessage;
import org.opensilex.graphimport.measure.DataDraft;
import org.opensilex.graphimport.measure.DataSheetSummary;

import java.util.List;
import java.util.Optional;

/**
 * What the step of data would do: the data it would create now, and how many it cannot create yet, the
 * instance already holds, or the files give none for.
 * <p>
 * Data are counted, not listed: a sheet may give tens of thousands.
 *
 * @param status       where the step stands
 * @param ready        the data it would create now
 * @param stopped      how many data could be created but for what the whole step lacks
 * @param reason       what the whole step lacks, such as a provenance; null when nothing
 * @param waiting      how many data are measured on rows of the file still to create
 * @param done         how many data the instance already holds
 * @param notAvailable how many cells are written as not available
 * @param refused      how many cells give no datum because of a problem
 * @param sheets       what each sheet of data gives
 * @author Arnaud Charleroy
 */
public record DataPlan(CreationPlan.Status status, List<DataDraft> ready, int stopped, ReportMessage reason,
                       int waiting, int done, int notAvailable, int refused, List<DataSheetSummary> sheets) {

    public DataPlan {
        ready = List.copyOf(ready);
        sheets = List.copyOf(sheets);
    }

    public Optional<ReportMessage> whyStopped() {
        return Optional.ofNullable(reason);
    }
}
