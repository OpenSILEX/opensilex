//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.profile.miappe;

import org.opensilex.aiimport.workbook.SheetStructure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * One section of a MIAPPE submission, read through the checklist's own conventions.
 * <p>
 * Three of them matter, and none is guesswork — the standard states them:
 * <ul>
 *   <li>a trailing asterisk on a field name marks it mandatory, which is how the checklist says
 *       what a valid submission must carry;</li>
 *   <li>the three rows under the header are documentation, not data: they hold the definition, an
 *       example and the expected format of each field, and a reader that took them for values
 *       would import the standard's own examples;</li>
 *   <li>the Investigation section is written the other way round — one field per row, its value in
 *       the second column — because it describes a single thing.</li>
 * </ul>
 *
 * @author Arnaud Charleroy
 */
public class MiappeSection {

    /**
     * The label in the first column of the rows that document a field rather than fill it. The
     * last one is a banner the template puts above the empty space left for the user.
     */
    private static final Set<String> DOCUMENTATION_ROWS = Collections.unmodifiableSet(
            new LinkedHashSet<>(Arrays.asList(
                    "definition", "example", "format", "values (add rows if necessary)")));

    private final SheetStructure sheet;

    /**
     * True for the Investigation section, laid out one field per row.
     */
    private final boolean transposed;

    public MiappeSection(SheetStructure sheet) {
        this.sheet = sheet;
        this.transposed = sheet.getHeaders().size() >= 2
                && normalise(sheet.getHeaders().get(1)).equals("value");
    }

    public String getName() {
        return sheet.getName();
    }

    public boolean isTransposed() {
        return transposed;
    }

    /**
     * The field names the section declares, asterisks removed.
     */
    public List<String> fields() {
        List<String> fields = new ArrayList<>();
        if (transposed) {
            for (List<String> row : sheet.getRows()) {
                String field = row.isEmpty() ? "" : bare(row.get(0));
                if (!field.isEmpty()) {
                    fields.add(field);
                }
            }
            return fields;
        }
        // The first column holds the row's own label ("Definition", "Example"), not a field.
        for (int i = 1; i < sheet.getHeaders().size(); i++) {
            fields.add(bare(sheet.getHeaders().get(i)));
        }
        return fields;
    }

    /**
     * The fields the checklist marks mandatory with a trailing asterisk.
     */
    public List<String> mandatoryFields() {
        List<String> mandatory = new ArrayList<>();
        if (transposed) {
            for (List<String> row : sheet.getRows()) {
                String raw = row.isEmpty() ? "" : row.get(0);
                if (isMandatory(raw)) {
                    mandatory.add(bare(raw));
                }
            }
            return mandatory;
        }
        for (String header : sheet.getHeaders()) {
            if (isMandatory(header)) {
                mandatory.add(bare(header));
            }
        }
        return mandatory;
    }

    /**
     * The rows the user filled in, documentation excluded.
     */
    public List<List<String>> values() {
        List<List<String>> values = new ArrayList<>();
        if (transposed) {
            // A transposed section holds one thing, so it has at most one row of values, and it is
            // the column rather than a row. Handled by {@link #value(String)}.
            return values;
        }
        for (List<String> row : sheet.getRows()) {
            String label = row.isEmpty() ? "" : normalise(row.get(0));
            if (DOCUMENTATION_ROWS.contains(label)) {
                continue;
            }
            if (row.stream().allMatch(cell -> cell == null || cell.trim().isEmpty())) {
                continue;
            }
            values.add(row);
        }
        return values;
    }

    /**
     * @return the row number the spreadsheet shows for a value row — the header is row 1 — found
     *         by identity, since two rows can hold the same values
     */
    public int rowNumberOf(List<String> valueRow) {
        List<List<String>> rows = sheet.getRows();
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i) == valueRow) {
                return i + 2;
            }
        }
        return 0;
    }

    /**
     * @return the value of a field in a value row, empty when the field is absent or blank
     */
    public String cell(List<String> row, String field) {
        int index = indexOf(field);
        if (index < 0 || index >= row.size()) {
            return "";
        }
        String value = row.get(index);
        return value == null ? "" : value.trim();
    }

    /**
     * @return the value of a field in a transposed section, empty when unfilled
     */
    public String value(String field) {
        if (!transposed) {
            return "";
        }
        for (List<String> row : sheet.getRows()) {
            if (!row.isEmpty() && bare(row.get(0)).equalsIgnoreCase(field)) {
                return row.size() > 1 && row.get(1) != null ? row.get(1).trim() : "";
            }
        }
        return "";
    }

    /**
     * @return how many mandatory fields are left empty across the section's value rows
     */
    public List<String> unfilledMandatoryFields() {
        List<String> unfilled = new ArrayList<>();
        for (String field : mandatoryFields()) {
            if (transposed) {
                if (value(field).isEmpty()) {
                    unfilled.add(field);
                }
                continue;
            }
            boolean filled = values().stream().anyMatch(row -> !cell(row, field).isEmpty());
            if (!filled) {
                unfilled.add(field);
            }
        }
        return unfilled;
    }

    /**
     * The distinct values of one field, in the order the file gives them.
     */
    public List<String> distinct(String field) {
        Set<String> seen = new LinkedHashSet<>();
        for (List<String> row : values()) {
            String value = cell(row, field);
            if (!value.isEmpty()) {
                seen.add(value);
            }
        }
        return new ArrayList<>(seen);
    }

    private int indexOf(String field) {
        List<String> headers = sheet.getHeaders();
        for (int i = 0; i < headers.size(); i++) {
            if (bare(headers.get(i)).equalsIgnoreCase(field)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isMandatory(String header) {
        return header != null && header.trim().endsWith("*");
    }

    /**
     * The field name without the asterisk that marks it mandatory.
     */
    static String bare(String header) {
        if (header == null) {
            return "";
        }
        String trimmed = header.trim();
        while (trimmed.endsWith("*")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
        }
        return trimmed;
    }

    private static String normalise(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
