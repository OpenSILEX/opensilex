//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.create.bulk;

import org.opensilex.aiimport.create.rows.RowError;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * What a bulk validation or import came to: nothing wrong and so many rows written, or refused with
 * the rows that stand in the way. Never both — a bulk import is all or nothing — with one stated
 * exception: data larger than the platform imports at once goes in several batches, all validated
 * before the first is written but each written in its own transaction. Should one fail after the
 * validation passed, the outcome says so, with the batches already written ({@link #interrupted}).
 *
 * @author Arnaud Charleroy
 */
public final class BulkOutcome {

    private final int rowsChecked;
    private final int imported;
    private final List<RowError> errors;

    /**
     * The platform's batch histories of what was written: where an import can be found, and undone,
     * from the platform's own screens.
     */
    private final List<URI> batches = new ArrayList<>();

    private BulkOutcome(int rowsChecked, int imported, List<RowError> errors) {
        this.rowsChecked = rowsChecked;
        this.imported = imported;
        this.errors = new ArrayList<>(errors);
    }

    public static BulkOutcome valid(int rowsChecked) {
        return new BulkOutcome(rowsChecked, 0, List.of());
    }

    public static BulkOutcome imported(int rowsChecked, int imported) {
        return new BulkOutcome(rowsChecked, imported, List.of());
    }

    public static BulkOutcome refused(int rowsChecked, List<RowError> errors) {
        return new BulkOutcome(rowsChecked, 0, errors);
    }

    /**
     * Written, with the batch histories the platform recorded.
     */
    public static BulkOutcome imported(int rowsChecked, int imported, List<URI> batches) {
        BulkOutcome outcome = new BulkOutcome(rowsChecked, imported, List.of());
        outcome.batches.addAll(batches);
        return outcome;
    }

    /**
     * Some batches written, then one refused: both are true, and both are said.
     */
    public static BulkOutcome interrupted(int rowsChecked, int imported, List<RowError> errors,
                                          List<URI> batches) {
        BulkOutcome outcome = new BulkOutcome(rowsChecked, imported, errors);
        outcome.batches.addAll(batches);
        return outcome;
    }

    public List<URI> getBatches() {
        return batches;
    }

    public boolean isRefused() {
        return !errors.isEmpty();
    }

    public int getRowsChecked() {
        return rowsChecked;
    }

    public int getImported() {
        return imported;
    }

    public List<RowError> getErrors() {
        return errors;
    }
}
