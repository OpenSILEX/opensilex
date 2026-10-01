//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.create;

import org.opensilex.aiimport.create.bulk.BulkOutcome;
import org.opensilex.aiimport.create.rows.RowError;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * What confirming a draft came to, whatever its target: one resource created, so many written, or
 * refused with the rows of the workbook that stand in the way — in which case nothing was written.
 * <p>
 * One shape for every target, so the API turns any of them into its response the same way instead
 * of knowing which operation returns what.
 *
 * @author Arnaud Charleroy
 */
public final class CreationOutcome {

    private final URI uri;
    private final int insertedCount;
    private final int rowsChecked;
    private final List<RowError> errors;
    private final List<URI> batches = new ArrayList<>();

    private CreationOutcome(URI uri, int insertedCount, int rowsChecked, List<RowError> errors) {
        this.uri = uri;
        this.insertedCount = insertedCount;
        this.rowsChecked = rowsChecked;
        this.errors = new ArrayList<>(errors);
    }

    /**
     * A project or an experiment: one resource, named by its URI.
     */
    public static CreationOutcome created(URI uri) {
        return new CreationOutcome(uri, 0, 0, List.of());
    }

    /**
     * Objects, variables, events, data points: a count.
     */
    public static CreationOutcome inserted(int count) {
        return new CreationOutcome(null, count, 0, List.of());
    }

    public static CreationOutcome refused(int rowsChecked, List<RowError> errors) {
        return new CreationOutcome(null, 0, rowsChecked, errors);
    }

    /**
     * From a bulk import: written, refused, or interrupted between batches — in which case some rows
     * were written and the refusal is about the rest.
     */
    public static CreationOutcome of(BulkOutcome bulk) {
        CreationOutcome outcome = new CreationOutcome(null, bulk.getImported(), bulk.getRowsChecked(),
                bulk.getErrors());
        outcome.batches.addAll(bulk.getBatches());
        return outcome;
    }

    public boolean isRefused() {
        return !errors.isEmpty();
    }

    public URI getUri() {
        return uri;
    }

    public int getInsertedCount() {
        return insertedCount;
    }

    public int getRowsChecked() {
        return rowsChecked;
    }

    public List<RowError> getErrors() {
        return errors;
    }

    /**
     * The platform's batch histories of the data written, when it went through the data import.
     */
    public List<URI> getBatches() {
        return batches;
    }
}
