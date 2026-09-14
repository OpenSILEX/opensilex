//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.profile.star;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.opensilex.aiimport.workbook.SheetStructure;
import org.opensilex.aiimport.workbook.WorkbookStructure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Reads the dictionary sheets of a STAR workbook, which describe every other column in it.
 * <p>
 * <b>Read by content, not by position.</b> The header declares eight columns, but a row carries
 * only the ones it has: empty cells in the middle are omitted rather than left blank, so a row is
 * four, five, six or seven cells wide and the same index means a different field from one line to
 * the next. Reading by index silently mixes the unit, the type and the URI.
 * <p>
 * This is not a defect in any one file — the reference template behaves identically, so it is a
 * property of the format, probably the mark of a generator that drops empty columns. It is
 * therefore handled quietly rather than reported as an anomaly: crying wolf about a format's normal
 * shape costs the credibility of the real warnings.
 * <p>
 * Two revisions exist. The older keeps everything in one {@code dictionary} sheet with an
 * {@code is_variable} flag; the newer splits it into {@code dictionary_variables} and
 * {@code dictionary_metadata}, where membership of a sheet is what says which is which.
 *
 * @author Arnaud Charleroy
 */
public class StarDictionary {

    public static final String MERGED_SHEET = "dictionary";
    public static final String VARIABLES_SHEET = "dictionary_variables";
    public static final String METADATA_SHEET = "dictionary_metadata";

    /**
     * The R classes the format uses. {@code interger} is misspelt in the files themselves and is
     * accepted as written, because refusing it would help nobody.
     */
    private static final List<String> R_CLASSES = Arrays.asList(
            "numeric", "character", "date", "datetime", "integer", "interger", "logical", "factor");

    private static final String MISSPELT_INTEGER = "interger";

    private final Map<String, StarDictionaryEntry> byName = new LinkedHashMap<>();
    private final List<String> variableNames = new ArrayList<>();
    private final List<String> notes = new ArrayList<>();

    public StarDictionary(WorkbookStructure workbook) {
        workbook.getSheet(VARIABLES_SHEET).ifPresent(sheet -> read(sheet, true));
        workbook.getSheet(METADATA_SHEET).ifPresent(sheet -> read(sheet, false));
        // Only when the split revision is absent, so a workbook carrying both is not read twice.
        if (byName.isEmpty()) {
            workbook.getSheet(MERGED_SHEET).ifPresent(sheet -> read(sheet, false));
        }
    }

    /**
     * @return true when the workbook carried a dictionary at all
     */
    public boolean isPresent() {
        return !byName.isEmpty();
    }

    public Optional<StarDictionaryEntry> get(String columnName) {
        return Optional.ofNullable(columnName == null
                ? null
                : byName.get(columnName.toLowerCase(Locale.ROOT)));
    }

    public List<StarDictionaryEntry> getEntries() {
        return new ArrayList<>(byName.values());
    }

    /**
     * @return the columns the dictionary declares as carrying measurements
     */
    public List<StarDictionaryEntry> getVariables() {
        List<StarDictionaryEntry> variables = new ArrayList<>();
        for (String name : variableNames) {
            StarDictionaryEntry entry = byName.get(name);
            if (entry != null) {
                variables.add(entry);
            }
        }
        return variables;
    }

    public boolean isVariable(String columnName) {
        return columnName != null && variableNames.contains(columnName.toLowerCase(Locale.ROOT));
    }

    /**
     * @return remarks worth passing on, such as the misspelt type
     */
    public List<String> getNotes() {
        return notes;
    }

    //#region reading

    private void read(SheetStructure sheet, boolean variablesSheet) {
        for (List<String> row : sheet.getRows()) {
            if (row.isEmpty() || row.get(0).isEmpty()) {
                continue;
            }
            StarDictionaryEntry entry = readRow(row);
            String key = entry.getName().toLowerCase(Locale.ROOT);
            byName.put(key, entry);
            if (entry.isVariable(variablesSheet) && !variableNames.contains(key)) {
                variableNames.add(key);
            }
        }
    }

    /**
     * Classifies each cell by what it looks like, since its position cannot be trusted.
     * <p>
     * The order of the tests matters: a boolean is unmistakable, a known R class is unmistakable,
     * a URI carries a colon. Whatever is left is a unit if the type has not been seen yet — units
     * come before the type in every revision — and the ELOA class otherwise.
     */
    private StarDictionaryEntry readRow(List<String> row) {
        StarDictionaryEntry entry = new StarDictionaryEntry().setName(row.get(0));
        if (row.size() > 1) {
            entry.setDescription(row.get(1));
        }

        for (int i = 2; i < row.size(); i++) {
            String cell = row.get(i);
            if (cell.isEmpty()) {
                continue;
            }
            if (isBoolean(cell)) {
                entry.setVariable(Boolean.parseBoolean(cell.trim()));
            } else if (isRClass(cell)) {
                entry.setRClass(cell.trim());
                noteMisspeltType(entry, cell);
            } else if (looksLikeUri(cell)) {
                entry.setUri(cell.trim());
            } else if (entry.getRClass() == null) {
                entry.setUnit(cell.trim());
            } else {
                entry.setEloaClass(cell.trim());
            }
        }
        return entry;
    }

    private void noteMisspeltType(StarDictionaryEntry entry, String cell) {
        if (!MISSPELT_INTEGER.equalsIgnoreCase(cell.trim())) {
            return;
        }
        String note = "The dictionary spells the integer type '" + MISSPELT_INTEGER
                + "'. It is read as an integer.";
        if (!notes.contains(note)) {
            notes.add(note);
        }
    }

    private boolean isBoolean(String cell) {
        String lower = cell.trim().toLowerCase(Locale.ROOT);
        return lower.equals("true") || lower.equals("false");
    }

    private boolean isRClass(String cell) {
        return R_CLASSES.contains(cell.trim().toLowerCase(Locale.ROOT));
    }

    private boolean looksLikeUri(String cell) {
        String trimmed = cell.trim();
        return trimmed.startsWith("http") || trimmed.contains(":");
    }

    //#endregion

    /**
     * @return the XSD datatype matching an R class, or {@code null} when it says nothing useful
     */
    public static String toDatatype(String rClass) {
        if (rClass == null) {
            return null;
        }
        switch (rClass.trim().toLowerCase(Locale.ROOT)) {
            case "numeric":
                return XSDDatatype.XSDdecimal.getURI();
            case "integer":
            case MISSPELT_INTEGER:
                return XSDDatatype.XSDinteger.getURI();
            case "date":
                return XSDDatatype.XSDdate.getURI();
            case "datetime":
                return XSDDatatype.XSDdateTime.getURI();
            case "logical":
                return XSDDatatype.XSDboolean.getURI();
            case "character":
            case "factor":
                return XSDDatatype.XSDstring.getURI();
            default:
                return null;
        }
    }
}
