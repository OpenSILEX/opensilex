//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.workbook;

import org.opensilex.aiimport.workbook.HeaderRows;
import org.opensilex.aiimport.workbook.SheetStructure;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Where the header of a sheet is and where its values start, as row positions from 0; the user sees
 * them from 1, as the spreadsheet numbers them.
 *
 * @param headerRow    the row holding the column headers
 * @param firstDataRow the first row holding values; the rows between the header and it are skipped,
 *                     such as a row of units
 * @author Arnaud Charleroy
 */
public record SheetLayout(int headerRow, int firstDataRow) {

    /**
     * @return why this layout does not fit a sheet of {@code rowCount} rows, or empty when it does
     */
    public Optional<String> whyNotFor(int rowCount) {
        if (headerRow < 0 || headerRow >= rowCount) {
            return Optional.of("The header row must be between 1 and " + rowCount + ".");
        }
        if (firstDataRow <= headerRow) {
            return Optional.of("The values must start below the header row.");
        }
        if (firstDataRow >= rowCount) {
            return Optional.of("The values must start at row " + rowCount + " at the latest.");
        }
        return Optional.empty();
    }

    /**
     * The sheet the reader would have made had it found the header at this row: the header row gives
     * the columns, every non-empty row from the first data row on is a row of values.
     *
     * @param detected the sheet as the reader made it, for what the rows do not tell: its index, its
     *                 date system and its charset
     * @param rows     every row of the sheet, as {@link RawRows} reads them
     */
    public SheetStructure applyTo(SheetStructure detected, List<List<String>> rows) {
        List<String> headers = new ArrayList<>(rows.get(headerRow));
        HeaderRows.trimTrailingEmptyCells(headers);
        List<List<String>> values = new ArrayList<>();
        for (int index = firstDataRow; index < rows.size(); index++) {
            List<String> row = rows.get(index);
            if (row.stream().anyMatch(cell -> !cell.isEmpty())) {
                values.add(row);
            }
        }
        return new SheetStructure()
                .setName(detected.getName())
                .setIndex(detected.getIndex())
                .setHeaders(headers)
                .setRows(values)
                .setTabular(true)
                .setDate1904(detected.isDate1904())
                .setCharset(detected.getCharset());
    }

    /**
     * The layout the reader found: its header is the first row equal to the headers it kept, its
     * values start right below.
     *
     * @return empty when the reader saw no table in the sheet
     */
    public static Optional<SheetLayout> detectedIn(SheetStructure detected, List<List<String>> rows) {
        if (!detected.isTabular()) {
            return Optional.empty();
        }
        for (int index = 0; index < rows.size(); index++) {
            if (rows.get(index).equals(detected.getHeaders())) {
                return Optional.of(new SheetLayout(index, index + 1));
            }
        }
        return Optional.empty();
    }
}
