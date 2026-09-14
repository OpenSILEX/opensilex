//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.profile.star;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.opensilex.aiimport.mapping.ColumnRole;
import org.opensilex.aiimport.profile.DataPoint;
import org.opensilex.aiimport.profile.EventCandidate;
import org.opensilex.aiimport.profile.ExtractedImportPlan;
import org.opensilex.aiimport.profile.ImportProfile;
import org.opensilex.aiimport.profile.VariableCandidate;
import org.opensilex.aiimport.report.ReportMessage;
import org.opensilex.aiimport.workbook.CellValue;
import org.opensilex.aiimport.workbook.ExcelValueParser;
import org.opensilex.aiimport.workbook.SheetStructure;
import org.opensilex.aiimport.workbook.WorkbookStructure;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Profile for the STAR data model, a normalised template with one entity per sheet.
 * <p>
 * Unlike a field observation template, STAR carries its own metadata: the experiment, its objective
 * and dates, the treatments, the plots, the field, and a dictionary describing every column. It
 * also carries the alignment to the ELOA ontology, which settles what each entity becomes in
 * OpenSILEX rather than leaving it to be guessed.
 * <p>
 * The ELOA classes are not loaded in this instance, so each resource takes an OESO type and keeps
 * its ELOA IRI as an external reference — the same treatment already given to the CropOntology
 * identifiers of variables.
 *
 * @author Arnaud Charleroy
 */
public class StarProfile implements ImportProfile {

    public static final String ID = "star";

    private static final String ANOMALY = "AiImport.report.anomaly.star.";

    /**
     * Note under which the plan records that the type of the scientific objects is undecided.
     */
    public static final String NOTE_OBJECT_TYPE = "scientificObjectType";

    public static final String COLUMN_PLANT_COUNT = "plot_n";

    /**
     * Note under which the plan records that plot identifiers had to be recomposed, and how. Read
     * back when the insertion asks the user to confirm it.
     */
    public static final String NOTE_RECONCILIATION = "plotIdReconciliation";

    //#region the columns of each entity

    public static final String COLUMN_EXPERIMENT = "expe_id";
    public static final String COLUMN_OBJECTIVE = "expe_obj";
    public static final String COLUMN_DESCRIPTION = "expe_desc";
    public static final String COLUMN_START_DATE = "expe_start_date";
    public static final String COLUMN_END_DATE = "expe_end_date";
    public static final String COLUMN_PROJECT = "proj_id";
    public static final String COLUMN_DESIGN = "design_plan";
    public static final String COLUMN_ORGANIZATION = "organization_name";
    public static final String COLUMN_SUBORGANIZATION = "suborganization_name";
    public static final String COLUMN_EMAIL = "email";

    public static final String COLUMN_FIELD = "field_id";
    public static final String COLUMN_FIELD_NAME = "field_name";
    public static final String COLUMN_CULTIVAR = "cultivar_name";

    public static final String COLUMN_PLOT = "plot_id";
    public static final String COLUMN_TREATMENT = "xp_trt_code";
    public static final String COLUMN_BLOCK = "block_code";
    public static final String COLUMN_OBSERVATION_DATE = "observation_date";

    public static final String COLUMN_EVENT_DATE = "event_date";
    public static final String COLUMN_EVENT_TYPE = "event_type";
    public static final String COLUMN_EVENT_DESCRIPTION = "event_description";

    public static final String COLUMN_APPLICATION_DATE = "p_app_date";
    public static final String COLUMN_PRODUCT_NAME = "p_name";
    public static final String COLUMN_PRODUCT_MARKETING_ID = "p_amm";
    public static final String COLUMN_DOSE = "p_dose";
    public static final String COLUMN_DOSE_UNIT = "p_dose_unit";
    public static final String COLUMN_APPLICATION_VOLUME = "p_app_vol_rate";
    public static final String COLUMN_APPLICATION_CODE = "p_app_code";
    public static final String COLUMN_SPRAYER = "sprayer_name";
    public static final String COLUMN_BBCH = "bbch_stage";

    /**
     * Columns saying where the field is: its coordinates and its commune. These describe the
     * facility, which does not move.
     */
    private static final List<String> LOCATION_COLUMNS = Collections.unmodifiableList(Arrays.asList(
            "field_latitude", "field_longitude", "commune_name", "commune_insee_id"));

    /**
     * The plot's coordinates within the field. OpenSILEX records a position as a dated move event,
     * not as an attribute, and its scientific object CSV import carries the very same columns.
     */
    private static final List<String> POSITION_COLUMNS = Collections.unmodifiableList(
            Arrays.asList("plot_x", "plot_y"));

    /**
     * Columns describing the plot itself: how it was planted, and how many plants it holds.
     * <p>
     * {@code plot_n} counts plants per scientific object, which only means something once the type
     * of that object is settled — see {@link #NOTE_OBJECT_TYPE}.
     */
    private static final List<String> OBJECT_PROPERTY_COLUMNS = Collections.unmodifiableList(
            Arrays.asList("row_spacing", "plant_spacing", "plot_n"));

    private static final List<String> DESCRIPTION_COLUMNS = Collections.unmodifiableList(
            Arrays.asList("plot_desc", "xp_trt_desc", "xp_trt_name", "event_description"));

    //#endregion

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getLabel() {
        return "STAR agronomic trial model";
    }

    @Override
    public int match(WorkbookStructure structure) {
        StarSheets sheets = new StarSheets(structure);
        boolean experiment = sheets.experiment().isPresent();
        boolean dictionary = sheets.hasDictionary();

        if (experiment && dictionary) {
            return 100;
        }
        // One of the two renamed is still a strong signal, but leave room for a better match.
        if (experiment || dictionary) {
            return 40;
        }
        return 0;
    }

    @Override
    public String getPromptContext(WorkbookStructure structure) {
        return String.join("\n", Arrays.asList(
                "This workbook follows the STAR data model: normalised, one entity per sheet, and",
                "self-describing. Sheet names carry meaning through their prefix:",
                "",
                "- 'expe': the experiment itself — identifier, objective, description, start and end",
                "  dates, design plan, and the project it belongs to.",
                "- 'ed_*' (or the same names unprefixed in older files): the experimental design.",
                "  'ed_parcelle' is the field, 'ed_placette' the unit plots.",
                "- 'modalite': the experimental treatments.",
                "- 'data_*': the observations. Each sheet gives the identifier of what was",
                "  observed, a date, then one column per variable — in that meaning, not that",
                "  order, which varies from sheet to sheet. The identifier is a plot ('plot_id')",
                "  or the field itself ('field_id'), so weather measured at the field attaches to",
                "  a facility rather than to a plot. 'data_template' holds no rows: it is there to",
                "  show that shape.",
                "- 'dictionary_variables' and 'dictionary_metadata' — or a single 'dictionary' in",
                "  older files — describe every column: unit, type, and ontology URI.",
                "- 'uri_list' and 'listes' are hidden lookup sheets: controlled values and the",
                "  reference URI of each.",
                "",
                "Mapping onto OpenSILEX, settled by the STAR to ELOA alignment:",
                "- 'expe' becomes the experiment. Unlike most data entry files, STAR states the",
                "  objective, so an experiment can usually be created without asking for it.",
                "- 'ed_parcelle' becomes a facility, with its commune as address and its latitude",
                "  and longitude as location.",
                "- 'ed_placette' becomes the scientific objects.",
                "- 'cultivar_name' is the germplasm; 'uri_list' gives its reference URI.",
                "- 'modalite' describes a factor and its levels.",
                "- the dictionary's variables become variables, with their unit and data type.",
                "- events, treatments applied and the data provenance sheet are out of scope here.",
                "",
                "The ELOA classes are not loaded in this instance, so each resource takes an",
                "existing OpenSILEX type and keeps its ELOA IRI as an external reference."
        ));
    }

    @Override
    public ColumnRole roleOf(WorkbookStructure structure, String sheetName, String header) {
        if (header == null || header.isEmpty()) {
            return ColumnRole.UNKNOWN;
        }
        String lower = header.toLowerCase(Locale.ROOT);

        // The dictionary is the file's own statement of what a column is, so it wins.
        StarDictionary dictionary = new StarDictionary(structure);
        if (dictionary.isVariable(header)) {
            return ColumnRole.VARIABLE;
        }

        switch (lower) {
            case COLUMN_EXPERIMENT:
                return ColumnRole.TRIAL;
            case COLUMN_PROJECT:
                return ColumnRole.PROJECT;
            case COLUMN_PLOT:
                return ColumnRole.OBJECT;
            case COLUMN_CULTIVAR:
                return ColumnRole.GERMPLASM;
            case COLUMN_TREATMENT:
                // A treatment code names the level of a factor declared on the experiment.
                return ColumnRole.FACTOR_LEVEL;
            case COLUMN_BLOCK:
                // A block groups plots. It is not a treatment: two plots in the same block can
                // carry different treatments, which is the point of blocking.
                return ColumnRole.PARENT_OBJECT;
            case COLUMN_OBSERVATION_DATE:
            case COLUMN_START_DATE:
            case COLUMN_END_DATE:
                return ColumnRole.DATE;
            case COLUMN_EMAIL:
                return ColumnRole.OBSERVER;
            case COLUMN_OBJECTIVE:
            case COLUMN_DESCRIPTION:
                return ColumnRole.COMMENT;
            default:
                break;
        }
        if (POSITION_COLUMNS.contains(lower)) {
            return ColumnRole.POSITION;
        }
        if (OBJECT_PROPERTY_COLUMNS.contains(lower)) {
            return ColumnRole.OBJECT_PROPERTY;
        }
        if (LOCATION_COLUMNS.contains(lower) || lower.equals(COLUMN_FIELD)
                || lower.equals(COLUMN_FIELD_NAME)) {
            return ColumnRole.LOCATION;
        }
        if (DESCRIPTION_COLUMNS.contains(lower)) {
            return ColumnRole.COMMENT;
        }
        if (ExcelValueParser.looksLikeDateColumn(header)) {
            return ColumnRole.DATE;
        }
        // Described but not declared a variable, and not a column this profile knows: say so rather
        // than calling it a measurement.
        return dictionary.get(header).isPresent() ? ColumnRole.COMMENT : ColumnRole.UNKNOWN;
    }

    @Override
    public ExtractedImportPlan extract(WorkbookStructure structure) {
        ExtractedImportPlan plan = new ExtractedImportPlan().setProfileId(ID);
        StarSheets sheets = new StarSheets(structure);
        StarDictionary dictionary = new StarDictionary(structure);

        readExperiment(sheets, plan);
        readField(sheets, plan);
        readPlots(sheets, plan);
        readTreatments(sheets, plan);
        readVariables(dictionary, plan);

        dictionary.getNotes().forEach(plan::addAnomaly);
        checkUndeclaredDataColumns(sheets, dictionary, plan);
        checkDataSheetsNameTheirTarget(sheets, plan);
        checkPlotIdentifiers(sheets, plan);

        return plan;
    }

    //#region reading each entity

    private void readExperiment(StarSheets sheets, ExtractedImportPlan plan) {
        Optional<SheetStructure> found = sheets.experiment();
        if (!found.isPresent()) {
            plan.addAnomaly(ReportMessage.of(ANOMALY + "experimentSheetMissing",
                    "The 'expe' sheet is missing, so the experiment this file describes cannot be "
                            + "identified."));
            return;
        }
        SheetStructure sheet = found.get();

        plan.getExperimentNames().addAll(sheet.distinctValues(COLUMN_EXPERIMENT));
        plan.getProjectNames().addAll(sheet.distinctValues(COLUMN_PROJECT));

        note(plan, "objective", sheet, COLUMN_OBJECTIVE);
        note(plan, "experiment description", sheet, COLUMN_DESCRIPTION);
        note(plan, "design plan", sheet, COLUMN_DESIGN);
        note(plan, "organisation", sheet, COLUMN_ORGANIZATION);
        note(plan, "research unit", sheet, COLUMN_SUBORGANIZATION);
        note(plan, "contact", sheet, COLUMN_EMAIL);

        experimentDate(sheet, COLUMN_START_DATE).ifPresent(date ->
                plan.note("declared start date", date.toString()));
        experimentDate(sheet, COLUMN_END_DATE).ifPresent(date ->
                plan.note("declared end date", date.toString()));
    }

    private void readField(StarSheets sheets, ExtractedImportPlan plan) {
        Optional<SheetStructure> found = sheets.field();
        if (!found.isPresent()) {
            return;
        }
        SheetStructure sheet = found.get();

        plan.getGermplasmNames().addAll(sheet.distinctValues(COLUMN_CULTIVAR));
        plan.getFacilityNames().addAll(nonEmpty(sheet, COLUMN_FIELD_NAME, COLUMN_FIELD));

        note(plan, "commune", sheet, "commune_name");
    }

    private void readPlots(StarSheets sheets, ExtractedImportPlan plan) {
        Optional<SheetStructure> found = sheets.plots();
        if (!found.isPresent()) {
            plan.addAnomaly(ReportMessage.of(ANOMALY + "plotSheetMissing",
                    "No plot sheet was found, so the observations have nothing to attach to."));
            return;
        }
        SheetStructure sheet = found.get();
        plan.getScientificObjectNames().addAll(sheet.distinctValues(COLUMN_PLOT));
        plan.note("unit plots", String.valueOf(sheet.getDataRowCount()));

        askForTheObjectType(sheet, plan);
        askHowBlocksAreModelled(sheet, plan);

        List<String> blocks = sheet.distinctValues(COLUMN_BLOCK);
        if (!blocks.isEmpty()) {
            plan.note("blocks", String.join(", ", blocks));
        }
    }

    private void readTreatments(StarSheets sheets, ExtractedImportPlan plan) {
        sheets.treatments().ifPresent(sheet -> {
            List<String> codes = sheet.distinctValues(COLUMN_TREATMENT);
            if (!codes.isEmpty()) {
                plan.note("treatments", String.join(", ", codes));
            }
        });
    }

    private void readVariables(StarDictionary dictionary, ExtractedImportPlan plan) {
        if (!dictionary.isPresent()) {
            plan.addAnomaly(ReportMessage.of(ANOMALY + "dictionaryMissing",
                    "No dictionary sheet was found, so the columns cannot be resolved to their "
                            + "unit, type or ontology identifier."));
            return;
        }
        for (StarDictionaryEntry entry : dictionary.getVariables()) {
            plan.getVariables().add(new VariableCandidate(entry.getName())
                    .setLabel(entry.getDescription())
                    .setExternalId(entry.getUri()));
        }
        plan.note("dictionary entries", String.valueOf(dictionary.getEntries().size()));
    }

    //#endregion

    //#region what is worth raising

    /**
     * A column measured in a data sheet but not declared a variable would be dropped on import
     * without anyone noticing.
     */
    private void checkUndeclaredDataColumns(StarSheets sheets, StarDictionary dictionary,
                                            ExtractedImportPlan plan) {
        Set<String> undeclared = new LinkedHashSet<>();
        for (SheetStructure sheet : sheets.dataSheets()) {
            for (String header : sheet.getHeaders()) {
                if (header.isEmpty() || isStructural(header) || dictionary.isVariable(header)) {
                    continue;
                }
                undeclared.add(header);
            }
        }
        if (!undeclared.isEmpty()) {
            plan.addAnomaly(ReportMessage.of(ANOMALY + "undeclaredVariables",
                            "These columns hold observations but the dictionary does not declare "
                                    + "them as variables: " + String.join(", ", undeclared)
                                    + ". They would be left out of an import. Ask the user whether "
                                    + "they are measurements, and add them to the dictionary if so.")
                    .with("columns", String.join(", ", undeclared)));
        }
    }

    /**
     * A data sheet must say what each row was observed on. Without that column its rows cannot be
     * attached to anything, and they would be dropped in silence.
     */
    private void checkDataSheetsNameTheirTarget(StarSheets sheets, ExtractedImportPlan plan) {
        List<String> nameless = new ArrayList<>();
        for (SheetStructure sheet : sheets.dataSheets()) {
            if (objectColumnOf(sheet) == null) {
                nameless.add(sheet.getName());
            }
        }
        if (!nameless.isEmpty()) {
            plan.addAnomaly(ReportMessage.of(ANOMALY + "sheetsNameNoObject",
                            "These observation sheets name neither a plot ('" + COLUMN_PLOT
                                    + "') nor a field ('" + COLUMN_FIELD + "'), so their rows "
                                    + "cannot be attached to anything: "
                                    + String.join(", ", nameless)
                                    + ". The 'data_template' sheet shows the expected shape: an "
                                    + "object identifier, a date, then the variables.")
                    .with("plotColumn", COLUMN_PLOT)
                    .with("fieldColumn", COLUMN_FIELD)
                    .with("sheets", String.join(", ", nameless)));
        }
    }

    /**
     * The file says how many plants each plot holds, never what a plot <em>is</em>.
     * <p>
     * That type decides what can be observed on it and how it is created, and no column carries it.
     * Raised rather than defaulted: creating several hundred objects under a type nobody chose is
     * not undone by editing one of them.
     */
    private void askForTheObjectType(SheetStructure sheet, ExtractedImportPlan plan) {
        plan.note(NOTE_OBJECT_TYPE, "undecided");
        if (sheet.hasHeader(COLUMN_PLANT_COUNT)) {
            plan.addAnomaly(ReportMessage.of(ANOMALY + "objectTypeUndecided",
                    "The plot sheet counts plants per plot ('" + COLUMN_PLANT_COUNT + "') but never "
                            + "says what a plot is. Ask the user which scientific object type these "
                            + "should be created under before creating any of them.")
                    .with("column", COLUMN_PLANT_COUNT));
            return;
        }
        plan.addAnomaly(ReportMessage.of(ANOMALY + "objectTypeUndecidedNoCount",
                "Nothing in the file says what type of scientific object the plots are. Ask the "
                        + "user before creating them."));
    }

    /**
     * A block can be an object or a property, and the file does not choose.
     */
    private void askHowBlocksAreModelled(SheetStructure sheet, ExtractedImportPlan plan) {
        if (!sheet.hasHeader(COLUMN_BLOCK) || sheet.distinctValues(COLUMN_BLOCK).isEmpty()) {
            return;
        }
        plan.addAnomaly(ReportMessage.of(ANOMALY + "blockModelling",
                        "The plots are grouped into blocks ('" + COLUMN_BLOCK + "'). A block can be "
                                + "a scientific object of its own, with the plots as its parts, or "
                                + "a property carried by each plot. Ask the user which: the first "
                                + "is right when something is observed on the block itself.")
                .with("column", COLUMN_BLOCK));
    }

    private void checkPlotIdentifiers(StarSheets sheets, ExtractedImportPlan plan) {
        PlotIdReconciliation reconciliation =
                new PlotIdReconciliation(sheets.plots().orElse(null), sheets.dataSheets());
        String description = reconciliation.describe();
        if (description != null) {
            // The reconciliation writes its own sentence, parameters and all, so it travels as
            // plain text: a key with a pre-built sentence behind it would say it twice.
            plan.addAnomaly(ReportMessage.plain(description));
            // Recorded as a note too, and not only as an anomaly: the insertion has to ask the user
            // to confirm this exact sentence, and an anomaly list is prose for the assistant.
            plan.note(NOTE_RECONCILIATION, description);
        }
        if (!reconciliation.getUnmatchedDataIds().isEmpty()) {
            plan.addAnomaly(ReportMessage.of(ANOMALY + "unmatchedPlotIdentifiers",
                            "These plot identifiers appear in the observations but match no plot: "
                                    + String.join(", ", reconciliation.getUnmatchedDataIds()) + ".")
                    .with("values", String.join(", ", reconciliation.getUnmatchedDataIds())));
        }
    }

    //#endregion

    /**
     * Reads what happened during the trial, from the two sheets that record it.
     * <p>
     * {@code evenement} holds trial-wide occurrences — an observation round, a hailstorm — with no
     * column saying what they concerned, so they concern the field. {@code ppp} holds plant
     * protection product applications, which name the treatment they were applied to, so they
     * concern the plots under that treatment.
     */
    @Override
    public List<EventCandidate> extractEvents(WorkbookStructure structure) {
        StarSheets sheets = new StarSheets(structure);
        List<EventCandidate> events = new ArrayList<>();

        sheets.events().ifPresent(sheet ->
                readTrialEvents(sheet, structure, fieldName(sheets), events));
        sheets.treatmentApplications().ifPresent(sheet ->
                readApplications(sheet, structure, sheets, events));

        return events;
    }

    private void readTrialEvents(SheetStructure sheet, WorkbookStructure structure,
                                 String fieldName, List<EventCandidate> events) {
        if (!sheet.hasHeader(COLUMN_EVENT_DATE)) {
            return;
        }
        for (int i = 0; i < sheet.getRows().size(); i++) {
            List<String> row = sheet.getRows().get(i);
            LocalDate date = asDate(COLUMN_EVENT_DATE, sheet.cell(row, COLUMN_EVENT_DATE),
                    structure.isDate1904());
            if (date == null) {
                continue;
            }
            EventCandidate event = new EventCandidate()
                    .setSheet(sheet.getName())
                    .setRowNumber(i + 2)
                    .setDate(date)
                    .setTypeLabel(sheet.cell(row, COLUMN_EVENT_TYPE))
                    .setDescription(sheet.cell(row, COLUMN_EVENT_DESCRIPTION))
                    .setTargetKind(DataPoint.TargetKind.FACILITY);
            // No column says what the event concerned, and the sheet sits at trial level: it
            // happened to the field. Naming the field rather than every plot keeps one event
            // where the file records one.
            event.addTarget(fieldName);
            events.add(event);
        }
    }

    /**
     * A spraying is recorded once per treatment, and a treatment covers several plots, so one row
     * becomes one event concerning all of them — which is what happened, a single pass of the
     * sprayer over the plots of that treatment.
     */
    private void readApplications(SheetStructure sheet, WorkbookStructure structure,
                                  StarSheets sheets, List<EventCandidate> events) {
        if (!sheet.hasHeader(COLUMN_APPLICATION_DATE)) {
            return;
        }
        for (int i = 0; i < sheet.getRows().size(); i++) {
            List<String> row = sheet.getRows().get(i);
            LocalDate date = asDate(COLUMN_APPLICATION_DATE,
                    sheet.cell(row, COLUMN_APPLICATION_DATE), structure.isDate1904());
            if (date == null) {
                continue;
            }
            EventCandidate event = new EventCandidate()
                    .setSheet(sheet.getName())
                    .setRowNumber(i + 2)
                    .setDate(date)
                    .setTypeLabel("Application phytosanitaire")
                    .setDescription(applicationDescription(sheet, row))
                    .setTargetKind(DataPoint.TargetKind.SCIENTIFIC_OBJECT);
            plotsOfTreatment(sheets, sheet.cell(row, COLUMN_TREATMENT))
                    .forEach(event::addTarget);
            events.add(event);
        }
    }

    /**
     * Everything the file says about the application, in one sentence — the vocabulary has no
     * class for a spraying, so the detail has nowhere else to go and must not be dropped.
     */
    private String applicationDescription(SheetStructure sheet, List<String> row) {
        StringBuilder text = new StringBuilder();
        append(text, sheet.cell(row, COLUMN_PRODUCT_NAME));
        String marketingId = sheet.cell(row, COLUMN_PRODUCT_MARKETING_ID);
        if (!marketingId.isEmpty()) {
            append(text, "AMM " + marketingId);
        }
        String dose = sheet.cell(row, COLUMN_DOSE);
        if (!dose.isEmpty()) {
            append(text, (dose + " " + sheet.cell(row, COLUMN_DOSE_UNIT)).trim());
        }
        String volume = sheet.cell(row, COLUMN_APPLICATION_VOLUME);
        if (!volume.isEmpty()) {
            append(text, volume + " L/ha");
        }
        append(text, sheet.cell(row, COLUMN_SPRAYER));
        String code = sheet.cell(row, COLUMN_APPLICATION_CODE);
        if (!code.isEmpty()) {
            append(text, "application " + code);
        }
        return text.toString();
    }

    private void append(StringBuilder text, String part) {
        if (part == null || part.trim().isEmpty()) {
            return;
        }
        if (text.length() > 0) {
            text.append(", ");
        }
        text.append(part.trim());
    }

    private List<String> plotsOfTreatment(StarSheets sheets, String treatment) {
        List<String> plots = new ArrayList<>();
        if (treatment == null || treatment.trim().isEmpty()) {
            return plots;
        }
        sheets.plots().ifPresent(plotSheet -> {
            if (!plotSheet.hasHeader(COLUMN_PLOT) || !plotSheet.hasHeader(COLUMN_TREATMENT)) {
                return;
            }
            for (List<String> row : plotSheet.getRows()) {
                if (treatment.trim().equalsIgnoreCase(plotSheet.cell(row, COLUMN_TREATMENT))) {
                    String plot = plotSheet.cell(row, COLUMN_PLOT);
                    if (!plot.isEmpty()) {
                        plots.add(plot);
                    }
                }
            }
        });
        return plots;
    }

    private String fieldName(StarSheets sheets) {
        return sheets.field()
                .filter(sheet -> sheet.hasHeader(COLUMN_FIELD))
                .filter(sheet -> !sheet.getRows().isEmpty())
                .map(sheet -> sheet.cell(sheet.getRows().get(0), COLUMN_FIELD))
                .orElse(null);
    }

    @Override
    public List<DataPoint> extractDataPoints(WorkbookStructure structure) {
        StarSheets sheets = new StarSheets(structure);
        StarDictionary dictionary = new StarDictionary(structure);
        PlotIdReconciliation reconciliation =
                new PlotIdReconciliation(sheets.plots().orElse(null), sheets.dataSheets());

        // The two sheets disagree on how a plot is named, and the observations are extracted
        // anyway, recomposed. Returning nothing used to hide the disagreement instead of raising
        // it: the page showed no observation and no reason. The recomposition is deterministic and
        // says what it did, so the right place to stop is the confirmation before writing, not the
        // reading — which is where every other decision of this kind already lives.
        // A weather sheet of the earlier revision names no object at all: the observations are the
        // field's, and the workbook declares one. Resolved here rather than guessed inside the
        // loop, and only when there is exactly one field to mean.
        String soleField = sheets.field().isPresent() ? soleFieldName(sheets) : null;

        List<DataPoint> points = new ArrayList<>();
        for (SheetStructure sheet : sheets.dataSheets()) {
            collect(sheet, dictionary, reconciliation, structure.isDate1904(), soleField, points);
        }
        return points;
    }

    private void collect(SheetStructure sheet, StarDictionary dictionary,
                         PlotIdReconciliation reconciliation, boolean date1904,
                         String soleField, List<DataPoint> points) {
        String objectColumn = objectColumnOf(sheet);
        String dateColumn = dateColumnOf(sheet, dictionary);
        if (dateColumn == null) {
            return;
        }
        if (objectColumn == null && soleField == null) {
            // Nothing says what was observed, and nothing can stand in for it. Reported by
            // checkDataSheets, which raises a sheet that names no object.
            return;
        }
        DataPoint.TargetKind kind = objectColumn == null || COLUMN_FIELD.equalsIgnoreCase(objectColumn)
                ? DataPoint.TargetKind.FACILITY
                : DataPoint.TargetKind.SCIENTIFIC_OBJECT;

        List<String> variableColumns = new ArrayList<>();
        for (String header : sheet.getHeaders()) {
            if (dictionary.isVariable(header)) {
                variableColumns.add(header);
            }
        }

        for (int i = 0; i < sheet.getRows().size(); i++) {
            List<String> row = sheet.getRows().get(i);
            // The header is row 1, so the first data row is row 2.
            int rowNumber = i + 2;

            String target = objectColumn == null ? soleField : sheet.cell(row, objectColumn);
            if (target == null || target.isEmpty()) {
                continue;
            }
            if (kind == DataPoint.TargetKind.SCIENTIFIC_OBJECT) {
                // Only plots are written two ways; a field is named the same everywhere.
                target = reconciliation.resolve(target);
                if (target == null) {
                    continue;
                }
            }
            LocalDate date = asDate(dateColumn, sheet.cell(row, dateColumn), date1904);
            if (date == null) {
                continue;
            }

            for (String column : variableColumns) {
                String value = sheet.cell(row, column);
                if (ExcelValueParser.isMissing(value)) {
                    continue;
                }
                points.add(new DataPoint()
                        .setSheet(sheet.getName())
                        .setRowNumber(rowNumber)
                        .setObjectName(target)
                        .setTargetKind(kind)
                        .setDate(date)
                        .setVariableKey(column)
                        .setRawValue(value));
            }
        }
    }

    //#region helpers

    /**
     * Finds the column naming what was observed.
     * <p>
     * By role rather than by position: {@code data_F1} puts it third and {@code data_G1} first, so
     * the shape shown by {@code data_template} — identifier, date, variables — describes the
     * contract, not the column order.
     *
     * @return {@code plot_id} for a plot, {@code field_id} for the field, or {@code null} when the
     * sheet names neither and its rows cannot be attached to anything
     */
    /**
     * @return the field's identifier when the workbook declares exactly one, null otherwise. More
     *         than one field and there is nothing to stand in for a missing object column.
     */
    private String soleFieldName(StarSheets sheets) {
        return sheets.field()
                .filter(sheet -> sheet.hasHeader(COLUMN_FIELD))
                .filter(sheet -> sheet.getRows().size() == 1)
                .map(sheet -> sheet.cell(sheet.getRows().get(0), COLUMN_FIELD))
                .filter(name -> !name.isEmpty())
                .orElse(null);
    }

    private String objectColumnOf(SheetStructure sheet) {
        if (sheet.hasHeader(COLUMN_PLOT)) {
            return COLUMN_PLOT;
        }
        if (sheet.hasHeader(COLUMN_FIELD)) {
            return COLUMN_FIELD;
        }
        return null;
    }

    /**
     * Finds the column dating each row.
     * <p>
     * The dictionary is asked first: the file declares the type of every column, and a declaration
     * beats a guess from the header's spelling. The name heuristic is only the fallback, for a
     * column the dictionary does not describe.
     */
    private String dateColumnOf(SheetStructure sheet, StarDictionary dictionary) {
        if (sheet.hasHeader(COLUMN_OBSERVATION_DATE)) {
            return COLUMN_OBSERVATION_DATE;
        }
        for (String header : sheet.getHeaders()) {
            if (declaresADate(dictionary, header)) {
                return header;
            }
        }
        for (String header : sheet.getHeaders()) {
            if (ExcelValueParser.looksLikeDateColumn(header)) {
                return header;
            }
        }
        return null;
    }

    private boolean declaresADate(StarDictionary dictionary, String header) {
        return dictionary.get(header)
                .map(entry -> XSDDatatype.XSDdate.getURI()
                        .equals(StarDictionary.toDatatype(entry.getRClass()))
                        || XSDDatatype.XSDdateTime.getURI()
                        .equals(StarDictionary.toDatatype(entry.getRClass())))
                .orElse(false);
    }

    private boolean isStructural(String header) {
        String lower = header.toLowerCase(Locale.ROOT);
        return lower.equals(COLUMN_PLOT) || lower.equals(COLUMN_FIELD)
                || lower.equals(COLUMN_BBCH) || lower.equals(COLUMN_OBSERVATION_DATE)
                || ExcelValueParser.looksLikeDateColumn(header);
    }

    private Optional<LocalDate> experimentDate(SheetStructure sheet, String column) {
        for (String value : sheet.distinctValues(column)) {
            LocalDate date = asDate(column, value, false);
            if (date != null) {
                return Optional.of(date);
            }
        }
        return Optional.empty();
    }

    private LocalDate asDate(String header, String value, boolean date1904) {
        CellValue parsed = ExcelValueParser.parse(header, value, date1904);
        if (parsed.getDate() != null) {
            return parsed.getDate();
        }
        try {
            return LocalDate.parse(value);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private void note(ExtractedImportPlan plan, String label, SheetStructure sheet, String column) {
        List<String> values = sheet.distinctValues(column);
        if (!values.isEmpty()) {
            plan.note(label, String.join(", ", values));
        }
    }

    private List<String> nonEmpty(SheetStructure sheet, String preferred, String fallback) {
        List<String> values = sheet.distinctValues(preferred);
        return values.isEmpty() ? sheet.distinctValues(fallback) : values;
    }

    //#endregion
}
