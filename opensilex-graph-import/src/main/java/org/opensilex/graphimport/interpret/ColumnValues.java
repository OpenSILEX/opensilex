//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.interpret;

import org.opensilex.aiimport.workbook.ExcelValueParser;
import org.opensilex.aiimport.workbook.SheetStructure;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Reads the cells of one column, the missing ones left out.
 *
 * @author Arnaud Charleroy
 */
public final class ColumnValues {

    /** How many distinct values of a column the evidence looks at: a sheet may have 50 000 rows. */
    public static final int EVIDENCE_SAMPLE = 50;

    private ColumnValues() {
    }

    /** The first distinct values, in row order, at most {@code max}. */
    public static List<String> distinct(SheetStructure sheet, int column, int max) {
        Set<String> values = new LinkedHashSet<>();
        for (List<String> row : sheet.getRows()) {
            String value = cell(row, column);
            if (value != null) {
                values.add(value);
                if (values.size() >= max) {
                    break;
                }
            }
        }
        return new ArrayList<>(values);
    }

    /** Every cell of the column, missing ones as null, one per row. */
    public static List<String> all(SheetStructure sheet, int column) {
        List<String> values = new ArrayList<>(sheet.getRows().size());
        for (List<String> row : sheet.getRows()) {
            values.add(cell(row, column));
        }
        return values;
    }

    /** The cleaned cell, or null when it is missing. */
    public static String cell(List<String> row, int column) {
        if (column >= row.size()) {
            return null;
        }
        String value = ExcelValueParser.clean(row.get(column));
        return ExcelValueParser.isMissing(value) ? null : value;
    }
}
