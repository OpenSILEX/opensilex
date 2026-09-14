//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.profile.star;

import org.opensilex.aiimport.workbook.SheetStructure;
import org.opensilex.aiimport.workbook.WorkbookStructure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

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
     * @return the field sheet, which becomes a facility
     */
    public Optional<SheetStructure> field() {
        return designSheet("parcelle");
    }

    /**
     * @return the plot sheet, which becomes the scientific objects
     */
    public Optional<SheetStructure> plots() {
        return designSheet("placette");
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
