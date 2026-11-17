//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.graph;

import java.util.Comparator;

/**
 * Where a value comes from: a sheet, one of its rows of values, one of its columns.
 * <p>
 * The row is a position among the rows of values, not the spreadsheet's row number: the reader skips
 * empty rows and keeps no numbers. A problem therefore also carries the identifier of the row and the
 * cell as written, so that the user finds it.
 *
 * @param sheet  the sheet, as the import names it
 * @param row    the row among the sheet's rows of values, from 0
 * @param column the column, from 0
 * @author Arnaud Charleroy
 */
public record CellRef(String sheet, int row, int column) {

    /** Sheet, then row, then column: the order the user reads a workbook in. */
    public static final Comparator<CellRef> READING_ORDER = Comparator.comparing(CellRef::sheet)
            .thenComparingInt(CellRef::row)
            .thenComparingInt(CellRef::column);
}
