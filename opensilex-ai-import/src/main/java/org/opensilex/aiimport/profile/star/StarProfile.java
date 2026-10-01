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
import org.opensilex.aiimport.profile.FactorLevelCandidate;
import org.opensilex.aiimport.profile.ObjectRow;
import org.opensilex.aiimport.profile.ObjectSheetDefaults;
import org.opensilex.aiimport.profile.ObjectTargets;
import org.opensilex.aiimport.profile.ImportProfile;
import org.opensilex.aiimport.profile.VariableCandidate;
import org.opensilex.aiimport.report.ReportMessage;
import org.opensilex.aiimport.workbook.CellValue;
import org.opensilex.aiimport.workbook.ExcelValueParser;
import org.opensilex.aiimport.workbook.SheetStructure;
import org.opensilex.aiimport.workbook.WorkbookStructure;

import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
    public static final String COLUMN_ROW_SPACING = "row_spacing";
    public static final String COLUMN_PLANT_SPACING = "plant_spacing";

    /**
     * The commune of the field. Named {@code commune_name} by the earlier revisions; both are read.
     */
    public static final String COLUMN_TOWN = "town_name";
    public static final String COLUMN_COMMUNE = "commune_name";
    public static final String COLUMN_INSEE = "commune_insee_id";

    /**
     * The field's centroid, in decimal degrees (WGS84): the facility's location, a WKT point.
     */
    public static final String COLUMN_LATITUDE = "field_latitude";
    public static final String COLUMN_LONGITUDE = "field_longitude";

    /**
     * The date of a weather reading, which the reference template writes with its time.
     */
    public static final String COLUMN_METEO_DATETIME = "meteo_datetime";

    public static final String COLUMN_PLOT = "plot_id";
    public static final String COLUMN_TREATMENT = "xp_trt_code";
    public static final String COLUMN_TREATMENT_NAME = "xp_trt_name";
    public static final String COLUMN_TREATMENT_DESCRIPTION = "xp_trt_desc";

    /**
     * Columns an exported workbook adds to a design sheet: the object each one is part of, and the
     * type of the objects of the sheet — read back as the suggested type.
     */
    public static final String COLUMN_PARENT = "parent_id";
    public static final String COLUMN_OBJECT_TYPE = "object_type";

    /**
     * The factor a treatment belongs to, which an exported workbook writes beside each level.
     */
    public static final String COLUMN_FACTOR = "factor_name";

    /**
     * The details a field's report row carries, as keys the facility form reads.
     */
    public static final String FIELD_TOWN = "town";
    public static final String FIELD_INSEE = "insee";
    public static final String FIELD_LATITUDE = "latitude";
    public static final String FIELD_LONGITUDE = "longitude";
    public static final String FIELD_ROW_SPACING = "row_spacing";
    public static final String FIELD_PLANT_SPACING = "plant_spacing";

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
            COLUMN_LATITUDE, COLUMN_LONGITUDE, COLUMN_TOWN, COLUMN_COMMUNE, COLUMN_INSEE));

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
            Arrays.asList(COLUMN_ROW_SPACING, COLUMN_PLANT_SPACING, "plot_n"));

    private static final List<String> DESCRIPTION_COLUMNS = Collections.unmodifiableList(
            Arrays.asList("plot_desc", "xp_trt_desc", "xp_trt_name", "event_description"));

    /**
     * Identifiers a data sheet can carry that name no observed object.
     */
    private static final Set<String> NOT_OBJECT_IDENTIFIERS = Set.of(
            COLUMN_FIELD, COLUMN_EXPERIMENT, COLUMN_PROJECT, COLUMN_INSEE);

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
                "- 'field*': the field, a facility — row and plant spacing, commune ('town_name',",
                "  'commune_name' in older files), INSEE code, and the latitude and longitude of its",
                "  centroid. Older files call it 'ed_parcelle' or 'parcelle'.",
                "- 'ed_*' (or the same names unprefixed in older files): the experimental design,",
                "  one kind of scientific object per sheet — 'ed_placette' holds the unit plots.",
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
                "- the field sheet becomes a facility, with its commune as address and its latitude",
                "  and longitude as location.",
                "- 'meteo' holds weather variables measured on the field: they attach to the facility.",
                "- each 'ed_*' sheet becomes scientific objects, of a type chosen for that sheet.",
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
        readOrganizations(sheet, plan);
        note(plan, "contact", sheet, COLUMN_EMAIL);

        experimentDate(sheet, COLUMN_START_DATE).ifPresent(date ->
                plan.note("declared start date", date.toString()));
        experimentDate(sheet, COLUMN_END_DATE).ifPresent(date ->
                plan.note("declared end date", date.toString()));
    }

    /**
     * The institution and the unit that run the trial, both to be declared in the instance: the
     * unit as part of the institution named on the same row.
     */
    private void readOrganizations(SheetStructure sheet, ExtractedImportPlan plan) {
        for (List<String> row : sheet.getRows()) {
            String institution = sheet.hasHeader(COLUMN_ORGANIZATION) ? sheet.cell(row, COLUMN_ORGANIZATION) : "";
            String unit = sheet.hasHeader(COLUMN_SUBORGANIZATION) ? sheet.cell(row, COLUMN_SUBORGANIZATION) : "";
            if (!institution.isEmpty() && !plan.getOrganizationNames().contains(institution)) {
                plan.getOrganizationNames().add(institution);
            }
            if (!unit.isEmpty() && !plan.getOrganizationNames().contains(unit)) {
                plan.getOrganizationNames().add(unit);
                if (!institution.isEmpty()) {
                    plan.getOrganizationParents().put(unit, institution);
                }
            }
        }
    }

    private void readField(StarSheets sheets, ExtractedImportPlan plan) {
        Optional<SheetStructure> found = sheets.field();
        if (!found.isPresent()) {
            return;
        }
        SheetStructure sheet = found.get();

        plan.getGermplasmNames().addAll(sheet.distinctValues(COLUMN_CULTIVAR));
        plan.getFacilityNames().addAll(nonEmpty(sheet, COLUMN_FIELD_NAME, COLUMN_FIELD));

        note(plan, "commune", sheet, sheet.hasHeader(COLUMN_TOWN) ? COLUMN_TOWN : COLUMN_COMMUNE);
        readFieldDetails(sheet, plan);
    }

    /**
     * What the field sheet says of each field, for the facility form to start from: its commune,
     * its INSEE code, the coordinates of its centroid — a WKT point once created — and how it is
     * planted, which no core facility property holds and which is kept in its description.
     */
    private void readFieldDetails(SheetStructure sheet, ExtractedImportPlan plan) {
        String townColumn = sheet.hasHeader(COLUMN_TOWN) ? COLUMN_TOWN : COLUMN_COMMUNE;
        for (List<String> row : sheet.getRows()) {
            String name = sheet.hasHeader(COLUMN_FIELD_NAME) ? sheet.cell(row, COLUMN_FIELD_NAME) : "";
            if (name.isEmpty() && sheet.hasHeader(COLUMN_FIELD)) {
                name = sheet.cell(row, COLUMN_FIELD);
            }
            if (name.isEmpty()) {
                continue;
            }
            Map<String, String> details = new LinkedHashMap<>();
            for (String[] detail : new String[][]{{FIELD_TOWN, townColumn}, {FIELD_INSEE, COLUMN_INSEE},
                    {FIELD_LATITUDE, COLUMN_LATITUDE}, {FIELD_LONGITUDE, COLUMN_LONGITUDE},
                    {FIELD_ROW_SPACING, COLUMN_ROW_SPACING}, {FIELD_PLANT_SPACING, COLUMN_PLANT_SPACING}}) {
                String value = sheet.hasHeader(detail[1]) ? sheet.cell(row, detail[1]) : "";
                if (!value.isEmpty()) {
                    details.put(detail[0], value);
                }
            }
            if (!details.isEmpty()) {
                plan.getFacilityDetails().put(name, details);
            }
        }
    }

    private void readPlots(StarSheets sheets, ExtractedImportPlan plan) {
        List<SheetStructure> objectSheets = sheets.objectSheets();
        if (objectSheets.isEmpty()) {
            plan.addAnomaly(ReportMessage.of(ANOMALY + "plotSheetMissing",
                    "No plot sheet was found, so the observations have nothing to attach to."));
            return;
        }
        for (SheetStructure objects : objectSheets) {
            plan.getScientificObjectNames().addAll(objects.distinctValues(StarSheets.identifierColumn(objects)));
        }
        if (objectSheets.size() > 1) {
            plan.note("object sheets", objectSheets.stream().map(SheetStructure::getName)
                    .collect(Collectors.joining(", ")));
        }
        SheetStructure sheet = sheets.plots().orElse(objectSheets.get(0));
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

    /**
     * The objects of every design sheet, one per row, hosted by the field — the plots of
     * {@code ed_placette}, and the objects of any other {@code ed_} sheet, each sheet getting the
     * type the user chooses for it.
     * <p>
     * Names come from the design sheets, which are the reference: the data sheets write the same
     * plots the other way round and are reconciled to them, never the reverse.
     */
    @Override
    public List<ObjectRow> extractObjectRows(WorkbookStructure structure) {
        StarSheets sheets = new StarSheets(structure);
        String field = hostingField(sheets);
        String fieldCultivar = fieldValue(sheets, COLUMN_CULTIVAR);

        List<ObjectRow> rows = new ArrayList<>();
        for (SheetStructure sheet : sheets.objectSheets()) {
            String idColumn = StarSheets.identifierColumn(sheet);
            String prefix = prefixOf(idColumn);
            for (int i = 0; i < sheet.getRows().size(); i++) {
                List<String> row = sheet.getRows().get(i);
                String name = sheet.cell(row, idColumn);
                if (name.isEmpty()) {
                    continue;
                }
                String cultivar = sheet.hasHeader(COLUMN_CULTIVAR) ? sheet.cell(row, COLUMN_CULTIVAR) : "";
                rows.add(new ObjectRow(sheet.getName(), i + 2, name)
                        .setGermplasm(cultivar.isEmpty() ? fieldCultivar : cultivar)
                        .setFactorLevel(sheet.cell(row, COLUMN_TREATMENT))
                        .setFacility(field)
                        .setPosition(sheet.cell(row, prefix + "_x"), sheet.cell(row, prefix + "_y"))
                        .setCells(sheet.getHeaders(), row));
            }
        }
        return rows;
    }

    /**
     * What the columns STAR names become, and the type an exported workbook states in its
     * {@code object_type} column. A column the dictionary ties to the full URI of a property — as
     * an exported workbook does for the type's own properties — starts mapped to it.
     */
    @Override
    public ObjectSheetDefaults objectSheetDefaults(WorkbookStructure structure, String sheetName) {
        Optional<SheetStructure> found = new StarSheets(structure).objectSheets().stream()
                .filter(sheet -> sheet.getName().equals(sheetName)).findFirst();
        if (!found.isPresent()) {
            return ObjectSheetDefaults.none();
        }
        SheetStructure sheet = found.get();
        String idColumn = StarSheets.identifierColumn(sheet);
        String prefix = prefixOf(idColumn);
        StarDictionary dictionary = new StarDictionary(structure);

        Map<String, String> targets = new LinkedHashMap<>();
        for (String header : sheet.getHeaders()) {
            if (!header.isEmpty()) {
                targets.put(header, defaultTarget(header, idColumn, prefix, dictionary));
            }
        }
        List<String> types = sheet.hasHeader(COLUMN_OBJECT_TYPE)
                ? sheet.distinctValues(COLUMN_OBJECT_TYPE)
                : Collections.emptyList();
        URI suggested = types.size() == 1 ? toUri(types.get(0)) : null;
        return new ObjectSheetDefaults(idColumn, targets, suggested);
    }

    private String defaultTarget(String header, String idColumn, String prefix, StarDictionary dictionary) {
        String lower = header.toLowerCase(Locale.ROOT);
        if (header.equalsIgnoreCase(idColumn)) {
            return ObjectTargets.NAME;
        }
        if (lower.equals(COLUMN_CULTIVAR)) {
            return ObjectTargets.GERMPLASM;
        }
        if (lower.equals(COLUMN_TREATMENT)) {
            return ObjectTargets.FACTOR_LEVEL;
        }
        if (lower.equals(prefix + "_x")) {
            return ObjectTargets.X;
        }
        if (lower.equals(prefix + "_y")) {
            return ObjectTargets.Y;
        }
        if (lower.equals(prefix + "_desc")) {
            return ObjectTargets.COMMENT;
        }
        if (lower.equals(COLUMN_PARENT)) {
            return ObjectTargets.PARENT;
        }
        return dictionary.get(header).map(StarDictionaryEntry::getUri)
                .filter(uri -> uri != null && (uri.startsWith("http://") || uri.startsWith("https://")))
                .orElse(ObjectTargets.NONE);
    }

    /**
     * The treatments of the {@code modalite} sheet, one per row: the code the plots write, its
     * short name and its description — and the factor, when an exported workbook names it.
     */
    @Override
    public List<FactorLevelCandidate> extractFactorLevels(WorkbookStructure structure) {
        Optional<SheetStructure> found = new StarSheets(structure).treatments()
                .filter(sheet -> sheet.hasHeader(COLUMN_TREATMENT));
        if (!found.isPresent()) {
            return Collections.emptyList();
        }
        SheetStructure sheet = found.get();
        List<FactorLevelCandidate> levels = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (int i = 0; i < sheet.getRows().size(); i++) {
            List<String> row = sheet.getRows().get(i);
            String code = sheet.cell(row, COLUMN_TREATMENT);
            String factor = sheet.hasHeader(COLUMN_FACTOR) ? sheet.cell(row, COLUMN_FACTOR) : "";
            if (code.isEmpty() || !seen.add((factor + "|" + code).toLowerCase(Locale.ROOT))) {
                continue;
            }
            levels.add(new FactorLevelCandidate(sheet.getName(), i + 2, factor.isEmpty() ? null : factor, code,
                    emptyToNull(sheet.cell(row, COLUMN_TREATMENT_NAME)),
                    emptyToNull(sheet.cell(row, COLUMN_TREATMENT_DESCRIPTION))));
        }
        return levels;
    }

    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    /**
     * {@code plot} for {@code plot_id}: the prefix the other columns of the sheet share.
     */
    private static String prefixOf(String idColumn) {
        String lower = idColumn.toLowerCase(Locale.ROOT);
        return lower.endsWith("_id") ? lower.substring(0, lower.length() - 3) : lower;
    }

    private static URI toUri(String value) {
        try {
            URI uri = URI.create(value.trim());
            return uri.isAbsolute() ? uri : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * The field as the resolution names it — its name when it has one, its identifier otherwise —
     * so the object and the report look it up the same way.
     */
    private String hostingField(StarSheets sheets) {
        String name = fieldValue(sheets, COLUMN_FIELD_NAME);
        return name == null ? fieldValue(sheets, COLUMN_FIELD) : name;
    }

    private String fieldValue(StarSheets sheets, String column) {
        return sheets.field()
                .filter(sheet -> sheet.hasHeader(column) && !sheet.getRows().isEmpty())
                .map(sheet -> sheet.cell(sheet.getRows().get(0), column))
                .filter(value -> !value.isEmpty())
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
            if (COLUMN_PLOT.equalsIgnoreCase(objectColumn)) {
                // Only plots are written two ways; a field, or any other object, is named the same
                // everywhere.
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

    /**
     * Finds the column naming what was observed.
     * <p>
     * By role rather than by position: {@code data_F1} puts it third and {@code data_G1} first, so
     * the shape shown by {@code data_template} — identifier, date, variables — describes the
     * contract, not the column order. An identifier other than a plot's — {@code plant_id}, for
     * the objects of an {@code ed_plant} sheet — names an object too.
     *
     * @return {@code plot_id} or another object identifier for an object, {@code field_id} for the
     * field, or {@code null} when the sheet names neither and its rows cannot be attached to anything
     */
    private String objectColumnOf(SheetStructure sheet) {
        if (sheet.hasHeader(COLUMN_PLOT)) {
            return COLUMN_PLOT;
        }
        for (String header : sheet.getHeaders()) {
            String lower = header.toLowerCase(Locale.ROOT);
            if (lower.endsWith("_id") && !NOT_OBJECT_IDENTIFIERS.contains(lower)) {
                return header;
            }
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

    /**
     * A column that places or dates a row rather than measuring anything. An identifier column —
     * {@code plot_id}, {@code field_id}, or the {@code plant_id} of another type of object — is
     * never a measurement, whatever the dictionary says of it.
     */
    private boolean isStructural(String header) {
        String lower = header.toLowerCase(Locale.ROOT);
        return lower.endsWith("_id") || lower.equals(COLUMN_METEO_DATETIME)
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
