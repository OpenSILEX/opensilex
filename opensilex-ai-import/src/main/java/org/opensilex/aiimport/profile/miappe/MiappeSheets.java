//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.profile.miappe;

import org.opensilex.aiimport.workbook.SheetStructure;
import org.opensilex.aiimport.workbook.WorkbookStructure;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Finds the sections of a MIAPPE submission by name.
 * <p>
 * By name and not by prefix, unlike the STAR template: MIAPPE fixes the section names in the
 * checklist, and a submission that renames them is no longer a MIAPPE submission. The matching
 * still ignores case and spacing, because spreadsheets acquire both.
 *
 * @author Arnaud Charleroy
 */
public class MiappeSheets {

    public static final String INVESTIGATION = "Investigation";
    public static final String STUDY = "Study";
    public static final String PERSON = "Person";
    public static final String DATA_FILE = "Data file";
    public static final String BIOLOGICAL_MATERIAL = "Biological Material";
    public static final String ENVIRONMENT = "Environment";
    public static final String EXPERIMENTAL_FACTOR = "Exp. Factor";
    public static final String EVENT = "Event";
    public static final String OBSERVATION_UNIT = "Observation Unit";
    public static final String SAMPLE = "Sample";
    public static final String OBSERVED_VARIABLE = "Observed Variable";

    /**
     * The eleven sections of the checklist, in the order it lists them.
     */
    public static final List<String> SECTIONS = Arrays.asList(
            INVESTIGATION, STUDY, PERSON, DATA_FILE, BIOLOGICAL_MATERIAL, ENVIRONMENT,
            EXPERIMENTAL_FACTOR, EVENT, OBSERVATION_UNIT, SAMPLE, OBSERVED_VARIABLE);

    /**
     * Reference lists the template ships alongside the sections — the vocabulary to choose from,
     * not anything to import.
     */
    private static final String APPENDIX_PREFIX = "appendix";

    private final WorkbookStructure workbook;

    public MiappeSheets(WorkbookStructure workbook) {
        this.workbook = workbook;
    }

    public Optional<MiappeSection> section(String name) {
        for (SheetStructure sheet : workbook.getSheets()) {
            if (!sheet.isTabular() || isAppendix(sheet)) {
                continue;
            }
            if (key(sheet.getName()).equals(key(name))) {
                return Optional.of(new MiappeSection(sheet));
            }
        }
        return Optional.empty();
    }

    /**
     * @return how many of the checklist's sections this workbook carries
     */
    public int sectionCount() {
        int found = 0;
        for (String name : SECTIONS) {
            if (section(name).isPresent()) {
                found++;
            }
        }
        return found;
    }

    public static boolean isAppendix(SheetStructure sheet) {
        return sheet.getName().toLowerCase(Locale.ROOT).trim().startsWith(APPENDIX_PREFIX);
    }

    private static String key(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
    }
}
