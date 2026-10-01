//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.create.bulk;

import org.opensilex.aiimport.create.rows.RowError;
import org.opensilex.aiimport.service.AiImportSession;

import java.util.List;
import java.util.Map;

/**
 * The shape every bulk creation takes, whatever it creates.
 * <p>
 * A template method: the order is fixed here, and only what differs from one kind of resource to
 * another is left to subclasses. The order is the point — the module first checks what it can
 * itself (a germplasm name that does not resolve), then asks the platform's own validator, brings
 * every error back to the workbook, and writes only when nothing is left. Writing this sequence
 * once is what stops a second implementation from writing before validating.
 *
 * @param <V> the validation model the platform's importer returns
 * @author Arnaud Charleroy
 */
public abstract class PlatformBulkImport<V> {

    /**
     * Checks everything, writes nothing.
     */
    public final BulkOutcome validate(AiImportSession session, Map<String, String> values)
            throws Exception {
        try {
            return check(generate(session, values), values);
        } finally {
            finish(false);
        }
    }

    /**
     * Validates, then writes everything or nothing.
     */
    public final BulkOutcome importAll(AiImportSession session, Map<String, String> values)
            throws Exception {
        boolean written = false;
        try {
            GeneratedCsv csv = generate(session, values);
            BulkOutcome checked = check(csv, values);
            if (checked.isRefused()) {
                return checked;
            }
            BulkOutcome imported = importWithPlatform(csv, values);
            written = imported.getImported() > 0;
            return imported;
        } finally {
            finish(written);
        }
    }

    private BulkOutcome check(GeneratedCsv csv, Map<String, String> values) throws Exception {
        // The module's own refusals first: asking the platform about names this module could not
        // resolve would only report the same rows again, in its words instead of ours.
        if (!csv.getModuleErrors().isEmpty()) {
            return BulkOutcome.refused(csv.getDataLineCount(), csv.getModuleErrors());
        }
        List<RowError> errors = errorsOf(validateWithPlatform(csv, values), csv);
        return errors.isEmpty()
                ? BulkOutcome.valid(csv.getDataLineCount())
                : BulkOutcome.refused(csv.getDataLineCount(), errors);
    }

    /**
     * Called once a run is over, whether it wrote anything or not, even after an exception: where a
     * subclass releases what its validation needed. Nothing by default.
     *
     * @param written whether rows reached the instance
     */
    protected void finish(boolean written) throws Exception {
    }

    /**
     * Writes the workbook's rows in the platform's CSV format, recording where each line came from,
     * and collecting what the module already knows cannot be imported.
     */
    protected abstract GeneratedCsv generate(AiImportSession session, Map<String, String> values)
            throws Exception;

    protected abstract V validateWithPlatform(GeneratedCsv csv, Map<String, String> values)
            throws Exception;

    /**
     * Brings the platform's findings back to the workbook's sheets, rows and columns.
     */
    protected abstract List<RowError> errorsOf(V validation, GeneratedCsv csv);

    /**
     * Writes, in one transaction, and reports what was written — or, if the platform still refused
     * something at that point, the rows it refused.
     */
    protected abstract BulkOutcome importWithPlatform(GeneratedCsv csv, Map<String, String> values)
            throws Exception;
}
