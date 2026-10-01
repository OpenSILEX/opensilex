//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.profile.vitis;

import org.opensilex.aiimport.mapping.ColumnRole;
import org.opensilex.aiimport.profile.DataPoint;
import org.opensilex.aiimport.profile.ExtractedImportPlan;
import org.opensilex.aiimport.profile.ImportProfile;
import org.opensilex.aiimport.profile.ObjectRow;
import org.opensilex.aiimport.profile.ObjectSheetDefaults;
import org.opensilex.aiimport.profile.ObjectTargets;
import org.opensilex.aiimport.profile.VariableCandidate;
import org.opensilex.aiimport.report.ReportMessage;
import org.opensilex.aiimport.workbook.CellValue;
import org.opensilex.aiimport.workbook.ExcelValueParser;
import org.opensilex.aiimport.workbook.SheetStructure;
import org.opensilex.aiimport.workbook.WorkbookStructure;
import org.opensilex.aiimport.workbook.HeaderMatcher;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

/**
 * Profile for the VitisExplorer grapevine observation template.
 * <p>
 * The template is a workbook of three fixed sheets followed by one sheet per phenological stage:
 * <ul>
 *   <li>{@code ReadMe} — the filling instructions written by the template author;</li>
 *   <li>{@code Chronologie} — the variable catalogue: label, variable name, short abbreviation and
 *       CropOntology identifier, plus which stages each variable is observed at;</li>
 *   <li>{@code Cartouche_Fixe} — one row per unit plot, repeated as the leading columns of every
 *       stage sheet;</li>
 *   <li>{@code 1_Hiver} … {@code 15_Fin_Cycle} — the cartouche columns, then {@code Date} and
 *       {@code Observateur}, then one column per variable observed at that stage, keyed by the
 *       abbreviation from {@code Chronologie}.</li>
 * </ul>
 *
 * @author Arnaud Charleroy
 */
public class VitisExplorerProfile implements ImportProfile {

    private static final String ANOMALY = "AiImport.report.anomaly.vitis.";

    public static final String ID = "vitis-explorer";

    public static final String CATALOGUE_SHEET = "Chronologie";
    public static final String CARTOUCHE_SHEET = "Cartouche_Fixe";
    public static final String README_SHEET = "ReadMe";

    public static final String COLUMN_TRIAL = "Dispositif";
    public static final String COLUMN_PLOT = "PU";
    public static final String COLUMN_GENOTYPE = "Genotype";
    public static final String COLUMN_STATUS = "Statut";
    public static final String COLUMN_SEASON = "Millesime";
    public static final String COLUMN_DATE = "Date";
    public static final String COLUMN_OBSERVER = "Observateur";
    public static final String COLUMN_FREE_COMMENT = "Obs_libre";

    /**
     * The cartouche columns, repeated at the start of every stage sheet, plus the two columns every
     * stage sheet adds. None of these is a variable.
     */
    /**
     * The columns that place the plot in the vineyard. They describe the scientific object, so they
     * are never observations.
     */
    /**
     * Columns that describe the unit plot itself: where it starts and ends in the row layout.
     * <p>
     * Properties of the object, not a location and not measurements. They are written once on the
     * scientific object when it is created, where a measurement would be written per observation.
     */
    private static final List<String> OBJECT_PROPERTY_COLUMNS = Collections.unmodifiableList(
            Arrays.asList("Premier_Rang", "Dernier_Rang", "Premiere_Souche", "Derniere_Souche"));

    private static final List<String> NON_VARIABLE_COLUMNS = Collections.unmodifiableList(Arrays.asList(
            COLUMN_TRIAL, COLUMN_PLOT, COLUMN_GENOTYPE, COLUMN_STATUS,
            "Premier_Rang", "Dernier_Rang", "Premiere_Souche", "Derniere_Souche",
            COLUMN_SEASON, COLUMN_DATE, COLUMN_OBSERVER, COLUMN_FREE_COMMENT));

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getLabel() {
        return "VitisExplorer grapevine observations";
    }

    @Override
    public int match(WorkbookStructure structure) {
        boolean hasCatalogue = structure.hasSheet(CATALOGUE_SHEET);
        boolean hasCartouche = structure.hasSheet(CARTOUCHE_SHEET);
        if (hasCatalogue && hasCartouche) {
            return 100;
        }
        // One of the two sheets renamed is still a strong signal, but leave room for a better match.
        if (hasCatalogue || hasCartouche) {
            return 40;
        }
        return 0;
    }

    @Override
    public String getPromptContext(WorkbookStructure structure) {
        return String.join("\n", Arrays.asList(
                "This workbook follows the VitisExplorer grapevine observation template.",
                "",
                "Sheet roles:",
                "- 'ReadMe': the filling instructions written by the template author. Treat them as",
                "  the authoritative description of the format.",
                "- 'Chronologie': the variable catalogue. Columns give the label, the variable name,",
                "  the short abbreviation used as a column header in the stage sheets, and a",
                "  CropOntology identifier of the form CO_356:xxxxxxx.",
                "- 'Cartouche_Fixe': one row per unit plot (PU). Its columns are repeated as the",
                "  leading columns of every stage sheet.",
                "- the numbered sheets: the cartouche columns, then 'Date' and 'Observateur', then",
                "  one column per variable observed at that phenological stage.",
                "",
                "Intended mapping onto OpenSILEX, to be confirmed with the user:",
                "- 'Dispositif' names the trial, and is the candidate experiment.",
                "- 'PU' identifies a unit plot, and is the candidate scientific object. The ReadMe",
                "  states the PU identifier must be unique.",
                "- 'Genotype' is the candidate germplasm.",
                "- 'Statut' is a treatment factor level: TT, TNT, DT or DNT.",
                "- 'Premier_Rang', 'Dernier_Rang', 'Premiere_Souche', 'Derniere_Souche' locate the",
                "  plot in the vineyard and belong on the scientific object, not in the data.",
                "- 'Millesime' is the season, and 'Date' the observation date.",
                "- 'Observateur' identifies who observed, and belongs in the data provenance.",
                "- 'Obs_libre' is a free comment, not a variable.",
                "",
                "Filling conventions stated by the template:",
                "- 'NA' means no value.",
                "- the decimal separator is a comma, and invisible characters must not be present.",
                "- dates are dd/MM/yyyy in the 1900 calendar system.",
                "- phenology values are interpolated to the 50% stage.",
                "- maturity controls are of two kinds, recorded in 'Type_CtrlMatu': CCM for a",
                "  running maturation control and MR for maturity at harvest."
        ));
    }

    @Override
    public ExtractedImportPlan extract(WorkbookStructure structure) {
        ExtractedImportPlan plan = new ExtractedImportPlan().setProfileId(ID);

        Map<String, VariableCandidate> catalogue = readCatalogue(structure, plan);
        readCartouche(structure, plan);
        readStageSheets(structure, plan, catalogue);
        checkDateSystem(structure, plan);
        checkSeasons(structure, plan);

        return plan;
    }

    @Override
    public ColumnRole roleOf(WorkbookStructure structure, String sheetName, String header) {
        if (header == null || header.isEmpty()) {
            return ColumnRole.UNKNOWN;
        }
        if (header.equalsIgnoreCase(COLUMN_TRIAL)) {
            return ColumnRole.TRIAL;
        }
        if (header.equalsIgnoreCase(COLUMN_PLOT)) {
            return ColumnRole.OBJECT;
        }
        if (header.equalsIgnoreCase(COLUMN_GENOTYPE)) {
            return ColumnRole.GERMPLASM;
        }
        if (header.equalsIgnoreCase(COLUMN_STATUS)) {
            return ColumnRole.FACTOR_LEVEL;
        }
        if (header.equalsIgnoreCase(COLUMN_SEASON)) {
            return ColumnRole.SEASON;
        }
        if (header.equalsIgnoreCase(COLUMN_OBSERVER)) {
            return ColumnRole.OBSERVER;
        }
        if (header.equalsIgnoreCase(COLUMN_FREE_COMMENT)) {
            return ColumnRole.COMMENT;
        }
        if (OBJECT_PROPERTY_COLUMNS.contains(header)) {
            return ColumnRole.OBJECT_PROPERTY;
        }
        // The plain Date column only dates the row. A stage column such as Deb_Date is the
        // observation itself, so it stays a variable.
        if (header.equalsIgnoreCase(COLUMN_DATE)) {
            return ColumnRole.DATE;
        }
        return ColumnRole.VARIABLE;
    }

    /**
     * The unit plots, one per row of the cartouche, which the template declares complete and
     * repeats on every stage sheet.
     */
    @Override
    public List<ObjectRow> extractObjectRows(WorkbookStructure structure) {
        Optional<SheetStructure> found = structure.getSheet(CARTOUCHE_SHEET)
                .filter(sheet -> sheet.hasHeader(COLUMN_PLOT));
        if (!found.isPresent()) {
            return Collections.emptyList();
        }
        SheetStructure sheet = found.get();
        List<ObjectRow> rows = new ArrayList<>();
        for (int i = 0; i < sheet.getRows().size(); i++) {
            List<String> row = sheet.getRows().get(i);
            String name = sheet.cell(row, COLUMN_PLOT);
            if (!name.isEmpty()) {
                rows.add(new ObjectRow(sheet.getName(), i + 2, name)
                        .setGermplasm(sheet.cell(row, COLUMN_GENOTYPE))
                        .setFactorLevel(sheet.cell(row, COLUMN_STATUS))
                        .setCells(sheet.getHeaders(), row));
            }
        }
        return rows;
    }

    /**
     * The cartouche names each plot, its genotype and the treatment it receives; the other columns
     * start unmapped.
     */
    @Override
    public ObjectSheetDefaults objectSheetDefaults(WorkbookStructure structure, String sheetName) {
        if (!CARTOUCHE_SHEET.equalsIgnoreCase(sheetName)) {
            return ObjectSheetDefaults.none();
        }
        Map<String, String> targets = new LinkedHashMap<>();
        targets.put(COLUMN_PLOT, ObjectTargets.NAME);
        targets.put(COLUMN_GENOTYPE, ObjectTargets.GERMPLASM);
        targets.put(COLUMN_STATUS, ObjectTargets.FACTOR_LEVEL);
        return new ObjectSheetDefaults(COLUMN_PLOT, targets, null);
    }

    @Override
    public List<DataPoint> extractDataPoints(WorkbookStructure structure) {
        List<DataPoint> points = new ArrayList<>();

        for (SheetStructure sheet : structure.getSheets()) {
            if (!sheet.isTabular() || isFixedSheet(sheet.getName())) {
                continue;
            }
            int plotColumn = sheet.indexOfHeader(COLUMN_PLOT);
            String dateColumn = dateColumnOf(sheet);
            if (plotColumn < 0 || dateColumn == null) {
                // Without a plot and a date, a row cannot be attached to anything. The template
                // states both are mandatory, so a sheet missing either is left out rather than
                // guessed at; readStageSheets already reported the sheet's columns.
                continue;
            }
            int observerColumn = sheet.indexOfHeader(COLUMN_OBSERVER);

            // The generic 'Date' column is already excluded as a cartouche column. A stage
            // specific one such as Deb_Date is deliberately kept: in a phenology sheet the date of
            // the stage is the observation, so that column is both the date and the value.
            List<String> variableColumns = new ArrayList<>();
            for (String header : sheet.getHeaders()) {
                if (!header.isEmpty() && !isNonVariableColumn(header)) {
                    variableColumns.add(header);
                }
            }

            collectRows(sheet, points, plotColumn, dateColumn, observerColumn, variableColumns,
                    structure.isDate1904());
        }
        return points;
    }

    private void collectRows(SheetStructure sheet, List<DataPoint> points, int plotColumn,
                             String dateColumn, int observerColumn, List<String> variableColumns,
                             boolean date1904) {
        for (int i = 0; i < sheet.getRows().size(); i++) {
            List<String> row = sheet.getRows().get(i);
            // The header is row 1 in the spreadsheet, so the first data row is row 2.
            int rowNumber = i + 2;

            String plot = plotColumn < row.size() ? row.get(plotColumn) : "";
            if (plot.isEmpty()) {
                continue;
            }
            LocalDate date = asDate(dateColumn, sheet.cell(row, dateColumn), date1904);
            if (date == null) {
                // A row with no readable date carries no observation date, and the template makes
                // it mandatory. Skipped here; the report already flags what the file is missing.
                continue;
            }
            String observer = observerColumn >= 0 && observerColumn < row.size()
                    ? row.get(observerColumn)
                    : null;

            for (String column : variableColumns) {
                String value = sheet.cell(row, column);
                if (ExcelValueParser.isMissing(value)) {
                    continue;
                }
                points.add(new DataPoint()
                        .setSheet(sheet.getName())
                        .setRowNumber(rowNumber)
                        .setObjectName(plot)
                        .setDate(date)
                        .setVariableKey(column)
                        .setRawValue(value)
                        .setObserver(observer));
            }
        }
    }

    /**
     * A stage sheet dates its observations either in a plain {@code Date} column or in a
     * stage-specific one such as {@code Deb_Date}, which is both the date and the observation.
     */
    private String dateColumnOf(SheetStructure sheet) {
        if (sheet.hasHeader(COLUMN_DATE)) {
            return COLUMN_DATE;
        }
        for (String header : sheet.getHeaders()) {
            if (ExcelValueParser.looksLikeDateColumn(header)) {
                return header;
            }
        }
        return null;
    }

    /**
     * Reads the {@code Chronologie} catalogue into a map keyed by the abbreviation used as a column
     * header in the stage sheets. Header names carry a version suffix that changes between template
     * revisions, so columns are located by the words they contain rather than by an exact name.
     */
    private Map<String, VariableCandidate> readCatalogue(WorkbookStructure structure, ExtractedImportPlan plan) {
        Map<String, VariableCandidate> catalogue = new LinkedHashMap<>();
        Optional<SheetStructure> found = structure.getSheet(CATALOGUE_SHEET);
        if (!found.isPresent()) {
            plan.addAnomaly(ReportMessage.of(ANOMALY + "catalogueSheetMissing",
                            "The '" + CATALOGUE_SHEET + "' sheet is missing, so variable columns "
                                    + "cannot be resolved to their catalogue label or CropOntology "
                                    + "identifier.")
                    .with("sheet", CATALOGUE_SHEET));
            return catalogue;
        }
        SheetStructure sheet = found.get();

        String abbreviationColumn = HeaderMatcher.find(sheet.getHeaders(), "abreviation");
        String nameColumn = HeaderMatcher.find(sheet.getHeaders(), "nom", "variable");
        String labelColumn = HeaderMatcher.find(sheet.getHeaders(), "libelle");
        String ontologyColumn = HeaderMatcher.find(sheet.getHeaders(), "cropontology");

        if (abbreviationColumn == null) {
            plan.addAnomaly(ReportMessage.of(ANOMALY + "abbreviationColumnMissing",
                            "No abbreviation column found in '" + CATALOGUE_SHEET
                                    + "', so stage sheet columns cannot be linked to the catalogue.")
                    .with("sheet", CATALOGUE_SHEET));
            return catalogue;
        }

        for (List<String> row : sheet.getRows()) {
            String abbreviation = sheet.cell(row, abbreviationColumn);
            if (abbreviation.isEmpty()) {
                continue;
            }
            String name = nameColumn == null ? "" : sheet.cell(row, nameColumn);
            String label = labelColumn == null ? "" : sheet.cell(row, labelColumn);
            String ontologyId = ontologyColumn == null ? "" : sheet.cell(row, ontologyColumn);

            VariableCandidate candidate = new VariableCandidate(abbreviation)
                    .setLabel(!name.isEmpty() ? name : label);
            if (!ontologyId.isEmpty()) {
                candidate.setExternalId(ontologyId);
            }
            catalogue.put(abbreviation.toLowerCase(), candidate);
        }

        plan.note("catalogue entries", String.valueOf(catalogue.size()));
        return catalogue;
    }

    private void readCartouche(WorkbookStructure structure, ExtractedImportPlan plan) {
        Optional<SheetStructure> found = structure.getSheet(CARTOUCHE_SHEET);
        if (!found.isPresent()) {
            plan.addAnomaly(ReportMessage.of(ANOMALY + "cartoucheSheetMissing",
                            "The '" + CARTOUCHE_SHEET + "' sheet is missing. The template states it "
                                    + "must be present and fully filled, since every stage sheet "
                                    + "repeats it.")
                    .with("sheet", CARTOUCHE_SHEET));
            return;
        }
        SheetStructure sheet = found.get();

        plan.getExperimentNames().addAll(sheet.distinctValues(COLUMN_TRIAL));
        plan.getGermplasmNames().addAll(sheet.distinctValues(COLUMN_GENOTYPE));
        plan.getScientificObjectNames().addAll(sheet.distinctValues(COLUMN_PLOT));

        List<String> statuses = sheet.distinctValues(COLUMN_STATUS);
        if (!statuses.isEmpty()) {
            plan.note("plot statuses", String.join(", ", statuses));
        }
        plan.note("unit plots", String.valueOf(sheet.getDataRowCount()));

        checkPlotUniqueness(sheet, plan);
        checkCartoucheCompleteness(sheet, plan);
    }

    private void readStageSheets(WorkbookStructure structure, ExtractedImportPlan plan,
                                 Map<String, VariableCandidate> catalogue) {
        List<String> unknownColumns = new ArrayList<>();

        for (SheetStructure sheet : structure.getSheets()) {
            if (!sheet.isTabular() || isFixedSheet(sheet.getName())) {
                continue;
            }
            plan.getObserverNames().addAll(missingFrom(plan.getObserverNames(),
                    sheet.distinctValues(COLUMN_OBSERVER)));

            for (String header : sheet.getHeaders()) {
                if (header.isEmpty() || isNonVariableColumn(header)) {
                    continue;
                }
                VariableCandidate catalogued = catalogue.get(header.toLowerCase());
                if (catalogued == null) {
                    if (!unknownColumns.contains(header)) {
                        unknownColumns.add(header);
                    }
                    candidate(plan, header).addSheet(sheet.getName());
                } else {
                    candidate(plan, header)
                            .setLabel(catalogued.getLabel())
                            .setExternalId(catalogued.getExternalId())
                            .addSheet(sheet.getName());
                }
            }
        }

        if (!unknownColumns.isEmpty()) {
            plan.addAnomaly(ReportMessage.of(ANOMALY + "columnsNotInCatalogue",
                            "These columns are not listed in '" + CATALOGUE_SHEET + "': "
                                    + String.join(", ", unknownColumns)
                                    + ". Ask the user what they measure and in which unit.")
                    .with("sheet", CATALOGUE_SHEET)
                    .with("columns", String.join(", ", unknownColumns)));
        }
    }

    /**
     * The template's instructions require the 1900 date system. A workbook saved in the 1904 system
     * reads every serial four years later, which is exactly the silent error the instructions warn
     * about, so it is worth saying out loud even when the dates happen to look right.
     */
    private void checkDateSystem(WorkbookStructure structure, ExtractedImportPlan plan) {
        if (!structure.isDate1904()) {
            plan.note("date system", "1900, as the template requires");
            return;
        }
        plan.note("date system", "1904, but the template requires 1900");
        plan.addAnomaly(ReportMessage.of(ANOMALY + "date1904",
                "This workbook uses the 1904 date system, while the template's instructions "
                        + "require 1900. The dates were read with the workbook's own system, so "
                        + "they are correct as shown, but confirm with the user that the years are "
                        + "the intended ones."));
    }

    /**
     * Checks the three places a year appears against each other.
     * <p>
     * The template states the cartouche is used as-is by every stage sheet, and its instructions
     * warn that a spreadsheet can silently carry the wrong calendar system. So the season declared
     * in the cartouche, the season repeated in each stage sheet, and the observation dates should
     * all agree. When they do not, importing anything would file the data under the wrong year.
     */
    private void checkSeasons(WorkbookStructure structure, ExtractedImportPlan plan) {
        TreeSet<Integer> allSeasons = new TreeSet<>();
        TreeSet<Integer> observedYears = new TreeSet<>();
        TreeSet<Integer> cartoucheSeasons = new TreeSet<>();
        Map<String, TreeSet<Integer>> seasonsBySheet = new LinkedHashMap<>();

        for (SheetStructure sheet : structure.getSheets()) {
            if (!sheet.isTabular()) {
                continue;
            }
            TreeSet<Integer> seasons = seasonsOf(sheet);
            if (!seasons.isEmpty()) {
                allSeasons.addAll(seasons);
                if (sheet.getName().equalsIgnoreCase(CARTOUCHE_SHEET)) {
                    cartoucheSeasons.addAll(seasons);
                } else {
                    seasonsBySheet.put(sheet.getName(), seasons);
                }
            }
            observedYears.addAll(observedYearsOf(sheet, structure.isDate1904()));
        }

        if (!allSeasons.isEmpty()) {
            plan.note("declared seasons", join(allSeasons));
        }
        if (!cartoucheSeasons.isEmpty()) {
            plan.note("cartouche season", join(cartoucheSeasons));
        }
        if (!observedYears.isEmpty()) {
            plan.note("observation years", join(observedYears));
        }

        reportSeasonDisagreeingWithCartouche(plan, cartoucheSeasons, seasonsBySheet);
        reportDatesDisagreeingWithSeason(plan, allSeasons, observedYears);
    }

    private void reportSeasonDisagreeingWithCartouche(ExtractedImportPlan plan,
                                                      TreeSet<Integer> cartoucheSeasons,
                                                      Map<String, TreeSet<Integer>> seasonsBySheet) {
        if (cartoucheSeasons.isEmpty()) {
            return;
        }
        List<String> disagreeing = new ArrayList<>();
        for (Map.Entry<String, TreeSet<Integer>> entry : seasonsBySheet.entrySet()) {
            if (!cartoucheSeasons.containsAll(entry.getValue())) {
                disagreeing.add(entry.getKey() + " (" + join(entry.getValue()) + ")");
            }
        }
        if (!disagreeing.isEmpty()) {
            plan.addAnomaly(ReportMessage.of(ANOMALY + "seasonDisagreement",
                            "The '" + CARTOUCHE_SHEET + "' sheet declares the season "
                                    + join(cartoucheSeasons) + ", but these sheets declare another "
                                    + "one: " + String.join(", ", disagreeing)
                                    + ". The template states the cartouche is used as-is by every "
                                    + "sheet, so ask the user which season the data belongs to.")
                    .with("sheet", CARTOUCHE_SHEET)
                    .with("declared", join(cartoucheSeasons))
                    .with("others", String.join(", ", disagreeing)));
        }
    }

    private void reportDatesDisagreeingWithSeason(ExtractedImportPlan plan,
                                                  TreeSet<Integer> declaredSeasons,
                                                  TreeSet<Integer> observedYears) {
        if (declaredSeasons.isEmpty() || observedYears.isEmpty()) {
            return;
        }
        TreeSet<Integer> unexplained = new TreeSet<>(observedYears);
        // An observation may legitimately fall in the calendar year following the season.
        for (Integer season : declaredSeasons) {
            unexplained.remove(season);
            unexplained.remove(season + 1);
        }
        if (!unexplained.isEmpty()) {
            plan.addAnomaly(ReportMessage.of(ANOMALY + "seasonVersusDates",
                            "The declared season is " + join(declaredSeasons)
                                    + " but observation dates fall in " + join(observedYears)
                                    + ". The template warns about the spreadsheet date system: ask "
                                    + "the user which year is right before importing anything.")
                    .with("season", join(declaredSeasons))
                    .with("years", join(observedYears)));
        }
    }

    private TreeSet<Integer> seasonsOf(SheetStructure sheet) {
        TreeSet<Integer> seasons = new TreeSet<>();
        for (String value : sheet.distinctValues(COLUMN_SEASON)) {
            Double season = ExcelValueParser.parseNumber(value);
            if (season != null) {
                seasons.add(season.intValue());
            }
        }
        return seasons;
    }

    private TreeSet<Integer> observedYearsOf(SheetStructure sheet, boolean date1904) {
        TreeSet<Integer> years = new TreeSet<>();
        for (String header : sheet.getHeaders()) {
            if (!ExcelValueParser.looksLikeDateColumn(header)) {
                continue;
            }
            for (String value : sheet.distinctValues(header)) {
                LocalDate date = asDate(header, value, date1904);
                if (date != null) {
                    years.add(date.getYear());
                }
            }
        }
        return years;
    }

    private void checkPlotUniqueness(SheetStructure sheet, ExtractedImportPlan plan) {
        int columnIndex = sheet.indexOfHeader(COLUMN_PLOT);
        if (columnIndex < 0) {
            plan.addAnomaly(ReportMessage.of(ANOMALY + "plotColumnMissing",
                            "The '" + COLUMN_PLOT + "' column is missing from '" + CARTOUCHE_SHEET
                                    + "'. The template states the unit plot identifier is mandatory "
                                    + "and unique.")
                    .with("column", COLUMN_PLOT)
                    .with("sheet", CARTOUCHE_SHEET));
            return;
        }
        List<String> seen = new ArrayList<>();
        List<String> duplicates = new ArrayList<>();
        for (List<String> row : sheet.getRows()) {
            if (columnIndex >= row.size()) {
                continue;
            }
            String plot = row.get(columnIndex);
            if (plot.isEmpty()) {
                continue;
            }
            if (seen.contains(plot) && !duplicates.contains(plot)) {
                duplicates.add(plot);
            }
            seen.add(plot);
        }
        if (!duplicates.isEmpty()) {
            plan.addAnomaly(ReportMessage.of(ANOMALY + "plotIdentifierRepeats",
                            "The unit plot identifier repeats in '" + CARTOUCHE_SHEET + "': "
                                    + String.join(", ", duplicates)
                                    + ". The template requires it to be unique, so these rows "
                                    + "cannot be told apart.")
                    .with("sheet", CARTOUCHE_SHEET)
                    .with("values", String.join(", ", duplicates)));
        }
    }

    private void checkCartoucheCompleteness(SheetStructure sheet, ExtractedImportPlan plan) {
        List<String> incomplete = new ArrayList<>();
        for (String column : NON_VARIABLE_COLUMNS) {
            if (column.equals(COLUMN_DATE) || column.equals(COLUMN_OBSERVER)
                    || column.equals(COLUMN_FREE_COMMENT)) {
                continue;
            }
            if (!sheet.hasHeader(column)) {
                incomplete.add(column);
            }
        }
        if (!incomplete.isEmpty()) {
            plan.addAnomaly(ReportMessage.of(ANOMALY + "cartoucheIncomplete",
                            "These cartouche columns are absent from '" + CARTOUCHE_SHEET + "': "
                                    + String.join(", ", incomplete)
                                    + ". The template requires the cartouche to be complete.")
                    .with("sheet", CARTOUCHE_SHEET)
                    .with("columns", String.join(", ", incomplete)));
        }
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

    private boolean isFixedSheet(String sheetName) {
        return sheetName.equalsIgnoreCase(CATALOGUE_SHEET)
                || sheetName.equalsIgnoreCase(CARTOUCHE_SHEET)
                || sheetName.equalsIgnoreCase(README_SHEET);
    }

    private boolean isNonVariableColumn(String header) {
        for (String column : NON_VARIABLE_COLUMNS) {
            if (column.equalsIgnoreCase(header)) {
                return true;
            }
        }
        return false;
    }

    private List<String> missingFrom(List<String> existing, List<String> candidates) {
        List<String> result = new ArrayList<>();
        for (String candidate : candidates) {
            if (!existing.contains(candidate) && !result.contains(candidate)) {
                result.add(candidate);
            }
        }
        return result;
    }

    private VariableCandidate candidate(ExtractedImportPlan plan, String columnKey) {
        for (VariableCandidate existing : plan.getVariables()) {
            if (existing.getColumnKey().equalsIgnoreCase(columnKey)) {
                return existing;
            }
        }
        VariableCandidate created = new VariableCandidate(columnKey);
        plan.getVariables().add(created);
        return created;
    }

    private String join(TreeSet<Integer> values) {
        List<String> asText = new ArrayList<>(values.size());
        values.forEach(value -> asText.add(String.valueOf(value)));
        return String.join(", ", asText);
    }
}
