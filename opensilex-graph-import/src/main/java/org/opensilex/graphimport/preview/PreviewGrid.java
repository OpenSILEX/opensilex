//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.preview;

import org.opensilex.aiimport.workbook.SheetStructure;
import org.opensilex.graphimport.graph.CellRef;
import org.opensilex.graphimport.interpret.ColumnValues;
import org.opensilex.graphimport.validate.GraphProblem;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A sheet as the preview shows it, a page at a time: its rows as written, each cell with what the
 * import would do with it, each row with the resource it is.
 *
 * @author Arnaud Charleroy
 */
public final class PreviewGrid {

    /** How many rows a page holds at most. */
    public static final int MAX_PAGE_SIZE = 100;

    private PreviewGrid() {
    }

    /**
     * A cell, as written, with its state.
     *
     * @param column its column, from 0
     * @param raw    as written
     * @param state  what the import would do with it
     */
    public record Cell(int column, String raw, CellState state) {
    }

    /**
     * A row.
     *
     * @param row   its place among the rows of values, from 0
     * @param node  the resource it is; null for a row of a sheet the graph leaves out
     * @param cells its filled cells
     */
    public record Row(int row, NodeComparison node, List<Cell> cells) {

        public Row {
            cells = List.copyOf(cells);
        }
    }

    /**
     * A page of rows.
     *
     * @param columns the columns of the sheet
     * @param rows    the rows of the page
     * @param total   how many rows the filter keeps, on every page
     */
    public record Page(List<PreviewColumn> columns, List<Row> rows, int total) {

        public Page {
            columns = List.copyOf(columns);
            rows = List.copyOf(rows);
        }
    }

    /**
     * @param state  keeps the rows whose resource is in this state; null keeps all
     * @param toFix  keeps only the rows whose resource the file must be corrected for
     * @param page   from 0
     * @param size   rows per page, at most {@link #MAX_PAGE_SIZE}
     */
    public static Page page(ImportPreview preview, SheetStructure sheet, NodeState state, boolean toFix, int page,
                            int size) {
        List<PreviewColumn> columns = preview.getColumns().getOrDefault(sheet.getName(), List.of());
        List<Integer> kept = new ArrayList<>();
        List<List<String>> rows = sheet.getRows();
        for (int row = 0; row < rows.size(); row++) {
            Optional<NodeComparison> node = nodeOf(preview, sheet.getName(), row);
            boolean filtered = state != null || toFix;
            if (!filtered || node.isPresent() && (state == null || node.get().state() == state)
                    && (!toFix || node.get().toFix())) {
                kept.add(row);
            }
        }
        int pageSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        int from = Math.min(kept.size(), Math.max(0, page) * pageSize);
        List<Row> shown = new ArrayList<>();
        for (int row : kept.subList(from, Math.min(kept.size(), from + pageSize))) {
            shown.add(new Row(row, nodeOf(preview, sheet.getName(), row).orElse(null),
                    cells(preview, sheet.getName(), row, rows.get(row), columns)));
        }
        return new Page(columns, shown, kept.size());
    }

    private static Optional<NodeComparison> nodeOf(ImportPreview preview, String sheet, int row) {
        Integer index = preview.rowsOf(sheet).get(row);
        return index == null ? Optional.empty() : preview.node(index);
    }

    private static List<Cell> cells(ImportPreview preview, String sheet, int row, List<String> raw,
                                    List<PreviewColumn> columns) {
        List<Cell> cells = new ArrayList<>();
        for (PreviewColumn column : columns) {
            String text = ColumnValues.cell(raw, column.index());
            if (text != null) {
                CellRef cell = new CellRef(sheet, row, column.index());
                cells.add(new Cell(column.index(), text, state(preview, cell, column)));
            }
        }
        return cells;
    }

    private static CellState state(ImportPreview preview, CellRef cell, PreviewColumn column) {
        if (column.role() == PreviewColumn.Role.LEFT_OUT) {
            return CellState.IGNORED;
        }
        boolean error = preview.severityAt(cell).filter(GraphProblem.Severity.ERROR::equals).isPresent();
        Optional<ValueComparison> value = preview.valueAt(cell);
        if (error || value.filter(compared -> compared.state() == ValueState.UNUSABLE).isPresent()) {
            return CellState.ERROR;
        }
        if (value.isPresent()) {
            return CellState.valueOf(value.get().state().name());
        }
        return switch (column.role()) {
            case TYPE -> CellState.TYPE;
            case IDENTIFIER -> CellState.IDENTIFIER;
            default -> CellState.IGNORED;
        };
    }
}
