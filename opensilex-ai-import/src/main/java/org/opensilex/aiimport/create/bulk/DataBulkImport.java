//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.create.bulk;

import org.opensilex.aiimport.create.SessionFacts;
import org.opensilex.aiimport.create.UnresolvedRow;
import org.opensilex.aiimport.create.rows.PlatformValidationAdapter;
import org.opensilex.aiimport.create.rows.RowError;
import org.opensilex.aiimport.profile.DataPoint;
import org.opensilex.aiimport.report.ReportMessage;
import org.opensilex.aiimport.resolve.ReportCategory;
import org.opensilex.aiimport.service.AiImportSession;
import org.opensilex.core.data.api.DataAPI;
import org.opensilex.core.data.bll.dataImport.DataImportLogic;
import org.opensilex.core.data.dal.DataCSVValidationModel;
import org.opensilex.core.provenance.dal.ProvenanceDAO;
import org.opensilex.core.provenance.dal.ProvenanceModel;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.service.SPARQLService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The workbook's observations, inserted through the platform's own data import.
 * <p>
 * {@link DataImportLogic} is what the data import screen runs: its validation checks every target,
 * date, variable and value type, and its insertion records a batch history and archives the CSV as
 * a document — so data imported from here can be found, traced and deleted from the platform's own
 * screens, like any other import. This class only writes that CSV, with URIs rather than names —
 * the module has resolved every name already — and brings the platform's findings back to the
 * user's workbook.
 * <p>
 * Three things the platform's import imposes, and how they are met:
 * <ul>
 *     <li><b>A provenance must exist before the validation.</b> One is created for the run and
 *     deleted again when nothing was written ({@link #finish}), so a refusal still leaves nothing
 *     behind. A fresh provenance also means the same data cannot already be in the instance: the
 *     unique index includes it, which is why only duplicates <em>within</em> the file are
 *     reported.</li>
 *     <li><b>At most {@link DataAPI#SIZE_MAX} lines per import.</b> A larger file goes in batches,
 *     <em>all</em> validated before the first is written. Each batch is then written in its own
 *     transaction: should one still fail, the outcome names the batches already written instead of
 *     pretending nothing was ({@link BulkOutcome#interrupted}).</li>
 *     <li><b>A {@code scientific_object} cell must not be empty.</b> Rows on facilities and rows on
 *     objects therefore share the generic {@code target} column, which accepts any resource by URI;
 *     the objects were resolved inside the experiment, so the experiment check that column would
 *     have made is already done.</li>
 * </ul>
 *
 * @author Arnaud Charleroy
 */
public class DataBulkImport extends PlatformBulkImport<List<DataCSVValidationModel>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataBulkImport.class);

    public static final String EXPERIMENT = "experiment";
    public static final String PROVENANCE_NAME = "provenance_name";
    public static final String PROVENANCE_DESCRIPTION = "provenance_description";

    /**
     * Identifiers, labels, descriptions: the data import reads the first two and skips the third.
     */
    static final int HEADER_LINES = 3;

    static final String TARGET_HEADER = "target";
    static final String DATE_HEADER = "date";

    private static final String KIND = "AiImport.rows.kind.";

    private final SPARQLService sparql;
    private final MongoDBService nosql;
    private final FileStorageService fs;
    private final AccountModel currentUser;
    private final int batchSize;

    /**
     * The provenance of this run: created for the validation, kept only when data was written.
     */
    private URI provenance;

    /**
     * One per batch: lets the insertion reuse the validation the platform has just cached rather
     * than run it again. A key expired meanwhile only costs a second validation.
     */
    private final List<String> validationKeys = new ArrayList<>();

    public DataBulkImport(SPARQLService sparql, MongoDBService nosql, FileStorageService fs,
                          AccountModel currentUser) {
        this(sparql, nosql, fs, currentUser, DataAPI.SIZE_MAX);
    }

    /**
     * @param batchSize lines per platform import; smaller than the platform's maximum only in tests
     */
    DataBulkImport(SPARQLService sparql, MongoDBService nosql, FileStorageService fs,
                   AccountModel currentUser, int batchSize) {
        this.sparql = sparql;
        this.nosql = nosql;
        this.fs = fs;
        this.currentUser = currentUser;
        this.batchSize = Math.max(1, Math.min(batchSize, DataAPI.SIZE_MAX));
    }

    //#region generation

    /**
     * One line per workbook row, target and date, one column per variable; a cell left empty where
     * that row did not measure that variable, which the platform skips.
     */
    @Override
    protected GeneratedCsv generate(AiImportSession session, Map<String, String> values) {
        SessionFacts facts = new SessionFacts(session);
        Map<String, URI> variablesByColumn = facts.resolvedUris(ReportCategory.VARIABLES);
        Map<String, URI> objectsByName = facts.resolvedUris(ReportCategory.SCIENTIFIC_OBJECTS);
        Map<String, URI> facilitiesByName = facts.resolvedUris(ReportCategory.FACILITIES);

        GeneratedCsv csv = new GeneratedCsv(HEADER_LINES);
        Map<URI, String> columns = new LinkedHashMap<>();
        Map<LineKey, Map<URI, String>> lines = new LinkedHashMap<>();

        for (DataPoint point : session.getDataPoints()) {
            URI variable = variablesByColumn.get(point.getVariableKey().toLowerCase());
            if (variable == null) {
                csv.getModuleErrors().add(RowError.fromUnresolved(new UnresolvedRow(point.getSheet(),
                        point.getRowNumber(), point.getVariableKey(),
                        "AiImport.proposal.unresolved.variable", point.getVariableKey())));
                continue;
            }
            boolean atFacility = point.getTargetKind() == DataPoint.TargetKind.FACILITY;
            URI target = (atFacility ? facilitiesByName : objectsByName)
                    .get(point.getObjectName().toLowerCase());
            if (target == null) {
                csv.getModuleErrors().add(RowError.fromUnresolved(new UnresolvedRow(point.getSheet(),
                        point.getRowNumber(), point.getVariableKey(),
                        atFacility
                                ? "AiImport.proposal.unresolved.facility"
                                : "AiImport.proposal.unresolved.object",
                        point.getObjectName())));
                continue;
            }

            columns.putIfAbsent(variable, point.getVariableKey());
            Map<URI, String> line = lines.computeIfAbsent(
                    new LineKey(point.getSheet(), point.getRowNumber(), target, point.getDate().toString()),
                    key -> new LinkedHashMap<>());
            String previous = line.putIfAbsent(variable, point.getRawValue());
            if (previous != null && !previous.equals(point.getRawValue())) {
                // One cell per variable and line in the platform's format: a second value for the
                // same row, target, date and variable has nowhere to go, and dropping it silently is
                // not an option.
                csv.getModuleErrors().add(duplicateInRow(point, previous));
            }
        }

        writeHeaders(csv, columns);
        for (Map.Entry<LineKey, Map<URI, String>> line : lines.entrySet()) {
            List<String> cells = new ArrayList<>(columns.size() + 2);
            cells.add(line.getKey().target.toString());
            cells.add(line.getKey().date);
            for (URI variable : columns.keySet()) {
                cells.add(line.getValue().getOrDefault(variable, ""));
            }
            csv.addDataLine(line.getKey().sheet, line.getKey().row, cells);
        }
        return csv;
    }

    private void writeHeaders(GeneratedCsv csv, Map<URI, String> columns) {
        List<String> identifiers = new ArrayList<>(List.of(TARGET_HEADER, DATE_HEADER));
        List<String> labels = new ArrayList<>(List.of(TARGET_HEADER, DATE_HEADER));
        List<String> descriptions = new ArrayList<>(List.of("", ""));
        columns.forEach((variable, workbookHeader) -> {
            identifiers.add(variable.toString());
            labels.add(workbookHeader);
            descriptions.add("");
            // The platform names a variable column in its errors as "label(uri)"; both forms lead
            // back to the column of the user's workbook.
            csv.getOrigins().addColumn(variable.toString(), workbookHeader);
            csv.getOrigins().addColumn(workbookHeader + "(" + variable + ")", workbookHeader);
        });
        csv.addHeaderLine(identifiers).addHeaderLine(labels).addHeaderLine(descriptions);
    }

    private RowError duplicateInRow(DataPoint point, String previous) {
        return new RowError(point.getSheet(), point.getRowNumber(), point.getVariableKey(),
                point.getRawValue(), RowError.Kind.DUPLICATE_IN_FILE,
                ReportMessage.of(KIND + RowError.Kind.DUPLICATE_IN_FILE.name(),
                                "The same row gives two values for '" + point.getVariableKey()
                                        + "' on the same date: '" + previous + "' and '"
                                        + point.getRawValue() + "'.")
                        .with("column", point.getVariableKey())
                        .with("value", point.getRawValue())
                        .with("detail", previous));
    }

    //#endregion

    //#region platform

    /**
     * Every batch validated, before anything is written.
     */
    @Override
    protected List<DataCSVValidationModel> validateWithPlatform(GeneratedCsv csv, Map<String, String> values)
            throws Exception {
        URI experiment = experimentOf(values);
        URI runProvenance = provenance(values);
        DataImportLogic logic = logic();

        List<DataCSVValidationModel> validations = new ArrayList<>();
        validationKeys.clear();
        for (int from = 0; from < csv.getDataLineCount(); from += batchSize) {
            int to = Math.min(from + batchSize, csv.getDataLineCount());
            DataCSVValidationModel validation = logic.validateWholeCsv(runProvenance, experiment,
                    batch(csv, from, to), batchName(values, from, to));
            validations.add(validation);
            validationKeys.add(validation.getValidationKey());
        }
        return validations;
    }

    @Override
    protected List<RowError> errorsOf(List<DataCSVValidationModel> validations, GeneratedCsv csv) {
        List<RowError> errors = new ArrayList<>();
        for (int i = 0; i < validations.size(); i++) {
            errors.addAll(PlatformValidationAdapter.fromDataImport(validations.get(i), csv.getOrigins(),
                    i * batchSize, PlatformValidationAdapter.Stage.VALIDATION));
        }
        return errors;
    }

    /**
     * Batch after batch, each in the platform's own transaction, with its batch history and its
     * archived CSV.
     */
    @Override
    protected BulkOutcome importWithPlatform(GeneratedCsv csv, Map<String, String> values)
            throws Exception {
        URI experiment = experimentOf(values);
        URI runProvenance = provenance(values);
        DataImportLogic logic = logic();

        int imported = 0;
        List<URI> batches = new ArrayList<>();
        for (int batch = 0, from = 0; from < csv.getDataLineCount(); batch++, from += batchSize) {
            int to = Math.min(from + batchSize, csv.getDataLineCount());
            DataCSVValidationModel result;
            try {
                result = logic.importCSVData(runProvenance, experiment, batch(csv, from, to),
                        batchName(values, from, to),
                        batch < validationKeys.size() ? validationKeys.get(batch) : null)
                        .getDataErrors();
            } catch (Exception e) {
                if (imported == 0) {
                    throw e;
                }
                // Earlier batches are in the instance and stay there: said, with where to find them.
                LOGGER.error("Data import interrupted after {} batch(es)", batches.size(), e);
                return BulkOutcome.interrupted(csv.getDataLineCount(), imported,
                        List.of(interruption(from, to, e)), batches);
            }

            List<RowError> errors = PlatformValidationAdapter.fromDataImport(result, csv.getOrigins(),
                    from, PlatformValidationAdapter.Stage.INSERTION);
            if (!errors.isEmpty()) {
                return imported == 0
                        ? BulkOutcome.refused(csv.getDataLineCount(), errors)
                        : BulkOutcome.interrupted(csv.getDataLineCount(), imported, errors, batches);
            }
            imported += result.getData() == null ? 0 : result.getData().size();
            if (result.getBatchHistoryUri() != null) {
                batches.add(result.getBatchHistoryUri());
            }
        }
        return BulkOutcome.imported(csv.getDataLineCount(), imported, batches);
    }

    /**
     * Nothing written, so the provenance made for the run goes too: a refusal leaves nothing behind.
     */
    @Override
    protected void finish(boolean written) throws Exception {
        if (!written && provenance != null) {
            try {
                new ProvenanceDAO(nosql, sparql).delete(provenance);
            } catch (Exception e) {
                // An unused provenance is clutter, not damage; the refusal itself must still reach
                // the user.
                LOGGER.warn("Could not delete the unused provenance {}", provenance, e);
            }
        }
        provenance = null;
        validationKeys.clear();
    }

    //#endregion

    //#region helpers

    protected DataImportLogic logic() {
        return new DataImportLogic(nosql, sparql, fs, currentUser);
    }

    private URI provenance(Map<String, String> values) throws Exception {
        if (provenance == null) {
            ProvenanceModel model = new ProvenanceModel();
            model.setName(required(values, PROVENANCE_NAME));
            String description = values.get(PROVENANCE_DESCRIPTION);
            model.setDescription(description == null || description.isBlank() ? null : description.trim());
            provenance = new ProvenanceDAO(nosql, sparql).create(model).getUri();
        }
        return provenance;
    }

    private static URI experimentOf(Map<String, String> values) {
        return URI.create(required(values, EXPERIMENT));
    }

    private static String required(Map<String, String> values, String field) {
        String value = values.get(field);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("The field '" + field + "' is required.");
        }
        return value.trim();
    }

    private static InputStream batch(GeneratedCsv csv, int from, int to) {
        return new ByteArrayInputStream(csv.render(from, to).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * The name the platform archives the batch under: the provenance, and which lines of the
     * generated file it holds, so a batch history reads as what it is.
     */
    private static String batchName(Map<String, String> values, int from, int to) {
        return values.getOrDefault(PROVENANCE_NAME, "ai-import") + " (" + (from + 1) + "-" + to + ").csv";
    }

    private static RowError interruption(int from, int to, Exception e) {
        return RowError.ofFile(ReportMessage.of("AiImport.rows.interrupted",
                        "The import stopped at lines " + (from + 1) + " to " + to + " of the generated "
                                + "file: " + e.getMessage() + ". The batches before were written and "
                                + "remain in the instance.")
                .with("from", from + 1)
                .with("to", to)
                .with("detail", e.getMessage() == null ? "" : e.getMessage()));
    }

    /**
     * The workbook row, target and date one line of the generated CSV stands for.
     */
    private static final class LineKey {

        private final String sheet;
        private final int row;
        private final URI target;
        private final String date;

        private LineKey(String sheet, int row, URI target, String date) {
            this.sheet = sheet;
            this.row = row;
            this.target = target;
            this.date = date;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof LineKey)) {
                return false;
            }
            LineKey that = (LineKey) other;
            return row == that.row && Objects.equals(sheet, that.sheet)
                    && Objects.equals(target, that.target) && Objects.equals(date, that.date);
        }

        @Override
        public int hashCode() {
            return Objects.hash(sheet, row, target, date);
        }
    }

    //#endregion
}
