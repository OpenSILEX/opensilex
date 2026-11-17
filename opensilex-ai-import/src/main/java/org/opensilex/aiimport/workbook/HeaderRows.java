//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.workbook;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns the rows of a sheet — read from a workbook or from a CSV file alike — into a
 * {@link SheetStructure}: where the header is, what is data, and what is prose.
 * <p>
 * Shared by every reader, so that a table is recognised the same way whatever file it came in.
 *
 * @author Arnaud Charleroy
 */
public final class HeaderRows {

    /**
     * A sheet whose first rows hold at most this many filled columns is treated as prose rather
     * than as a table.
     */
    private static final int MIN_TABULAR_COLUMNS = 2;

    /**
     * Longest a cell may be and still pass for a column header. Headers are labels, even versioned
     * ones such as {@code Nouvelle_Abreviation_FR_maj_03_2023}; the lines of a ReadMe are
     * sentences. Without this bound a paragraph followed by a date-formatted cell reads as a header
     * row, and the instructions the template author wrote are lost.
     */
    private static final int MAX_HEADER_LENGTH = 80;

    private HeaderRows() {
    }

    /**
     * @param rows every row of the sheet, as text, empty cells included; emptied of its trailing
     *             empty rows by this call
     */
    public static SheetStructure sheetOf(String name, int index, List<List<String>> rows) {
        SheetStructure result = new SheetStructure().setName(name).setIndex(index);
        trimTrailingEmptyRows(rows);

        int headerRowIndex = findHeaderRow(rows);
        if (headerRowIndex < 0) {
            // Prose sheet, such as a ReadMe: keep the text so the assistant can read the
            // instructions the author wrote for whoever fills the file.
            return result.setTabular(false).setText(joinAsText(rows));
        }

        result.setHeaders(rows.get(headerRowIndex));
        List<List<String>> dataRows = new ArrayList<>();
        for (int i = headerRowIndex + 1; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            if (!isEmpty(row)) {
                dataRows.add(row);
            }
        }
        return result.setRows(dataRows);
    }

    public static void trimTrailingEmptyCells(List<String> values) {
        while (!values.isEmpty() && values.get(values.size() - 1).isEmpty()) {
            values.remove(values.size() - 1);
        }
    }

    /**
     * The header row is the first row holding at least {@value #MIN_TABULAR_COLUMNS} non-empty
     * cells that all look like labels: short, and not numbers. Field templates put title rows above
     * the header, so scanning rather than assuming row 0 matters.
     */
    private static int findHeaderRow(List<List<String>> rows) {
        for (int i = 0; i < rows.size(); i++) {
            if (isHeaderRow(rows.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isHeaderRow(List<String> row) {
        int filled = 0;
        for (String value : row) {
            if (value.isEmpty()) {
                continue;
            }
            filled++;
            if (value.length() > MAX_HEADER_LENGTH || ExcelValueParser.parseNumber(value) != null) {
                return false;
            }
        }
        return filled >= MIN_TABULAR_COLUMNS;
    }

    private static String joinAsText(List<List<String>> rows) {
        StringBuilder builder = new StringBuilder();
        for (List<String> row : rows) {
            String line = String.join(" ", row).trim();
            if (!line.isEmpty()) {
                builder.append(line).append('\n');
            }
        }
        return builder.toString().trim();
    }

    private static boolean isEmpty(List<String> row) {
        for (String value : row) {
            if (!value.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static void trimTrailingEmptyRows(List<List<String>> rows) {
        while (!rows.isEmpty() && isEmpty(rows.get(rows.size() - 1))) {
            rows.remove(rows.size() - 1);
        }
    }
}
