//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.create.rows;

import org.opensilex.aiimport.report.ReportMessage;
import org.opensilex.core.data.dal.DataCSVValidationModel;
import org.opensilex.sparql.csv.CSVCell;
import org.opensilex.sparql.csv.CSVValidationModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.IntUnaryOperator;

/**
 * Turns what the platform's importers found into errors on the user's workbook.
 * <p>
 * Two validators, two numbering conventions, one result. The scientific-object importer (and the
 * other importers built on the same engine) reports the <em>physical line</em> of its CSV, counted
 * from 1, after a header line and a description line. The data import reports the index in the
 * <em>body</em>, counted from 0, after three header lines. Both are brought back to the body index,
 * and from there to the sheet and row recorded in {@link RowOrigins}.
 * <p>
 * The platform's own message is kept as the detail: it is precise ("Unknown germplasm"), and
 * rewriting it here would drift from the rules it reports.
 *
 * @author Arnaud Charleroy
 */
public final class PlatformValidationAdapter {

    /**
     * Lines before the first data row in a CSV of the shared importer engine: the header and the
     * description line. The first data row is physical line 3.
     */
    static final int CSV_ENGINE_FIRST_DATA_LINE = 3;

    private static final String KIND = "AiImport.rows.kind.";

    private PlatformValidationAdapter() {
    }

    /**
     * For the scientific-object importer and the importers sharing its engine.
     */
    public static List<RowError> fromCsvImporter(CSVValidationModel validation, RowOrigins origins) {
        List<RowError> errors = new ArrayList<>();
        IntUnaryOperator bodyIndex = line -> line - CSV_ENGINE_FIRST_DATA_LINE;

        collect(validation.getMissingRequiredValueErrors(), RowError.Kind.MISSING_VALUE, bodyIndex, origins, errors);
        collect(validation.getInvalidValueErrors(), RowError.Kind.INVALID_VALUE, bodyIndex, origins, errors);
        collect(validation.getDatatypeErrors(), RowError.Kind.INVALID_DATATYPE, bodyIndex, origins, errors);
        collect(validation.getInvalidDateErrors(), RowError.Kind.INVALID_DATE, bodyIndex, origins, errors);
        collect(validation.getInvalidURIErrors(), RowError.Kind.INVALID_URI, bodyIndex, origins, errors);
        collect(validation.getUriNotFoundErrors(), RowError.Kind.UNKNOWN_REFERENCE, bodyIndex, origins, errors);
        collect(validation.getAlreadyExistingURIErrors(), RowError.Kind.ALREADY_EXISTS, bodyIndex, origins, errors);
        collect(validation.getDuplicateURIErrors(), RowError.Kind.DUPLICATE_IN_FILE, bodyIndex, origins, errors);
        collect(validation.getInvalidRowSizeErrors(), RowError.Kind.INVALID_ROW, bodyIndex, origins, errors);
        headerErrors(validation, errors);
        return errors;
    }

    /**
     * When the data import reported its findings: the meaning of a duplicate depends on it.
     */
    public enum Stage {
        /**
         * {@code validateWholeCsv}: a duplicate is two lines of the file carrying the same date,
         * variable and target — the validation only compares the file with itself.
         */
        VALIDATION,
        /**
         * {@code importCSVData}: a duplicate is one MongoDB's unique index refused, the same data
         * already in the instance.
         */
        INSERTION
    }

    /**
     * For the data import, whose rows are counted in the body from 0, as reported by its validation.
     */
    public static List<RowError> fromDataImport(DataCSVValidationModel validation, RowOrigins origins) {
        return fromDataImport(validation, origins, 0, Stage.VALIDATION);
    }

    /**
     * @param firstBodyRow where the batch the validation ran on starts among the body rows of
     *                     {@code origins}: a file split in batches of the platform's maximum size
     *                     is validated batch by batch, each counting its rows from 0
     * @param stage        whether the findings come from the validation or from the insertion
     */
    public static List<RowError> fromDataImport(DataCSVValidationModel validation, RowOrigins origins,
                                                int firstBodyRow, Stage stage) {
        List<RowError> errors = new ArrayList<>();
        IntUnaryOperator bodyIndex = row -> row < 0 ? row : firstBodyRow + row;

        collect(validation.getMissingRequiredValueErrors(), RowError.Kind.MISSING_VALUE, bodyIndex, origins, errors);
        collect(validation.getInvalidValueErrors(), RowError.Kind.INVALID_VALUE, bodyIndex, origins, errors);
        collect(validation.getInvalidURIErrors(), RowError.Kind.INVALID_URI, bodyIndex, origins, errors);
        collect(validation.getInvalidDateErrors(), RowError.Kind.INVALID_DATE, bodyIndex, origins, errors);
        collect(validation.getInvalidDataTypeErrors(), RowError.Kind.INVALID_DATATYPE, bodyIndex, origins, errors);
        collect(validation.getInvalidObjectErrors(), RowError.Kind.UNKNOWN_REFERENCE, bodyIndex, origins, errors);
        collect(validation.getInvalidTargetErrors(), RowError.Kind.UNKNOWN_REFERENCE, bodyIndex, origins, errors);
        collect(validation.getInvalidExperimentErrors(), RowError.Kind.UNKNOWN_REFERENCE, bodyIndex, origins, errors);
        collect(validation.getInvalidDeviceErrors(), RowError.Kind.UNKNOWN_REFERENCE, bodyIndex, origins, errors);
        collect(validation.getInvalidAnnotationErrors(), RowError.Kind.INVALID_VALUE, bodyIndex, origins, errors);
        collect(validation.getDeviceChoiceAmbiguityErrors(), RowError.Kind.INVALID_VALUE, bodyIndex, origins, errors);
        collect(validation.getDuplicatedDataErrors(),
                stage == Stage.VALIDATION ? RowError.Kind.DUPLICATE_IN_FILE : RowError.Kind.DUPLICATE_IN_INSTANCE,
                bodyIndex, origins, errors);
        collect(validation.getDuplicatedObjectErrors(), RowError.Kind.INVALID_VALUE, bodyIndex, origins, errors);
        collect(validation.getDuplicatedTargetErrors(), RowError.Kind.INVALID_VALUE, bodyIndex, origins, errors);
        collect(validation.getDuplicatedExperimentErrors(), RowError.Kind.INVALID_VALUE, bodyIndex, origins, errors);
        collect(validation.getDuplicatedDeviceErrors(), RowError.Kind.INVALID_VALUE, bodyIndex, origins, errors);
        headerErrors(validation, errors);

        if (validation.isTooLargeDataset()) {
            errors.add(RowError.ofFile(ReportMessage.of(KIND + "tooLarge",
                    "The batch holds more rows than the platform imports at once.")));
        }
        if (validation.getErrorMessage() != null && !validation.getErrorMessage().isEmpty()) {
            errors.add(RowError.ofFile(ReportMessage.plain(validation.getErrorMessage())));
        }
        return errors;
    }

    private static void collect(Map<Integer, ? extends List<? extends CSVCell>> byRow,
                                RowError.Kind kind, IntUnaryOperator bodyIndexOf,
                                RowOrigins origins, List<RowError> errors) {
        if (byRow == null) {
            return;
        }
        for (Map.Entry<Integer, ? extends List<? extends CSVCell>> entry : byRow.entrySet()) {
            for (CSVCell cell : entry.getValue()) {
                int index = bodyIndexOf.applyAsInt(entry.getKey() == null ? -1 : entry.getKey());
                String detail = cell.getMessage();
                ReportMessage message = ReportMessage.of(KIND + kind.name(),
                                describe(kind, cell))
                        .with("column", origins.workbookHeaderOf(cell.getHeader()))
                        .with("value", cell.getValue() == null ? "" : cell.getValue())
                        .with("detail", detail == null ? "" : detail);

                RowOrigins.Origin origin = origins.ofBodyRow(index).orElse(null);
                if (origin == null) {
                    // Chunk-wide errors come back on row 0, which no data row has: said about the
                    // file rather than pinned on a row it does not belong to.
                    errors.add(RowError.ofFile(message));
                } else {
                    String column = origins.workbookHeaderOf(origin.getSheet(), cell.getHeader());
                    errors.add(new RowError(origin.getSheet(), origin.getRow(), column, cell.getValue(),
                            kind, message.with("column", column)));
                }
            }
        }
    }

    /**
     * Headers are this module's own output, so a header error means the generated CSV is wrong —
     * reported about the file, with the platform's words, rather than blamed on a row.
     */
    private static void headerErrors(CSVValidationModel validation, List<RowError> errors) {
        if (validation.getMissingHeaders() != null) {
            for (String header : validation.getMissingHeaders()) {
                errors.add(RowError.ofFile(ReportMessage.of(KIND + "missingHeader",
                        "The platform expects a column '" + header + "' that is missing.")
                        .with("column", header)));
            }
        }
        if (validation.getInvalidHeaderURIs() != null) {
            validation.getInvalidHeaderURIs().forEach((index, header) ->
                    errors.add(RowError.ofFile(ReportMessage.of(KIND + "invalidHeader",
                            "The platform does not recognise the column '" + header + "'.")
                            .with("column", header))));
        }
    }

    private static String describe(RowError.Kind kind, CSVCell cell) {
        String where = "'" + (cell.getHeader() == null ? "" : cell.getHeader()) + "'";
        String what = cell.getValue() == null || cell.getValue().isEmpty()
                ? ""
                : " ('" + cell.getValue() + "')";
        String detail = cell.getMessage() == null ? "" : " — " + cell.getMessage();
        return kind.name().toLowerCase().replace('_', ' ') + " in " + where + what + detail;
    }
}
