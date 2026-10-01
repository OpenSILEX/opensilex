//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.profile.star;

import org.opensilex.aiimport.workbook.SheetStructure;
import org.opensilex.aiimport.workbook.WorkbookStructure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Finds the sheets of a STAR workbook, by prefix rather than by name.
 * <p>
 * The prefix carries the meaning — {@code ed_} for the experimental design, {@code data_} for
 * observations, {@code dictionary_} for the schema — and it is stable where the names are not: the
 * reference template calls a sheet {@code ed_placette} where an earlier revision called it
 * {@code placette}. Matching on the prefix covers both, and absorbs a future {@code data_} sheet
 * without a code change.
 *
 * @author Arnaud Charleroy
 */
public class StarSheets {

    public static final String EXPERIMENTAL_DESIGN_PREFIX = "ed_";
    public static final String DATA_PREFIX = "data_";
    public static final String DICTIONARY_PREFIX = "dictionary";

    /**
     * The prefix of the sheets describing a facility — the field, in STAR's words. Earlier
     * revisions filed the field among the design sheets as {@code ed_parcelle}, or bare as
     * {@code parcelle}; both are still read.
     */
    public static final String FIELD_PREFIX = "field";

    /**
     * The name the earlier revisions gave the field sheet, with or without the design prefix.
     */
    public static final String LEGACY_FIELD_SHEET = "parcelle";

    /**
     * The plot sheet of the earlier revision, which carried no prefix.
     */
    public static final String LEGACY_PLOT_SHEET = "placette";

    private static final String ID_SUFFIX = "_id";
    private static final String FIELD_ID = "field_id";

    public static final String EXPERIMENT_SHEET = "expe";
    public static final String TREATMENT_SHEET = "modalite";

    /**
     * What happened during the trial: incidents, observation rounds, anything dated.
     */
    public static final String EVENT_SHEET = "evenement";

    /**
     * Plant protection product applications — sprayings, dated and dosed.
     */
    public static final String TREATMENT_APPLICATION_SHEET = "ppp";

    /**
     * An empty sheet whose only purpose is to show the shape a data sheet should take.
     */
    public static final String DATA_TEMPLATE_SHEET = "data_template";

    /**
     * Observation sheets the earlier revision named without the prefix.
     * <p>
     * Only weather so far, and named explicitly rather than guessed: deciding by content which
     * sheet holds observations would sweep in {@code suivi_data} and {@code listes}, which hold
     * neither. Without this, a workbook of the earlier revision loses its weather in silence —
     * the sheet is read, then never looked at.
     */
    private static final List<String> UNPREFIXED_DATA_SHEETS =
            Collections.singletonList("meteo");

    private final WorkbookStructure workbook;

    public StarSheets(WorkbookStructure workbook) {
        this.workbook = workbook;
    }

    public Optional<SheetStructure> experiment() {
        return workbook.getSheet(EXPERIMENT_SHEET);
    }

    public Optional<SheetStructure> treatments() {
        return workbook.getSheet(TREATMENT_SHEET);
    }

    public Optional<SheetStructure> events() {
        return designSheet(EVENT_SHEET);
    }

    public Optional<SheetStructure> treatmentApplications() {
        return designSheet(TREATMENT_APPLICATION_SHEET);
    }

    /**
     * @return the field sheet, which becomes a facility: the first sheet named with the field
     * prefix, or the sheet the earlier revisions used
     */
    public Optional<SheetStructure> field() {
        for (SheetStructure sheet : workbook.getSheets()) {
            if (sheet.isTabular() && sheet.getName().toLowerCase(Locale.ROOT).startsWith(FIELD_PREFIX)) {
                return Optional.of(sheet);
            }
        }
        return designSheet(LEGACY_FIELD_SHEET);
    }

    /**
     * @return the plot sheet: {@code ed_placette} as the template names it, or else the first
     * design sheet identifying its rows by {@code plot_id} — an exported workbook names its design
     * sheets after the type of their objects, {@code ed_plot} for plots
     */
    public Optional<SheetStructure> plots() {
        Optional<SheetStructure> named = designSheet(LEGACY_PLOT_SHEET);
        if (named.isPresent()) {
            return named;
        }
        for (SheetStructure sheet : workbook.getSheets()) {
            String name = sheet.getName().toLowerCase(Locale.ROOT);
            if (sheet.isTabular() && name.startsWith(EXPERIMENTAL_DESIGN_PREFIX)
                    && sheet.hasHeader(PlotIdReconciliation.COLUMN_PLOT)) {
                return Optional.of(sheet);
            }
        }
        return Optional.empty();
    }

    /**
     * @return the sheets listing scientific objects: every design sheet — {@code ed_placette}, or
     * {@code ed_plot} and {@code ed_plant} in an exported workbook, or the earlier revision's bare
     * {@code placette} — except those describing something else (the field, the events, the
     * sprayings) and those whose rows identify nothing
     */
    public List<SheetStructure> objectSheets() {
        Set<String> elsewhere = new HashSet<>();
        field().ifPresent(sheet -> elsewhere.add(sheet.getName()));
        events().ifPresent(sheet -> elsewhere.add(sheet.getName()));
        treatmentApplications().ifPresent(sheet -> elsewhere.add(sheet.getName()));

        List<SheetStructure> sheets = new ArrayList<>();
        for (SheetStructure sheet : workbook.getSheets()) {
            String name = sheet.getName().toLowerCase(Locale.ROOT);
            boolean design = name.startsWith(EXPERIMENTAL_DESIGN_PREFIX) || name.equals(LEGACY_PLOT_SHEET);
            if (design && sheet.isTabular() && !elsewhere.contains(sheet.getName())
                    && identifierColumn(sheet) != null) {
                sheets.add(sheet);
            }
        }
        return sheets;
    }

    /**
     * The column naming the objects of a design sheet: the one named after the sheet —
     * {@code plant_id} in {@code ed_plant} — else {@code plot_id}, else the first identifier that
     * does not name the field the objects stand in.
     *
     * @return the column, or {@code null} when the sheet identifies nothing
     */
    public static String identifierColumn(SheetStructure sheet) {
        String name = sheet.getName().toLowerCase(Locale.ROOT);
        String own = (name.startsWith(EXPERIMENTAL_DESIGN_PREFIX)
                ? name.substring(EXPERIMENTAL_DESIGN_PREFIX.length())
                : name) + ID_SUFFIX;
        String firstOther = null;
        for (String header : sheet.getHeaders()) {
            String lower = header.toLowerCase(Locale.ROOT);
            if (lower.equals(own)) {
                return header;
            }
            if (firstOther == null && lower.endsWith(ID_SUFFIX) && !lower.equals(FIELD_ID)) {
                firstOther = header;
            }
        }
        return sheet.hasHeader(PlotIdReconciliation.COLUMN_PLOT) ? PlotIdReconciliation.COLUMN_PLOT : firstOther;
    }

    /**
     * @return the observation sheets, template excluded — it holds headers and no rows
     */
    public List<SheetStructure> dataSheets() {
        List<SheetStructure> sheets = new ArrayList<>();
        for (SheetStructure sheet : workbook.getSheets()) {
            String name = sheet.getName().toLowerCase(Locale.ROOT);
            boolean named = name.startsWith(DATA_PREFIX) || UNPREFIXED_DATA_SHEETS.contains(name);
            if (sheet.isTabular()
                    && named
                    && !name.equals(DATA_TEMPLATE_SHEET)
                    && sheet.getDataRowCount() > 0) {
                sheets.add(sheet);
            }
        }
        return sheets;
    }

    public boolean hasDictionary() {
        for (SheetStructure sheet : workbook.getSheets()) {
            if (sheet.getName().toLowerCase(Locale.ROOT).startsWith(DICTIONARY_PREFIX)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Looks for a design sheet under both revisions: prefixed, then bare.
     */
    private Optional<SheetStructure> designSheet(String bareName) {
        Optional<SheetStructure> prefixed =
                workbook.getSheet(EXPERIMENTAL_DESIGN_PREFIX + bareName);
        return prefixed.isPresent() ? prefixed : workbook.getSheet(bareName);
    }
}
